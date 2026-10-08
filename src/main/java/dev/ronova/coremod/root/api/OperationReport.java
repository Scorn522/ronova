package dev.ronova.coremod.root.api;

import java.util.*;

/** Execution evidence is separate from committed Truth and from whole-operation completion. */
public record OperationReport(TerminalOperationPlan plan, List<StepReceipt> steps, boolean opaqueEntered,
    boolean completionSatisfied, String state) {
    public enum Status { CONVERGED, DISPATCHED, EFFECT_UNKNOWN, UNAVAILABLE, FENCED }
    public record StepReceipt(TerminalOperationPlan.Step step, Status status, String evidence, List<WriteSetReceipt> writeSets) {
        public StepReceipt(TerminalOperationPlan.Step step, Status status, String evidence) {
            this(step,status,evidence,List.of());
        }
        public StepReceipt(TerminalOperationPlan.Step step, Status status, String evidence, WriteSetReceipt writeSet) {
            this(step,status,evidence,writeSet==null?List.of():List.of(writeSet));
        }
        public StepReceipt {
            Objects.requireNonNull(step); Objects.requireNonNull(status); Objects.requireNonNull(evidence);
            writeSets=List.copyOf(Objects.requireNonNull(writeSets,"writeSets"));
            var hashes=new HashSet<String>();
            for(var writeSet:writeSets)if(!hashes.add(writeSet.evidenceHash()))
                throw new IllegalArgumentException("duplicate write-set receipt");
            if (evidence.isBlank() || evidence.length() > 256) throw new IllegalArgumentException("unbounded step evidence");
        }
    }
    public OperationReport {
        Objects.requireNonNull(plan); Objects.requireNonNull(state); steps = List.copyOf(steps);
        if (completionSatisfied || !Set.of("PARTIAL", "FENCED", "REENTRANT").contains(state) || steps.size() > plan.orderedSteps().size())
            throw new IllegalArgumentException("unsupported completion assertion");
        for (int i = 0; i < steps.size(); i++) {
            var receipt=steps.get(i);
            if (receipt.step() != plan.orderedSteps().get(i)) throw new IllegalArgumentException("out-of-order plan receipt");
            for(var writeSet:receipt.writeSets())if(!writeSet.matches(plan))
                throw new IllegalArgumentException("write-set receipt does not match operation plan");
            if (receipt.step() == TerminalOperationPlan.Step.TERMINAL_LIFECYCLE
                && !Set.of(Status.EFFECT_UNKNOWN, Status.FENCED, Status.UNAVAILABLE).contains(receipt.status()))
                throw new IllegalArgumentException("opaque lifecycle cannot claim convergence");
        }
    }
}
