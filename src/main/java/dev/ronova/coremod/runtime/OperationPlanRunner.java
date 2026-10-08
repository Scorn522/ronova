package dev.ronova.coremod.runtime;

import dev.ronova.coremod.root.api.*;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.function.BooleanSupplier;

/** Fixed ordered plan runner; ports cannot renew a stale Root plan or choose another effect identity. */
public final class OperationPlanRunner {
    public interface Ports {
        boolean current(TerminalOperationPlan plan);
        OperationReport.StepReceipt controlled(TerminalOperationPlan plan, TerminalOperationPlan.Step step);
        default OperationReport.StepReceipt controlled(TerminalOperationPlan plan, TerminalOperationPlan.Step step,
            BooleanSupplier permitted) {
            return permitted.getAsBoolean() ? controlled(plan, step)
                : new OperationReport.StepReceipt(step, OperationReport.Status.FENCED, "final-write-permission-expired");
        }
        EffectClaim prepare(TerminalOperationPlan plan);
        boolean enter(EffectClaim claim);
        void opaque(TerminalOperationPlan plan);
        default void opaque(TerminalOperationPlan plan, BooleanSupplier permitted) {
            if (permitted.getAsBoolean()) opaque(plan);
        }
        void finishUnknown(EffectClaim claim);
    }
    private final ThreadLocal<Set<String>> active = ThreadLocal.withInitial(HashSet::new);
    private final LongSupplier clock;
    public OperationPlanRunner() { this(System::nanoTime); }
    OperationPlanRunner(LongSupplier clock) { this.clock = Objects.requireNonNull(clock); }
    public OperationReport run(TerminalOperationPlan plan, Ports ports) {
        Objects.requireNonNull(plan); Objects.requireNonNull(ports);
        Set<String> stack = active.get();
        String cause = plan.commandId() + "/" + plan.subjectKey().canonical();
        if (!stack.add(cause)) return new OperationReport(plan, List.of(), false, false, "REENTRANT");
        List<OperationReport.StepReceipt> receipts = new ArrayList<>(); boolean entered = false, scopedFailure = false;
        try (var lease = new PlanExecutionLease(plan, clock, PlanExecutionLease.MAX_DURATION_NANOS)) {
            for (var step : plan.orderedSteps()) {
                if (!lease.valid(plan) || !ports.current(plan)) {
                    receipts.add(new OperationReport.StepReceipt(step, OperationReport.Status.FENCED, "qualification-or-plan-revision-changed"));
                    return new OperationReport(plan, receipts, entered, false, "FENCED");
                }
                if (step == TerminalOperationPlan.Step.TERMINAL_LIFECYCLE) {
                    EffectClaim claim = ports.prepare(plan);
                    if (claim.granted()) {
                        if (!claim.record().key().equals(plan.effectKey())) throw new IllegalStateException("Root returned a different semantic effect key");
                        try {
                            if (lease.valid(plan) && ports.current(plan) && ports.enter(claim)) {
                                // Entering the durable claim can itself invalidate the execution context.
                                if (lease.valid(plan) && ports.current(plan)) {
                                    entered = true;
                                    ports.opaque(plan, () -> lease.valid(plan) && ports.current(plan));
                                }
                            }
                        } catch (RuntimeException failed) {
                            // A callback exception cannot prove absence of its physical effects.
                        } finally { ports.finishUnknown(claim); }
                    }
                    boolean unresolved = claim.record() != null || claim.status() == EffectClaim.Status.LEGACY_CLAIM_UNKNOWN;
                    receipts.add(new OperationReport.StepReceipt(step, unresolved ? OperationReport.Status.EFFECT_UNKNOWN : OperationReport.Status.FENCED,
                        "root-effect-claim:" + claim.status()));
                    if (!unresolved) scopedFailure = true;
                } else {
                    OperationReport.StepReceipt result;
                    try { result = Objects.requireNonNull(ports.controlled(plan, step, () -> lease.valid(plan) && ports.current(plan))); }
                    catch (RuntimeException failed) { result = new OperationReport.StepReceipt(step, OperationReport.Status.UNAVAILABLE, failed.getClass().getSimpleName()); }
                    if (result.step() != step) throw new IllegalStateException("executor returned another step receipt");
                    receipts.add(result);
                    if (result.status() == OperationReport.Status.FENCED || result.status() == OperationReport.Status.UNAVAILABLE) {
                        scopedFailure = true;
                        if (step == TerminalOperationPlan.Step.ADMISSION_FENCE || step == TerminalOperationPlan.Step.SERVER_PRESENCE)
                            return new OperationReport(plan, receipts, entered, false, "FENCED");
                    }
                }
                if (!lease.valid(plan) || !ports.current(plan)) {
                    // Preserve EFFECT_UNKNOWN: revocation cannot erase an effect already attempted.
                    return new OperationReport(plan, receipts, entered, false, "FENCED");
                }
            }
            return new OperationReport(plan, receipts, entered, false, scopedFailure ? "FENCED" : "PARTIAL");
        } finally { stack.remove(cause); if (stack.isEmpty()) active.remove(); }
    }
}
