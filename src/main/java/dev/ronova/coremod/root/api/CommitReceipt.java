package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectKey;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable result of one durable Root mutation attempt.
 *
 * <p>For a newly committed command, {@code snapshot} is the exact snapshot at
 * {@code after}.  A compact journal may rehydrate an older receipt against the
 * current authoritative snapshot instead of retaining one full historical
 * snapshot per row.  In that case the snapshot epoch is newer than
 * {@code after}; callers must use {@code after} for command finality and the
 * snapshot only for forward projection reconciliation.</p>
 */
public record CommitReceipt(
    DominionCommand command,
    DominionEpoch before,
    DominionEpoch after,
    DominionSnapshot snapshot,
    UUID commandId,
    String canonicalPayloadHash,
    long commitIndex,
    Status status,
    List<SubjectQualification> qualifications
) {
    public CommitReceipt {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        Objects.requireNonNull(snapshot, "snapshot");
        commandId = Objects.requireNonNull(commandId, "commandId");
        status = Objects.requireNonNull(status, "status");
        qualifications = Objects.requireNonNull(qualifications, "qualifications").stream()
            .sorted(java.util.Comparator.comparing(SubjectQualification::subjectKey)).toList();
        HashSet<SubjectKey> qualifiedSubjects = new HashSet<>();
        dev.ronova.coremod.contract.RealmId qualificationRealm = null;
        for (SubjectQualification qualification : qualifications) {
            if (status != Status.COMMITTED || after.equals(before)
                || qualification.targetEpoch().generation() == 0L && qualification.revisions() == null
                || !qualifiedSubjects.add(qualification.subjectKey())) {
                throw new IllegalArgumentException("invalid receipt qualification");
            }
            if (qualificationRealm != null && !qualificationRealm.equals(qualification.subjectKey().realmId())) {
                throw new IllegalArgumentException("receipt qualifications cross Realms");
            }
            qualificationRealm = qualification.subjectKey().realmId();
        }
        validateQualificationMembership(command, qualifications);
        if (status == Status.COMMITTED && command instanceof DominionCommand.CommitJudgmentBatch batch
            && batch.batch().preconditions() != null) {
            List<SubjectQualification> expectedNext = batch.batch().preconditions().subjects().stream()
                .map(value -> value.judgmentTransition(batch.batch().incarnationNonces().get(value.subjectKey().subjectId()))).toList();
            if (!expectedNext.equals(qualifications)) {
                throw new IllegalArgumentException("receipt does not prove the exact requested qualification transition");
            }
        }
        if (status == Status.COMMITTED && command instanceof DominionCommand.TransitionSubjects transitions
            && (!transitions.batch().changedQualifications().equals(qualifications)
                || !transitions.batch().expectedEpoch().equals(before)
                || qualifications.isEmpty() != after.equals(before))) {
            throw new IllegalArgumentException("receipt does not prove the exact subject transitions");
        }
        if (snapshot.epoch().compareTo(after) < 0) {
            throw new IllegalArgumentException("receipt snapshot predates command result");
        }
        if (after.compareTo(before) < 0) {
            throw new IllegalArgumentException("Root epoch rolled back");
        }
        if (status != Status.COMMITTED && !after.equals(before)) {
            throw new IllegalArgumentException(
                "unresolved or rejected receipt cannot claim a state transition"
            );
        }
        if (commitIndex < 0L) {
            throw new IllegalArgumentException("commit index must be non-negative");
        }
        UUID expectedId = DominionCommand.commandId(command);
        if (!expectedId.equals(commandId)) {
            throw new IllegalArgumentException("receipt command id does not match command");
        }
        String expectedHash = DominionCommand.canonicalPayloadHash(command);
        String suppliedHash = Objects.toString(canonicalPayloadHash, "").trim();
        if (!expectedHash.equalsIgnoreCase(suppliedHash)) {
            throw new IllegalArgumentException("receipt payload hash does not match command");
        }
        canonicalPayloadHash = expectedHash.toLowerCase(Locale.ROOT);
    }

    /** Historical receipts have no per-subject qualification proof. */
    public CommitReceipt(DominionCommand command, DominionEpoch before, DominionEpoch after,
        DominionSnapshot snapshot, UUID commandId, String canonicalPayloadHash,
        long commitIndex, Status status) {
        this(command, before, after, snapshot, commandId, canonicalPayloadHash, commitIndex, status, List.of());
    }

    /** Compatibility constructor for the original 7.16 journal call shape. */
    public CommitReceipt(
        DominionCommand command,
        DominionEpoch before,
        DominionEpoch after,
        DominionSnapshot snapshot,
        UUID commandId,
        long commitIndex,
        Status status
    ) {
        this(
            command, before, after, snapshot, commandId,
            DominionCommand.canonicalPayloadHash(command), commitIndex, status
        );
    }

    /** Compatibility constructor retained for pre-7.16 callers and tests. */
    public CommitReceipt(
        DominionCommand command,
        DominionEpoch before,
        DominionEpoch after,
        DominionSnapshot snapshot
    ) {
        this(
            command, before, after, snapshot,
            DominionCommand.commandId(command),
            DominionCommand.canonicalPayloadHash(command),
            0L, Status.COMMITTED
        );
    }

    public boolean changed() {
        return status == Status.COMMITTED && after.compareTo(before) > 0;
    }

    /** Only a resolved committed transition proves a durable mutation. */
    public boolean durableMutation() {
        return changed();
    }

    public boolean committed() {
        return status == Status.COMMITTED;
    }

    public boolean rejected() {
        return status == Status.REJECTED;
    }

    public boolean unknown() {
        return status == Status.COMMIT_UNKNOWN;
    }

    public boolean resolved() {
        return status != Status.COMMIT_UNKNOWN;
    }

    /** True when this receipt still carries its exact commit-time projection. */
    public boolean exactSnapshot() {
        return after.equals(snapshot.epoch());
    }

    /** Only the original Root-issued subject generation can qualify execution. */
    public TargetEpoch targetEpoch(SubjectKey subjectKey) {
        if (!committed()) return null;
        for (SubjectQualification qualification : qualifications) {
            if (qualification.subjectKey().equals(subjectKey)) return qualification.targetEpoch();
        }
        return null;
    }

    private static void validateQualificationMembership(DominionCommand command, List<SubjectQualification> values) {
        if (command instanceof DominionCommand.TransitionSubjects) return; // Exact transition comparison is performed above.
        if (command instanceof DominionCommand.UnbindOwner unbind) {
            for (SubjectQualification value : values) {
                if (value.revisions() == null || value.revisions().ownership().values().stream()
                    .anyMatch(binding -> binding.owner() != null && binding.owner().subjectId().equals(unbind.owner()))) {
                    throw new IllegalArgumentException("owner revocation retained an owned domain");
                }
            }
            return;
        }
        if (values.isEmpty()) return; // Historical receipt, not proof of a 7.17 qualification.
        HashSet<UUID> expected = new HashSet<>();
        boolean terminal;
        if (command instanceof DominionCommand.CommitJudgmentBatch batch) {
            batch.batch().judgments().forEach(value -> expected.add(value.subject().uuid()));
            terminal = true;
        } else if (command instanceof DominionCommand.CommitJudgment judgment) {
            expected.add(judgment.subject().uuid());
            terminal = true;
        } else if (command instanceof DominionCommand.ClearTombstone clear) {
            expected.add(clear.subject());
            terminal = false;
        } else if (command instanceof DominionCommand.ClearTombstones) {
            values.forEach(value -> expected.add(value.subjectKey().subjectId()));
            terminal = false;
        } else {
            throw new IllegalArgumentException("command cannot issue subject qualifications");
        }
        if (expected.size() != values.size()) throw new IllegalArgumentException("receipt qualification set is incomplete");
        for (SubjectQualification value : values) {
            if (value.terminal() != terminal || !expected.remove(value.subjectKey().subjectId())) {
                throw new IllegalArgumentException("qualification does not belong to the command");
            }
        }
    }

    /** Admission tokens derived from this receipt are qualified by this revision. */
    public DominionEpoch admissionRevision() {
        return after;
    }

    /** Any qualification at or before this revision is revoked by a changed commit. */
    public DominionEpoch revokeThroughEpoch() {
        return changed() ? before : after;
    }

    public static CommitReceipt rejected(
        DominionCommand command,
        DominionEpoch at,
        DominionSnapshot snapshot,
        long commitIndex
    ) {
        return new CommitReceipt(
            command, at, at, snapshot,
            DominionCommand.commandId(command),
            DominionCommand.canonicalPayloadHash(command),
            commitIndex, Status.REJECTED
        );
    }

    public static CommitReceipt rejected(
        DominionCommand command,
        DominionEpoch at,
        DominionSnapshot snapshot
    ) {
        return rejected(command, at, snapshot, 0L);
    }

    public static CommitReceipt unknown(
        DominionCommand command,
        DominionEpoch at,
        DominionSnapshot snapshot,
        long commitIndex
    ) {
        return new CommitReceipt(
            command, at, at, snapshot,
            DominionCommand.commandId(command),
            DominionCommand.canonicalPayloadHash(command),
            commitIndex, Status.COMMIT_UNKNOWN
        );
    }

    public enum Status {
        COMMITTED,
        REJECTED,
        COMMIT_UNKNOWN
    }
}
