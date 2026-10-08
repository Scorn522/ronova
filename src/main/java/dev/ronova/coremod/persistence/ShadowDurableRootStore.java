package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.contract.SubjectKey;
import dev.ronova.coremod.engine.JudgmentScope;
import dev.ronova.coremod.nativebridge.NativeSnapshotCodec;
import dev.ronova.coremod.policy.StrengthPolicyConfiguration;
import dev.ronova.coremod.policy.StrengthPolicySnapshot;
import dev.ronova.coremod.root.api.DurableRootStore;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.root.api.JudgmentBatch;
import dev.ronova.coremod.root.api.SubjectQualification;
import dev.ronova.coremod.root.api.TargetEpoch;
import dev.ronova.coremod.root.api.EffectKey;
import dev.ronova.coremod.root.api.EffectRecord;
import dev.ronova.coremod.root.api.EffectClaim;
import dev.ronova.coremod.root.api.StorageCapacity;
import dev.ronova.coremod.terminal.LifecycleIntegrationReceipt;
import dev.ronova.coremod.terminal.LifecycleClaimResult;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Java durable store introduced as a 7.15 shadow and promoted in place during
 * the 7.16 cutover.  The historical class/data name is retained so existing
 * worlds migrate without copying truth through a second file.
 */
public final class ShadowDurableRootStore extends SavedData implements DurableRootStore {
    private static final String DATA_NAME = "ronova_durable_root_shadow";
    private static final String REALM_ID_MIGRATED_TAG = "realm_id_migrated";
    private static final String AUTHORITATIVE_TAG = "authoritative_7_16";
    private static final int SCHEMA = 14;
    private static final int MAX_NBT_ROWS = 1_000_000;
    private static final Map<MinecraftServer, ShadowDurableRootStore> SERVER_CACHE =
        java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Comparator<UUID> UUID_ORDER = UUID::compareTo;

    private final RealmId realmId;
    private OwnerRegistry ownerRegistry;
    private TombstoneStore tombstoneStore;
    private EpochClock epochClock;
    private LinkedHashMap<UUID, LifecycleIntegrationReceipt> lifecycleReceipts;
    private RootCommandJournal commandJournal;
    private StrengthPolicyConfiguration strengthPolicyConfiguration;
    private StrengthPolicyConfiguration activeStrengthPolicyConfiguration;
    private DominionSnapshot snapshot;
    // The private working image may contain an ambiguous write. Runtime reads
    // stay on this acknowledged head until the same image is durably recovered.
    private ReadHead readHead;
    private Map<SubjectKey, SubjectQualification> qualifications = Map.of();
    private EffectJournal effectJournal = new EffectJournal();
    private final dev.ronova.coremod.root.api.RootExecutionFence executionFence=new dev.ronova.coremod.root.api.RootExecutionFence();
    private dev.ronova.coremod.obligation.ObligationCheckpoint obligationCheckpoint=dev.ronova.coremod.obligation.ObligationCheckpoint.EMPTY;
    private final Map<EffectKey, EffectClaim> effectClaims = new LinkedHashMap<>();
    private final java.util.Set<EffectKey> enteredEffects = new java.util.HashSet<>();
    // Ephemeral one-use provenance for typed write-set receipts. Durable Truth never depends on this cache.
    private final java.util.LinkedHashSet<String> acceptedWriteSetReceipts = new java.util.LinkedHashSet<>();
    // Live observer-issued plans only. Weak scope cache is not persisted authority;
    // reopening a historical memory record cannot reconstruct this permission.
    private final List<java.lang.ref.WeakReference<dev.ronova.coremod.root.api.RecoveryMutationIntent>> liveMemoryScopes = new ArrayList<>();
    private final List<java.lang.ref.WeakReference<dev.ronova.coremod.root.api.RecoveryTaskPlan>> liveTaskScopes = new ArrayList<>();
    private long storageRejections;
    private boolean initialized;
    private boolean realmIdMigrated;
    private String stateHash;
    private String baseStateHash;
    private long mirrorCount;
    private long verificationFailures;
    private boolean readOnly;
    private String readOnlyReason;
    private CrashFailpoint failpoint;
    private RootCommitFailpoint commitFailpoint;
    private boolean persistencePending;
    private boolean authoritative;
    private ShadowVerification lastVerification;
    private transient DurableRootFileBackend durableBackend;
    private transient dev.ronova.coremod.budget.HostResourceGovernor runtimeResources;
    public synchronized void attachRuntimeResources(dev.ronova.coremod.budget.HostResourceGovernor resources) {
        runtimeResources=Objects.requireNonNull(resources);
        if(durableBackend!=null)durableBackend.attachRuntimeResources(resources);
    }
    private transient String persistenceFailure;

    public ShadowDurableRootStore() {
        this(RealmId.random());
    }

    public ShadowDurableRootStore(RealmId realmId) {
        this.realmId = Objects.requireNonNull(realmId, "realmId");
        this.ownerRegistry = new OwnerRegistry();
        this.tombstoneStore = new TombstoneStore();
        this.snapshot = initialSnapshot();
        this.epochClock = new EpochClock(snapshot.epoch());
        this.lifecycleReceipts = new LinkedHashMap<>();
        this.commandJournal = new RootCommandJournal();
        this.strengthPolicyConfiguration = StrengthPolicyConfiguration.defaults();
        this.activeStrengthPolicyConfiguration = strengthPolicyConfiguration;
        this.initialized = false;
        this.baseStateHash = snapshotHash(realmId, snapshot);
        this.stateHash = stateHash(realmId, snapshot, List.of());
        this.readOnlyReason = "";
        this.failpoint = CrashFailpoint.NONE;
        this.commitFailpoint = RootCommitFailpoint.NONE;
        this.persistencePending = false;
        this.authoritative = false;
        this.persistenceFailure = "";
        acknowledgeReadHead();
        this.lastVerification = verification(
            ShadowVerification.Status.MIRRORED, snapshot.epoch(),
            this.stateHash, this.stateHash, "shadow store initialized"
        );
    }

    public ShadowDurableRootStore(
        RealmId realmId,
        DominionSnapshot initialSnapshot,
        Collection<LifecycleIntegrationReceipt> receipts
    ) {
        this(realmId);
        if (initialSnapshot == null) {
            throw new IllegalArgumentException("initial snapshot is required");
        }
        applyState(initialSnapshot, receipts);
        initialized = true;
        persistencePending = false;
    }

    public static ShadowDurableRootStore get(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        synchronized (SERVER_CACHE) {
            return SERVER_CACHE.computeIfAbsent(server, current -> {
                ServerLevel overworld = current.overworld();
                DurableRootFileBackend backend = DurableRootFileBackend.forServer(current);
                return overworld.getDataStorage().computeIfAbsent(
                    root -> loadPreferred(root, null, backend),
                    () -> loadPreferred(null, null, backend),
                    DATA_NAME
                );
            });
        }
    }

    /** Binds a server-side shadow to the authoritative ledger realm. */
    public static ShadowDurableRootStore get(MinecraftServer server, RealmId expectedRealm) {
        if (expectedRealm == null) {
            return get(server);
        }
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        synchronized (SERVER_CACHE) {
            ShadowDurableRootStore store = SERVER_CACHE.get(server);
            if (store == null) {
                ServerLevel overworld = server.overworld();
                DurableRootFileBackend backend = DurableRootFileBackend.forServer(server);
                store = overworld.getDataStorage().computeIfAbsent(
                    root -> loadPreferred(root, expectedRealm, backend),
                    () -> loadPreferred(null, expectedRealm, backend),
                    DATA_NAME
                );
                SERVER_CACHE.put(server, store);
            } else if (store.durableBackend == null) {
                store.attachDurableBackend(DurableRootFileBackend.forServer(server));
            }
            if (!expectedRealm.equals(store.realmId())) {
                store.fenceReadOnly("shadow realm does not match authoritative ledger realm");
            }
            return store;
        }
    }

    public static void release(MinecraftServer server) {
        if (server != null) {
            SERVER_CACHE.remove(server);
        }
    }

    static void install(MinecraftServer server, ShadowDurableRootStore store, boolean persistMirror) {
        SERVER_CACHE.put(server, store);
        if (persistMirror) {
            // The canonical file is already validated; refresh even a missing or stale shadow mirror.
            store.setDirty();
            server.overworld().getDataStorage().set(DATA_NAME, store);
        }
    }

    public static ShadowDurableRootStore load(CompoundTag root) {
        return load(root, null);
    }

    /** Loads a shadow and uses the authoritative realm for a missing legacy field. */
    public static ShadowDurableRootStore load(CompoundTag root, RealmId expectedRealm) {
        boolean migrationMarker = readRealmIdMigratedMarker(root);
        RealmLoad realm = readRealm(root, expectedRealm, migrationMarker);
        ShadowDurableRootStore store = new ShadowDurableRootStore(realm.realmId());
        store.realmIdMigrated = realm.migrated() || migrationMarker;
        store.restore(root);
        if (expectedRealm != null && !expectedRealm.equals(store.realmId())) {
            store.fenceReadOnly("shadow realm does not match authoritative ledger realm");
        }
        if (realm.migrated()) {
            store.setDirty();
        }
        return store;
    }

    private static ShadowDurableRootStore loadPreferred(
        CompoundTag savedDataRoot,
        RealmId expectedRealm,
        DurableRootFileBackend backend
    ) {
        CompoundTag durableRoot = backend.readIfPresent();
        ShadowDurableRootStore store;
        if (durableRoot != null) {
            store = load(durableRoot, expectedRealm);
            if (!store.authoritative) {
                store.fenceReadOnly("durable Root file does not contain the 7.16 writer marker");
            }
        } else {
            if (savedDataRoot != null && savedDataRoot.contains(AUTHORITATIVE_TAG)
                && !savedDataRoot.contains(AUTHORITATIVE_TAG, Tag.TAG_BYTE)) {
                throw new DurableRootPersistenceException(
                    "7.16 writer marker has an invalid SavedData type"
                );
            }
            if (savedDataRoot != null
                && savedDataRoot.getBoolean(AUTHORITATIVE_TAG)) {
                throw new DurableRootPersistenceException(
                    "durable Root file is missing after the 7.16 cutover"
                );
            }
            store = savedDataRoot == null
                ? new ShadowDurableRootStore(
                    expectedRealm == null ? RealmId.random() : expectedRealm
                )
                : load(savedDataRoot, expectedRealm);
        }
        store.attachDurableBackend(backend);
        return store;
    }

    @Override
    public synchronized RealmId realmId() {
        return realmId;
    }

    public synchronized boolean realmIdMigrated() {
        return realmIdMigrated;
    }

    @Override
    public synchronized DominionEpoch epoch() {
        return readHead.snapshot().epoch();
    }

    @Override
    public synchronized DominionSnapshot snapshot() {
        return readHead.snapshot();
    }

    @Override
    public synchronized TargetEpoch targetEpoch(SubjectKey key) {
        SubjectQualification qualification = readHead.qualifications().get(key);
        return qualification == null ? null : qualification.targetEpoch();
    }

    public synchronized SubjectQualification subjectQualification(SubjectKey key) { return readHead.qualifications().get(key); }
    public synchronized dev.ronova.coremod.root.api.AuthorityStamp authorityStamp(UUID subject) {
        SubjectKey key = new SubjectKey(realmId, Objects.requireNonNull(subject, "subject"));
        return new dev.ronova.coremod.root.api.AuthorityStamp(key, readHead.qualifications().get(key),
            readHead.policy().revision().value(), readHead.owners().contains(subject),
            readHead.ownerAuthorizationRevisions().getOrDefault(subject, 0L), authoritative && !readOnly && !persistencePending,
            readHead.snapshot().epoch());
    }

    public synchronized dev.ronova.coremod.root.api.SubjectTransitionBatch prepareTransitions(UUID commandId, UUID actor,
        List<dev.ronova.coremod.root.api.SubjectTransitionBatch.Transition> transitions) {
        var expected = transitions.stream().map(t -> readHead.qualifications().getOrDefault(t.subjectKey(),
            dev.ronova.coremod.root.api.QualificationPreconditions.baseline(t.subjectKey()))).toList();
        return new dev.ronova.coremod.root.api.SubjectTransitionBatch(commandId, actor, readHead.snapshot().epoch(),
            new dev.ronova.coremod.root.api.QualificationPreconditions(realmId, readHead.policy().revision().value(), expected), transitions);
    }

    @Override public synchronized JudgmentBatch qualify(JudgmentBatch batch) {
        Objects.requireNonNull(batch, "batch");
        if (batch.preconditions() != null) return batch;
        var expected = batch.subjects().stream().map(subject -> {
            SubjectKey key = new SubjectKey(realmId, subject.uuid());
            return readHead.qualifications().getOrDefault(key,
                dev.ronova.coremod.root.api.QualificationPreconditions.baseline(key));
        }).toList();
        return batch.withPreconditions(new dev.ronova.coremod.root.api.QualificationPreconditions(
            realmId, readHead.policy().revision().value(), expected));
    }

    @Override
    public synchronized String stateHash() {
        return readHead.stateHash();
    }

    @Override
    public synchronized String baseStateHash() {
        return readHead.baseStateHash();
    }

    @Override
    public synchronized List<LifecycleIntegrationReceipt> lifecycleIntegrationReceipts() {
        return List.copyOf(lifecycleReceipts.values());
    }

    @Override
    public synchronized StrengthPolicyConfiguration strengthPolicyConfiguration() {
        return readHead.policy();
    }

    public synchronized StrengthPolicySnapshot strengthPolicySnapshot() {
        return StrengthPolicySnapshot.fromConfigurations(
            readHead.policy(), activeStrengthPolicyConfiguration
        );
    }

    public synchronized StrengthPolicyConfiguration activeStrengthPolicyConfiguration() {
        return activeStrengthPolicyConfiguration;
    }

    public synchronized OwnerRegistry ownerRegistry() {
        OwnerRegistry copy = new OwnerRegistry();
        copy.replaceAll(readHead.owners().snapshot());
        return copy;
    }

    public synchronized TombstoneStore tombstoneStore() {
        TombstoneStore copy = new TombstoneStore();
        copy.replaceAll(readHead.tombstones().snapshot());
        return copy;
    }

    public synchronized EpochClock epochClock() {
        return new EpochClock(epoch());
    }

    public synchronized int ownerCount() {
        return readHead.owners().size();
    }

    public synchronized int tombstoneCount() {
        return readHead.tombstones().size();
    }

    public synchronized boolean initialized() {
        return initialized;
    }

    public synchronized String readOnlyReason() {
        return readOnlyReason;
    }

    public synchronized ShadowVerification lastVerification() {
        return lastVerification;
    }

    public synchronized boolean persistencePending() {
        return persistencePending;
    }
    /** Retry the exact private pending image; no new command, checkpoint or semantic attempt is created. */
    public synchronized boolean retryPendingPersistence() {
        if (!authoritative || readOnly) return false;
        return !persistencePending || persistAuthoritative();
    }

    public synchronized String persistenceFailure() {
        return persistenceFailure;
    }

    public synchronized boolean durableFilePresent() {
        return durableBackend != null && durableBackend.exists();
    }

    synchronized void attachDurableBackend(DurableRootFileBackend backend) {
        durableBackend = Objects.requireNonNull(backend, "backend");
        if(runtimeResources!=null)durableBackend.attachRuntimeResources(runtimeResources);
    }

    /** True after the irreversible-in-this-runtime 7.16 writer handoff. */
    public synchronized boolean cutoverComplete() {
        return authoritative;
    }

    /**
     * Performs the one-time 7.15 shadow -> 7.16 writer promotion.  Before the
     * marker is durable the legacy ledger is the migration source; afterwards
     * this store is always the source and a ledger mismatch is repaired only in
     * the mirror direction.
     */
    public synchronized ShadowVerification promoteFromLegacy(
        RealmId sourceRealm,
        DominionSnapshot sourceSnapshot,
        Collection<LifecycleIntegrationReceipt> sourceReceipts
    ) {
        if (authoritative) {
            if (readOnly) {
                throw new IllegalStateException("durable root is fenced: " + readOnlyReason);
            }
            persistencePending = persistencePending
                || (durableBackend != null && !durableBackend.exists());
            if (!persistAuthoritative()) {
                throw new DurableRootPersistenceException(
                    "cannot prove the existing 7.16 Root image durable: "
                        + persistenceFailure
                );
            }
            // A post-cutover mirror is not an authority input. Its descriptions,
            // age, absence or damage cannot veto an independently validated Root.
            return remember(verification(ShadowVerification.Status.VERIFIED,
                snapshot.epoch(), stateHash, stateHash, "validated authoritative Root recovered"));
        }
        if (readOnly) {
            throw new IllegalStateException("durable root is fenced: " + readOnlyReason);
        }
        ShadowVerification migrated = mirror(sourceRealm, sourceSnapshot, sourceReceipts);
        if (!migrated.accepted()) {
            throw new IllegalStateException(
                "durable root cutover rejected: " + migrated.status() + ": "
                    + migrated.detail()
            );
        }
        authoritative = true;
        persistencePending = true;
        setDirty();
        if (!persistAuthoritative()) {
            authoritative = false;
            persistencePending = false;
            setDirty();
            throw new DurableRootPersistenceException(
                "7.16 Root cutover was not durably installed: " + persistenceFailure
            );
        }
        return remember(verification(
            ShadowVerification.Status.VERIFIED, snapshot.epoch(), stateHash, stateHash,
            "7.16 durable root writer cutover complete"
        ));
    }

