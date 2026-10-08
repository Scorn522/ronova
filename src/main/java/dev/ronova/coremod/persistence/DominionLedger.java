package dev.ronova.coremod.persistence;

import dev.ronova.coremod.RonovaDominion;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionRequest;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.root.api.JudgmentBatch;
import dev.ronova.coremod.engine.JudgmentScope;
import dev.ronova.coremod.engine.ExecutionTargetPolicy;
import dev.ronova.coremod.engine.JudgmentMark;
import dev.ronova.coremod.engine.LineageIndex;
import dev.ronova.coremod.engine.LineageRecord;
import dev.ronova.coremod.terminal.LifecycleIntegrationReceipt;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-owned durable identity state. Reads occur only on entity admission or
 * explicit judgment, never as a full-world per-tick scan.
 */
public final class DominionLedger extends SavedData {
    private static final String DATA_NAME = RonovaDominion.MOD_ID + "_ledger";
    private static final String REALM_ID_MIGRATED_TAG = "realm_id_migrated";
    /**
     * Tombstones are logical durable facts. 7.16 deliberately has no semantic
     * FIFO eviction; physical representation may be compacted only with an
     * equivalent index.
     */
    private static final int MAX_NBT_ROWS = 1_000_000;
    private static final Map<MinecraftServer, DominionLedger> SERVER_CACHE =
        Collections.synchronizedMap(new WeakHashMap<>());

    private final RealmId realmId;
    private Set<UUID> owners = new LinkedHashSet<>();
    private LineageIndex<Tombstone> tombstones = new LineageIndex<>();
    private LineageIndex<ClassLineage> classLineages = new LineageIndex<>();
    private Map<UUID, LifecycleIntegrationReceipt> lifecycleReceipts =
        new LinkedHashMap<>();
    private RootCommandJournal commandJournal = new RootCommandJournal();
    private boolean realmIdMigrated;
    private boolean mirrorOnly;
    private long generation = DominionEpoch.LOCAL_INITIAL.generation();
    private long sequence;

    public DominionLedger() {
        this(RealmId.random());
        // A newly allocated realm is durable identity, even before the first
        // owner or judgment mutation. Force SavedData to write it on the
        // world's first save so the shadow cannot outlive an unpersisted realm.
        setDirty();
    }

    public DominionLedger(RealmId realmId) {
        this.realmId = Objects.requireNonNull(realmId, "realmId");
    }

    public RealmId realmId() {
        return realmId;
    }

    public boolean realmIdMigrated() {
        return realmIdMigrated;
    }

    boolean isDurableMirror() { return mirrorOnly; }

    public static DominionLedger get(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        synchronized (SERVER_CACHE) {
            return SERVER_CACHE.computeIfAbsent(server, current -> {
                ServerLevel overworld = current.overworld();
                return overworld.getDataStorage().computeIfAbsent(
                    DominionLedger::load, DominionLedger::new, DATA_NAME
                );
            });
        }
    }

    public static void release(MinecraftServer server) {
        SERVER_CACHE.remove(server);
    }

    static void install(MinecraftServer server, DominionLedger ledger, boolean persistMirror) {
        SERVER_CACHE.put(server, ledger);
        if (persistMirror) server.overworld().getDataStorage().set(DATA_NAME, ledger);
    }

