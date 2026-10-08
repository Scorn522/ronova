package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.root.api.JudgmentBatch;
import dev.ronova.coremod.root.api.SubjectQualification;
import dev.ronova.coremod.root.api.TargetEpoch;
import dev.ronova.coremod.root.api.EffectRecord;
import dev.ronova.coremod.contract.SubjectKey;
import dev.ronova.coremod.contract.RealmId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * Durable command-id journal used by the 7.16 Java Root writer. It is deliberately
 * independent of projections: its only job is replay identity and receipt recovery.
 */
public final class RootCommandJournal {
    private static final String ENTRIES = "command_journal_v1";
    private static final String COMMIT_INDEX = "command_commit_index";
    private static final String FORMAT = "command_journal_format";
    private static final int CURRENT_FORMAT = 3;
    private static final int MAX_ROWS = 1_000_000;
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private final Map<SubjectKey, QualificationAuthority> latestQualifications = new LinkedHashMap<>();
    private final Map<UUID, Long> ownerAuthorizationRevisions = new LinkedHashMap<>();
    private final java.util.NavigableMap<Long, dev.ronova.coremod.policy.StrengthPolicyConfiguration> policyHistory = new TreeMap<>();
    private long commitIndex;
    private DominionSnapshot authoritativeSnapshot;

    public synchronized long commitIndex() {
        return commitIndex;
    }

    /**
     * Looks up a resolved receipt by stable ingress identity.  This overload is
     * read-only; payload conflict checking still belongs to commit/recover with
     * a canonical hash.
     */
    public synchronized CommitReceipt find(UUID commandId) {
        if (commandId == null) {
            return null;
        }
        Entry entry = entries.get(commandId);
        return entry == null ? null : entry.receipt(authoritativeSnapshot);
    }

    public synchronized CommitReceipt find(UUID commandId, String payloadHash) {
        CommitReceipt receipt = find(commandId);
        if (receipt == null) {
            return null;
        }
        String expected = normalizeHash(payloadHash);
        Entry entry = entries.get(commandId);
        if (!entry.payloadHash().equalsIgnoreCase(expected)
            && !DominionCommand.matchesCanonicalPayloadHash(
                receipt.command(), expected
            )) {
            throw new CommandIdConflictException(commandId.toString());
        }
        return receipt;
    }

    /** Records one resolved durable result and returns its canonical receipt. */
    public synchronized CommitReceipt append(
        DominionCommand command,
        DominionEpoch before,
        DominionSnapshot snapshot,
        CommitReceipt.Status status
    ) {
        return append(command, before, snapshot, status, List.of());
    }

    public synchronized CommitReceipt append(
        DominionCommand command, DominionEpoch before, DominionSnapshot snapshot,
        CommitReceipt.Status status, List<SubjectQualification> qualifications
    ) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(status, "status");
        if (status == CommitReceipt.Status.COMMIT_UNKNOWN) {
            throw new IllegalArgumentException("unresolved receipts are not durable journal rows");
        }
        UUID commandId = commandId(command);
        String payloadHash = payloadHash(command);
        Entry existing = entries.get(commandId);
        if (existing != null) {
            if (!existing.payloadHash().equalsIgnoreCase(payloadHash)) {
                throw new CommandIdConflictException(commandId.toString());
            }
            return existing.receipt(authoritativeSnapshot);
        }
        if (entries.size() >= MAX_ROWS) {
            throw new IllegalStateException("command journal row limit exceeded");
        }