    @Override
    public synchronized boolean authoritative() {
        return authoritative;
    }

    @Override
    public synchronized long commitIndex() {
        return readHead.commitIndex();
    }

    @Override
    public synchronized CommitReceipt recover(UUID commandId, String canonicalPayloadHash) {
        CommitReceipt recovered = commandJournal.find(commandId, canonicalPayloadHash);
        return finishRecovery(recovered);
    }

    @Override
    public synchronized CommitReceipt recover(UUID commandId) {
        return finishRecovery(commandJournal.find(commandId));
    }

    private CommitReceipt finishRecovery(CommitReceipt recovered) {
        if (recovered == null) {
            return null;
        }
        if (!persistAuthoritative()) {
            return CommitReceipt.unknown(
                recovered.command(), epoch(), snapshot(), commitIndex()
            );
        }
        return recovered;
    }

    /** Arms a one-shot authoritative commit ambiguity for recovery tests. */
    public synchronized void armCommitFailpoint(RootCommitFailpoint point) {
        commitFailpoint = Objects.requireNonNull(point, "point");
    }

    public synchronized RootCommitFailpoint commitFailpoint() {
        return commitFailpoint;
    }

    /** Isolated journal image used only to keep the r51 rollback mirror coherent. */
    synchronized RootCommandJournal commandJournalSnapshot() {
        return commandJournal.copy();
    }

    /**
     * The single 7.16 authoritative mutation entry.  State and command journal
     * are staged independently and become visible together while this monitor
     * is held; no caller or projection observes an intermediate epoch.
     */
    @Override
    public synchronized CommitReceipt commit(DominionCommand command) {
        Objects.requireNonNull(command, "command");
        DominionEpoch before = snapshot.epoch();
        CommitReceipt existing = commandJournal.find(
            DominionCommand.commandId(command),
            DominionCommand.canonicalPayloadHash(command)
        );
        if (existing != null) {
            return persistAuthoritative()
                ? existing
                : CommitReceipt.unknown(command, epoch(), snapshot(), commitIndex());
        }
        if (!authoritative || readOnly || executionFence.busy()) {
            return CommitReceipt.rejected(command, epoch(), snapshot(), commitIndex());
        }
        boolean newJudgment = command instanceof DominionCommand.CommitJudgment
            || command instanceof DominionCommand.CommitJudgmentBatch batch && !batch.batch().empty();
        if (newJudgment && !activeStrengthPolicyConfiguration.permitsBaselineAction(
            dev.ronova.coremod.policy.AttackAction.TERMINALIZE)) {
            return CommitReceipt.rejected(command, epoch(), snapshot(), commitIndex());
        }
        if (!persistAuthoritative()) {
            return CommitReceipt.unknown(command, epoch(), snapshot(), commitIndex());
        }
        if (consumeCommitFailpoint(RootCommitFailpoint.BEFORE_APPLY)) {
            return CommitReceipt.unknown(command, before, snapshot, commitIndex());
        }

        if (command instanceof DominionCommand.CommitJudgmentBatch batch
            && batch.batch().preconditions() != null
            && !batch.batch().preconditions().matches(realmId,
                strengthPolicyConfiguration.revision().value(), qualifications)) {
            return CommitReceipt.rejected(command, epoch(), snapshot(), commitIndex());
        }
        if (command instanceof DominionCommand.TransitionSubjects transitions) {
            var request = transitions.batch();
            if (!request.expectedEpoch().equals(epoch()) || !ownerRegistry.contains(request.actor())
                || !request.preconditions().matches(realmId, strengthPolicyConfiguration.revision().value(), qualifications)
                || request.transitions().stream().anyMatch(t -> ownerRegistry.contains(t.subjectKey().subjectId())
                    || t.owner() != null && !ownerRegistry.contains(t.owner().subjectId()))) {
                return CommitReceipt.rejected(command, epoch(), snapshot(), commitIndex());
            }
            try { request.changedQualifications(); }
            catch (IllegalArgumentException | IllegalStateException | ArithmeticException invalid) {
                return CommitReceipt.rejected(command, epoch(), snapshot(), commitIndex());
            }
        }
        StagedState staged = stage(command);
        RootCommandJournal stagedJournal = commandJournal.copy();
        CommitReceipt committed = stagedJournal.append(
            command, before, staged.snapshot(), CommitReceipt.Status.COMMITTED,
            staged.issuedQualifications()
        );

        DurableRootFileBackend.PreparedWrite preparedWrite;
        try {
            CompoundTag image = writeImage(new CompoundTag(), staged.snapshot(), staged.receipts(),
                staged.strengthPolicyConfiguration(), staged.qualifications(), stagedJournal, effectJournal);
            image.putBoolean("persistence_pending", false);
            preparedWrite = preflight(image, effectJournal, !(command instanceof DominionCommand.ClearTombstones
                || command instanceof DominionCommand.ClearTombstone || command instanceof DominionCommand.UnbindOwner));
        } catch (StorageCapacityException | DurableRootPersistenceException blocked) {
            storageRejections++;
            persistenceFailure = normalizedFailure(blocked);
            return CommitReceipt.rejected(command, epoch(), snapshot(), commitIndex());
        }

        // All validation/encoding above completed before the authoritative head moves.
        applyState(staged.snapshot(), staged.receipts());
        strengthPolicyConfiguration = staged.strengthPolicyConfiguration();
        qualifications = staged.qualifications();
        commandJournal.replaceFrom(stagedJournal);
        persistencePending = true;
        setDirty();

        if (!persistAuthoritative(preparedWrite)) {
            return CommitReceipt.unknown(
                command, before, staged.beforeSnapshot(), commitIndex()
            );
        }

        if (consumeCommitFailpoint(RootCommitFailpoint.AFTER_APPLY_BEFORE_ACK)) {
            return CommitReceipt.unknown(command, before, staged.beforeSnapshot(), commitIndex());
        }
        return committed;
    }

    @Override
    public synchronized CommitReceipt commit(JudgmentBatch batch) {
        Objects.requireNonNull(batch, "batch");
        return commit(new DominionCommand.CommitJudgmentBatch(batch));
    }

    public synchronized boolean hasTombstone(UUID uuid) {
        return readHead.tombstones().contains(uuid);
    }

    public synchronized TombstoneStore.Record tombstone(UUID uuid) {
        return readHead.tombstones().get(uuid);
    }

    public synchronized boolean hasLifecycleIntegration(UUID judgmentId) {
        return judgmentId != null && lifecycleReceipts.containsKey(judgmentId);
    }

    /** O(1) diagnostic count; these intents do not prove completed effects. */
    public synchronized int lifecycleClaimCount() {
        return lifecycleReceipts.size();
    }

    public synchronized LifecycleIntegrationReceipt lifecycleIntegrationReceipt(
        UUID judgmentId
    ) {
        return judgmentId == null ? null : lifecycleReceipts.get(judgmentId);
    }

    /** Compatibility adapter: an existing or uncertain claim never grants execution. */
    public synchronized LifecycleIntegrationReceipt claimLifecycleIntegration(
        UUID judgmentId,
        String firstPhysicalIdentity
    ) {
        LifecycleClaimResult result = prepareLifecycleIntegration(judgmentId, firstPhysicalIdentity);
        return result.mayExecute() ? result.receipt() : null;
    }

    /**
     * Persists an intent before entering the inherited opaque death callback.
     * Retain uncertain intents: a failed ACK does not prove the atomic replace
     * failed, and forgetting the claim could grant the same side effect twice.
     */
    public synchronized LifecycleClaimResult prepareLifecycleIntegration(
        UUID judgmentId,
        String firstPhysicalIdentity
    ) {
        if (!authoritative || readOnly || judgmentId == null) {
            return new LifecycleClaimResult(LifecycleClaimResult.Status.REJECTED, null);
        }
        if (!persistAuthoritative()) {
            return new LifecycleClaimResult(LifecycleClaimResult.Status.CLAIM_PERSISTENCE_UNKNOWN,
                lifecycleReceipts.get(judgmentId));
        }
        TargetEpoch epoch = judgmentId == null ? null : targetEpoch(new SubjectKey(realmId, judgmentId));
        if (epoch != null && epoch.generation() > 0L) {
            EffectClaim claim = prepareLifecycleEffect(judgmentId, firstPhysicalIdentity);
            LifecycleIntegrationReceipt legacy = lifecycleReceipts.get(judgmentId);
            if (claim.granted() && enterEffect(claim)) {
                // The boolean compatibility API cannot return a typed handle.
                // Its acknowledged intent remains; discard only the consumed RAM grant.
                effectClaims.remove(claim.record().key());
                enteredEffects.remove(claim.record().key());
                return new LifecycleClaimResult(LifecycleClaimResult.Status.GRANTED, legacy);
            }
            if (claim.status() == EffectClaim.Status.STORAGE_BLOCKED) {
                return new LifecycleClaimResult(LifecycleClaimResult.Status.STORAGE_BLOCKED, null);
            }
            if (claim.status() == EffectClaim.Status.PERSISTENCE_UNKNOWN) {
                return new LifecycleClaimResult(LifecycleClaimResult.Status.CLAIM_PERSISTENCE_UNKNOWN, legacy);
            }
            return new LifecycleClaimResult(legacy == null ? LifecycleClaimResult.Status.REJECTED
                : LifecycleClaimResult.Status.ALREADY_CLAIMED_EFFECT_UNKNOWN, legacy);
        }
        return prepareLegacyLifecycleIntegration(judgmentId, firstPhysicalIdentity);
    }

    private LifecycleClaimResult prepareLegacyLifecycleIntegration(UUID judgmentId, String firstPhysicalIdentity) {
        if (!authoritative || readOnly || judgmentId == null
            || !tombstoneStore.contains(judgmentId) || ownerRegistry.contains(judgmentId)
            || firstPhysicalIdentity != null && firstPhysicalIdentity.length() > 256
            || !activeStrengthPolicyConfiguration.permitsBaselineAction(dev.ronova.coremod.policy.AttackAction.TERMINALIZE)) {
            return new LifecycleClaimResult(LifecycleClaimResult.Status.REJECTED, null);
        }
        if (lifecycleReceipts.containsKey(judgmentId)) {
            return recoverLifecycleIntegration(judgmentId);
        }
        // A staged but unresolved Judgment is not permission to perform an effect.
        if (!persistAuthoritative()) {
            return new LifecycleClaimResult(
                LifecycleClaimResult.Status.CLAIM_PERSISTENCE_UNKNOWN, null);
        }
        LifecycleIntegrationReceipt receipt = new LifecycleIntegrationReceipt(
            judgmentId, firstPhysicalIdentity, snapshot.epoch()
        );
        LinkedHashMap<UUID, LifecycleIntegrationReceipt> nextReceipts = new LinkedHashMap<>(lifecycleReceipts);
        nextReceipts.put(judgmentId, receipt);
        DurableRootFileBackend.PreparedWrite prepared;
        try {
            CompoundTag image = writeImage(new CompoundTag(), snapshot, nextReceipts.values(),
                strengthPolicyConfiguration, qualifications, commandJournal, effectJournal);
            image.putBoolean("persistence_pending", false);
            prepared = preflight(image, effectJournal, true);
        } catch (StorageCapacityException | DurableRootPersistenceException blocked) {
            storageRejections++;
            persistenceFailure = normalizedFailure(blocked);
            return new LifecycleClaimResult(LifecycleClaimResult.Status.STORAGE_BLOCKED, null);
        }
        lifecycleReceipts.put(judgmentId, receipt);
        stateHash = stateHash(realmId, snapshot, lifecycleReceipts.values());
        persistencePending = true;
        setDirty();
        if (!persistAuthoritative(prepared)) {
            return new LifecycleClaimResult(
                LifecycleClaimResult.Status.CLAIM_PERSISTENCE_UNKNOWN, receipt);
        }
        return new LifecycleClaimResult(LifecycleClaimResult.Status.GRANTED, receipt);
    }

    /** Resolve only intent persistence; recovery never reissues execution permission. */
    public synchronized LifecycleClaimResult recoverLifecycleIntegration(UUID judgmentId) {
        if (!authoritative || readOnly || judgmentId == null) {
            return new LifecycleClaimResult(LifecycleClaimResult.Status.REJECTED, null);
        }
        LifecycleIntegrationReceipt receipt = lifecycleReceipts.get(judgmentId);
        if (receipt == null) {
            return new LifecycleClaimResult(LifecycleClaimResult.Status.REJECTED, null);
        }
        return new LifecycleClaimResult(persistAuthoritative()
            ? LifecycleClaimResult.Status.ALREADY_CLAIMED_EFFECT_UNKNOWN
            : LifecycleClaimResult.Status.CLAIM_PERSISTENCE_UNKNOWN, receipt);
    }