    public static DominionLedger load(CompoundTag root) {
        if (root == null) {
            return new DominionLedger();
        }
        boolean migrationMarker = readRealmIdMigratedMarker(root);
        RealmLoad realm = readRealm(root, migrationMarker);
        DominionLedger ledger = new DominionLedger(realm.realmId());
        ledger.mirrorOnly = root.getBoolean(SavedDataMirrorFiles.MIRROR_ONLY);
        ledger.realmIdMigrated = realm.migrated() || migrationMarker;
        if (realm.migrated()) {
            ledger.setDirty();
        }
        ledger.generation = Math.max(
            DominionEpoch.LOCAL_INITIAL.generation(), root.getLong("epoch_generation")
        );
        ledger.sequence = Math.max(0L, root.getLong("sequence"));

        ListTag ownerTags = root.getList("owners_v2", Tag.TAG_COMPOUND);
        for (Tag tag : ownerTags) {
            CompoundTag row = (CompoundTag) tag;
            if (row.hasUUID("uuid")) {
                ledger.owners.add(row.getUUID("uuid"));
            }
        }

        ListTag tombstoneTags = root.getList("tombstones", Tag.TAG_COMPOUND);
        for (Tag raw : tombstoneTags) {
            Tombstone tombstone = Tombstone.load((CompoundTag) raw);
            if (tombstone != null) {
                ledger.tombstones.put(tombstone);
                ledger.indexClassLineage(tombstone);
            }
        }
        ListTag receiptTags = root.getList("lifecycle_receipts_v1", Tag.TAG_COMPOUND);
        Set<UUID> conflictingReceipts = new LinkedHashSet<>();
        if (receiptTags.size() > MAX_NBT_ROWS) {
            throw new IllegalArgumentException("ledger receipt row limit exceeded");
        }
        for (Tag raw : receiptTags) {
            LifecycleIntegrationReceipt receipt =
                LifecycleIntegrationReceipt.fromTag((CompoundTag) raw);
            if (receipt == null || conflictingReceipts.contains(receipt.judgmentId())
                || receipt.integratedEpoch().compareTo(ledger.epoch()) > 0) {
                continue;
            }
            LifecycleIntegrationReceipt previous = ledger.lifecycleReceipts.put(
                receipt.judgmentId(), receipt
            );
            if (previous != null && !previous.equals(receipt)) {
                ledger.lifecycleReceipts.remove(receipt.judgmentId());
                conflictingReceipts.add(receipt.judgmentId());
            }
        }
        ledger.removeOrphanedLifecycleReceipts();
        ledger.commandJournal.restore(root, ledger.snapshot());
        return ledger;
    }

    public synchronized boolean isOwner(UUID uuid) {
        return owners.contains(uuid);
    }

    public synchronized boolean bindOwner(UUID uuid) {
        if (owners.add(Objects.requireNonNull(uuid, "uuid"))) {
            advanceSequence();
            setDirty();
            return true;
        }
        return false;
    }

    public boolean unbindOwner(UUID uuid) {
        if (owners.remove(uuid)) {
            advanceSequence();
            setDirty();
            return true;
        }
        return false;
    }

    public Tombstone record(Entity entity, JudgmentScope scope, double radius) {
        return record(entity, new DominionDecision.Judgment(
            subject(entity), nextDecisionEpoch(), true, toContractScope(scope),
            Math.max(0.0D, radius), "legacy-commit"
        ));
    }

    /** Commits an already-decided immutable Judgment into the local durable store. */
    public Tombstone record(Entity entity, DominionDecision.Judgment decision) {
        return record(subject(entity), decision);
    }

    /**
     * Atomically records an action-level batch. Every entry must target the
     * single next epoch; the epoch is advanced once for the whole batch.
     */
    public synchronized List<Tombstone> recordBatch(JudgmentBatch batch) {
        if (batch == null) {
            throw new IllegalArgumentException("judgment batch is required");
        }
        if (batch.empty()) {
            return List.of();
        }
        DominionEpoch expectedEpoch = nextDecisionEpoch();
        if (!expectedEpoch.equals(batch.epoch())) {
            throw new IllegalStateException(
                "judgment batch epoch mismatch: expected " + expectedEpoch
                    + ", received " + batch.epoch()
            );
        }

        // Build every row before changing the clock or either lineage index. A malformed
        // row therefore rejects the whole action without leaving a partial transaction.
        List<TombstoneSpec> staged = new ArrayList<>(batch.size());
        for (DominionDecision.Judgment decision : batch.judgments()) {
            SubjectIdentity subject = decision.subject();
            if (subject.uuid() == null) {
                throw new IllegalArgumentException("committed judgment requires a UUID");
            }
            JudgmentScope scope = toEngineScope(decision.scope());
            staged.add(new TombstoneSpec(
                subject.uuid(), subject.registryType(), subject.runtimeClass(), subject.dimension(),
                subject.x(), subject.y(), subject.z(),
                scope == JudgmentScope.IDENTITY ? Math.max(0.0D, decision.radius()) : 0.0D,
                scope, subject.player()
            ));
        }

        DominionEpoch committedEpoch = advanceSequence();
        List<Tombstone> result = new ArrayList<>(staged.size());
        for (TombstoneSpec spec : staged) {
            Tombstone tombstone = new Tombstone(
                spec.uuid(), spec.type(), spec.className(), spec.dimension(),
                spec.x(), spec.y(), spec.z(), spec.radius(), spec.scope(),
                committedEpoch.sequence(), spec.player()
            );
            Tombstone previous = tombstones.put(tombstone);
            if (previous != null) {
                lifecycleReceipts.remove(spec.uuid());
            }
            indexClassLineage(tombstone);
            result.add(tombstone);
        }
        setDirty();
        return List.copyOf(result);
    }

