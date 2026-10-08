package dev.ronova.coremod.projection;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.engine.JudgmentMark;
import dev.ronova.coremod.engine.JudgmentScope;
import dev.ronova.coremod.engine.LineageIndex;
import dev.ronova.coremod.engine.LineageRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Indexed immutable tombstone view plus server-lifetime admission observability. */
public final class AdmissionProjection {
    private final DominionEpoch epoch;
    private final LineageIndex<ProjectedLineage> registryLineages = new LineageIndex<>();
    private final List<JudgmentMark> marks;
    private final long tombstoneRevision;
    private final AdmissionMetrics metrics;

    AdmissionProjection(DominionSnapshot snapshot, AdmissionMetrics metrics) {
        this.epoch = snapshot.epoch();
        this.metrics = metrics;
        List<JudgmentMark> projectedMarks = new ArrayList<>();
        long revision = 0xcbf29ce484222325L;
        for (DominionDecision.Judgment judgment : snapshot.judgments()) {
            if (!judgment.committed()) {
                continue;
            }
            SubjectIdentity subject = judgment.subject();
            JudgmentScope scope = scope(judgment.scope());
            ProjectedLineage registry = new ProjectedLineage(
                subject.uuid(), subject.registryType(), subject.dimension(),
                subject.x(), subject.y(), subject.z(), judgment.radius(), scope
            );
            registryLineages.put(registry);
            projectedMarks.add(new JudgmentMark(
                subject.uuid(), subject.registryType(), subject.dimension(),
                subject.x(), subject.y(), subject.z(), judgment.radius(), scope
            ));
            revision ^= subject.uuid() == null ? 0L : subject.uuid().getMostSignificantBits();
            revision *= 0x100000001b3L;
            revision ^= subject.uuid() == null ? 0L : subject.uuid().getLeastSignificantBits();
            revision *= 0x100000001b3L;
            revision ^= judgment.epoch().sequence();
            revision *= 0x100000001b3L;
        }
        this.marks = List.copyOf(projectedMarks);
        this.tombstoneRevision = revision ^ projectedMarks.size();
    }

    public DominionEpoch epoch() {
        return epoch;
    }

    public boolean matches(SubjectIdentity subject) {
        if (subject == null) {
            return false;
        }
        if (registryLineages.matches(
            subject.uuid(), subject.registryType(), subject.dimension(),
            subject.x(), subject.y(), subject.z()
        )) {
            return true;
        }
        // Serialized names do not attest defining-loader identity or actual inheritance.
        return false;
    }

    /**
     * Evaluates a spawn against this immutable Root-approved snapshot. Admission
     * is a hot path and therefore must not make a per-entity JNI/IPC decision.
     */
    public DominionDecision.Admission decide(
        SubjectIdentity subject,
        boolean combatObject,
        boolean protectedSubject,
        boolean alreadyTerminal,
        boolean passedFinalBoundary,
        boolean finalBoundary
    ) {
        boolean tombstoneMatch = combatObject && !protectedSubject && !alreadyTerminal
            && !passedFinalBoundary && matches(subject);
        boolean admitted = !combatObject || protectedSubject
            || (!alreadyTerminal && !tombstoneMatch);
        String reason;
        if (!combatObject) {
            reason = "projection-world-result";
        } else if (protectedSubject) {
            reason = "projection-authority-subject";
        } else if (alreadyTerminal) {
            reason = "projection-terminal-object";
        } else if (tombstoneMatch) {
            reason = "projection-tombstone-match";
        } else if (finalBoundary && passedFinalBoundary) {
            reason = "projection-cached-final-pass";
        } else {
            reason = finalBoundary
                ? "projection-final-pass" : "projection-provisional-pass";
        }
        return new DominionDecision.Admission(subject, epoch, admitted, reason);
    }

    public int tombstoneCount() {
        return registryLineages.size();
    }

    public boolean hasTombstone(UUID uuid) {
        return registryLineages.contains(uuid);
    }
    public JudgmentMark mark(UUID uuid) {
        var record = registryLineages.get(uuid);
        return record == null ? null : new JudgmentMark(record.uuid(), record.type(), record.dimension(), record.x(), record.y(), record.z(), record.radius(), record.scope());
    }

    /** Cheap watch reconciliation key; computed while the immutable index is built. */
    public long tombstoneRevision() {
        return tombstoneRevision;
    }

    public List<JudgmentMark> marks() {
        return marks;
    }

    public void recordObjectRejection() {
        metrics.objectRejections.incrementAndGet();
    }

    public void recordNbtRejection() {
        metrics.nbtRejections.incrementAndGet();
    }

    public long rejectedAdmissions() {
        return metrics.objectRejections.get() + metrics.nbtRejections.get();
    }

    public long objectRejections() {
        return metrics.objectRejections.get();
    }

    public long nbtRejections() {
        return metrics.nbtRejections.get();
    }

    private static JudgmentScope scope(DominionDecision.LineageScope scope) {
        return switch (scope) {
            case IDENTITY -> JudgmentScope.IDENTITY;
            case LOCAL_LINEAGE -> JudgmentScope.LOCAL_LINEAGE;
            case GLOBAL_LINEAGE -> JudgmentScope.GLOBAL_LINEAGE;
        };
    }

    static final class AdmissionMetrics {
        private final AtomicLong objectRejections = new AtomicLong();
        private final AtomicLong nbtRejections = new AtomicLong();
    }

    private record ProjectedLineage(
        UUID uuid,
        String type,
        String dimension,
        double x,
        double y,
        double z,
        double radius,
        JudgmentScope scope
    ) implements LineageRecord {}
}
