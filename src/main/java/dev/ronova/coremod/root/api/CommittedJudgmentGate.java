package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.SubjectKey;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

/** One receipt-bound index per batch. No per-target scan of the batch or history. */
public final class CommittedJudgmentGate {
    private final Set<DominionDecision.Judgment> judgments;
    private final Map<UUID, TargetEpoch> targetEpochs;
    private final Map<UUID, SubjectQualification> qualifications;

    private CommittedJudgmentGate(Set<DominionDecision.Judgment> judgments, Map<UUID, TargetEpoch> epochs,
        Map<UUID, SubjectQualification> qualifications) {
        this.judgments = Set.copyOf(judgments);
        this.targetEpochs = Map.copyOf(epochs);
        this.qualifications = Map.copyOf(qualifications);
    }

    public static CommittedJudgmentGate from(CommitReceipt receipt) {
        Set<DominionDecision.Judgment> allowed = new HashSet<>();
        Map<UUID, TargetEpoch> epochs = new HashMap<>();
        Map<UUID, SubjectQualification> qualifications = new HashMap<>();
        if (receipt != null && receipt.committed()) {
            if (receipt.command() instanceof DominionCommand.CommitJudgmentBatch command) {
                for (DominionDecision.Judgment decision : command.batch().judgments()) {
                    if (decision.committed() && decision.epoch().equals(receipt.after())) allowed.add(decision);
                }
            } else if (receipt.command() instanceof DominionCommand.CommitJudgment command
                && command.decision().committed()
                && command.decision().epoch().equals(receipt.after())) {
                allowed.add(command.decision());
            }
        }
        if (receipt != null && receipt.committed()) {
            for (SubjectQualification qualification : receipt.qualifications()) {
                if (qualification.terminal()) epochs.put(qualification.subjectKey().subjectId(), qualification.targetEpoch());
                qualifications.put(qualification.subjectKey().subjectId(), qualification);
            }
        }
        return new CommittedJudgmentGate(allowed, epochs, qualifications);
    }

    /** Checks the complete issued vector and the actual physical object before entry. */
    public boolean runQualified(DominionDecision.Judgment decision, UUID physicalNonce,
        Function<SubjectKey, SubjectQualification> current, Runnable application) {
        if (decision == null || current == null || application == null || !judgments.contains(decision)) return false;
        SubjectQualification issued = qualifications.get(decision.subject().uuid());
        if (issued == null || !issued.terminal() || issued.revisions() == null
            || issued.revisions().activeIncarnation() == null
            || !issued.revisions().activeIncarnation().nonce().equals(physicalNonce)
            || !issued.equals(current.apply(issued.subjectKey()))) return false;
        application.run(); return true;
    }

    /** This proves receipt membership, not current native integrity or effect completion. */
    public boolean run(DominionDecision.Judgment decision, Runnable application) {
        if (decision == null || application == null || !judgments.contains(decision)) return false;
        application.run();
        return true;
    }

    /** Current-generation gate for 7.17 execution; legacy receipts cannot mint a new lease. */
    public boolean run(DominionDecision.Judgment decision,
        Function<SubjectKey, TargetEpoch> currentEpoch, Runnable application) {
        if (decision == null || currentEpoch == null || application == null || !judgments.contains(decision)) return false;
        TargetEpoch issued = targetEpochs.get(decision.subject().uuid());
        if (issued == null || !issued.accepts(currentEpoch.apply(issued.subjectKey()))) return false;
        application.run();
        return true;
    }
}