    /** Entity-free durable commit used by the loader-neutral Root command bus. */
    public Tombstone record(SubjectIdentity subject, DominionDecision.Judgment decision) {
        if (subject == null || decision == null || !decision.committed()) {
            throw new IllegalArgumentException("committed judgment is required");
        }
        if (!subject.equals(decision.subject())) {
            throw new IllegalArgumentException("judgment subject mismatch");
        }
        if (subject.uuid() == null) {
            throw new IllegalArgumentException("committed judgment requires a UUID");
        }
        DominionEpoch expectedEpoch = nextDecisionEpoch();
        if (!expectedEpoch.equals(decision.epoch())) {
            throw new IllegalStateException(
                "judgment epoch mismatch: expected " + expectedEpoch
                    + ", received " + decision.epoch()
            );
        }
        DominionEpoch committedEpoch = advanceSequence();
        JudgmentScope scope = toEngineScope(decision.scope());
        double radius = scope == JudgmentScope.IDENTITY ? decision.radius() : 0.0D;
        Tombstone tombstone = new Tombstone(
            subject.uuid(), subject.registryType(), subject.runtimeClass(), subject.dimension(),
            subject.x(), subject.y(), subject.z(),
            Math.max(0.0D, radius), scope, committedEpoch.sequence(), subject.player()
        );
        Tombstone previous = tombstones.put(tombstone);
        if (previous != null) {
            lifecycleReceipts.remove(subject.uuid());
        }
        indexClassLineage(tombstone);
        setDirty();
        return tombstone;
    }

    public boolean matches(Entity entity) {
        // Old versions could persist dropped-item identities. Never let those
        // records suppress legitimate loot after upgrading.
        if (!ExecutionTargetPolicy.isCombatObject(entity)) {
            return false;
        }
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String dimension = entity.level().dimension().location().toString();
        return matchesSpawn(
            entity.getUUID(), type, entity.getClass().getName(), dimension,
            entity.getX(), entity.getY(), entity.getZ()
        );
    }

    /** Raw admission lookup used before an entity object is constructed. */
    public boolean matchesSpawn(
        UUID uuid,
        String type,
        String className,
        String dimension,
        double x,
        double y,
        double z
    ) {
        if (tombstones.matches(uuid, type, dimension, x, y, z)) {
            return true;
        }
        return className != null && !className.isBlank()
            && classLineages.matches(uuid, className, dimension, x, y, z);
    }

    public int tombstoneCount() {
        return tombstones.size();
    }

    public boolean hasTombstone(UUID uuid) {
        return uuid != null && tombstones.contains(uuid);
    }

    /** Returns the exact durable tombstone for runtime watch registration. */
    public Tombstone tombstone(UUID uuid) {
        return uuid == null ? null : tombstones.get(uuid);
    }

    public List<Tombstone> tombstones() {
        return tombstones.values();
    }

    public int ownerCount() {
        return owners.size();
    }

    public DominionEpoch epoch() {
        return new DominionEpoch(generation, sequence);
    }

    public DominionEpoch nextDecisionEpoch() {
        return epoch().nextSequence();
    }

    public long commitIndex() {
        return commandJournal.commitIndex();
    }

    public CommitReceipt recoverCommand(UUID commandId, String payloadHash) {
        return commandJournal.find(commandId, payloadHash);
    }

    public CommitReceipt appendCommand(
        DominionCommand command,
        DominionEpoch before,
        DominionSnapshot committedSnapshot,
        CommitReceipt.Status status
    ) {
        CommitReceipt receipt = commandJournal.append(
            command, before, committedSnapshot, status
        );
        setDirty();
        return receipt;
    }

    /** Immutable contract snapshot assembled strictly from persisted facts. */
    public DominionSnapshot snapshot() {
        return SnapshotAssembler.assemble(epoch(), owners, tombstones.values());
    }

    /**
     * 7.16 downgrade mirror.  This method never decides or advances truth: it
     * copies an already committed DurableRootStore snapshot verbatim so an r51
     * rollback has a coherent recovery image.
     */
    public synchronized void mirrorFromDurableRoot(
        RealmId sourceRealm,
        DominionSnapshot sourceSnapshot,
        Collection<LifecycleIntegrationReceipt> sourceReceipts
    ) {
        mirrorFromDurableRoot(sourceRealm, sourceSnapshot, sourceReceipts, null);
    }