        // Complete every operation that can fail before changing the journal head.
        CompoundTag encodedCommand = commandTag(command);
        dev.ronova.coremod.nativebridge.NativeSnapshotCodec.encode(snapshot);
        DominionEpoch after = snapshot.epoch();
        boolean mutation = status == CommitReceipt.Status.COMMITTED
            && after.compareTo(before) > 0;
        long nextCommitIndex = commitIndex;
        if (mutation) {
            if (nextCommitIndex == Long.MAX_VALUE) {
                throw new IllegalStateException("durable commit index exhausted");
            }
            nextCommitIndex++;
        }
        CommitReceipt receipt = new CommitReceipt(
            command, before, after, snapshot, commandId, payloadHash, nextCommitIndex, status, qualifications
        );
        Entry next = Entry.from(receipt, encodedCommand);
        authoritativeSnapshot = snapshot;
        commitIndex = nextCommitIndex;
        entries.put(commandId, next);
        indexQualifications(next);
        return receipt;
    }

    /**
     * Restores compact rows against the one authoritative Root snapshot.
     * Format-1 rows remain readable and their embedded historical snapshot is
     * validated during migration; format-2 rows never duplicate that payload.
     */
    public synchronized void restore(
        CompoundTag root,
        DominionSnapshot authoritativeSnapshot
    ) {
        entries.clear();
        latestQualifications.clear();
        ownerAuthorizationRevisions.clear();
        policyHistory.clear();
        commitIndex = root == null ? 0L : nonNegative(root.getLong(COMMIT_INDEX));
        this.authoritativeSnapshot = authoritativeSnapshot;
        if (root == null || !root.contains(ENTRIES, Tag.TAG_LIST)) {
            return;
        }
        int format = root.contains(FORMAT, Tag.TAG_INT)
            ? root.getInt(FORMAT) : 1;
        if (format < 1 || format > CURRENT_FORMAT) {
            throw new IllegalArgumentException("unsupported command journal format");
        }
        if (format >= 2) {
            Objects.requireNonNull(
                authoritativeSnapshot, "authoritativeSnapshot"
            );
        }
        ListTag rows = root.getList(ENTRIES, Tag.TAG_COMPOUND);
        if (rows.size() > MAX_ROWS) {
            throw new IllegalArgumentException("command journal row limit exceeded");
        }
        long priorRowIndex = 0L;
        long maximumRowIndex = 0L;
        for (Tag raw : rows) {
            Entry entry = readEntry(
                (CompoundTag) raw,
                format >= 2 ? authoritativeSnapshot : null,
                format >= 2, format >= 3
            );
            if (entry == null) {
                throw new IllegalArgumentException("malformed durable command journal entry");
            }
            Entry previous = entries.put(entry.commandId(), entry);
            indexQualifications(entry);
            if (previous != null && !previous.equals(entry)) {
                throw new CommandIdConflictException(entry.commandId().toString());
            }
            long rowIndex = entry.commitIndex();
            if (rowIndex < priorRowIndex || rowIndex > commitIndex) {
                throw new IllegalArgumentException(
                    "command journal commit index is inconsistent"
                );
            }
            priorRowIndex = rowIndex;
            maximumRowIndex = Math.max(maximumRowIndex, rowIndex);
        }
        if (maximumRowIndex != commitIndex) {
            throw new IllegalArgumentException(
                "command journal head differs from its rows"
            );
        }
    }

    public synchronized void save(CompoundTag root) {
        Objects.requireNonNull(root, "root");
        root.putInt(FORMAT, CURRENT_FORMAT);
        root.putLong(COMMIT_INDEX, commitIndex);
        ListTag rows = new ListTag();
        for (Entry entry : entries.values()) {
            rows.add(writeEntry(entry));
        }
        root.put(ENTRIES, rows);
    }

    /**
     * Returns an isolated journal copy for a staged transaction.  Entry tags are
     * copied as well because NBT compounds are mutable.
     */
    public synchronized RootCommandJournal copy() {
        RootCommandJournal copy = new RootCommandJournal();
        copy.commitIndex = commitIndex;
        copy.authoritativeSnapshot = authoritativeSnapshot;
        copy.latestQualifications.putAll(latestQualifications);
        copy.ownerAuthorizationRevisions.putAll(ownerAuthorizationRevisions);
        copy.policyHistory.putAll(policyHistory);
        for (Map.Entry<UUID, Entry> value : entries.entrySet()) {
            Entry entry = value.getValue();
            copy.entries.put(value.getKey(), new Entry(
                entry.commandId(), entry.payloadHash(), entry.command(),
                entry.before(), entry.after(), entry.commitIndex(), entry.status(),
                entry.commandTag().copy(), entry.qualifications()
            ));
        }
        return copy;
    }

    /** Installs a previously staged journal while the owning store is locked. */
    public synchronized void replaceFrom(RootCommandJournal staged) {
        Objects.requireNonNull(staged, "staged");
        RootCommandJournal copy = staged.copy();
        entries.clear();
        entries.putAll(copy.entries);
        policyHistory.clear(); policyHistory.putAll(copy.policyHistory);
        latestQualifications.clear();
        latestQualifications.putAll(copy.latestQualifications);
        ownerAuthorizationRevisions.clear(); ownerAuthorizationRevisions.putAll(copy.ownerAuthorizationRevisions);
        commitIndex = copy.commitIndex;
        authoritativeSnapshot = copy.authoritativeSnapshot;
    }

    public synchronized int size() {
        return entries.size();
    }

    synchronized QualificationAuthority latestQualification(TargetEpoch epoch) {
        QualificationAuthority value = latestQualifications.get(epoch.subjectKey());
        return value != null && value.qualification().targetEpoch().equals(epoch) ? value : null;
    }

    synchronized boolean ownsEffectAuthority(EffectRecord effect) {
        Entry entry = entries.get(effect.key().canonicalCommandId());
        if (entry == null || entry.status() != CommitReceipt.Status.COMMITTED
            || entry.commitIndex() != effect.authorityCommitIndex()
            || !entry.payloadHash().equals(effect.authorityPayloadHash())) return false;
        SubjectKey subject = effect.key().targetEpoch().subjectKey();
        int low = 0, high = entry.qualifications().size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            SubjectQualification value = entry.qualifications().get(middle);
            int comparison = value.subjectKey().compareTo(subject);
            if (comparison < 0) low = middle + 1;
            else if (comparison > 0) high = middle - 1;
            else return value.terminal() && value.targetEpoch().equals(effect.key().targetEpoch());
        }
        return false;
    }

    private void indexQualifications(Entry entry) {
        if (entry.status() == CommitReceipt.Status.COMMITTED && entry.command() instanceof DominionCommand.ConfigureStrengthPolicy configured)
            policyHistory.put(entry.commitIndex(), configured.configuration());
        if (entry.status() == CommitReceipt.Status.COMMITTED && entry.after().compareTo(entry.before()) > 0) {
            if (entry.command() instanceof DominionCommand.BindOwner bind) ownerAuthorizationRevisions.put(bind.owner(), entry.commitIndex());
            if (entry.command() instanceof DominionCommand.UnbindOwner unbind) ownerAuthorizationRevisions.put(unbind.owner(), entry.commitIndex());
        }
        for (SubjectQualification qualification : entry.qualifications()) {
            QualificationAuthority previous = latestQualifications.get(qualification.subjectKey());
            if (qualification.targetEpoch().generation() > 0L && (previous == null
                || qualification.targetEpoch().generation() > previous.qualification().targetEpoch().generation())) {
                latestQualifications.put(qualification.subjectKey(), new QualificationAuthority(
                    entry.commandId(), entry.payloadHash(), entry.commitIndex(), qualification));
            }
        }
    }
    synchronized dev.ronova.coremod.policy.StrengthPolicyConfiguration policyAt(long revision) {
        if (revision < 0 || revision > commitIndex) throw new IllegalArgumentException("unknown policy history revision");
        var entry = policyHistory.floorEntry(revision);
        return entry == null ? dev.ronova.coremod.policy.StrengthPolicyConfiguration.defaults() : entry.getValue();
    }

    record QualificationAuthority(UUID commandId, String payloadHash, long commitIndex,
        SubjectQualification qualification) {}

    synchronized Map<UUID, Long> ownerAuthorizationRevisions() { return Map.copyOf(ownerAuthorizationRevisions); }

    /** Validates Root-issued counters against retained receipts, including cleared subjects. */
    synchronized void validateQualifications(RealmId realm, Map<SubjectKey, SubjectQualification> current) {
        Map<SubjectKey, SubjectQualification> latest = new LinkedHashMap<>();
        for (Entry entry : entries.values()) {
            DominionCommand command = readCommand(entry.commandTag(), entry.commandId());
            var expectedInputs = command instanceof DominionCommand.TransitionSubjects t ? t.batch().preconditions()
                : command instanceof DominionCommand.CommitJudgmentBatch b ? b.batch().preconditions() : null;
            var ownershipSubjects = new java.util.HashSet<SubjectKey>();
            if (command instanceof DominionCommand.UnbindOwner unbind) {
                SubjectKey owner = new SubjectKey(realm, unbind.owner());
                var changed = latest.values().stream().map(value -> value.revokeOwner(owner))
                    .filter(value -> !value.equals(latest.get(value.subjectKey())))
                    .sorted(java.util.Comparator.comparing(SubjectQualification::subjectKey)).toList();
                if (!changed.equals(entry.qualifications())) throw new IllegalArgumentException("incomplete owner revocation history");
                changed.forEach(value -> ownershipSubjects.add(value.subjectKey()));
            }
            if (command instanceof DominionCommand.TransitionSubjects t) {
                t.batch().transitions().stream().filter(value -> value.kind()
                    == dev.ronova.coremod.root.api.SubjectTransitionBatch.Kind.SET_OWNERSHIP)
                    .forEach(value -> ownershipSubjects.add(value.subjectKey()));
            }
            if (expectedInputs != null) for (SubjectQualification expectedInput : expectedInputs.subjects()) {
                SubjectQualification previous = latest.get(expectedInput.subjectKey());
                if (!expectedInput.subjectKey().realmId().equals(realm)
                    || previous != null && !previous.equals(expectedInput)
                    || previous == null && (expectedInput.targetEpoch().generation() != 0L || expectedInput.revisions() != null)) {
                    throw new IllegalArgumentException("request preconditions disagree with qualification history");
                }
            }
            for (SubjectQualification value : entry.qualifications()) {
                SubjectQualification prior = latest.get(value.subjectKey());
                boolean ownershipOnly = ownershipSubjects.contains(value.subjectKey());
                long baseline = prior == null ? 0L : prior.targetEpoch().generation();
                long expected = ownershipOnly ? baseline : Math.addExact(baseline, 1L);
                if (!value.subjectKey().realmId().equals(realm) || value.targetEpoch().generation() != expected) {
                    throw new IllegalArgumentException("qualification receipt history is inconsistent");
                }
                latest.put(value.subjectKey(), value);
            }
        }
        for (SubjectQualification value : current.values()) {
            if ((value.targetEpoch().generation() > 0L || value.revisions() != null) && !value.equals(latest.get(value.subjectKey()))) {
                throw new IllegalArgumentException("qualification head differs from its receipt");
            }
        }
        for (SubjectQualification value : latest.values()) {
            if (!value.equals(current.get(value.subjectKey()))) {
                throw new IllegalArgumentException("qualification history was forgotten");
            }
        }
    }

    public static UUID commandId(DominionCommand command) {
        return DominionCommand.commandId(command);
    }

    public static String payloadHash(DominionCommand command) {
        return DominionCommand.canonicalPayloadHash(command);
    }

    private static Entry readEntry(
        CompoundTag row,
        DominionSnapshot authoritativeSnapshot,
        boolean compactFormat,
        boolean qualifiedFormat
    ) {
        if (row == null || !row.hasUUID("command_id")
            || !row.contains("payload_hash", Tag.TAG_STRING)
            || !row.contains("before_generation", Tag.TAG_LONG)
            || !row.contains("before_sequence", Tag.TAG_LONG)
            || !row.contains("after_generation", Tag.TAG_LONG)
            || !row.contains("after_sequence", Tag.TAG_LONG)
            || !row.contains("commit_index", Tag.TAG_LONG)
            || !row.contains("status", Tag.TAG_STRING)
            || !row.contains("command", Tag.TAG_COMPOUND)) {
            return null;
        }
        try {
            UUID id = row.getUUID("command_id");
            DominionCommand command = readCommand(row.getCompound("command"), id);
            boolean embeddedSnapshot = row.contains(
                "snapshot_payload", Tag.TAG_BYTE_ARRAY
            );
            if (compactFormat && embeddedSnapshot) {
                return null;
            }
            DominionSnapshot snapshot;
            if (embeddedSnapshot) {
                snapshot = dev.ronova.coremod.nativebridge.NativeSnapshotCodec
                    .decode(row.getByteArray("snapshot_payload"));
            } else {
                snapshot = Objects.requireNonNull(
                    authoritativeSnapshot, "compact journal snapshot"
                );
            }
            DominionEpoch before = new DominionEpoch(
                row.getLong("before_generation"), row.getLong("before_sequence")
            );
            DominionEpoch after = new DominionEpoch(
                row.getLong("after_generation"), row.getLong("after_sequence")
            );
            CommitReceipt.Status status = CommitReceipt.Status.valueOf(row.getString("status"));
            if (qualifiedFormat && !row.contains("qualifications", Tag.TAG_COMPOUND)) return null;
            if (!qualifiedFormat && row.contains("qualifications")) return null;
            List<SubjectQualification> qualifications = qualifiedFormat
                ? QualificationNbtCodec.decode(row.getCompound("qualifications")) : List.of();
            CommitReceipt receipt = new CommitReceipt(
                command, before, after, snapshot, id,
                DominionCommand.canonicalPayloadHash(command),
                nonNegative(row.getLong("commit_index")), status, qualifications
            );
            if (after.compareTo(before) > 0 && !after.equals(before.nextSequence())) {
                return null;
            }
            if (command instanceof DominionCommand.CommitJudgmentBatch batch) {
                if (batch.batch().empty() != after.equals(before)
                    || !batch.batch().empty()
                    && !after.equals(batch.batch().epoch())) {
                    return null;
                }
            } else if (command instanceof DominionCommand.CommitJudgment judgment
                && !after.equals(judgment.decision().epoch())) {
                return null;
            }
            String hash = normalizeHash(row.getString("payload_hash"));
            if (!DominionCommand.matchesCanonicalPayloadHash(command, hash)
                || !id.equals(commandId(command))) {
                return null;
            }
            // Strength commands preserve their source schema/hash. Retrofitting
            // new action permission into an old receipt would change its meaning.
            return Entry.from(receipt, commandTag(command));
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static CompoundTag writeEntry(Entry entry) {
        CompoundTag row = new CompoundTag();
        row.putUUID("command_id", entry.commandId());
        row.putString("payload_hash", entry.payloadHash());
        row.putLong("before_generation", entry.before().generation());
        row.putLong("before_sequence", entry.before().sequence());
        row.putLong("after_generation", entry.after().generation());
        row.putLong("after_sequence", entry.after().sequence());
        row.putLong("commit_index", entry.commitIndex());
        row.putString("status", entry.status().name());
        row.put("command", entry.commandTag().copy());
        row.put("qualifications", QualificationNbtCodec.encode(entry.qualifications()));
        return row;
    }

    private static CompoundTag commandTag(DominionCommand command) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("command_id", commandId(command));
        if (command instanceof DominionCommand.TransitionSubjects transitions) {
            tag.putString("kind", "transition_subjects");
            tag.put("transition", SubjectTransitionNbtCodec.encode(transitions.batch()));
        } else if (command instanceof DominionCommand.BindOwner bind) {
            tag.putString("kind", "bind_owner");
            tag.putUUID("uuid", bind.owner());
        } else if (command instanceof DominionCommand.UnbindOwner unbind) {
            tag.putString("kind", "unbind_owner");
            tag.putUUID("uuid", unbind.owner());
        } else if (command instanceof DominionCommand.ClearTombstones) {
            tag.putString("kind", "clear_tombstones");
        } else if (command instanceof DominionCommand.ClearTombstone clear) {
            tag.putString("kind", "clear_tombstone");
            tag.putUUID("uuid", clear.subject());
        } else if (command instanceof DominionCommand.CommitJudgment judgment) {
            tag.putString("kind", "commit_judgment");
            tag.put("judgment", judgmentTag(judgment.decision()));
        } else if (command instanceof DominionCommand.CommitJudgmentBatch batch) {
            tag.putString("kind", "commit_judgment_batch");
            tag.putString("command_domain", batch.batch().commandDomain());
            tag.putString("canonical_hash", batch.batch().canonicalPayloadHash());
            ListTag judgments = new ListTag();
            for (DominionDecision.Judgment judgment : batch.batch().judgments()) {
                judgments.add(judgmentTag(judgment));
            }
            tag.put("judgments", judgments);
            if (!batch.batch().incarnationNonces().isEmpty()) {
                ListTag incarnations = new ListTag();
                batch.batch().incarnationNonces().forEach((subject, nonce) -> {
                    CompoundTag row = new CompoundTag(); row.putUUID("subject", subject); row.putUUID("nonce", nonce);
                    incarnations.add(row);
                });
                tag.put("incarnations", incarnations);
            }
            if (batch.batch().preconditions() != null) {
                var expected = batch.batch().preconditions();
                tag.put("qualification_preconditions", preconditionsTag(expected));
            }
        } else if (command instanceof DominionCommand.ConfigureStrengthPolicy configure) {
            tag.putString("kind", "configure_strength_policy");
            tag.put("configuration", StrengthPolicyNbtCodec.encodeCommand(
                configure.configuration()
            ));
        } else {
            throw new IllegalArgumentException("unsupported command: " + command);
        }
        return tag;
    }

    private static DominionCommand readCommand(CompoundTag tag, UUID fallbackId) {
        if (tag == null || !tag.contains("kind", Tag.TAG_STRING)) {
            throw new IllegalArgumentException("missing command kind");
        }
        UUID id = tag.hasUUID("command_id") ? tag.getUUID("command_id") : fallbackId;
        if (id == null) {
            throw new IllegalArgumentException("missing command id");
        }
        String kind = tag.getString("kind");
        return switch (kind) {
            case "transition_subjects" -> new DominionCommand.TransitionSubjects(
                SubjectTransitionNbtCodec.decode(id, tag.getCompound("transition")));
            case "bind_owner" -> new DominionCommand.BindOwner(id, tag.getUUID("uuid"));
            case "unbind_owner" -> new DominionCommand.UnbindOwner(id, tag.getUUID("uuid"));
            case "clear_tombstones" -> new DominionCommand.ClearTombstones(id);
            case "clear_tombstone" -> new DominionCommand.ClearTombstone(id, tag.getUUID("uuid"));
            case "commit_judgment" -> {
                DominionDecision.Judgment judgment = readJudgment(tag.getCompound("judgment"));
                yield new DominionCommand.CommitJudgment(id, judgment.subject(), judgment);
            }
            case "commit_judgment_batch" -> {
                ListTag rows = tag.getList("judgments", Tag.TAG_COMPOUND);
                List<DominionDecision.Judgment> judgments = new ArrayList<>();
                for (Tag row : rows) {
                    judgments.add(readJudgment((CompoundTag) row));
                }
                yield new DominionCommand.CommitJudgmentBatch(new JudgmentBatch(
                    id, tag.getString("command_domain"), judgments,
                    tag.getString("canonical_hash"), readPreconditions(tag), readIncarnations(tag)
                ));
            }
            case "configure_strength_policy" ->
                new DominionCommand.ConfigureStrengthPolicy(
                    id, StrengthPolicyNbtCodec.decode(tag.getCompound("configuration"))
                );
            default -> throw new IllegalArgumentException("unknown command kind: " + kind);
        };
    }

    static CompoundTag preconditionsTag(dev.ronova.coremod.root.api.QualificationPreconditions expected) {
        CompoundTag tag = new CompoundTag(); tag.putInt("schema", expected.schemaVersion());
        tag.putString("realm_id", expected.realmId().canonical()); tag.putLong("policy_revision", expected.policyRevision());
        tag.put("qualifications", QualificationNbtCodec.encode(expected.subjects())); return tag;
    }

    private static Map<UUID, UUID> readIncarnations(CompoundTag command) {
        if (!command.contains("incarnations")) return Map.of();
        if (!(command.get("incarnations") instanceof ListTag original)) throw new IllegalArgumentException("invalid incarnations");
        ListTag rows = command.getList("incarnations", Tag.TAG_COMPOUND);
        if (rows.size() != original.size() || rows.size() > 1_000_000 || rows.isEmpty()) throw new IllegalArgumentException("invalid incarnation rows");
        Map<UUID, UUID> result = new TreeMap<>();
        for (Tag raw : rows) {
            CompoundTag row = (CompoundTag) raw;
            if (!row.hasUUID("subject") || !row.hasUUID("nonce")
                || result.putIfAbsent(row.getUUID("subject"), row.getUUID("nonce")) != null) {
                throw new IllegalArgumentException("invalid or duplicate incarnation subject");
            }
        }
        return Map.copyOf(result);
    }

    static dev.ronova.coremod.root.api.QualificationPreconditions readPreconditions(CompoundTag command) {
        if (!command.contains("qualification_preconditions")) return null;
        if (!command.contains("qualification_preconditions", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("wrong qualification precondition type");
        }
        CompoundTag tag = command.getCompound("qualification_preconditions");
        if (!tag.contains("schema", Tag.TAG_INT) || tag.getInt("schema") < 1 || tag.getInt("schema") > 2
            || !tag.contains("realm_id", Tag.TAG_STRING)
            || !tag.contains("policy_revision", Tag.TAG_LONG)
            || !tag.contains("qualifications", Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("malformed qualification preconditions");
        }
        var result = new dev.ronova.coremod.root.api.QualificationPreconditions(
            RealmId.fromString(tag.getString("realm_id")), tag.getLong("policy_revision"),
            QualificationNbtCodec.decode(tag.getCompound("qualifications")));
        if (result.schemaVersion() != tag.getInt("schema")) throw new IllegalArgumentException("non-canonical preconditions schema");
        return result;
    }

    private static CompoundTag judgmentTag(DominionDecision.Judgment judgment) {
        SubjectIdentity subject = judgment.subject();
        CompoundTag tag = new CompoundTag();
        if (subject.uuid() != null) {
            tag.putUUID("uuid", subject.uuid());
        }
        tag.putString("registry_type", subject.registryType());
        tag.putString("runtime_class", subject.runtimeClass());
        tag.putString("dimension", subject.dimension());
        tag.putDouble("x", subject.x());
        tag.putDouble("y", subject.y());
        tag.putDouble("z", subject.z());
        tag.putBoolean("player", subject.player());
        tag.putLong("generation", judgment.epoch().generation());
        tag.putLong("sequence", judgment.epoch().sequence());
        tag.putBoolean("committed", judgment.committed());
        tag.putString("scope", judgment.scope().name());
        tag.putDouble("radius", judgment.radius());
        tag.putString("reason", judgment.reason());
        return tag;
    }

    private static DominionDecision.Judgment readJudgment(CompoundTag tag) {
        UUID uuid = tag.hasUUID("uuid") ? tag.getUUID("uuid") : null;
        SubjectIdentity subject = new SubjectIdentity(
            uuid, tag.getString("registry_type"), tag.getString("runtime_class"),
            tag.getString("dimension"), tag.getDouble("x"), tag.getDouble("y"),
            tag.getDouble("z"), tag.getBoolean("player")
        );
        return new DominionDecision.Judgment(
            subject,
            new DominionEpoch(tag.getLong("generation"), tag.getLong("sequence")),
            tag.getBoolean("committed"),
            DominionDecision.LineageScope.valueOf(tag.getString("scope")),
            tag.getDouble("radius"), tag.getString("reason")
        );
    }

    private static String normalizeHash(String value) {
        String normalized = Objects.toString(value, "").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("command payload hash is required");
        }
        return normalized;
    }

    private static long nonNegative(long value) {
        if (value < 0L) {
            throw new IllegalArgumentException("durable journal value must be non-negative");
        }
        return value;
    }

    private record Entry(
        UUID commandId,
        String payloadHash,
        DominionCommand command,
        DominionEpoch before,
        DominionEpoch after,
        long commitIndex,
        CommitReceipt.Status status,
        CompoundTag commandTag,
        List<SubjectQualification> qualifications
    ) {
        private Entry {
            Objects.requireNonNull(commandId, "commandId");
            Objects.requireNonNull(payloadHash, "payloadHash");
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(before, "before");
            Objects.requireNonNull(after, "after");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(commandTag, "commandTag");
            qualifications = List.copyOf(qualifications);
        }

        private static Entry from(
            CommitReceipt receipt,
            CompoundTag commandTag
        ) {
            return new Entry(
                receipt.commandId(), receipt.canonicalPayloadHash(),
                receipt.command(), receipt.before(), receipt.after(),
                receipt.commitIndex(), receipt.status(), commandTag, receipt.qualifications()
            );
        }

        private CommitReceipt receipt(DominionSnapshot currentSnapshot) {
            return new CommitReceipt(
                command, before, after,
                Objects.requireNonNull(currentSnapshot, "journal Root snapshot"),
                commandId, payloadHash, commitIndex, status, qualifications
            );
        }
    }
}
