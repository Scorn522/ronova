package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.SubjectKey;
import java.util.Objects;

/** Root-owned terminal/clear qualification; zero denotes a legacy or unseen baseline. */
public record SubjectQualification(TargetEpoch targetEpoch, boolean terminal, SubjectRevisions revisions) {
    public SubjectQualification {
        Objects.requireNonNull(targetEpoch, "targetEpoch");
        if (revisions != null) {
            if (revisions.revokedThroughEpoch() > targetEpoch.generation()
                || revisions.activeIncarnation() != null
                    && !revisions.activeIncarnation().subjectKey().equals(targetEpoch.subjectKey())
                || revisions.ownership().values().stream().anyMatch(value -> value.owner() != null
                    && !value.owner().realmId().equals(targetEpoch.subjectKey().realmId()))) {
                throw new IllegalArgumentException("revision vector crosses qualification scope");
            }
        }
    }
    /** Old journal records retain their original extension and hash. */
    public SubjectQualification(TargetEpoch targetEpoch, boolean terminal) { this(targetEpoch, terminal, null); }
    public SubjectKey subjectKey() { return targetEpoch.subjectKey(); }
    public SubjectQualification transition(boolean nextTerminal) {
        return new SubjectQualification(targetEpoch.next(), nextTerminal,
            revisions == null ? null : revisions.qualify(targetEpoch.generation(), true));
    }
    public SubjectQualification judgmentTransition(java.util.UUID incarnationNonce) {
        if (incarnationNonce == null) return transition(true);
        SubjectRevisions next = (revisions == null ? SubjectRevisions.initial() : revisions)
            .bind(subjectKey(), incarnationNonce).qualify(targetEpoch.generation(), true);
        return new SubjectQualification(targetEpoch.next(), true, next);
    }
    public SubjectQualification rebind(java.util.UUID nonce) {
        if (terminal) throw new IllegalStateException("terminal subject requires explicit reactivation");
        SubjectRevisions before = revisions == null ? SubjectRevisions.initial() : revisions;
        SubjectRevisions bound = before.bind(subjectKey(), nonce);
        return bound.equals(before) ? this : new SubjectQualification(targetEpoch.next(), false,
            bound.qualify(targetEpoch.generation(), false));
    }
    public SubjectQualification reactivate() {
        if (!terminal) return this;
        return new SubjectQualification(targetEpoch.next(), false,
            (revisions == null ? SubjectRevisions.initial() : revisions).qualify(targetEpoch.generation(), true));
    }
    public SubjectQualification own(dev.ronova.coremod.contract.StateDomainId domain,
        dev.ronova.coremod.contract.SubjectKey owner) {
        SubjectRevisions before = revisions == null ? SubjectRevisions.initial() : revisions;
        SubjectRevisions next = before.own(domain, owner);
        return next.equals(before) ? this : new SubjectQualification(targetEpoch, terminal, next);
    }
    public SubjectQualification revokeOwner(SubjectKey owner) {
        if (revisions == null) return this;
        SubjectQualification next = this;
        for (var entry : revisions.ownership().entrySet()) {
            if (owner.equals(entry.getValue().owner())) next = next.own(entry.getKey(), null);
        }
        return next;
    }
}