    /**
     * Mirrors Root truth and its replay journal into the old r51 file. The
     * journal copy preserves original command ids if a downgrade follows an
     * acknowledged-lost commit.
     */
    public synchronized void mirrorFromDurableRoot(
        RealmId sourceRealm,
        DominionSnapshot sourceSnapshot,
        Collection<LifecycleIntegrationReceipt> sourceReceipts,
        RootCommandJournal sourceJournal
    ) {
        Objects.requireNonNull(sourceRealm, "sourceRealm");
        Objects.requireNonNull(sourceSnapshot, "sourceSnapshot");
        if (!realmId.equals(sourceRealm)) {
            throw new IllegalArgumentException("legacy mirror realm mismatch");
        }

        LinkedHashSet<UUID> nextOwners = new LinkedHashSet<>();
        LineageIndex<Tombstone> nextTombstones = new LineageIndex<>();
        LineageIndex<ClassLineage> nextClassLineages = new LineageIndex<>();
        for (DominionDecision.Authority authority : sourceSnapshot.authorities()) {
            if (authority.authorized() && authority.subject().uuid() != null) {
                nextOwners.add(authority.subject().uuid());
            }
        }
        for (DominionDecision.Judgment judgment : sourceSnapshot.judgments()) {
            SubjectIdentity subject = judgment.subject();
            if (!judgment.committed() || subject.uuid() == null) {
                continue;
            }
            JudgmentScope scope = toEngineScope(judgment.scope());
            Tombstone tombstone = new Tombstone(
                subject.uuid(), subject.registryType(), subject.runtimeClass(),
                subject.dimension(), subject.x(), subject.y(), subject.z(),
                scope == JudgmentScope.IDENTITY ? Math.max(0.0D, judgment.radius()) : 0.0D,
                scope, judgment.epoch().sequence(), subject.player()
            );
            nextTombstones.put(tombstone);
            if (tombstone.className() != null && !tombstone.className().isBlank()) {
                nextClassLineages.put(ClassLineage.from(tombstone));
            }
        }

        LinkedHashMap<UUID, LifecycleIntegrationReceipt> nextReceipts =
            new LinkedHashMap<>();
        if (sourceReceipts != null) {
            for (LifecycleIntegrationReceipt receipt : sourceReceipts) {
                if (receipt == null || !nextTombstones.contains(receipt.judgmentId())
                    || receipt.integratedEpoch().compareTo(sourceSnapshot.epoch()) > 0) {
                    continue;
                }
                LifecycleIntegrationReceipt previous = nextReceipts.put(
                    receipt.judgmentId(), receipt
                );
                if (previous != null && !previous.equals(receipt)) {
                    throw new IllegalArgumentException(
                        "conflicting lifecycle receipt in durable mirror"
                    );
                }
            }
        }

        owners = nextOwners;
        tombstones = nextTombstones;
        classLineages = nextClassLineages;
        lifecycleReceipts = nextReceipts;
        if (sourceJournal != null) {
            commandJournal.replaceFrom(sourceJournal);
        }
        generation = sourceSnapshot.epoch().generation();
        sequence = sourceSnapshot.epoch().sequence();
        mirrorOnly = true;
        setDirty();
    }

    /** Immutable client projection used for login and resynchronization. */
    public List<JudgmentMark> snapshotMarks() {
        return snapshot().judgments().stream().map(DominionLedger::toMark).toList();
    }

    public List<LifecycleIntegrationReceipt> lifecycleIntegrationReceipts() {
        List<LifecycleIntegrationReceipt> result = new ArrayList<>(lifecycleReceipts.values());
        result.sort(Comparator.comparing(LifecycleIntegrationReceipt::judgmentId));
        return List.copyOf(result);
    }

    public boolean hasLifecycleIntegration(UUID judgmentId) {
        return judgmentId != null && lifecycleReceipts.containsKey(judgmentId);
    }

    public LifecycleIntegrationReceipt lifecycleIntegrationReceipt(UUID judgmentId) {
        return judgmentId == null ? null : lifecycleReceipts.get(judgmentId);
    }

    /** Claims the one-time lifecycle integration slot for an existing tombstone. */
    public LifecycleIntegrationReceipt claimLifecycleIntegration(
        UUID judgmentId, String firstPhysicalIdentity
    ) {
        if (judgmentId == null || !tombstones.contains(judgmentId)
            || lifecycleReceipts.containsKey(judgmentId)) {
            return null;
        }
        LifecycleIntegrationReceipt receipt = new LifecycleIntegrationReceipt(
            judgmentId, firstPhysicalIdentity, epoch()
        );
        lifecycleReceipts.put(judgmentId, receipt);
        setDirty();
        return receipt;
    }

