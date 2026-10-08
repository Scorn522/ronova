package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectKey;
import java.util.UUID;

/** Durable Root port. Implementations never publish gameplay projections themselves. */
public interface DominionRootGateway {
    DominionSnapshot snapshot();

    DominionEpoch epoch();

    DominionEpoch nextDecisionEpoch();

    default TargetEpoch targetEpoch(SubjectKey key) { return null; }
    default SubjectQualification subjectQualification(SubjectKey key) { return null; }
    default SubjectTransitionBatch prepareTransitions(UUID commandId, UUID actor,
        java.util.List<SubjectTransitionBatch.Transition> transitions) {
        throw new UnsupportedOperationException("subject transitions are unavailable");
    }

    default JudgmentBatch qualify(JudgmentBatch batch) {
        throw new UnsupportedOperationException("qualification capture is unavailable");
    }

    CommitReceipt commit(DominionCommand command);

    default CommitReceipt commit(JudgmentBatch batch) {
        return commit(new DominionCommand.CommitJudgmentBatch(batch));
    }

    /** Resolves an ambiguous commit without allocating a replacement command id. */
    default CommitReceipt recover(UUID commandId, String canonicalPayloadHash) {
        return null;
    }

    /** Replays a previously resolved ingress command by its stable identity. */
    default CommitReceipt recover(UUID commandId) {
        return null;
    }
}