    /** A plan is reconstructed from retained Authority receipts; no executor supplies steps or new Truth. */
    public synchronized dev.ronova.coremod.root.api.TerminalOperationPlan approveTerminalPlan(UUID subjectId, UUID nonce) {
        if (!authoritative || readOnly || persistencePending || subjectId == null || nonce == null || ownerRegistry.contains(subjectId)) return null;
        SubjectQualification current = readHead.qualifications().get(new SubjectKey(realmId, subjectId));
        var resolved = dev.ronova.coremod.contract.SubjectResolver.incarnation(realmId,
            new SubjectKey(realmId, subjectId), nonce, current, readHead.commitIndex());
        if (!resolved.bound() || !current.terminal() || !tombstoneStore.contains(subjectId)) return null;
        var authority = commandJournal.latestQualification(current.targetEpoch());
        if (authority == null || !authority.qualification().terminal()) return null;
        return dev.ronova.coremod.root.api.TerminalOperationPlan.terminal(authority.commandId(), authority.payloadHash(),
            authority.commitIndex(), readHead.commitIndex(), current, activeStrengthPolicyConfiguration.revision().value(), readHead.policy().revision().value());
    }
    public synchronized boolean validateTerminalPlan(dev.ronova.coremod.root.api.TerminalOperationPlan plan) {
        return plan != null && plan.equals(approveTerminalPlan(plan.subjectKey().subjectId(), plan.physicalNonce()));
    }
    /** No callback runs under the Root monitor; only a validated step receives the exclusion token. */
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireTerminalWrite(
        dev.ronova.coremod.root.api.TerminalOperationPlan plan) {
        return validateTerminalPlan(plan)?executionFence.tryAcquire(plan):null;
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireOwnerWrite(dev.ronova.coremod.root.api.AuthorityStamp expected) {
        if(!authoritative||readOnly||persistencePending||expected==null||!expected.owner()||!expected.settled()
            ||!expected.sameAuthority(authorityStamp(expected.subjectKey().subjectId())))return null;
        return executionFence.tryAcquireOwner(expected);
    }
    /** Read-only conditional permission derived from the already committed terminal command. */
    public synchronized dev.ronova.coremod.root.api.RecoverySourcePlan approveRecoverySourcePlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence) {
        if (!authoritative || readOnly || persistencePending || terminal == null || evidence == null || !evidence.fresh(System.nanoTime())) return null;
        var current = approveTerminalPlan(terminal.subjectKey().subjectId(), terminal.physicalNonce());
        if (current == null || !current.qualification().equals(terminal.qualification())
            || !current.commandId().equals(terminal.commandId()) || !current.authorityHash().equals(terminal.authorityHash())
            || current.authorityCommitIndex() != terminal.authorityCommitIndex()
            || !evidence.authority().sameAuthority(authorityStamp(current.subjectKey().subjectId()))) return null;
        var direct = dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(strengthPolicySnapshot(), evidence.copyDepth(), true);
        if (!direct.executionPermitted()) return null;
        var derived = dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(strengthPolicySnapshot(), 2, true);
        return new dev.ronova.coremod.root.api.RecoverySourcePlan(current, evidence.sourceLocator(),
            derived.executionPermitted() ? Integer.MAX_VALUE : 1, dev.ronova.coremod.root.api.RecoverySourcePlan.CONTRACT);
    }
    /** Independent record-write permission; disabling the rebuild outlet does not disable a permitted direct record action. */
    public synchronized dev.ronova.coremod.root.api.RecoverySnapshotPlan approveStaticSnapshotPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, long sourceVersion, boolean bindingVerified) {
        if (!authoritative || readOnly || persistencePending || !bindingVerified || terminal == null || evidence == null
            || sourceVersion < 1 || sourceVersion > evidence.sourceRevision() || !evidence.fresh(System.nanoTime())) return null;
        var current = approveTerminalPlan(terminal.subjectKey().subjectId(), terminal.physicalNonce());
        if (current == null || !current.qualification().equals(terminal.qualification())
            || !current.commandId().equals(terminal.commandId()) || !current.authorityHash().equals(terminal.authorityHash())
            || current.authorityCommitIndex() != terminal.authorityCommitIndex()
            || !evidence.authority().sameAuthority(authorityStamp(current.subjectKey().subjectId()))
            || !dev.ronova.coremod.policy.RecoveryChainPolicy.registeredAtomicSnapshot(
                strengthPolicySnapshot(), evidence.copyDepth(), bindingVerified).executionPermitted()) return null;
        return new dev.ronova.coremod.root.api.RecoverySnapshotPlan(current, evidence.sourceLocator(), sourceVersion,
            evidence.sourceRevision(), evidence.copyDepth());
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireStaticSnapshotWrite(
        dev.ronova.coremod.root.api.RecoverySnapshotPlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean bindingVerified) {
        if (plan == null) return null;
        var expected = approveStaticSnapshotPlan(plan.terminal(), evidence, plan.sourceVersion(), bindingVerified);
        return plan.equals(expected) ? executionFence.tryAcquireSnapshot(plan) : null;
    }
    /** Same record-disposal policy, but a distinct scope from clearing a snapshot cell. */
    public synchronized dev.ronova.coremod.root.api.RecoverySnapshotPlan approveLoadedSnapshotPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, long sourceVersion, boolean loadedBindingVerified) {
        var source = approveStaticSnapshotPlan(terminal, evidence, sourceVersion, loadedBindingVerified);
        return source == null ? null : new dev.ronova.coremod.root.api.RecoverySnapshotPlan(source.terminal(),
            source.sourceLocator(), source.sourceVersion(), source.witnessRevision(), source.copyDepth(),
            dev.ronova.coremod.root.api.RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA);
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireLoadedSnapshotWrite(
        dev.ronova.coremod.root.api.RecoverySnapshotPlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean loadedBindingVerified) {
        if (plan == null) return null;
        var expected = approveLoadedSnapshotPlan(plan.terminal(), evidence, plan.sourceVersion(), loadedBindingVerified);
        return plan.equals(expected) ? executionFence.tryAcquireSnapshot(plan) : null;
    }
    /** Same record-disposal policy, but a distinct scope from clearing a snapshot cell. */
    public synchronized dev.ronova.coremod.root.api.RecoveryRecordPlan approveStaticRecordPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, long sourceVersion, boolean directBindingVerified) {
        var source = approveStaticSnapshotPlan(terminal, evidence, sourceVersion, directBindingVerified);
        return source == null ? null : new dev.ronova.coremod.root.api.RecoveryRecordPlan(source);
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireStaticRecordWrite(
        dev.ronova.coremod.root.api.RecoveryRecordPlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean directBindingVerified) {
        if (plan == null) return null;
        var source = plan.source();
        var expected = approveStaticRecordPlan(source.terminal(), evidence, source.sourceVersion(), directBindingVerified);
        return plan.equals(expected) ? executionFence.tryAcquireRecord(plan) : null;
    }
    /** Loaded direct records have a separate storage scope; binding must be established by the registered executor. */
    public synchronized dev.ronova.coremod.root.api.RecoveryRecordPlan approveLoadedRecordPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, long sourceVersion, boolean directBindingVerified) {
        var source = approveLoadedSnapshotPlan(terminal, evidence, sourceVersion, directBindingVerified);
        return source == null ? null : new dev.ronova.coremod.root.api.RecoveryRecordPlan(source);
    }
    /** Persists the intent before memory mutation; no lifecycle slot or external completion is recorded. */
    public synchronized boolean prepareRecoveryMutation(dev.ronova.coremod.root.api.RecoveryMutationIntent intent,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean bindingVerified) {
        return prepareRecoveryMutation(intent, evidence, bindingVerified, null);
    }
    public synchronized boolean prepareRecoveryMutation(dev.ronova.coremod.root.api.RecoveryMutationIntent intent,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean bindingVerified,
        dev.ronova.coremod.root.api.RecoverySourceLocation location) {
        if (intent == null || !authoritative || readOnly || executionFence.busy()) return false;
        if (location != null && (!location.locatorHash().equals(intent.source().sourceLocator())
            || location.scope() != intent.source().storageScope() || intent.kind().record() && !location.directRecord())) return false;
        if (persistencePending && !persistAuthoritative()) return false;
        if (!validRecoveryMutation(intent, evidence, bindingVerified)) return false;
        liveMemoryScopes.removeIf(reference->reference.get()==null);
        if(liveMemoryScopes.stream().noneMatch(reference->reference.get()==intent)) {
            if(liveMemoryScopes.size()>=256)return false;
            liveMemoryScopes.add(new java.lang.ref.WeakReference<>(intent));
        }
        var existing = effectJournal.get(intent.effectKey());
        if (existing != null) {
            if (!recoveryMutationPrepared(intent)) return false;
            var priorLocation = effectJournal.location(intent.effectKey());
            if (location == null) return true;
            if (priorLocation != null) return priorLocation.equals(location);
        }
        var nextEffects = effectJournal.copy();
        if (existing == null) {
            var record = intent.prepared();
            if (!commandJournal.policyAt(record.planRevision()).equals(activeStrengthPolicyConfiguration)) return false;
            nextEffects.put(record);
        }
        if (location != null) nextEffects.locate(intent.effectKey(), location);
        DurableRootFileBackend.PreparedWrite prepared;
        try {
            CompoundTag image = writeImage(new CompoundTag(), snapshot, lifecycleReceipts.values(),
                strengthPolicyConfiguration, qualifications, commandJournal, nextEffects);
            image.putBoolean("persistence_pending", false);
            prepared = preflight(image, nextEffects, true);
        } catch (StorageCapacityException | DurableRootPersistenceException blocked) {
            storageRejections++; persistenceFailure = normalizedFailure(blocked); return false;
        }
        effectJournal = nextEffects; persistencePending = true; setDirty();
        return persistAuthoritative(prepared);
    }
    /** Read-only locator hint; neither current binding proof nor a grant to execute. */
    public synchronized dev.ronova.coremod.root.api.RecoverySourceLocation recoveryMutationLocation(EffectKey key) {
        return effectJournal.location(key);
    }
    /** Read-only intent ACK gate; the executor must separately acquire a current source write lease. */
    public synchronized boolean recoveryMutationPrepared(dev.ronova.coremod.root.api.RecoveryMutationIntent intent) {
        if (intent == null || !authoritative || readOnly || persistencePending) return false;
        var record = effectJournal.get(intent.effectKey()); var terminal = intent.source().terminal();
        return record != null && record.unresolved() && record.authorityPayloadHash().equals(terminal.authorityHash())
            && record.authorityCommitIndex() == terminal.authorityCommitIndex()
            && record.effectPayloadHash().equals(dev.ronova.coremod.root.api.RecoveryMutationIntent.payloadHash(
                intent.effectKey(), terminal.authorityHash()));
    }
    private boolean validRecoveryMutation(dev.ronova.coremod.root.api.RecoveryMutationIntent intent,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean bindingVerified) {
        var source = intent.source();
        var expected = intent.kind().loaded()
            ? approveLoadedSnapshotPlan(source.terminal(), evidence, source.sourceVersion(), bindingVerified)
            : approveStaticSnapshotPlan(source.terminal(), evidence, source.sourceVersion(), bindingVerified);
        return source.equals(expected);
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireRecoveryMutationWrite(
        dev.ronova.coremod.root.api.RecoveryMutationIntent intent,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean bindingVerified) {
        if (!recoveryMutationPrepared(intent) || !validRecoveryMutation(intent, evidence, bindingVerified)) return null;
        return intent.kind().record()
            ? executionFence.tryAcquireRecord(new dev.ronova.coremod.root.api.RecoveryRecordPlan(intent.source()))
            : executionFence.tryAcquireSnapshot(intent.source());
    }
    /** Historical memory outcome only. Neither a save receipt nor permission to replay an opaque effect. */
    public synchronized boolean observeRecoveryMutation(dev.ronova.coremod.root.api.RecoveryMutationIntent intent,
        dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation observation) {
        if (intent == null || observation == null) return false;
        return observeRecoveryMutations(List.of(new dev.ronova.coremod.root.api.RecoveryMutationIntent.Receipt(intent, observation)));
    }
    /** One Root image for a bounded batch of existing results; no external calls and no new write permission. */
    public synchronized boolean observeRecoveryMutations(
        List<dev.ronova.coremod.root.api.RecoveryMutationIntent.Receipt> receipts) {
        if (receipts == null || receipts.size() > 256 || !authoritative || readOnly || executionFence.busy()) return false;
        var batch = List.copyOf(receipts);
        if (persistencePending && !persistAuthoritative()) return false;
        // Validate every original intent before changing even the in-memory journal.
        for (var receipt : batch) if (!recoveryMutationPrepared(receipt.intent())) return false;
        var nextEffects = effectJournal.copy(); boolean changed = false;
        for (var receipt : batch) {
            var prior = nextEffects.get(receipt.intent().effectKey());
            String evidence = dev.ronova.coremod.root.api.RecoveryMutationIntent.observations(prior, receipt.observation());
            if (prior.state() == EffectRecord.State.EFFECT_UNKNOWN && prior.evidenceRef().equals(evidence)) continue;
            nextEffects.put(prior.unknown(evidence)); changed = true;
        }
        if (!changed) return true;
        effectJournal = nextEffects; persistencePending = true; setDirty();
        return persistAuthoritative();
    }
    /** Conditional registered task permission; durable intent ACK is a separate gate. */
    public synchronized dev.ronova.coremod.root.api.RecoveryTaskPlan approveRecoveryTaskPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,
        dev.ronova.coremod.root.api.RecoveryWorkDescriptor description,boolean bindingVerified) {
        if(description==null||description.kind()!=dev.ronova.coremod.root.api.RecoveryWorkDescriptor.Kind.TASK_CANCEL)return null;
        var source=approveRecoveryWorkSource(terminal,evidence,description,bindingVerified);
        return source==null?null:new dev.ronova.coremod.root.api.RecoveryTaskPlan(source,description);
    }
    public synchronized dev.ronova.coremod.root.api.RecoveryStoragePlan approveRecoveryStoragePlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,
        dev.ronova.coremod.root.api.RecoveryWorkDescriptor description,boolean bindingVerified) {
        if(description==null||description.kind()!=dev.ronova.coremod.root.api.RecoveryWorkDescriptor.Kind.STORAGE_COMMIT)return null;
        var source=approveRecoveryWorkSource(terminal,evidence,description,bindingVerified);
        return source==null?null:new dev.ronova.coremod.root.api.RecoveryStoragePlan(source,description);
    }
    /** Continue an acknowledged memory disposition. Historical evidence is not made fresh. */
    public synchronized dev.ronova.coremod.root.api.RecoveryStoragePlan approveStorageAfterMemory(
        dev.ronova.coremod.root.api.RecoveryMutationIntent memory,
        dev.ronova.coremod.root.api.RecoveryWorkDescriptor description,boolean bindingVerified) {
        if(memory==null||description==null||!bindingVerified||!authoritative||readOnly||persistencePending
            ||description.kind()!=dev.ronova.coremod.root.api.RecoveryWorkDescriptor.Kind.STORAGE_COMMIT
            ||description.storageGeneration()==null||description.fileIdentity().isEmpty()
            ||liveMemoryScopes.stream().noneMatch(reference->reference.get()==memory)
            ||!memory.acknowledgedWrite(recoveryMutationResult(memory)))return null;
        var source=memory.source();var terminal=source.terminal();
        if(!description.registration().equals(memory.incarnation())
            ||!description.location().equals(effectJournal.location(memory.effectKey()))
            ||description.sourceVersion()!=source.sourceVersion()||description.copyDepth()!=source.copyDepth()
            ||description.location().scope()!=source.storageScope()
            ||!description.location().locatorHash().equals(source.sourceLocator()))return null;
        var current=approveTerminalPlan(terminal.subjectKey().subjectId(),terminal.physicalNonce());
        if(current==null||!current.qualification().equals(terminal.qualification())
            ||!current.commandId().equals(terminal.commandId())||!current.authorityHash().equals(terminal.authorityHash())
            ||current.authorityCommitIndex()!=terminal.authorityCommitIndex()
            ||!dev.ronova.coremod.policy.RecoveryChainPolicy.registeredStorageCommit(
                strengthPolicySnapshot(),source.copyDepth(),true).executionPermitted())return null;
        return new dev.ronova.coremod.root.api.RecoveryStoragePlan(source,description);
    }
    public synchronized boolean prepareStorageAfterMemory(dev.ronova.coremod.root.api.RecoveryStoragePlan plan,
        dev.ronova.coremod.root.api.RecoveryMutationIntent memory,boolean bindingVerified) {
        if(plan==null||executionFence.busy())return false;
        if(persistencePending&&!persistAuthoritative())return false;
        return plan.equals(approveStorageAfterMemory(memory,plan.descriptor(),bindingVerified))
            &&persistRecoveryWork(plan.source(),plan.descriptor());
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireStorageAfterMemory(
        dev.ronova.coremod.root.api.RecoveryStoragePlan plan,
        dev.ronova.coremod.root.api.RecoveryMutationIntent memory,boolean bindingVerified) {
        return plan!=null&&plan.equals(approveStorageAfterMemory(memory,plan.descriptor(),bindingVerified))
            &&recoveryWorkPrepared(plan.source(),plan.descriptor())?executionFence.tryAcquireStorage(plan):null;
    }
    private dev.ronova.coremod.root.api.RecoverySnapshotPlan approveRecoveryWorkSource(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,
        dev.ronova.coremod.root.api.RecoveryWorkDescriptor description,boolean bindingVerified) {
        if(!authoritative||readOnly||persistencePending||terminal==null||evidence==null||description==null||!bindingVerified
            ||!evidence.fresh(System.nanoTime())||description.sourceVersion()>evidence.sourceRevision()
            ||description.copyDepth()!=evidence.copyDepth()||!description.location().locatorHash().equals(evidence.sourceLocator()))return null;
        var current=approveTerminalPlan(terminal.subjectKey().subjectId(),terminal.physicalNonce());
        if(current==null||!current.qualification().equals(terminal.qualification())||!current.commandId().equals(terminal.commandId())
            ||!current.authorityHash().equals(terminal.authorityHash())||current.authorityCommitIndex()!=terminal.authorityCommitIndex()
            ||!evidence.authority().sameAuthority(authorityStamp(current.subjectKey().subjectId())))return null;
        var policy=description.kind()==dev.ronova.coremod.root.api.RecoveryWorkDescriptor.Kind.STORAGE_COMMIT
            ?dev.ronova.coremod.policy.RecoveryChainPolicy.registeredStorageCommit(strengthPolicySnapshot(),description.copyDepth(),true)
            :dev.ronova.coremod.policy.RecoveryChainPolicy.registeredTaskDisposition(strengthPolicySnapshot(),description.permissionDepth(),true);
        if(!policy.executionPermitted())return null;
        return new dev.ronova.coremod.root.api.RecoverySnapshotPlan(current,evidence.sourceLocator(),description.sourceVersion(),
            evidence.sourceRevision(),description.copyDepth(),description.location().scope());
    }
    public synchronized boolean prepareRecoveryTask(dev.ronova.coremod.root.api.RecoveryTaskPlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,boolean bindingVerified) {
        if(plan==null)return false;
        if(enteredEffects.contains(plan.effectKey()))return false;
        if(persistencePending&&!persistAuthoritative())return false;
        if(!plan.source().equals(approveRecoveryWorkSource(plan.source().terminal(),evidence,plan.descriptor(),bindingVerified)))return false;
        liveTaskScopes.removeIf(reference->reference.get()==null);
        if(liveTaskScopes.stream().noneMatch(reference->reference.get()==plan)) {
            if(liveTaskScopes.size()>=128)return false;
            liveTaskScopes.add(new java.lang.ref.WeakReference<>(plan));
        }
        return plan!=null&&prepareRecoveryWork(plan.source(),plan.descriptor(),evidence,bindingVerified);
    }
    /** Original live observer scope plus persistent ACK; no fresh reconstruction is invented. */
    public synchronized boolean recoveryTaskContinuationCurrent(dev.ronova.coremod.root.api.RecoveryTaskPlan plan,boolean bindingVerified) {
        if(plan==null||!bindingVerified||enteredEffects.contains(plan.effectKey())||liveTaskScopes.stream().noneMatch(reference->reference.get()==plan)
            ||!recoveryWorkPrepared(plan.source(),plan.descriptor()))return false;
        var record=effectJournal.get(plan.effectKey());
        if(record.state()!=EffectRecord.State.PREPARED)return false;
        var terminal=plan.source().terminal();var current=approveTerminalPlan(terminal.subjectKey().subjectId(),terminal.physicalNonce());
        return current!=null&&current.qualification().equals(terminal.qualification())&&current.commandId().equals(terminal.commandId())
            &&current.authorityHash().equals(terminal.authorityHash())&&current.authorityCommitIndex()==terminal.authorityCommitIndex()
            &&dev.ronova.coremod.policy.RecoveryChainPolicy.registeredTaskDisposition(
                strengthPolicySnapshot(),plan.descriptor().permissionDepth(),true).executionPermitted();
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireTaskContinuation(
        dev.ronova.coremod.root.api.RecoveryTaskPlan plan,boolean bindingVerified) {
        return recoveryTaskContinuationCurrent(plan,bindingVerified)?executionFence.tryAcquireTask(plan):null;
    }
    /** Consume this process's task scope even when the eventual receipt is delayed or lost. */
    public synchronized void retireTaskContinuation(dev.ronova.coremod.root.api.RecoveryTaskPlan plan) {
        if(plan==null||liveTaskScopes.stream().noneMatch(reference->reference.get()==plan))return;
        enteredEffects.add(plan.effectKey());
        liveTaskScopes.removeIf(reference->{var value=reference.get();return value==null||value.effectKey().equals(plan.effectKey());});
    }
    public synchronized void releaseTaskContinuationGuard(EffectKey key) {
        var result=recoveryWorkResult(key);
        if(result!=null&&result.descriptor().kind()==dev.ronova.coremod.root.api.RecoveryWorkDescriptor.Kind.TASK_CANCEL
            &&result.record().state()!=EffectRecord.State.PREPARED)enteredEffects.remove(key);
    }
    public synchronized boolean prepareRecoveryStorage(dev.ronova.coremod.root.api.RecoveryStoragePlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,boolean bindingVerified) {
        return plan!=null&&prepareRecoveryWork(plan.source(),plan.descriptor(),evidence,bindingVerified);
    }
    private boolean prepareRecoveryWork(dev.ronova.coremod.root.api.RecoverySnapshotPlan source,
        dev.ronova.coremod.root.api.RecoveryWorkDescriptor description,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,boolean bindingVerified) {
        if(!authoritative||readOnly||executionFence.busy())return false;
        if(persistencePending&&!persistAuthoritative())return false;
        if(!source.equals(approveRecoveryWorkSource(source.terminal(),evidence,description,bindingVerified)))return false;
        return persistRecoveryWork(source,description);
    }
    private boolean persistRecoveryWork(dev.ronova.coremod.root.api.RecoverySnapshotPlan source,
        dev.ronova.coremod.root.api.RecoveryWorkDescriptor description) {
        var key=description.effectKey(source.terminal());
        var alias=effectJournal.workAliasKey(key,description);if(alias!=null&&!alias.equals(key))return false;
        if(effectJournal.get(key)!=null)return recoveryWorkPrepared(source,description);
        var record=description.prepared(source.terminal());
        if(!commandJournal.policyAt(record.planRevision()).equals(activeStrengthPolicyConfiguration))return false;
        var nextEffects=effectJournal.copy();nextEffects.put(record);nextEffects.describeWork(key,description);
        DurableRootFileBackend.PreparedWrite prepared;
        try {
            CompoundTag image=writeImage(new CompoundTag(),snapshot,lifecycleReceipts.values(),strengthPolicyConfiguration,qualifications,commandJournal,nextEffects);
            image.putBoolean("persistence_pending",false);prepared=preflight(image,nextEffects,true);
        } catch(StorageCapacityException|DurableRootPersistenceException blocked){storageRejections++;persistenceFailure=normalizedFailure(blocked);return false;}
        effectJournal=nextEffects;persistencePending=true;setDirty();return persistAuthoritative(prepared);
    }
    private boolean recoveryWorkPrepared(dev.ronova.coremod.root.api.RecoverySnapshotPlan source,
        dev.ronova.coremod.root.api.RecoveryWorkDescriptor description) {
        if(!authoritative||readOnly||persistencePending)return false;
        var key=description.effectKey(source.terminal());var record=effectJournal.get(key);
        return record!=null&&record.unresolved()&&description.equals(effectJournal.work(key))
            &&record.authorityCommitIndex()==source.terminal().authorityCommitIndex()
            &&record.authorityPayloadHash().equals(source.terminal().authorityHash())
            &&record.effectPayloadHash().equals(description.payloadHash(key,source.terminal().authorityHash()));
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireRecoveryTaskWrite(
        dev.ronova.coremod.root.api.RecoveryTaskPlan plan,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,boolean bindingVerified) {
        return recoveryTaskContinuationCurrent(plan,bindingVerified)
            &&plan.source().equals(approveRecoveryWorkSource(plan.source().terminal(),evidence,plan.descriptor(),bindingVerified))
            ?executionFence.tryAcquireTask(plan):null;
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireRecoveryStorageWrite(
        dev.ronova.coremod.root.api.RecoveryStoragePlan plan,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,boolean bindingVerified) {
        return plan!=null&&recoveryWorkPrepared(plan.source(),plan.descriptor())
            &&plan.source().equals(approveRecoveryWorkSource(plan.source().terminal(),evidence,plan.descriptor(),bindingVerified))
            ?executionFence.tryAcquireStorage(plan):null;
    }
    /** Historical query of the original intent. It neither resolves unknown effects nor issues a write permit. */
    public synchronized dev.ronova.coremod.root.api.RecoveryWorkResult recoveryWorkResult(EffectKey key) {
        if(!authoritative||readOnly||persistencePending)return null;
        var record=effectJournal.get(key);var description=effectJournal.work(key);
        return record==null||description==null?null:new dev.ronova.coremod.root.api.RecoveryWorkResult(record,description,
            dev.ronova.coremod.root.api.RecoveryWorkResult.facts(record));
    }
    /** Original acknowledged memory result only; no new intent or execution permission. */
    public synchronized EffectRecord recoveryMutationResult(dev.ronova.coremod.root.api.RecoveryMutationIntent intent) {
        if(intent==null||!authoritative||readOnly||persistencePending)return null;
        var record=effectJournal.get(intent.effectKey());
        return record!=null&&record.authorityPayloadHash().equals(intent.source().terminal().authorityHash())
            &&record.effectPayloadHash().equals(dev.ronova.coremod.root.api.RecoveryMutationIntent.payloadHash(
                intent.effectKey(),intent.source().terminal().authorityHash()))?record:null;
    }
    /** Reuse the Root's existing exact-instance/one-entry claims, never issue a second live attempt for an ACK. */
    public synchronized EffectClaim claimRecoveryStorage(dev.ronova.coremod.root.api.RecoveryStoragePlan plan) {
        if(plan==null||plan.descriptor().storageGeneration()==null||!recoveryWorkPrepared(plan.source(),plan.descriptor()))return null;
        var record=effectJournal.get(plan.effectKey());
        if(record==null||record.state()!=EffectRecord.State.PREPARED||effectClaims.containsKey(record.key())
            ||enteredEffects.contains(record.key())||effectClaims.size()>=128)return null;
        var claim=new EffectClaim(EffectClaim.Status.GRANTED,record);effectClaims.put(record.key(),claim);return claim;
    }
    public synchronized boolean recoveryStorageClaimCurrent(EffectClaim claim,dev.ronova.coremod.root.api.RecoveryStoragePlan plan) {
        return claim!=null&&claim.granted()&&plan!=null&&plan.effectKey().equals(claim.record().key())
            &&effectClaims.get(plan.effectKey())==claim&&!enteredEffects.contains(plan.effectKey())
            &&effectJournal.get(plan.effectKey())==claim.record()&&recoveryWorkPrepared(plan.source(),plan.descriptor());
    }
    public synchronized boolean enterRecoveryStorageClaim(EffectClaim claim,dev.ronova.coremod.root.api.RecoveryStoragePlan plan,
        dev.ronova.coremod.root.api.RootExecutionFence.Lease lease) {
        if(!recoveryStorageClaimCurrent(claim,plan)||!executionFence.ownsStorage(lease,plan))return false;
        enteredEffects.add(plan.effectKey());return true;
    }
    public synchronized void releaseRecoveryStorageClaim(EffectClaim claim) {
        if(claim==null||claim.record()==null||effectClaims.get(claim.record().key())!=claim)return;
        var key=claim.record().key();var record=effectJournal.get(key);
        var description=effectJournal.work(key);
        if(description==null||description.kind()!=dev.ronova.coremod.root.api.RecoveryWorkDescriptor.Kind.STORAGE_COMMIT)return;
        if(enteredEffects.contains(key)&&record!=null&&record.state()==EffectRecord.State.PREPARED) {
            observeRecoveryWork(key,dev.ronova.coremod.root.api.RecoveryWorkResult.Fact.EFFECT_UNKNOWN);
            // Retain the consumed claim if Root could not even record uncertainty in its pending image.
            if(effectJournal.get(key).state()==EffectRecord.State.PREPARED)return;
        }
        effectClaims.remove(key);enteredEffects.remove(key);
    }
    public synchronized boolean observeRecoveryWork(EffectKey key,dev.ronova.coremod.root.api.RecoveryWorkResult.Fact fact) {
        if(key==null||fact==null||!authoritative||readOnly||executionFence.busy())return false;
        if(persistencePending&&!persistAuthoritative())return false;
        var prior=effectJournal.get(key);if(prior==null||effectJournal.work(key)==null||!effectJournal.work(key).allows(fact))return false;
        String evidence=dev.ronova.coremod.root.api.RecoveryWorkResult.observe(prior,fact);
        if(prior.evidenceRef().equals(evidence))return true;
        var next=effectJournal.copy();next.put(prior.unknown(evidence));effectJournal=next;persistencePending=true;setDirty();return persistAuthoritative();
    }
    /** Persists only a Root-owned source qualification. No foreign container/callback is accessed here. */
    public synchronized dev.ronova.coremod.root.api.RecoverySourcePlan qualifyRecoverySource(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence) {
        if (!authoritative || readOnly || terminal == null || evidence == null || !evidence.fresh(System.nanoTime())) return null;
        if (persistencePending && !persistAuthoritative()) return null;
        var plan = approveRecoverySourcePlan(terminal, evidence);
        if (plan == null) return null;
        var existing = effectJournal.get(plan.effectKey());
        if (existing != null) return existing.state() == EffectRecord.State.EFFECT_CONFIRMED ? plan : null;
        var record = EffectRecord.sourceQualification(plan);
        // A migrated image without retained policy provenance may be observed, but cannot mint an unverifiable durable row.
        if (!commandJournal.policyAt(record.planRevision()).equals(activeStrengthPolicyConfiguration)) return null;
        var nextEffects = effectJournal.copy(); nextEffects.put(record);
        DurableRootFileBackend.PreparedWrite prepared;
        try {
            CompoundTag image = writeImage(new CompoundTag(), snapshot, lifecycleReceipts.values(),
                strengthPolicyConfiguration, qualifications, commandJournal, nextEffects);
            image.putBoolean("persistence_pending", false);
            prepared = preflight(image, nextEffects, true);
        } catch (StorageCapacityException | DurableRootPersistenceException blocked) {
            storageRejections++; persistenceFailure = normalizedFailure(blocked); return null;
        }
        effectJournal = nextEffects; persistencePending = true; setDirty();
        return persistAuthoritative(prepared) ? plan : null;
    }
    /** Read-only final outlet decision; an old plan cannot survive clear, Owner promotion or policy change. */
    public synchronized boolean recoverySourceHeld(dev.ronova.coremod.root.api.RecoverySourcePlan plan,
        int copyDepth, UUID proposedSubject) {
        if (plan == null || !authoritative || readOnly || persistencePending || !plan.coversDepth(copyDepth)
            || proposedSubject != null && ownerRegistry.contains(proposedSubject)) return false;
        var prior = plan.terminal();
        var current = approveTerminalPlan(prior.subjectKey().subjectId(), prior.physicalNonce());
        if (current == null || !current.qualification().equals(prior.qualification())
            || !current.commandId().equals(prior.commandId()) || !current.authorityHash().equals(prior.authorityHash())
            || current.activePolicyRevision() != prior.activePolicyRevision()
            || current.configuredPolicyRevision() != prior.configuredPolicyRevision()) return false;
        var record = effectJournal.get(plan.effectKey());
        return record != null && record.state() == EffectRecord.State.EFFECT_CONFIRMED
            && record.authorityPayloadHash().equals(current.authorityHash())
            && dev.ronova.coremod.policy.RecoveryChainPolicy.registeredNbtOutlet(strengthPolicySnapshot(), copyDepth, true).executionPermitted();
    }
    /** Holds a freshly proved result without waiting for source metadata I/O or allowing a probe revival. */
    public synchronized boolean recoverySourceHeld(dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, UUID proposedSubject) {
        return !(proposedSubject != null && ownerRegistry.contains(proposedSubject))
            && approveRecoverySourcePlan(terminal, evidence) != null;
    }
    public synchronized boolean terminalEffectResolved(dev.ronova.coremod.root.api.TerminalOperationPlan plan) {
        if (!validateTerminalPlan(plan)) return false;
        var effect = effectJournal.get(plan.effectKey());
        var legacy = effectJournal.get(new EffectKey(plan.commandId(), plan.qualification().targetEpoch(), EffectRecord.PUBLIC_DEATH, EffectRecord.ONCE_SLOT));
        return effect != null && !effect.unresolved() && (legacy == null || !legacy.unresolved());
    }
    /** Root approves the connected participants before execution; protected owners are required for this view sub-contract. */
    public synchronized dev.ronova.coremod.root.api.ClientParticipationContract approveClientParticipation(
        dev.ronova.coremod.root.api.TerminalOperationPlan plan,java.util.List<dev.ronova.coremod.root.api.ClientParticipationContract.Endpoint> endpoints) {
        if(!validateTerminalPlan(plan)||endpoints.size()>64)return null;
        var members=endpoints.stream().map(e->new dev.ronova.coremod.root.api.ClientParticipationContract.Member(e,
            readHead.owners().contains(e.client()),readHead.ownerAuthorizationRevisions().getOrDefault(e.client(),0L))).toList();
        var marks=readHead.snapshot().judgments().stream().map(j->new dev.ronova.coremod.engine.JudgmentMark(j.subject().uuid(),j.subject().registryType(),
            j.subject().dimension(),j.subject().x(),j.subject().y(),j.subject().z(),j.radius(),dev.ronova.coremod.engine.JudgmentScope.valueOf(j.scope().name()))).toList();
        return new dev.ronova.coremod.root.api.ClientParticipationContract(plan,readHead.snapshot().epoch(),dev.ronova.coremod.network.ProjectionDigest.of(marks),members);
    }
    /** Replanning updates the image, never silently removes disconnected required members. */
    public synchronized dev.ronova.coremod.root.api.ClientParticipationContract refreshClientParticipation(
        dev.ronova.coremod.root.api.ClientParticipationContract previous,dev.ronova.coremod.root.api.TerminalOperationPlan plan) {
        if(previous==null||!validateTerminalPlan(plan)||!previous.plan().effectKey().equals(plan.effectKey())
            ||!previous.plan().subjectKey().realmId().equals(realmId))return null;
        var marks=readHead.snapshot().judgments().stream().map(j->new dev.ronova.coremod.engine.JudgmentMark(j.subject().uuid(),j.subject().registryType(),
            j.subject().dimension(),j.subject().x(),j.subject().y(),j.subject().z(),j.radius(),dev.ronova.coremod.engine.JudgmentScope.valueOf(j.scope().name()))).toList();
        return new dev.ronova.coremod.root.api.ClientParticipationContract(plan,readHead.snapshot().epoch(),
            dev.ronova.coremod.network.ProjectionDigest.of(marks),previous.members());
    }
    public synchronized boolean registerWriteSetReceipt(dev.ronova.coremod.root.api.TerminalOperationPlan plan,
        dev.ronova.coremod.root.api.RootExecutionFence.Lease lease,dev.ronova.coremod.root.api.WriteSetReceipt receipt) {
        if(receipt==null||!receipt.matches(plan)||!receipt.exact()||!executionFence.ownsTerminal(lease,plan))return false;
        String hash=receipt.evidenceHash();
        if(acceptedWriteSetReceipts.contains(hash)||acceptedWriteSetReceipts.size()>=256)return false;
        acceptedWriteSetReceipts.add(hash);return true;
    }
    public synchronized boolean acceptOperationReport(dev.ronova.coremod.root.api.OperationReport report) {
        if (report == null || !validateTerminalPlan(report.plan()) || report.completionSatisfied()) return false;
        var presented=new java.util.ArrayList<String>();
        boolean accepted=true;
        for (var step : report.steps()) {
            for(var writeSet:step.writeSets()) {
                presented.add(writeSet.evidenceHash());
                if(!writeSet.matches(report.plan())||!writeSet.exact()
                    ||!acceptedWriteSetReceipts.contains(writeSet.evidenceHash()))accepted=false;
            }
            if(step.step()==dev.ronova.coremod.root.api.TerminalOperationPlan.Step.TERMINAL_STATE
                && (step.status()==dev.ronova.coremod.root.api.OperationReport.Status.CONVERGED
                    ||step.status()==dev.ronova.coremod.root.api.OperationReport.Status.DISPATCHED)
                && step.writeSets().isEmpty())accepted=false;
            if (step.step() == dev.ronova.coremod.root.api.TerminalOperationPlan.Step.TERMINAL_LIFECYCLE
                && step.status() == dev.ronova.coremod.root.api.OperationReport.Status.EFFECT_UNKNOWN) {
                var plan = report.plan();
                var legacy = new EffectKey(plan.commandId(), plan.qualification().targetEpoch(), EffectRecord.PUBLIC_DEATH, EffectRecord.ONCE_SLOT);
                if (effectJournal.get(plan.effectKey()) == null && effectJournal.get(legacy) == null
                    && !lifecycleReceipts.containsKey(plan.subjectKey().subjectId())) accepted=false;
            }
        }
        // Typed receipts are one-use execution evidence. A rejected report cannot replay them later.
        presented.forEach(acceptedWriteSetReceipts::remove);
        return accepted;
    }
    public synchronized EffectClaim prepareTerminalEffect(dev.ronova.coremod.root.api.TerminalOperationPlan plan, String physicalIdentity) {
        if (!validateTerminalPlan(plan) || physicalIdentity == null || physicalIdentity.length() > 256)
            return new EffectClaim(EffectClaim.Status.REJECTED, null);
        EffectKey key = plan.effectKey();
        if (effectJournal.get(key) != null) return recoverEffect(key);
        EffectKey old = new EffectKey(plan.commandId(), plan.qualification().targetEpoch(), EffectRecord.PUBLIC_DEATH, EffectRecord.ONCE_SLOT);
        if (effectJournal.get(old) != null) return new EffectClaim(EffectClaim.Status.LEGACY_CLAIM_UNKNOWN, effectJournal.get(old));
        if (lifecycleReceipts.containsKey(plan.subjectKey().subjectId())) return new EffectClaim(EffectClaim.Status.LEGACY_CLAIM_UNKNOWN, null);
        // A retained terminal qualification still supports independent, separately permitted actions.
        // The ordinary terminalize switch only gates new public lifecycle attempts here.
        if (!activeStrengthPolicyConfiguration.permitsBaselineAction(dev.ronova.coremod.policy.AttackAction.TERMINALIZE))
            return new EffectClaim(EffectClaim.Status.REJECTED, null);
        var authority = commandJournal.latestQualification(plan.qualification().targetEpoch());
        if (authority == null || !authority.qualification().equals(plan.qualification())) return new EffectClaim(EffectClaim.Status.REJECTED, null);
        return persistEffectIntent(EffectRecord.terminalLifecycle(key, plan.authorityHash(), plan.authorityCommitIndex(),
            plan.activePolicyRevision(), plan.planRevision()), physicalIdentity);
    }

    /** Root-owned semantic slot: a different plan/physical object cannot obtain a second attempt. */
    public synchronized EffectClaim prepareLifecycleEffect(UUID subjectId, String physicalIdentity) {
        if (!authoritative || readOnly || subjectId == null) return new EffectClaim(EffectClaim.Status.REJECTED, null);
        if (!persistAuthoritative()) return new EffectClaim(EffectClaim.Status.PERSISTENCE_UNKNOWN, null);
        SubjectKey subject = new SubjectKey(realmId, subjectId);
        SubjectQualification qualification = qualifications.get(subject);
        if (qualification == null || !qualification.terminal() || !tombstoneStore.contains(subjectId)
            || ownerRegistry.contains(subjectId)) return new EffectClaim(EffectClaim.Status.REJECTED, null);
        if (qualification.targetEpoch().generation() == 0L) return new EffectClaim(EffectClaim.Status.LEGACY_UNQUALIFIED, null);
        RootCommandJournal.QualificationAuthority authority = commandJournal.latestQualification(qualification.targetEpoch());
        if (authority == null || !authority.qualification().terminal()) return new EffectClaim(EffectClaim.Status.REJECTED, null);
        EffectKey key = new EffectKey(authority.commandId(), qualification.targetEpoch(), EffectRecord.PUBLIC_DEATH, EffectRecord.ONCE_SLOT);
        if (effectJournal.get(key) != null) return recoverEffect(key);
        EffectKey bundle = new EffectKey(authority.commandId(), qualification.targetEpoch(), EffectRecord.TERMINAL_LIFECYCLE, EffectRecord.ONCE_SLOT);
        if (effectJournal.get(bundle) != null) return new EffectClaim(EffectClaim.Status.LEGACY_CLAIM_UNKNOWN, effectJournal.get(bundle));
        if (!authority.qualification().equals(qualification)) return new EffectClaim(EffectClaim.Status.REJECTED, null);
        if (lifecycleReceipts.containsKey(subjectId)) return new EffectClaim(EffectClaim.Status.LEGACY_CLAIM_UNKNOWN, null);
        if (!activeStrengthPolicyConfiguration.permitsBaselineAction(dev.ronova.coremod.policy.AttackAction.TERMINALIZE)
            || physicalIdentity != null && physicalIdentity.length() > 256) return new EffectClaim(EffectClaim.Status.REJECTED, null);

        EffectRecord record = EffectRecord.publicDeath(key, authority.payloadHash(), authority.commitIndex(),
            activeStrengthPolicyConfiguration.revision().value());
        return persistEffectIntent(record, physicalIdentity);
    }

    private EffectClaim persistEffectIntent(EffectRecord record, String physicalIdentity) {
        EffectKey key = record.key(); UUID subjectId = key.targetEpoch().subjectKey().subjectId();
        EffectJournal nextEffects = effectJournal.copy();
        nextEffects.put(record);
        LinkedHashMap<UUID, LifecycleIntegrationReceipt> nextReceipts = new LinkedHashMap<>(lifecycleReceipts);
        nextReceipts.put(subjectId, new LifecycleIntegrationReceipt(subjectId, physicalIdentity, snapshot.epoch()));
        DurableRootFileBackend.PreparedWrite prepared;
        try {
            CompoundTag image = writeImage(new CompoundTag(), snapshot, nextReceipts.values(),
                strengthPolicyConfiguration, qualifications, commandJournal, nextEffects);
            image.putBoolean("persistence_pending", false);
            prepared = preflight(image, nextEffects, true);
        } catch (StorageCapacityException | DurableRootPersistenceException blocked) {
            storageRejections++;
            persistenceFailure = normalizedFailure(blocked);
            return new EffectClaim(EffectClaim.Status.STORAGE_BLOCKED, null);
        }
        effectJournal = nextEffects;
        lifecycleReceipts = nextReceipts;
        stateHash = stateHash(realmId, snapshot, lifecycleReceipts.values());
        persistencePending = true;
        setDirty();
        if (!persistAuthoritative(prepared)) {
            effectJournal.put(record.unknown("intent-persistence-unknown"));
            return new EffectClaim(EffectClaim.Status.PERSISTENCE_UNKNOWN, effectJournal.get(key));
        }
        EffectClaim claim = new EffectClaim(EffectClaim.Status.GRANTED, record);
        effectClaims.put(key, claim);
        return claim;
    }

    /** This checks one attempt at entry; it is not proof of all downstream WriteSink/Native paths. */
    public synchronized boolean enterEffect(EffectClaim claim) {
        if (claim == null || !claim.granted() || !authoritative || readOnly) return false;
        EffectRecord record = claim.record();
        EffectKey key = record.key();
        if (effectJournal.work(key)!=null||effectClaims.get(key) != claim || effectJournal.get(key) != record
            || enteredEffects.contains(key) || !persistAuthoritative()) return false;
        SubjectQualification current = qualifications.get(key.targetEpoch().subjectKey());
        var authority = commandJournal.latestQualification(key.targetEpoch());
        if (current == null || !current.terminal() || !current.targetEpoch().equals(key.targetEpoch())
            || authority == null || !authority.qualification().equals(current)
            || ownerRegistry.contains(key.targetEpoch().subjectKey().subjectId())
            || record.policyRevision() != activeStrengthPolicyConfiguration.revision().value()
            || !activeStrengthPolicyConfiguration.permitsBaselineAction(dev.ronova.coremod.policy.AttackAction.TERMINALIZE)) return false;
        enteredEffects.add(key);
        return true;
    }

    /** Opaque callbacks remain unknown even when the Java call returned normally. */
    public synchronized boolean finishEffectUnknown(EffectClaim claim) {
        if (claim == null || claim.record() == null || effectClaims.get(claim.record().key()) != claim) return false;
        EffectKey key = claim.record().key();
        if(effectJournal.work(key)!=null) {
            boolean acknowledged=observeRecoveryWork(key,dev.ronova.coremod.root.api.RecoveryWorkResult.Fact.EFFECT_UNKNOWN);
            releaseRecoveryStorageClaim(claim);return acknowledged;
        }
        effectClaims.remove(key);
        boolean entered = enteredEffects.remove(key);
        EffectRecord record = effectJournal.get(key);
        effectJournal.put(record.unknown(entered ? "opaque-step-returned-or-interrupted" : "entry-not-executed-unverified"));
        persistencePending = true;
        setDirty();
        return persistAuthoritative();
    }

    public synchronized EffectClaim recoverEffect(EffectKey key) {
        EffectRecord record = effectJournal.get(key);
        if (record == null || !authoritative || readOnly) return new EffectClaim(EffectClaim.Status.REJECTED, record);
        if (record.state() == EffectRecord.State.PREPARED && !effectClaims.containsKey(key)) {
            record = record.unknown("prepared-without-live-attempt");
            effectJournal.put(record);
            persistencePending = true;
            setDirty();
        }
        return new EffectClaim(persistAuthoritative() ? EffectClaim.Status.ALREADY_RECORDED
            : EffectClaim.Status.PERSISTENCE_UNKNOWN, record);
    }

    public synchronized List<EffectRecord> effectRecords() { return effectJournal.snapshot(); }
    public synchronized List<EffectRecord> effectPage(int offset, int limit) { return effectJournal.page(offset,limit); }
    public synchronized dev.ronova.coremod.obligation.ObligationCheckpoint obligationCheckpoint() { return readHead.obligations(); }
    public synchronized dev.ronova.coremod.root.api.CheckpointReceipt checkpointObligations(long expectedRevision,
        dev.ronova.coremod.obligation.ObligationCheckpoint requested) {
        Objects.requireNonNull(requested);
        if(!authoritative||readOnly)return checkpointResult(dev.ronova.coremod.root.api.CheckpointReceipt.Status.REJECTED);
        if(!persistAuthoritative())return checkpointResult(dev.ronova.coremod.root.api.CheckpointReceipt.Status.UNKNOWN);
        // Recover the original submitted observation image before recalculating Root-owned
        // reasons. A later qualification change must not turn an ACK retry into another write.
        if(expectedRevision>=0&&expectedRevision<Long.MAX_VALUE&&obligationCheckpoint.revision()==expectedRevision+1
            &&requested.sameObservationContent(obligationCheckpoint))
            return checkpointResult(dev.ronova.coremod.root.api.CheckpointReceipt.Status.UNCHANGED);
        if(requested.clock()<obligationCheckpoint.clock())return checkpointResult(dev.ronova.coremod.root.api.CheckpointReceipt.Status.REJECTED);
        dev.ronova.coremod.obligation.ObligationCheckpoint next;
        DurableRootFileBackend.PreparedWrite prepared;
        try {
            // Candidate restrictions are derived here, never accepted as caller-issued authority.
            var restrictions = obligationCheckpoint.restrictions().reconcile(requested.allEntries(), qualifications,
                key -> ownerRegistry.contains(key.subjectId()), strengthPolicyConfiguration.revision().value(), requested.clock());
            next=new dev.ronova.coremod.obligation.ObligationCheckpoint(Math.addExact(obligationCheckpoint.revision(),1),requested.clock(),requested.entries(),requested.coverage(), restrictions,requested.overflow());
            if(obligationCheckpoint.sameContent(next))return checkpointResult(dev.ronova.coremod.root.api.CheckpointReceipt.Status.UNCHANGED);
            if(expectedRevision!=obligationCheckpoint.revision())return checkpointResult(dev.ronova.coremod.root.api.CheckpointReceipt.Status.CONFLICT);
            validateObligationCheckpoint(next);
            var requestedEntries=new java.util.HashMap<dev.ronova.coremod.obligation.ObligationKey,dev.ronova.coremod.obligation.ObligationRegistry.View>();
            next.allEntries().forEach(entry->requestedEntries.put(entry.key(),entry));
            var priorEntries=new java.util.HashSet<dev.ronova.coremod.obligation.ObligationKey>();
            obligationCheckpoint.allEntries().forEach(entry->priorEntries.add(entry.key()));
            for(var entry:next.allEntries())if(!priorEntries.contains(entry.key())&&supersededObligation(entry.key().targetEpoch()))
                throw new IllegalArgumentException("retired qualification cannot create new live obligation detail");
            if(next.coverage().overflowCount()<obligationCheckpoint.coverage().overflowCount())throw new IllegalArgumentException("checkpoint forgot coverage gap");
            for(var prior:obligationCheckpoint.allEntries()) {
                var entry=requestedEntries.get(prior.key());
                // Retiring obsolete execution detail cannot change the independently retained EffectJournal.
                if(entry==null&&supersededObligation(prior.key().targetEpoch()))continue;
                if (entry==null && expiredDiagnostic(prior,next.clock())) continue;
                if (entry!=null && expiredDiagnostic(prior,next.clock()) && entry.firstSeen()>prior.lastSeen()
                    &&prior.samples().stream().allMatch(sample->sample.validUntil()<entry.firstSeen())) continue;
                if(entry==null||entry.count()<prior.count()||entry.firstSeen()!=prior.firstSeen()||entry.lastSeen()<prior.lastSeen()
                    ||entry.reviewCount()<prior.reviewCount()||!entry.durableEvidenceRefs().containsAll(prior.durableEvidenceRefs())
                    ||prior.candidate()==dev.ronova.coremod.obligation.ObligationRegistry.Candidate.CANDIDATE
                        &&entry.candidate()!=prior.candidate() || prior.mixedContext()&&!entry.mixedContext())
                    throw new IllegalArgumentException("checkpoint forgot unresolved obligation");
            }
            validateOverflowExtension(obligationCheckpoint,next);
            CompoundTag image=writeImage(new CompoundTag(),snapshot,lifecycleReceipts.values(),strengthPolicyConfiguration,qualifications,commandJournal,effectJournal,next);
            image.putBoolean("persistence_pending",false);
            prepared=preflight(image,effectJournal,true);
        }catch(StorageCapacityException|DurableRootPersistenceException blocked) {
            storageRejections++;persistenceFailure=normalizedFailure(blocked);
            return checkpointResult(dev.ronova.coremod.root.api.CheckpointReceipt.Status.REJECTED);
        }catch(IllegalArgumentException|ArithmeticException invalid) {
            return checkpointResult(dev.ronova.coremod.root.api.CheckpointReceipt.Status.REJECTED);
        }
        obligationCheckpoint=next;persistencePending=true;setDirty();
        return checkpointResult(persistAuthoritative(prepared)?dev.ronova.coremod.root.api.CheckpointReceipt.Status.COMMITTED
            :dev.ronova.coremod.root.api.CheckpointReceipt.Status.UNKNOWN);
    }
    private dev.ronova.coremod.root.api.CheckpointReceipt checkpointResult(dev.ronova.coremod.root.api.CheckpointReceipt.Status status) {
        return new dev.ronova.coremod.root.api.CheckpointReceipt(status,readHead.obligations());
    }
    private void validateObligationCheckpoint(dev.ronova.coremod.obligation.ObligationCheckpoint checkpoint) {
        for(var entry:checkpoint.allEntries()) {
            SubjectKey key=entry.key().targetEpoch().subjectKey();SubjectQualification current=qualifications.get(key);
            if(!key.realmId().equals(realmId)||current==null||entry.key().targetEpoch().generation()>current.targetEpoch().generation()
                ||entry.policyRevision()>strengthPolicyConfiguration.revision().value())throw new IllegalArgumentException("obligation references invalid Root scope");
        }
        for (var scope : checkpoint.overflow().scopes()) {
            SubjectKey key=scope.target().subjectKey();SubjectQualification current=qualifications.get(key);
            if(!key.realmId().equals(realmId)||current==null||scope.target().generation()>current.targetEpoch().generation())
                throw new IllegalArgumentException("overflow references invalid Root scope");
        }
        for (var reason : checkpoint.restrictions().reasons()) {
            var key = reason.target().subjectKey(); var current = qualifications.get(key);
            if (!key.realmId().equals(realmId) || current == null || reason.target().generation() > current.targetEpoch().generation()
                || reason.policyRevision() > strengthPolicyConfiguration.revision().value())
                throw new IllegalArgumentException("restriction references invalid Root scope");
        }
    }
    private static boolean expiredDiagnostic(dev.ronova.coremod.obligation.ObligationRegistry.View entry,long clock) {
        return entry.candidate()==dev.ronova.coremod.obligation.ObligationRegistry.Candidate.OBSERVED
            &&!entry.mixedContext()&&entry.durableEvidenceRefs().isEmpty()&&!entry.samples().isEmpty()
            &&entry.lastSeen()<clock&&entry.samples().stream().allMatch(sample->sample.validUntil()<clock);
    }
    private boolean supersededObligation(TargetEpoch target) {
        var current=qualifications.get(target.subjectKey());
        return target.subjectKey().realmId().equals(realmId)&&current!=null&&current.targetEpoch().generation()>target.generation();
    }
    private void validateOverflowExtension(dev.ronova.coremod.obligation.ObligationCheckpoint prior,
        dev.ronova.coremod.obligation.ObligationCheckpoint next) {
        if(next.overflow().unscopedCount()<prior.overflow().unscopedCount())
            throw new IllegalArgumentException("checkpoint forgot unscoped overflow");
        var scopes=new java.util.HashMap<TargetEpoch,dev.ronova.coremod.obligation.OverflowCheckpoint.Scope>();
        next.overflow().scopes().forEach(scope->scopes.put(scope.target(),scope));
        var priorTargets=new java.util.HashSet<TargetEpoch>();prior.overflow().scopes().forEach(scope->priorTargets.add(scope.target()));
        for(var scope:next.overflow().scopes())if(!priorTargets.contains(scope.target())&&supersededObligation(scope.target()))
            throw new IllegalArgumentException("retired qualification cannot recreate an overflow scope");
        for(var old:prior.overflow().scopes()) {
            var scope=scopes.get(old.target());
            if(scope==null&&supersededObligation(old.target()))continue;
            if(scope==null||scope.count()<old.count()||scope.firstSeen()!=old.firstSeen()||scope.lastSeen()<old.lastSeen()
                ||!scope.sources().containsAll(old.sources()))
                throw new IllegalArgumentException("checkpoint forgot overflow scope without recovery proof");
        }
    }
    /** Reads only durable acknowledged reasons, always subordinate to current Root qualification. */
    public synchronized boolean candidateAdmissionHeld(UUID subject, UUID physicalNonce) {
        if (subject == null || physicalNonce == null || readHead.owners().contains(subject)) return false;
        var current = readHead.qualifications().get(new SubjectKey(realmId, subject));
        if (current == null || current.terminal() || current.revisions() == null || current.revisions().activeIncarnation() == null
            || current.revisions().activeIncarnation().nonce().equals(physicalNonce)) return false;
        return readHead.obligations().restrictions().holds(current, physicalNonce);
    }
    public synchronized boolean conflictingPhysicalBinding(UUID observedSubject,UUID nonce) {
        var bound=readHead.incarnations().get(nonce);
        return bound!=null&&bound.stream().noneMatch(key->key.subjectId().equals(observedSubject));
    }
    public synchronized int effectRecordCount() { return effectJournal.size(); }
    public synchronized long effectUnresolvedCount() { return effectJournal.unresolved(); }
    public synchronized long effectReservedGrowthBytes() { return effectJournal.reservedGrowthBytes(); }
    public synchronized long storageRejections() { return storageRejections; }
    public synchronized StorageCapacity storageCapacity() { return durableBackend == null ? null : durableBackend.capacity(); }

    /** Arms one failpoint; it is consumed at most once by the next mirror. */
    public synchronized void armFailpoint(CrashFailpoint point) {
        failpoint = Objects.requireNonNull(point, "point");
    }

    public synchronized CrashFailpoint failpoint() {
        return failpoint;
    }

    /** Permanently fences this shadow instance after an integrity problem. */
    public synchronized void fenceReadOnly(String reason) {
        readOnly = true;
        readOnlyReason = normalize(reason, "shadow store fenced read-only");
        setDirty();
    }

    @Override
    public synchronized ShadowVerification mirror(
        RealmId sourceRealm,
        DominionSnapshot sourceSnapshot,
        Collection<LifecycleIntegrationReceipt> sourceReceipts
    ) {
        // After cutover, a legacy SavedData image may only be compared with Root.
        // Allowing this method to apply it would silently restore a second writer.
        if (authoritative) {
            return verify(sourceRealm, sourceSnapshot, sourceReceipts);
        }
        if (sourceRealm == null || sourceSnapshot == null) {
            return remember(verification(
                ShadowVerification.Status.INVALID_INPUT, epoch(), stateHash, stateHash,
                "shadow mirror requires realm and snapshot"
            ));
        }
        if (!realmId.equals(sourceRealm)) {
            return remember(verification(
                ShadowVerification.Status.REALM_MISMATCH, sourceSnapshot.epoch(),
                stateHash(sourceRealm, sourceSnapshot, sourceReceipts), stateHash,
                "source realm does not match bound shadow realm"
            ));
        }
        List<LifecycleIntegrationReceipt> receipts;
        try {
            receipts = normalizeReceipts(sourceSnapshot, sourceReceipts);
        } catch (IllegalArgumentException invalid) {
            return remember(verification(
                ShadowVerification.Status.INVALID_INPUT, sourceSnapshot.epoch(), stateHash,
                stateHash, invalid.getMessage()
            ));
        }
        String expectedBase = snapshotHash(realmId, sourceSnapshot);
        String expectedState = stateHash(realmId, sourceSnapshot, receipts);
        if (readOnly) {
            return remember(verification(
                ShadowVerification.Status.READ_ONLY, sourceSnapshot.epoch(), expectedState,
                stateHash, readOnlyReason
            ));
        }
        if (initialized) {
            int byEpoch = sourceSnapshot.epoch().compareTo(snapshot.epoch());
            if (byEpoch < 0) {
                return remember(verification(
                    ShadowVerification.Status.ROLLBACK_REJECTED, sourceSnapshot.epoch(),
                    expectedState, stateHash, "shadow mirror would roll back epoch"
                ));
            }
            if (byEpoch == 0 && expectedBase.equals(baseStateHash)) {
                if (!receiptSetExtends(receipts)) {
                    return remember(verification(
                        ShadowVerification.Status.RECEIPT_DRIFT, sourceSnapshot.epoch(),
                        expectedState, stateHash,
                        "authoritative lifecycle receipts removed or rewrote shadow evidence"
                    ));
                }
                if (expectedState.equals(stateHash)) {
                    if (persistencePending) {
                        setDirty();
                        persistencePending = false;
                        return remember(verification(
                            ShadowVerification.Status.MIRRORED, sourceSnapshot.epoch(),
                            expectedState, stateHash,
                            "pending shadow state marked dirty after recovery"
                        ));
                    }
                    return remember(verification(
                        ShadowVerification.Status.VERIFIED, sourceSnapshot.epoch(),
                        expectedState, stateHash, "shadow already matches authoritative state"
                    ));
                }
                triggerFailpoint(
                    CrashFailpoint.BEFORE_APPLY, sourceSnapshot.epoch(), expectedState
                );
                applyState(sourceSnapshot, receipts);
                persistencePending = true;
                triggerFailpoint(
                    CrashFailpoint.AFTER_APPLY_BEFORE_DIRTY,
                    sourceSnapshot.epoch(), expectedState
                );
                setDirty();
                persistencePending = false;
                mirrorCount++;
                triggerFailpoint(
                    CrashFailpoint.AFTER_DIRTY_BEFORE_ACK,
                    sourceSnapshot.epoch(), expectedState
                );
                return remember(verification(
                    ShadowVerification.Status.RECEIPT_MIRRORED, sourceSnapshot.epoch(),
                    expectedState, stateHash, "authoritative lifecycle receipts mirrored"
                ));
            }
            if (byEpoch == 0) {
                return remember(verification(
                    ShadowVerification.Status.STATE_CONFLICT, sourceSnapshot.epoch(),
                    expectedState, stateHash, "same epoch contains different shadow state"
                ));
            }
        }

        triggerFailpoint(CrashFailpoint.BEFORE_APPLY, sourceSnapshot.epoch(), expectedState);
        applyState(sourceSnapshot, receipts);
        persistencePending = true;
        triggerFailpoint(
            CrashFailpoint.AFTER_APPLY_BEFORE_DIRTY, sourceSnapshot.epoch(), expectedState
        );
        setDirty();
        persistencePending = false;
        mirrorCount++;
        triggerFailpoint(
            CrashFailpoint.AFTER_DIRTY_BEFORE_ACK, sourceSnapshot.epoch(), expectedState
        );
        return remember(verification(
            ShadowVerification.Status.MIRRORED, sourceSnapshot.epoch(),
            expectedState, stateHash, "authoritative SavedData snapshot mirrored"
        ));
    }

    @Override
    public synchronized ShadowVerification verify(
        RealmId sourceRealm,
        DominionSnapshot sourceSnapshot,
        Collection<LifecycleIntegrationReceipt> sourceReceipts
    ) {
        if (sourceRealm == null || sourceSnapshot == null) {
            return remember(verification(
                ShadowVerification.Status.INVALID_INPUT, epoch(), stateHash, stateHash,
                "shadow verification requires realm and snapshot"
            ));
        }
        String expectedState;
        String expectedBase;
        try {
            List<LifecycleIntegrationReceipt> receipts = normalizeReceipts(
                sourceSnapshot, sourceReceipts
            );
            expectedBase = snapshotHash(sourceRealm, sourceSnapshot);
            expectedState = stateHash(sourceRealm, sourceSnapshot, receipts);
        } catch (IllegalArgumentException invalid) {
            return remember(verification(
                ShadowVerification.Status.INVALID_INPUT, sourceSnapshot.epoch(), stateHash,
                stateHash, invalid.getMessage()
            ));
        }
        if (!realmId.equals(sourceRealm)) {
            return remember(verification(
                ShadowVerification.Status.REALM_MISMATCH, sourceSnapshot.epoch(), expectedState,
                stateHash, "source realm does not match bound shadow realm"
            ));
        }
        if (sourceSnapshot.epoch().compareTo(snapshot.epoch()) < 0) {
            return remember(verification(
                ShadowVerification.Status.ROLLBACK_REJECTED, sourceSnapshot.epoch(),
                expectedState, stateHash, "verification input is older than shadow state"
            ));
        }
        if (expectedState.equals(stateHash) && expectedBase.equals(baseStateHash)
            && sourceSnapshot.equals(snapshot)
            && sameReceipts(sourceReceipts)) {
            return remember(verification(
                ShadowVerification.Status.VERIFIED, sourceSnapshot.epoch(),
                expectedState, stateHash, "shadow state hash and receipt set match"
            ));
        }
        ShadowVerification.Status status = expectedBase.equals(baseStateHash)
            ? ShadowVerification.Status.RECEIPT_DRIFT
            : ShadowVerification.Status.STATE_CONFLICT;
        return remember(verification(
            status, sourceSnapshot.epoch(), expectedState, stateHash,
            status == ShadowVerification.Status.RECEIPT_DRIFT
                ? "lifecycle integration receipt drift"
                : "shadow state differs from authoritative snapshot"
        ));
    }

    @Override
    public synchronized boolean readOnly() {
        return readOnly;
    }

    @Override
    public synchronized long mirrorCount() {
        return mirrorCount;
    }

    @Override
    public synchronized long verificationFailures() {
        return verificationFailures;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag root) {
        return writeImage(root, snapshot, lifecycleReceipts.values(), strengthPolicyConfiguration,
            qualifications, commandJournal, effectJournal);
    }

    private CompoundTag writeImage(CompoundTag root, DominionSnapshot imageSnapshot,
        Collection<LifecycleIntegrationReceipt> imageReceipts, StrengthPolicyConfiguration imagePolicy,
        Map<SubjectKey, SubjectQualification> imageQualifications, RootCommandJournal imageCommands,
        EffectJournal imageEffects) {
        return writeImage(root,imageSnapshot,imageReceipts,imagePolicy,imageQualifications,imageCommands,imageEffects,obligationCheckpoint);
    }
    private CompoundTag writeImage(CompoundTag root, DominionSnapshot imageSnapshot,
        Collection<LifecycleIntegrationReceipt> imageReceipts, StrengthPolicyConfiguration imagePolicy,
        Map<SubjectKey, SubjectQualification> imageQualifications, RootCommandJournal imageCommands,
        EffectJournal imageEffects, dev.ronova.coremod.obligation.ObligationCheckpoint checkpoint) {
        byte[] encodedSnapshot;
        try { encodedSnapshot = NativeSnapshotCodec.encode(imageSnapshot).payload(); }
        catch (RuntimeException invalid) {
            readOnly = true;
            readOnlyReason = normalizedFailure(invalid);
            throw new DurableRootPersistenceException("Root snapshot serialization failed", invalid);
        }
        root.putInt("schema", imageEffects.hasStorageGeneration()?14:imageEffects.hasStorageFileIdentity()?13:imageEffects.hasRecoveryWork()?12:imageEffects.hasLocations()?11:imageEffects.hasRecoveryMutations()?10:ObligationCheckpointCodec.hasOverflowDetail(checkpoint)?9:imageEffects.hasSourceQualifications()?8:checkpoint.restrictions().revision()>0?7:checkpoint.revision()>0?6:5);
        if(checkpoint.revision()>0 || imageEffects.hasSourceQualifications() || imageEffects.hasRecoveryMutations()||imageEffects.hasRecoveryWork())root.put("obligation_checkpoint",ObligationCheckpointCodec.encode(checkpoint));
        root.putString("realm_id", realmId.canonical());
        root.putBoolean(REALM_ID_MIGRATED_TAG, realmIdMigrated);
        root.putBoolean("initialized", initialized);
        root.putBoolean(AUTHORITATIVE_TAG, authoritative);
        root.putLong("epoch_generation", imageSnapshot.epoch().generation());
        root.putLong("epoch_sequence", imageSnapshot.epoch().sequence());
        root.putString("state_hash", stateHash(realmId, encodedSnapshot, imageReceipts));
        root.putString("base_state_hash", snapshotHash(realmId, encodedSnapshot));
        root.putLong("mirror_count", mirrorCount);
        root.putLong("verification_failures", verificationFailures);
        root.putBoolean("read_only", readOnly);
        root.putString("read_only_reason", readOnlyReason);
        root.putBoolean("persistence_pending", persistencePending);
        root.put("strength_policy", StrengthPolicyNbtCodec.encode(
            imagePolicy
        ));
        root.putString("strength_policy_hash", policyHash(imagePolicy));
        root.put("subject_qualifications", QualificationNbtCodec.encode(imageQualifications.values()));
        root.put("effect_journal", imageEffects.encode());

        root.putByteArray("snapshot_payload", encodedSnapshot);

        ListTag ownerTags = new ListTag();
        List<UUID> imageOwners = new ArrayList<>(ownersFromSnapshot(imageSnapshot));
        imageOwners.sort(UUID_ORDER);
        for (UUID owner : imageOwners) {
            CompoundTag row = new CompoundTag();
            row.putUUID("uuid", owner);
            ownerTags.add(row);
        }
        root.put("owners", ownerTags);

        List<TombstoneStore.Record> records = new ArrayList<>(recordsFromSnapshot(imageSnapshot));
        records.sort(Comparator.comparing(TombstoneStore.Record::uuid, UUID_ORDER));
        ListTag tombstoneTags = new ListTag();
        for (TombstoneStore.Record record : records) {
            tombstoneTags.add(record.toTag());
        }
        root.put("tombstones", tombstoneTags);

        ListTag receiptTags = new ListTag();
        for (LifecycleIntegrationReceipt receipt : sortedReceipts(imageReceipts)) {
            receiptTags.add(receipt.toTag());
        }
        root.put("lifecycle_receipts", receiptTags);
        imageCommands.save(root);
        return root;
    }

    public static String snapshotHash(RealmId realmId, DominionSnapshot snapshot) {
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(snapshot, "snapshot");
        return snapshotHash(realmId, NativeSnapshotCodec.encode(snapshot).payload());
    }

    private static String snapshotHash(RealmId realmId, byte[] payload) {
        return digest(output -> {
            output.writeInt(0x52313553); // R15S
            writeUuid(output, realmId.value());
            output.writeInt(payload.length);
            output.write(payload);
        });
    }

    public static String stateHash(
        RealmId realmId,
        DominionSnapshot snapshot,
        Collection<LifecycleIntegrationReceipt> receipts
    ) {
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(snapshot, "snapshot");
        return stateHash(realmId, NativeSnapshotCodec.encode(snapshot).payload(), receipts);
    }

    private static String stateHash(RealmId realmId, byte[] payload, Collection<LifecycleIntegrationReceipt> receipts) {
        return digest(output -> {
            output.writeInt(0x52313546); // R15F
            writeUuid(output, realmId.value());
            output.writeInt(payload.length);
            output.write(payload);
            List<LifecycleIntegrationReceipt> sorted = sortedReceipts(receipts);
            output.writeInt(sorted.size());
            for (LifecycleIntegrationReceipt receipt : sorted) {
                writeUuid(output, receipt.judgmentId());
                writeString(output, receipt.firstPhysicalIdentity());
                output.writeLong(receipt.integratedEpoch().generation());
                output.writeLong(receipt.integratedEpoch().sequence());
            }
        });
    }

    private StagedState stage(DominionCommand command) {
        DominionSnapshot beforeSnapshot = snapshot;
        DominionEpoch before = beforeSnapshot.epoch();
        DominionEpoch after = before;
        LinkedHashSet<UUID> owners = new LinkedHashSet<>(ownerRegistry.snapshot());
        LinkedHashMap<UUID, TombstoneStore.Record> records = new LinkedHashMap<>();
        for (TombstoneStore.Record record : tombstoneStore.snapshot()) {
            records.put(record.uuid(), record);
        }
        LinkedHashMap<UUID, LifecycleIntegrationReceipt> receipts =
            new LinkedHashMap<>(lifecycleReceipts);
        StrengthPolicyConfiguration nextPolicy = strengthPolicyConfiguration;
        Map<SubjectKey, SubjectQualification> nextQualifications = new LinkedHashMap<>(qualifications);

        if (command instanceof DominionCommand.TransitionSubjects transitions) {
            for (SubjectQualification value : transitions.batch().changedQualifications()) {
                nextQualifications.put(value.subjectKey(), value);
                if (!value.terminal()) {
                    records.remove(value.subjectKey().subjectId());
                    receipts.remove(value.subjectKey().subjectId());
                }
                after = before.nextSequence();
            }
        } else if (command instanceof DominionCommand.CommitJudgmentBatch batchCommand) {
            JudgmentBatch batch = batchCommand.batch();
            if (!batch.empty()) {
                after = stageBatch(before, batch, records, receipts, nextQualifications);
            }
        } else if (command instanceof DominionCommand.CommitJudgment judgment) {
            after = stageBatch(
                before,
                JudgmentBatch.of(DominionCommand.commandId(command), judgment.decision()),
                records,
                receipts, nextQualifications
            );
        } else if (command instanceof DominionCommand.BindOwner bind) {
            if (owners.add(bind.owner())) {
                after = before.nextSequence();
            }
        } else if (command instanceof DominionCommand.UnbindOwner unbind) {
            if (owners.remove(unbind.owner())) {
                SubjectKey revokedOwner = new SubjectKey(realmId, unbind.owner());
                nextQualifications.replaceAll((key, value) -> value.revokeOwner(revokedOwner));
                after = before.nextSequence();
            }
        } else if (command instanceof DominionCommand.ClearTombstones) {
            if (!records.isEmpty() || !receipts.isEmpty()) {
                for (UUID subject : records.keySet()) stageQualification(subject, false, nextQualifications);
                records.clear();
                receipts.clear();
                after = before.nextSequence();
            }
        } else if (command instanceof DominionCommand.ClearTombstone clear) {
            if (records.remove(clear.subject()) != null) {
                stageQualification(clear.subject(), false, nextQualifications);
                receipts.remove(clear.subject());
                after = before.nextSequence();
            }
        } else if (command instanceof DominionCommand.ConfigureStrengthPolicy configure) {
            StrengthPolicyConfiguration requested = configure.configuration();
            if (!requested.equals(strengthPolicyConfiguration)) {
                long expectedRevision = strengthPolicyConfiguration.revision().next().value();
                if (requested.revision().value() != expectedRevision) {
                    throw new IllegalStateException(
                        "strength policy revision mismatch: expected "
                            + expectedRevision + ", received "
                            + requested.revision().value()
                    );
                }
                nextPolicy = requested;
                after = before.nextSequence();
            }
        } else {
            throw new IllegalArgumentException("unsupported command: " + command);
        }

        DominionSnapshot next = assembleSnapshot(after, owners, records.values());
        return new StagedState(
            beforeSnapshot, next, List.copyOf(receipts.values()), nextPolicy,
            Map.copyOf(nextQualifications), nextQualifications.values().stream()
                .filter(value -> !value.equals(qualifications.get(value.subjectKey())))
                .sorted(Comparator.comparing(SubjectQualification::subjectKey)).toList()
        );
    }

    private DominionEpoch stageBatch(
        DominionEpoch before,
        JudgmentBatch batch,
        Map<UUID, TombstoneStore.Record> records,
        Map<UUID, LifecycleIntegrationReceipt> receipts,
        Map<SubjectKey, SubjectQualification> nextQualifications
    ) {
        DominionEpoch expected = before.nextSequence();
        if (!expected.equals(batch.epoch())) {
            throw new IllegalStateException(
                "judgment batch epoch mismatch: expected " + expected
                    + ", received " + batch.epoch()
            );
        }
        for (DominionDecision.Judgment judgment : batch.judgments()) {
            SubjectIdentity subject = judgment.subject();
            if (subject.uuid() == null) {
                throw new IllegalArgumentException("committed judgment requires a UUID");
            }
            if (ownerRegistry.contains(subject.uuid())) {
                throw new IllegalArgumentException("batch targets a protected owner");
            }
            SubjectKey key = new SubjectKey(realmId, subject.uuid());
            SubjectQualification previous = nextQualifications.getOrDefault(key,
                dev.ronova.coremod.root.api.QualificationPreconditions.baseline(key));
            nextQualifications.put(key, previous.judgmentTransition(batch.incarnationNonces().get(subject.uuid())));
            JudgmentScope scope = toEngineScope(judgment.scope());
            records.put(subject.uuid(), new TombstoneStore.Record(
                subject.uuid(), subject.registryType(), subject.runtimeClass(),
                subject.dimension(), subject.x(), subject.y(), subject.z(),
                scope == JudgmentScope.IDENTITY ? judgment.radius() : 0.0D,
                scope, expected.sequence(), subject.player()
            ));
            receipts.remove(subject.uuid());
        }
        return expected;
    }

    private void stageQualification(UUID subject, boolean terminal,
        Map<SubjectKey, SubjectQualification> staged) {
        SubjectKey key = new SubjectKey(realmId, subject);
        SubjectQualification before = staged.getOrDefault(key,
            new SubjectQualification(new TargetEpoch(key, 0L), false));
        staged.put(key, before.transition(terminal));
    }

    /**
     * Makes the current state+journal image durable before any caller may
     * observe COMMITTED. A null backend exists only in isolated unit fixtures;
     * every server-opened store is attached to a world-local file backend.
     */
    private boolean persistAuthoritative() {
        return persistAuthoritative(null);
    }

    private DurableRootFileBackend.PreparedWrite preflight(CompoundTag image, EffectJournal effects, boolean acceptingNew) {
        return durableBackend == null ? null : durableBackend.prepare(image, effects.reservedGrowthBytes(), acceptingNew);
    }

    private boolean persistAuthoritative(DurableRootFileBackend.PreparedWrite preparedWrite) {
        if (durableBackend != null && authoritative && !durableBackend.exists()) {
            persistencePending = true;
        }
        if (!persistencePending) {
            return true;
        }
        if (!authoritative) {
            persistenceFailure = "store is not the authoritative 7.16 writer";
            return false;
        }
        if (durableBackend == null) {
            persistencePending = false;
            persistenceFailure = "";
            acknowledgeReadHead();
            setDirty();
            return true;
        }

        boolean pending = persistencePending;
        CompoundTag image;
        persistencePending = false;
        try {
            image = preparedWrite == null ? save(new CompoundTag()) : null;
        } catch (RuntimeException failure) {
            persistencePending = pending;
            persistenceFailure = normalizedFailure(failure);
            setDirty();
            return false;
        } finally {
            persistencePending = pending;
        }
        if (readOnly) {
            persistencePending = true;
            persistenceFailure = normalize(
                readOnlyReason, "durable Root serialization fenced the store"
            );
            setDirty();
            return false;
        }
        try {
            durableBackend.write(preparedWrite == null ? preflight(image, effectJournal, false) : preparedWrite);
            persistencePending = false;
            persistenceFailure = "";
            acknowledgeReadHead();
            setDirty();
            return true;
        } catch (DurableRootPersistenceException | StorageCapacityException failure) {
            persistencePending = true;
            persistenceFailure = normalizedFailure(failure);
            setDirty();
            return false;
        }
    }

    private static String normalizedFailure(RuntimeException failure) {
        return normalize(failure.getMessage(), failure.getClass().getSimpleName());
    }

    private boolean consumeCommitFailpoint(RootCommitFailpoint expected) {
        if (commitFailpoint != expected) {
            return false;
        }
        commitFailpoint = RootCommitFailpoint.NONE;
        return true;
    }

    private void restore(CompoundTag root) {
        if (root == null) {
            fenceReadOnly("missing shadow root");
            return;
        }
        int storedSchema = root.contains("schema", Tag.TAG_INT)
            ? root.getInt("schema") : 1;
        if (storedSchema < 1 || storedSchema > SCHEMA) {
            fenceReadOnly("unsupported shadow schema");
        }
        initialized = root.getBoolean("initialized");
        authoritative = storedSchema >= 2 && root.getBoolean(AUTHORITATIVE_TAG);
        mirrorCount = nonNegative(root.getLong("mirror_count"));
        verificationFailures = nonNegative(root.getLong("verification_failures"));
        readOnly = readOnly || root.getBoolean("read_only");
        readOnlyReason = normalize(root.getString("read_only_reason"), readOnlyReason);
        persistencePending = root.getBoolean("persistence_pending");

        boolean hasStrengthPolicy = root.contains("strength_policy");
        boolean migratedStrengthPolicy = storedSchema < 3 && !hasStrengthPolicy;
        if (hasStrengthPolicy && !root.contains("strength_policy", Tag.TAG_COMPOUND)) {
            strengthPolicyConfiguration = StrengthPolicyConfiguration.defaults();
            readOnly = true;
            readOnlyReason = "strength policy has an invalid NBT type";
        } else if (root.contains("strength_policy", Tag.TAG_COMPOUND)) {
            try {
                StrengthPolicyNbtCodec.Decoded decodedPolicy =
                    StrengthPolicyNbtCodec.decodeWithMetadata(
                    root.getCompound("strength_policy")
                );
                strengthPolicyConfiguration = decodedPolicy.configuration();
                String storedPolicyHash = root.getString("strength_policy_hash");
                String actualPolicyHash = policyHashForSchema(
                    strengthPolicyConfiguration, decodedPolicy.sourceSchema()
                );
                if (storedPolicyHash.isEmpty() || !storedPolicyHash.equals(actualPolicyHash)) {
                    readOnly = true;
                    readOnlyReason = "strength policy hash mismatch during load";
                }
                migratedStrengthPolicy = migratedStrengthPolicy
                    || decodedPolicy.migrated();
            } catch (RuntimeException malformedPolicy) {
                strengthPolicyConfiguration = StrengthPolicyConfiguration.defaults();
                readOnly = true;
                readOnlyReason = normalize(
                    malformedPolicy.getMessage(), "malformed strength policy payload"
                );
            }
        } else if (storedSchema >= 3) {
            strengthPolicyConfiguration = StrengthPolicyConfiguration.defaults();
            readOnly = true;
            readOnlyReason = "schema 3 durable root is missing strength policy";
        } else {
            strengthPolicyConfiguration = StrengthPolicyConfiguration.defaults();
        }
        // Loading is the activation boundary; later ConfigureStrengthPolicy
        // commands update only the durable request until the next bootstrap.
        activeStrengthPolicyConfiguration = strengthPolicyConfiguration;

        DominionSnapshot restored = null;
        if (root.contains("snapshot_payload", Tag.TAG_BYTE_ARRAY)) {
            byte[] payload = root.getByteArray("snapshot_payload");
            if (payload.length > 0) {
                try {
                    restored = NativeSnapshotCodec.decode(payload);
                } catch (RuntimeException malformed) {
                    readOnly = true;
                    readOnlyReason = normalize(
                        malformed.getMessage(), "malformed shadow snapshot payload"
                    );
                }
            }
        }
        DominionEpoch persistedEpoch = readEpoch(root);
        if (restored == null) {
            restored = assembleSnapshot(persistedEpoch, readOwners(root), readRecords(root));
        }
        snapshot = restored;
        epochClock = new EpochClock(snapshot.epoch());
        ownerRegistry = new OwnerRegistry();
        ownerRegistry.replaceAll(ownersFromSnapshot(snapshot));
        tombstoneStore = new TombstoneStore();
        tombstoneStore.replaceAll(recordsFromSnapshot(snapshot));
        try {
            lifecycleReceipts = readReceipts(root);
            normalizeReceipts(snapshot, lifecycleReceipts.values());
        } catch (IllegalArgumentException invalidReceipts) {
            lifecycleReceipts = new LinkedHashMap<>();
            readOnly = true;
            readOnlyReason = normalize(
                invalidReceipts.getMessage(), "invalid shadow lifecycle receipts"
            );
        }
        commandJournal = new RootCommandJournal();
        try {
            commandJournal.restore(root, snapshot);
        } catch (RuntimeException invalidJournal) {
            readOnly = true;
            readOnlyReason = normalize(
                invalidJournal.getMessage(), "invalid durable command journal"
            );
        }

        try {
            Map<SubjectKey, SubjectQualification> restoredQualifications = new LinkedHashMap<>();
            if (storedSchema >= 4) {
                if (!root.contains("subject_qualifications", Tag.TAG_COMPOUND)) {
                    throw new IllegalArgumentException("schema 4 Root is missing qualifications");
                }
                for (SubjectQualification value : QualificationNbtCodec.decode(root.getCompound("subject_qualifications"))) {
                    if (!value.subjectKey().realmId().equals(realmId)
                        || value.terminal() != tombstoneStore.contains(value.subjectKey().subjectId())) {
                        throw new IllegalArgumentException("qualification identity/state differs from Root truth");
                    }
                    restoredQualifications.put(value.subjectKey(), value);
                }
                for (TombstoneStore.Record record : tombstoneStore.snapshot()) {
                    if (!restoredQualifications.containsKey(new SubjectKey(realmId, record.uuid()))) {
                        throw new IllegalArgumentException("tombstone lacks its subject qualification");
                    }
                }
            } else {
                // Explicit legacy mapping: the store Realm plus its exact UUID key.
                // Generation zero creates no historical execution receipt or physical binding.
                for (TombstoneStore.Record record : tombstoneStore.snapshot()) {
                    SubjectKey key = new SubjectKey(realmId, record.uuid());
                    restoredQualifications.put(key, new SubjectQualification(new TargetEpoch(key, 0L), true));
                }
            }
            qualifications = Map.copyOf(restoredQualifications);
            commandJournal.validateQualifications(realmId, qualifications);
        } catch (RuntimeException malformedQualifications) {
            readOnly = true;
            readOnlyReason = normalizedFailure(malformedQualifications);
        }

        if (!snapshot.epoch().equals(persistedEpoch)) {
            readOnly = true;
            readOnlyReason = "shadow epoch metadata differs from snapshot";
        }
        String actualBase = snapshotHash(realmId, snapshot);
        boolean recoveredEffects = false;
        try {
            if (storedSchema >= 5) {
                if (!root.contains("effect_journal", Tag.TAG_COMPOUND)) throw new IllegalArgumentException("schema 5 Root is missing effect journal");
                effectJournal = EffectJournal.decode(root.getCompound("effect_journal"), realmId, commandJournal);
                if ((storedSchema>=12)!=effectJournal.hasRecoveryWork()||(storedSchema>=13)!=effectJournal.hasStorageFileIdentity()
                    ||(storedSchema==14)!=effectJournal.hasStorageGeneration())throw new IllegalArgumentException("Root/recovery work schema mismatch");
                if (storedSchema<12&&(storedSchema >= 11) != effectJournal.hasLocations())
                    throw new IllegalArgumentException("Root/source location schema mismatch");
                if (storedSchema<12&&(storedSchema >= 10) != effectJournal.hasRecoveryMutations())
                    throw new IllegalArgumentException("Root/recovery mutation schema mismatch");
                if (storedSchema==8&&!effectJournal.hasSourceQualifications() || storedSchema<8&&effectJournal.hasSourceQualifications())
                    throw new IllegalArgumentException("Root/source qualification schema mismatch");
                recoveredEffects = effectJournal.recoverPrepared();
            } else {
                if (root.contains("effect_journal")) throw new IllegalArgumentException("effect journal appears under an older Root schema");
                effectJournal = new EffectJournal();
            }
        } catch (RuntimeException malformedEffects) {
            readOnly = true;
            readOnlyReason = normalizedFailure(malformedEffects);
        }
        try {
            if(storedSchema>=6) {
                if(!root.contains("obligation_checkpoint",Tag.TAG_COMPOUND))throw new IllegalArgumentException("schema 6 Root lacks obligations");
                obligationCheckpoint=ObligationCheckpointCodec.decode(root.getCompound("obligation_checkpoint"));
                if((storedSchema==9&&!ObligationCheckpointCodec.hasOverflowDetail(obligationCheckpoint))
                    ||storedSchema<9&&ObligationCheckpointCodec.hasOverflowDetail(obligationCheckpoint)
                    ||storedSchema<8&&obligationCheckpoint.revision()==0)
                    throw new IllegalArgumentException("Root/overflow checkpoint schema mismatch");
                if (storedSchema < 8 && (storedSchema >= 7) != (obligationCheckpoint.restrictions().revision() > 0))
                    throw new IllegalArgumentException("Root/restriction schema mismatch");
                validateObligationCheckpoint(obligationCheckpoint);
            }else if(root.contains("obligation_checkpoint"))throw new IllegalArgumentException("obligation checkpoint under older schema");
        }catch(RuntimeException malformed) {readOnly=true;readOnlyReason=normalizedFailure(malformed);}
        String actualState = stateHash(realmId, snapshot, lifecycleReceipts.values());
        String storedBase = root.getString("base_state_hash");
        String storedState = root.getString("state_hash");
        baseStateHash = actualBase;
        stateHash = actualState;
        if ((!storedBase.isEmpty() && !storedBase.equals(actualBase))
            || (!storedState.isEmpty() && !storedState.equals(actualState))) {
            readOnly = true;
            readOnlyReason = "shadow state hash mismatch during load";
        }
        acknowledgeReadHead();
        lastVerification = verification(
            readOnly ? ShadowVerification.Status.STATE_CONFLICT : ShadowVerification.Status.VERIFIED,
            snapshot.epoch(), actualState, actualState,
            readOnly ? readOnlyReason : "shadow state restored"
        );
        if ((migratedStrengthPolicy || storedSchema < 5 || recoveredEffects) && !readOnly) {
            persistencePending = true;
            setDirty();
        }
        acknowledgeReadHead();
    }

    private void applyState(
        DominionSnapshot sourceSnapshot,
        Collection<LifecycleIntegrationReceipt> sourceReceipts
    ) {
        if (sourceSnapshot == null) {
            throw new IllegalArgumentException("source snapshot is required");
        }
        snapshot = sourceSnapshot;
        epochClock = new EpochClock(sourceSnapshot.epoch());
        ownerRegistry = new OwnerRegistry();
        ownerRegistry.replaceAll(ownersFromSnapshot(sourceSnapshot));
        tombstoneStore = new TombstoneStore();
        tombstoneStore.replaceAll(recordsFromSnapshot(sourceSnapshot));
        lifecycleReceipts = toReceiptMap(sourceReceipts);
        normalizeReceipts(sourceSnapshot, lifecycleReceipts.values());
        byte[] encoded = NativeSnapshotCodec.encode(sourceSnapshot).payload();
        baseStateHash = snapshotHash(realmId, encoded);
        stateHash = stateHash(realmId, encoded, lifecycleReceipts.values());
        initialized = true;
        if (!authoritative) {
            Map<SubjectKey, SubjectQualification> legacy = new LinkedHashMap<>();
            for (TombstoneStore.Record record : tombstoneStore.snapshot()) {
                SubjectKey key = new SubjectKey(realmId, record.uuid());
                legacy.put(key, new SubjectQualification(new TargetEpoch(key, 0L), true));
            }
            qualifications = Map.copyOf(legacy);
            acknowledgeReadHead();
        }
    }

    private void acknowledgeReadHead() {
        readHead = new ReadHead(snapshot, ownerRegistry, tombstoneStore,
            strengthPolicyConfiguration, stateHash, baseStateHash, commandJournal.commitIndex(), qualifications,
            commandJournal.ownerAuthorizationRevisions(), obligationCheckpoint,
            dev.ronova.coremod.contract.SubjectResolver.bindingIndex(qualifications.values()));
    }

    private record ReadHead(
        DominionSnapshot snapshot, OwnerRegistry owners, TombstoneStore tombstones,
        StrengthPolicyConfiguration policy, String stateHash, String baseStateHash,
        long commitIndex, Map<SubjectKey, SubjectQualification> qualifications, Map<UUID, Long> ownerAuthorizationRevisions,
        dev.ronova.coremod.obligation.ObligationCheckpoint obligations, Map<UUID,java.util.Set<SubjectKey>> incarnations
    ) {}

    private ShadowVerification remember(ShadowVerification result) {
        lastVerification = result;
        if (result.rejected()) {
            verificationFailures++;
        }
        return result;
    }

    private void triggerFailpoint(
        CrashFailpoint point,
        DominionEpoch expectedEpoch,
        String expectedHash
    ) {
        if (failpoint != point) {
            return;
        }
        failpoint = CrashFailpoint.NONE;
        lastVerification = verification(
            ShadowVerification.Status.FAILPOINT, expectedEpoch, expectedHash, stateHash,
            "shadow failpoint interrupted operation: " + point
        );
        throw new ShadowCrashException(point);
    }

    private ShadowVerification verification(
        ShadowVerification.Status status,
        DominionEpoch expectedEpoch,
        String expectedHash,
        String observedHash,
        String detail
    ) {
        return new ShadowVerification(
            status, expectedEpoch, epoch(), expectedHash, observedHash, detail
        );
    }

    private boolean sameReceipts(Collection<LifecycleIntegrationReceipt> values) {
        return toReceiptMap(values).equals(lifecycleReceipts);
    }

    /** Same-epoch mirrors may append receipts, but never delete or rewrite one. */
    private boolean receiptSetExtends(Collection<LifecycleIntegrationReceipt> values) {
        Map<UUID, LifecycleIntegrationReceipt> incoming = toReceiptMap(values);
        for (Map.Entry<UUID, LifecycleIntegrationReceipt> existing
            : lifecycleReceipts.entrySet()) {
            if (!existing.getValue().equals(incoming.get(existing.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private static DominionSnapshot initialSnapshot() {
        return new DominionSnapshot(DominionEpoch.LOCAL_INITIAL, List.of());
    }

    private static RealmLoad readRealm(
        CompoundTag root, RealmId expectedRealm, boolean migrationMarker
    ) {
        if (root == null || !root.contains("realm_id")) {
            if (migrationMarker) {
                throw new RealmIdLoadException("shadow realm_id is missing after migration");
            }
            return new RealmLoad(
                expectedRealm == null ? RealmId.random() : expectedRealm, true
            );
        }
        if (!root.contains("realm_id", Tag.TAG_STRING)) {
            throw new RealmIdLoadException(
                "shadow realm_id has an invalid NBT type: " + root.getTagType("realm_id")
            );
        }
        try {
            return new RealmLoad(RealmId.fromString(root.getString("realm_id")), false);
        } catch (RuntimeException invalid) {
            throw new RealmIdLoadException(
                "shadow realm_id is not a canonical UUID", invalid
            );
        }
    }

    private static boolean readRealmIdMigratedMarker(CompoundTag root) {
        if (root == null || !root.contains(REALM_ID_MIGRATED_TAG)) {
            return false;
        }
        if (!root.contains(REALM_ID_MIGRATED_TAG, Tag.TAG_BYTE)) {
            throw new RealmIdLoadException(
                "shadow realm_id_migrated has an invalid NBT type: "
                    + root.getTagType(REALM_ID_MIGRATED_TAG)
            );
        }
        return root.getBoolean(REALM_ID_MIGRATED_TAG);
    }

    private record RealmLoad(RealmId realmId, boolean migrated) {}

    private record StagedState(
        DominionSnapshot beforeSnapshot,
        DominionSnapshot snapshot,
        List<LifecycleIntegrationReceipt> receipts,
        StrengthPolicyConfiguration strengthPolicyConfiguration,
        Map<SubjectKey, SubjectQualification> qualifications,
        List<SubjectQualification> issuedQualifications
    ) {}

    private static DominionEpoch readEpoch(CompoundTag root) {
        if (root == null) {
            return DominionEpoch.LOCAL_INITIAL;
        }
        try {
            return new DominionEpoch(
                Math.max(0L, root.getLong("epoch_generation")),
                Math.max(0L, root.getLong("epoch_sequence"))
            );
        } catch (IllegalArgumentException ignored) {
            return DominionEpoch.LOCAL_INITIAL;
        }
    }

    private static List<UUID> readOwners(CompoundTag root) {
        List<UUID> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        ListTag rows = root.getList("owners", Tag.TAG_COMPOUND);
        if (rows.size() > MAX_NBT_ROWS) {
            throw new IllegalArgumentException("shadow owner row limit exceeded");
        }
        for (Tag raw : rows) {
            CompoundTag row = (CompoundTag) raw;
            if (row.hasUUID("uuid")) {
                result.add(row.getUUID("uuid"));
            }
        }
        return List.copyOf(result);
    }

    private static List<TombstoneStore.Record> readRecords(CompoundTag root) {
        List<TombstoneStore.Record> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        ListTag rows = root.getList("tombstones", Tag.TAG_COMPOUND);
        if (rows.size() > MAX_NBT_ROWS) {
            throw new IllegalArgumentException("shadow tombstone row limit exceeded");
        }
        for (Tag raw : rows) {
            TombstoneStore.Record record = TombstoneStore.Record.fromTag((CompoundTag) raw);
            if (record != null) {
                result.add(record);
            }
        }
        return List.copyOf(result);
    }

    private static LinkedHashMap<UUID, LifecycleIntegrationReceipt> readReceipts(
        CompoundTag root
    ) {
        LinkedHashMap<UUID, LifecycleIntegrationReceipt> result = new LinkedHashMap<>();
        if (root == null) {
            return result;
        }
        ListTag rows = root.getList("lifecycle_receipts", Tag.TAG_COMPOUND);
        if (rows.size() > MAX_NBT_ROWS) {
            throw new IllegalArgumentException("shadow receipt row limit exceeded");
        }
        for (Tag raw : rows) {
            LifecycleIntegrationReceipt receipt =
                LifecycleIntegrationReceipt.fromTag((CompoundTag) raw);
            if (receipt != null) {
                LifecycleIntegrationReceipt previous = result.put(receipt.judgmentId(), receipt);
                if (previous != null && !previous.equals(receipt)) {
                    throw new IllegalArgumentException("conflicting shadow lifecycle receipt");
                }
            }
        }
        return result;
    }

    private static DominionSnapshot assembleSnapshot(
        DominionEpoch epoch,
        Collection<UUID> owners,
        Collection<TombstoneStore.Record> records
    ) {
        List<DominionDecision> decisions = new ArrayList<>();
        if (owners != null) {
            List<UUID> sortedOwners = new ArrayList<>(owners);
            sortedOwners.sort(UUID_ORDER);
            for (UUID owner : sortedOwners) {
                SubjectIdentity subject = new SubjectIdentity(
                    owner, "minecraft:player", "net.minecraft.server.level.ServerPlayer",
                    "unknown:unknown", 0.0D, 0.0D, 0.0D, true
                );
                decisions.add(new DominionDecision.Authority(
                    subject, epoch, true, CapabilitySet.OWNER_DEFAULT, "shadow-persisted-owner"
                ));
            }
        }
        if (records != null) {
            List<TombstoneStore.Record> sortedRecords = new ArrayList<>(records);
            sortedRecords.sort(Comparator.comparing(TombstoneStore.Record::uuid, UUID_ORDER));
            for (TombstoneStore.Record record : sortedRecords) {
                decisions.add(record.toDecision(epoch));
            }
        }
        return new DominionSnapshot(epoch, decisions);
    }

    private static List<UUID> ownersFromSnapshot(DominionSnapshot sourceSnapshot) {
        List<UUID> result = new ArrayList<>();
        for (DominionDecision.Authority authority : sourceSnapshot.authorities()) {
            if (authority.authorized() && authority.subject().uuid() != null) {
                result.add(authority.subject().uuid());
            }
        }
        return List.copyOf(result);
    }

    private static List<TombstoneStore.Record> recordsFromSnapshot(
        DominionSnapshot sourceSnapshot
    ) {
        List<TombstoneStore.Record> result = new ArrayList<>();
        for (DominionDecision.Judgment judgment : sourceSnapshot.judgments()) {
            SubjectIdentity subject = judgment.subject();
            if (!judgment.committed() || subject.uuid() == null) {
                continue;
            }
            JudgmentScope scope = toEngineScope(judgment.scope());
            result.add(new TombstoneStore.Record(
                subject.uuid(), subject.registryType(), subject.runtimeClass(), subject.dimension(),
                subject.x(), subject.y(), subject.z(),
                scope == JudgmentScope.IDENTITY ? judgment.radius() : 0.0D,
                scope, judgment.epoch().sequence(), subject.player()
            ));
        }
        return List.copyOf(result);
    }

    private static LinkedHashMap<UUID, LifecycleIntegrationReceipt> toReceiptMap(
        Collection<LifecycleIntegrationReceipt> values
    ) {
        LinkedHashMap<UUID, LifecycleIntegrationReceipt> result = new LinkedHashMap<>();
        if (values == null) {
            return result;
        }
        for (LifecycleIntegrationReceipt value : values) {
            if (value == null) {
                continue;
            }
            LifecycleIntegrationReceipt previous = result.put(value.judgmentId(), value);
            if (previous != null && !previous.equals(value)) {
                throw new IllegalArgumentException("conflicting lifecycle integration receipt");
            }
        }
        return result;
    }

    private static List<LifecycleIntegrationReceipt> normalizeReceipts(
        DominionSnapshot sourceSnapshot,
        Collection<LifecycleIntegrationReceipt> values
    ) {
        Objects.requireNonNull(sourceSnapshot, "sourceSnapshot");
        LinkedHashMap<UUID, LifecycleIntegrationReceipt> normalized = toReceiptMap(values);
        Map<UUID, DominionDecision.Judgment> judgments = new LinkedHashMap<>();
        for (DominionDecision.Judgment judgment : sourceSnapshot.judgments()) {
            if (judgment.committed() && judgment.subject().uuid() != null) {
                judgments.put(judgment.subject().uuid(), judgment);
            }
        }
        for (LifecycleIntegrationReceipt receipt : normalized.values()) {
            DominionDecision.Judgment judgment = judgments.get(receipt.judgmentId());
            if (judgment == null) {
                throw new IllegalArgumentException(
                    "lifecycle receipt has no committed judgment: " + receipt.judgmentId()
                );
            }
            if (receipt.integratedEpoch().compareTo(sourceSnapshot.epoch()) > 0) {
                throw new IllegalArgumentException(
                    "lifecycle receipt is newer than its snapshot: " + receipt.judgmentId()
                );
            }
        }
        return sortedReceipts(normalized.values());
    }

    private static List<LifecycleIntegrationReceipt> sortedReceipts(
        Collection<LifecycleIntegrationReceipt> values
    ) {
        List<LifecycleIntegrationReceipt> result = new ArrayList<>();
        if (values != null) {
            for (LifecycleIntegrationReceipt value : values) {
                if (value != null) {
                    result.add(value);
                }
            }
        }
        result.sort(Comparator.comparing(LifecycleIntegrationReceipt::judgmentId, UUID_ORDER));
        return List.copyOf(result);
    }

    private static JudgmentScope toEngineScope(DominionDecision.LineageScope scope) {
        return switch (scope) {
            case IDENTITY -> JudgmentScope.IDENTITY;
            case LOCAL_LINEAGE -> JudgmentScope.LOCAL_LINEAGE;
            case GLOBAL_LINEAGE -> JudgmentScope.GLOBAL_LINEAGE;
        };
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }

    private static String normalize(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String digest(IoWriter writer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writer.write(output);
            }
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray())
            );
        } catch (IOException impossible) {
            throw new IllegalStateException("shadow hash encoding failed", impossible);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String policyHash(StrengthPolicyConfiguration configuration) {
        return policyHashForSchema(
            configuration, StrengthPolicySnapshot.SCHEMA_VERSION
        );
    }

    static String policyHashForSchema(
        StrengthPolicyConfiguration configuration,
        int schemaVersion
    ) {
        Objects.requireNonNull(configuration, "configuration");
        return digest(output -> {
            output.writeInt(0x52313650); // R16P
            writeString(output, configuration.canonicalKeyForSchema(schemaVersion));
        });
    }

    private static void writeUuid(DataOutputStream output, UUID uuid) throws IOException {
        output.writeLong(uuid.getMostSignificantBits());
        output.writeLong(uuid.getLeastSignificantBits());
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    @FunctionalInterface
    private interface IoWriter {
        void write(DataOutputStream output) throws IOException;
    }
}