    public boolean clearTombstones() {
        if (tombstones.size() == 0 && classLineages.size() == 0
            && lifecycleReceipts.isEmpty()) {
            return false;
        }
        tombstones.clear();
        classLineages.clear();
        lifecycleReceipts.clear();
        advanceSequence();
        setDirty();
        return true;
    }

    public boolean clearTombstone(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        Tombstone removed = tombstones.remove(uuid);
        if (removed != null) {
            classLineages.remove(uuid);
            lifecycleReceipts.remove(uuid);
            advanceSequence();
            setDirty();
            return true;
        }
        return false;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        removeOrphanedLifecycleReceipts();
        root.putString("realm_id", realmId.canonical());
        root.putBoolean(REALM_ID_MIGRATED_TAG, realmIdMigrated);
        root.putBoolean(SavedDataMirrorFiles.MIRROR_ONLY, mirrorOnly);
        root.putLong("epoch_generation", generation);
        root.putLong("sequence", sequence);
        ListTag ownerTags = new ListTag();
        for (UUID owner : owners) {
            CompoundTag row = new CompoundTag();
            row.putUUID("uuid", owner);
            ownerTags.add(row);
        }
        // Use compounds instead of the NBT int-array list to keep the schema extensible.
        root.put("owners_v2", ownerTags);

        ListTag tombstoneTags = new ListTag();
        tombstones.values().forEach(t -> tombstoneTags.add(t.save()));
        root.put("tombstones", tombstoneTags);

        ListTag receiptTags = new ListTag();
        for (LifecycleIntegrationReceipt receipt : lifecycleIntegrationReceipts()) {
            receiptTags.add(receipt.toTag());
        }
        root.put("lifecycle_receipts_v1", receiptTags);
        commandJournal.save(root);
        return root;
    }

    private void indexClassLineage(Tombstone tombstone) {
        if (tombstone != null && tombstone.className() != null
            && !tombstone.className().isBlank()) {
            classLineages.put(ClassLineage.from(tombstone));
        }
    }

    private void removeOrphanedLifecycleReceipts() {
        Set<UUID> retained = new LinkedHashSet<>();
        for (Tombstone tombstone : tombstones.values()) {
            retained.add(tombstone.uuid());
        }
        lifecycleReceipts.keySet().removeIf(uuid -> !retained.contains(uuid));
    }

    private DominionEpoch advanceSequence() {
        DominionEpoch next = epoch().nextSequence();
        generation = next.generation();
        sequence = next.sequence();
        return next;
    }

    private static SubjectIdentity subject(Entity entity) {
        return new SubjectIdentity(
            entity.getUUID(),
            BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
            entity.getClass().getName(), entity.level().dimension().location().toString(),
            finiteOrZero(entity.getX()), finiteOrZero(entity.getY()), finiteOrZero(entity.getZ()),
            entity instanceof net.minecraft.world.entity.player.Player
        );
    }

    private static JudgmentMark toMark(DominionDecision.Judgment decision) {
        SubjectIdentity subject = decision.subject();
        return new JudgmentMark(
            subject.uuid(), subject.registryType(), subject.dimension(),
            subject.x(), subject.y(), subject.z(), decision.radius(),
            toEngineScope(decision.scope())
        );
    }

    private static DominionDecision.LineageScope toContractScope(JudgmentScope scope) {
        return switch (scope) {
            case IDENTITY -> DominionDecision.LineageScope.IDENTITY;
            case LOCAL_LINEAGE -> DominionDecision.LineageScope.LOCAL_LINEAGE;
            case GLOBAL_LINEAGE -> DominionDecision.LineageScope.GLOBAL_LINEAGE;
        };
    }

