package dev.ronova.coremod.runtime;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import java.util.*;

public final class OperationPlanRunnerSelfTest {
    public static void main(String[] args) {
        SubjectKey key = new SubjectKey(new RealmId(new UUID(63, 1)), new UUID(63, 2));
        var qualification = QualificationPreconditions.baseline(key).judgmentTransition(new UUID(63, 3));
        var plan = TerminalOperationPlan.terminal(new UUID(63, 4), EffectRecord.hash("authority"), 2, 2, qualification, 1, 1);
        var runner = new OperationPlanRunner();
        class Ports implements OperationPlanRunner.Ports {
            boolean valid = true, revokeAtFence, failAfterEffect, revokeAtEnter, revokeAtNetwork;
            int effects, finishes, prepares, stateRepairs;
            EffectRecord record;
            @Override public boolean current(TerminalOperationPlan p) { return valid; }
            @Override public OperationReport.StepReceipt controlled(TerminalOperationPlan p, TerminalOperationPlan.Step step) {
                if (step == TerminalOperationPlan.Step.TERMINAL_STATE) stateRepairs++;
                if (revokeAtFence && step == TerminalOperationPlan.Step.ADMISSION_FENCE) valid = false;
                if (revokeAtNetwork && step == TerminalOperationPlan.Step.NETWORK_PROJECTION) valid = false;
                return new OperationReport.StepReceipt(step, OperationReport.Status.CONVERGED, "fixture-controlled-postcondition");
            }
            @Override public EffectClaim prepare(TerminalOperationPlan p) {
                prepares++;
                if (record != null) return new EffectClaim(EffectClaim.Status.ALREADY_RECORDED, record);
                record = EffectRecord.terminalLifecycle(p.effectKey(), p.authorityHash(), p.authorityCommitIndex(), 1, p.planRevision());
                return new EffectClaim(EffectClaim.Status.GRANTED, record);
            }
            @Override public boolean enter(EffectClaim claim) { if (revokeAtEnter) valid = false; return true; }
            @Override public void opaque(TerminalOperationPlan p) {
                effects++;
                require(runner.run(p, this).state().equals("REENTRANT"), "callback feedback recursively generated execution");
                if (failAfterEffect) throw new IllegalStateException("after-effect");
            }
            @Override public void finishUnknown(EffectClaim claim) { finishes++; record = record.unknown("fixture-unknown"); }
        }
        Ports ports = new Ports(); ports.failAfterEffect = true;
        var first = runner.run(plan, ports);
        require(first.state().equals("PARTIAL") && first.opaqueEntered() && !first.completionSatisfied()
            && ports.effects == 1 && ports.finishes == 1 && ports.prepares == 1, "opaque failure or reentrancy was misclassified");
        var repeat = runner.run(plan, ports);
        require(!repeat.opaqueEntered() && ports.effects == 1 && repeat.steps().stream()
            .filter(step->step.step()==TerminalOperationPlan.Step.TERMINAL_LIFECYCLE).findFirst().orElseThrow().status() == OperationReport.Status.EFFECT_UNKNOWN,
            "unknown step was replayed or considered complete");
        require(ports.stateRepairs == 2 && repeat.steps().stream().anyMatch(step->step.step()==TerminalOperationPlan.Step.TERMINAL_STATE),
            "once-only lifecycle suppressed independent state recovery");
        Ports stale = new Ports(); stale.revokeAtFence = true;
        for(var unavailable:List.of(TerminalOperationPlan.Step.SOURCE_QUALIFICATION,
            TerminalOperationPlan.Step.TERMINAL_STATE,TerminalOperationPlan.Step.WORLD_RECORDS)) {
            Ports isolated=new Ports() {
                @Override public OperationReport.StepReceipt controlled(TerminalOperationPlan p,TerminalOperationPlan.Step step) {
                    if(step==unavailable)return new OperationReport.StepReceipt(step,OperationReport.Status.UNAVAILABLE,"fixture-scoped-unavailable");
                    return super.controlled(p,step);
                }
            };
            var partial=runner.run(plan,isolated);
            require(partial.state().equals("FENCED")&&partial.steps().size()==plan.orderedSteps().size()
                &&isolated.effects==1,"a scoped failure suppressed later independent steps: "+unavailable);
        }
        Ports disabledLifecycle=new Ports() {
            @Override public EffectClaim prepare(TerminalOperationPlan p){return new EffectClaim(EffectClaim.Status.REJECTED,null);}
        };
        var independent=runner.run(plan,disabledLifecycle);
        require(independent.state().equals("FENCED")&&independent.steps().size()==plan.orderedSteps().size()
            &&disabledLifecycle.effects==0&&disabledLifecycle.stateRepairs==1,"disabled lifecycle suppressed authorized state work");
        Ports missingAdmission=new Ports() {
            @Override public OperationReport.StepReceipt controlled(TerminalOperationPlan p,TerminalOperationPlan.Step step) {
                return new OperationReport.StepReceipt(step,OperationReport.Status.UNAVAILABLE,"fixture-prerequisite-missing");
            }
        };
        require(runner.run(plan,missingAdmission).steps().size()==1&&missingAdmission.prepares==0,
            "independent execution bypassed mandatory admission prerequisite");
        var stopped = runner.run(plan, stale);
        require(stopped.state().equals("FENCED") && stale.effects == 0 && stale.prepares == 0, "stale plan continued into effects");
        Ports revoked = new Ports(); revoked.revokeAtEnter = true;
        var revokedReport = runner.run(plan, revoked);
        require(revokedReport.state().equals("FENCED") && revoked.effects == 0 && revoked.finishes == 1,
            "revocation during durable effect entry escaped the final check");
        Ports last = new Ports(); last.revokeAtNetwork = true;
        require(runner.run(plan, last).state().equals("FENCED"), "final step lacked postvalidation");
        var retainedPermission = new java.util.concurrent.atomic.AtomicReference<java.util.function.BooleanSupplier>();
        Ports captured = new Ports() {
            @Override public OperationReport.StepReceipt controlled(TerminalOperationPlan p, TerminalOperationPlan.Step step,
                java.util.function.BooleanSupplier permitted) {
                retainedPermission.set(permitted);
                return super.controlled(p, step, permitted);
            }
        };
        runner.run(plan, captured);
        require(!retainedPermission.get().getAsBoolean(), "a deferred sink reused the closed plan lease");
        Ports revokedAtWrite = new Ports() {
            @Override public OperationReport.StepReceipt controlled(TerminalOperationPlan p, TerminalOperationPlan.Step step,
                java.util.function.BooleanSupplier permitted) {
                if (step == TerminalOperationPlan.Step.TERMINAL_STATE) valid = false;
                return super.controlled(p, step, permitted);
            }
        };
        require(runner.run(plan, revokedAtWrite).state().equals("FENCED") && revokedAtWrite.stateRepairs == 0,
            "revoked permission reached the final state write");
        long[] now = {0};
        var expiringRunner = new OperationPlanRunner(() -> now[0]);
        Ports slow = new Ports() {
            @Override public OperationReport.StepReceipt controlled(TerminalOperationPlan p, TerminalOperationPlan.Step step) {
                now[0] = PlanExecutionLease.MAX_DURATION_NANOS;
                return super.controlled(p, step);
            }
        };
        require(expiringRunner.run(plan, slow).state().equals("FENCED") && slow.prepares == 0, "expired lease renewed between steps");
        now[0] = Long.MAX_VALUE - 10;
        try (var lease = new PlanExecutionLease(plan, () -> now[0], 100)) {
            now[0] += 20;
            require(lease.valid(plan), "monotonic timer wrap broke a live lease");
            var crossThreadAccepted = new java.util.concurrent.atomic.AtomicBoolean(true);
            Thread other = new Thread(() -> crossThreadAccepted.set(lease.valid(plan)));
            other.start();
            try { other.join(); } catch (InterruptedException failure) { throw new AssertionError(failure); }
            require(!crossThreadAccepted.get(), "lease crossed execution threads");
            lease.close();
            require(!lease.valid(plan), "closed lease remained usable");
        }
        try { new OperationReport(plan, first.steps(), true, true, "PARTIAL"); throw new AssertionError("unproven completion accepted"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("OperationPlanRunnerSelfTest: PASS (ordered Root checks, stale fence, opaque unknown, no replay, feedback suppression, no false completion)");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