    private static JudgmentScope toEngineScope(DominionDecision.LineageScope scope) {
        return switch (scope) {
            case IDENTITY -> JudgmentScope.IDENTITY;
            case LOCAL_LINEAGE -> JudgmentScope.LOCAL_LINEAGE;
            case GLOBAL_LINEAGE -> JudgmentScope.GLOBAL_LINEAGE;
        };
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    private static RealmLoad readRealm(CompoundTag root, boolean migrationMarker) {
        if (root == null || !root.contains("realm_id")) {
            if (migrationMarker) {
                throw new RealmIdLoadException("realm_id is missing after migration");
            }
            return new RealmLoad(RealmId.random(), true);
        }
        if (!root.contains("realm_id", Tag.TAG_STRING)) {
            throw new RealmIdLoadException(
                "realm_id has an invalid NBT type: " + root.getTagType("realm_id")
            );
        }
        try {
            return new RealmLoad(RealmId.fromString(root.getString("realm_id")), false);
        } catch (IllegalArgumentException invalid) {
            throw new RealmIdLoadException("realm_id is not a canonical UUID", invalid);
        }
    }

    private static boolean readRealmIdMigratedMarker(CompoundTag root) {
        if (root == null || !root.contains(REALM_ID_MIGRATED_TAG)) {
            return false;
        }
        if (!root.contains(REALM_ID_MIGRATED_TAG, Tag.TAG_BYTE)) {
            throw new RealmIdLoadException(
                "realm_id_migrated has an invalid NBT type: "
                    + root.getTagType(REALM_ID_MIGRATED_TAG)
            );
        }
        return root.getBoolean(REALM_ID_MIGRATED_TAG);
    }

    private record RealmLoad(RealmId realmId, boolean migrated) {}

    private record TombstoneSpec(
        UUID uuid,
        String type,
        String className,
        String dimension,
        double x,
        double y,
        double z,
        double radius,
        JudgmentScope scope,
        boolean player
    ) {}

    private record ClassLineage(
        UUID uuid,
        String type,
        String dimension,
        double x,
        double y,
        double z,
        double radius,
        JudgmentScope scope
    ) implements LineageRecord {
        private static ClassLineage from(Tombstone tombstone) {
            return new ClassLineage(
                tombstone.uuid(), tombstone.className(), tombstone.dimension(),
                tombstone.x(), tombstone.y(), tombstone.z(), tombstone.radius(), tombstone.scope()
            );
        }
    }

    public record Tombstone(
        UUID uuid,
        String type,
        String className,
        String dimension,
        double x,
        double y,
        double z,
        double radius,
        JudgmentScope scope,
        long sequence,
        Boolean playerOverride
    ) implements LineageRecord {
        public Tombstone {
            if (playerOverride != null && playerOverride == (scope == JudgmentScope.IDENTITY)) playerOverride = null;
        }
        public Tombstone(UUID uuid, String type, String className, String dimension, double x, double y, double z,
            double radius, JudgmentScope scope, long sequence) {
            this(uuid, type, className, dimension, x, y, z, radius, scope, sequence, null);
        }
        private JudgmentMark toMark() {
            return new JudgmentMark(uuid, type, dimension, x, y, z, radius, scope);
        }

        DominionDecision.Judgment toDecision(DominionEpoch epoch) {
            SubjectIdentity subject = new SubjectIdentity(
                uuid, type, className, dimension,
                finiteOrZero(x), finiteOrZero(y), finiteOrZero(z),
                playerOverride == null ? scope == JudgmentScope.IDENTITY : playerOverride
            );
            return new DominionDecision.Judgment(
                subject, epoch, true, toContractScope(scope), Math.max(0.0D, radius),
                "persisted-judgment"
            );
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", uuid);
            tag.putString("type", type);
            tag.putString("class", className);
            tag.putString("dimension", dimension);
            tag.putDouble("x", x);
            tag.putDouble("y", y);
            tag.putDouble("z", z);
            tag.putDouble("radius", radius);
            tag.putString("scope", scope.name());
            tag.putLong("sequence", sequence);
            if (playerOverride != null) tag.putBoolean("subject_player", playerOverride);
            return tag;
        }

        private static Tombstone load(CompoundTag tag) {
            if (!tag.hasUUID("uuid") || !tag.contains("type", Tag.TAG_STRING)) {
                return null;
            }
            try {
                if (tag.contains("subject_player") && (!tag.contains("subject_player", Tag.TAG_BYTE)
                    || tag.getByte("subject_player") != 0 && tag.getByte("subject_player") != 1)) return null;
                return new Tombstone(
                    tag.getUUID("uuid"), tag.getString("type"), tag.getString("class"),
                    tag.getString("dimension"), tag.getDouble("x"), tag.getDouble("y"),
                    tag.getDouble("z"), tag.getDouble("radius"),
                    JudgmentScope.valueOf(tag.getString("scope")), tag.getLong("sequence"),
                    tag.contains("subject_player") ? tag.getBoolean("subject_player") : null
                );
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }
}
