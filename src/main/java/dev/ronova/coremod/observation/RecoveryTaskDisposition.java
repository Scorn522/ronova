package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.integrity.EnforcementRuntime;
import dev.ronova.coremod.nativebridge.NativeBridge;
import dev.ronova.coremod.persistence.LocalDominionStore;
import dev.ronova.coremod.root.api.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.*;

/** Executes registered task-state actions; task exit, descendants and queue resources remain independent. */
final class RecoveryTaskDisposition implements AutoCloseable {
    @FunctionalInterface interface Canceller {
        int cancel(Object task,RootExecutionFence.Lease lease,RecoveryTaskPlan plan);
    }
    record Candidate(Object task,RecoveryTaskPlan plan,RecoverySourceEvidence evidence,BooleanSupplier bindingCurrent) {}
    private static final class Pending {
        final WeakReference<Object> task;
        final EffectKey key;
        RecoveryTaskPlan plan;
        final Runnable release;
        final EnumSet<RecoveryWorkResult.Fact> facts=EnumSet.noneOf(RecoveryWorkResult.Fact.class);
        boolean invoked;
        /** Queue settlement observed from the task stream rather than produced by this executor. */
        boolean observedQueueSettlement;
        /** An observed exit still owes its successor-closure proof; discharged on a later pass. */
        boolean closurePending;
        long reviewedRevision=-1,nextReviewTick;
        int reviewDelay=1;
        Pending(Object task,RecoveryTaskPlan plan,Runnable release){this.task=new WeakReference<>(task);this.plan=plan;this.key=plan.effectKey();this.release=release;}
    }
    private final LocalDominionStore store;
    private final EnforcementRuntime enforcement;
    private final TaskExecutionGraph graph;
    private final BiFunction<TaskExecutionGraph.View,RecoveryTaskPlan,Candidate> resolve;
    private final IntFunction<Runnable> reserve;
    private final BooleanSupplier synchronizeCancellationEvents;
    private final Canceller canceller;
    private final Map<UUID,Pending> pending=new LinkedHashMap<>();
    private int cursor;
    private long accepted,rejected,unknown,attempts,exits,queuedSettled,successorsClosed,expiredWitnessContinuations,maxInvocationWitnessAge;
    private boolean closed;
    private String lastStage="IDLE";
    private EffectKey lastKey;
    private WeakReference<Object> delayedTask=new WeakReference<>(null);
    private long invocationDelayNanos,delayStarted;
    /** Bounded timing fault only: no evidence, ACK, permission, or result is supplied by the caller. */
    boolean delayInvocation(Object task,long nanos) {
        if(closed||task==null||nanos<1||nanos>30_000_000_000L||delayedTask.get()!=null)return false;
        delayedTask=new WeakReference<>(task);invocationDelayNanos=nanos;delayStarted=0;return true;
    }
    RecoveryTaskDisposition(LocalDominionStore store,EnforcementRuntime enforcement,TaskExecutionGraph graph,
        BiFunction<TaskExecutionGraph.View,RecoveryTaskPlan,Candidate> resolve,IntFunction<Runnable> reserve,
        BooleanSupplier synchronizeCancellationEvents) {
        this(store,enforcement,graph,resolve,reserve,synchronizeCancellationEvents,NativeBridge::cancelRegisteredFuture);
    }
    RecoveryTaskDisposition(LocalDominionStore store,EnforcementRuntime enforcement,TaskExecutionGraph graph,
        BiFunction<TaskExecutionGraph.View,RecoveryTaskPlan,Candidate> resolve,IntFunction<Runnable> reserve,
        BooleanSupplier synchronizeCancellationEvents,Canceller canceller) {
        this.store=Objects.requireNonNull(store);this.enforcement=Objects.requireNonNull(enforcement);this.graph=Objects.requireNonNull(graph);
        this.resolve=Objects.requireNonNull(resolve);this.reserve=Objects.requireNonNull(reserve);
        this.synchronizeCancellationEvents=Objects.requireNonNull(synchronizeCancellationEvents);
        this.canceller=Objects.requireNonNull(canceller);
    }
    void maintain(WorkBudget budget) {
        if(closed||store.readOnly())return;
        for(int i=0;i<4&&budget.tryConsumeCount(1);i++) {
            if(cursor>=graph.size())cursor=0;
            var rows=graph.page(cursor,1);if(rows.isEmpty())break;cursor++;
            var view=rows.get(0);Pending work=pending.get(view.registration());
            if(work!=null) {
                long tick=budget.tickId();boolean changed=work.reviewedRevision!=view.revision();
                // A latched closure obligation is an independent reason to keep reviewing. The retry
                // key is this node's own revision, which a settling descendant does not bump, so
                // without this the latch would never be discharged.
                if(!changed&&(work.invoked&&work.facts.isEmpty()&&!work.closurePending||tick<work.nextReviewTick))continue;
                if(changed)work.reviewDelay=1;
                work.reviewedRevision=view.revision();work.nextReviewTick=tick+work.reviewDelay;
                work.reviewDelay=Math.min(20,work.reviewDelay*2);
                // Queue settlement is observable independently of which actor performed the successful
                // FutureTask cancel. If a linearized CANCEL reaches the Graph after this work already
                // invoked (or even lost a race and received CANCEL_REJECTED), retain NOT_EXECUTED too:
                // it describes the queued epoch, not ownership of the cancelling CAS.
                if(view.queuedEpochPrecluded()&&view.pendingSubmissions()==0
                    &&!work.facts.contains(RecoveryWorkResult.Fact.NOT_EXECUTED)) {
                    var persisted=store.recoveryWorkResult(work.key);
                    if(persisted==null||!persisted.has(RecoveryWorkResult.Fact.NOT_EXECUTED)) {
                        work.facts.add(RecoveryWorkResult.Fact.NOT_EXECUTED);queuedSettled++;work.closurePending=true;
                    }
                }
                if(view.executionsExited()&&!work.facts.contains(RecoveryWorkResult.Fact.EXECUTION_EXITED)) {
                    // Entries>0 only: a queued wrapper that never entered execution is settled
                    // through NOT_EXECUTED instead, and never claims an exit it did not have.
                    var persisted=store.recoveryWorkResult(work.key);
                    // An exit and a queue settlement are facts about *different* epochs and must both
                    // survive: a periodic task can genuinely exit the running epoch while a later
                    // queued epoch was precluded by an accepted cancel. Suppressing this one because
                    // NOT_EXECUTED exists would hide a real observed exit, so the two are kept
                    // separate rather than treated as mutually exclusive whole-work bits.
                    if(persisted!=null&&!persisted.has(RecoveryWorkResult.Fact.EXECUTION_EXITED)) {
                        work.facts.add(RecoveryWorkResult.Fact.EXECUTION_EXITED);exits++;work.closurePending=true;
                    }
                }
                flush(work);
                // A task that ran and exited carries its own closure proof on the normal path too;
                // descendants and the exited node itself must all be settled before this is recorded.
                // The obligation is latched rather than gated on the fact set: flush() removes the
                // facts it persisted, so a completed flush no longer contains EXECUTION_EXITED and a
                // set-membership test would silently restrict this call to the failed-persist path.
                settleClosure(work,view.registration());
                var prior=store.recoveryWorkResult(work.key);
                if(prior!=null&&(prior.has(RecoveryWorkResult.Fact.CANCEL_REQUESTED)||prior.has(RecoveryWorkResult.Fact.EFFECT_UNKNOWN)))work.invoked=true;
                if(work.invoked)continue;
            }
            if(view.evidenceLost()||view.sources().size()!=1)continue;
            Candidate candidate=resolve.apply(view,work==null?null:work.plan);if(candidate==null||!candidate.bindingCurrent.getAsBoolean())continue;
            if(work!=null&&!work.key.equals(candidate.plan.effectKey())) {
                // No native operation was invoked for this old scope. Preserve its history,
                // then acquire a distinct intent for the stronger newly observed scope.
                if(store.recoveryWorkResult(work.key)!=null){work.facts.add(RecoveryWorkResult.Fact.NOT_CURRENT);flush(work);}
                else if(store.persistencePending())continue;
                if(!work.facts.isEmpty())continue;
                pending.remove(view.registration());work.task.clear();work.release.run();work=null;
            }
            if(work==null) {
                if(pending.size()>=128)continue;
                Runnable release=reserve.apply(4096);if(release==null)continue;
                work=new Pending(candidate.task,candidate.plan,release);pending.put(view.registration(),work);
            }
            if(work.task.get()!=candidate.task||!work.key.equals(candidate.plan.effectKey()))continue;
            lastKey=work.key;
            if(!store.recoveryTaskContinuationCurrent(candidate.plan,candidate.bindingCurrent.getAsBoolean())
                &&!store.prepareRecoveryTask(candidate.plan,candidate.evidence,candidate.bindingCurrent.getAsBoolean())){lastStage="PREPARE_REJECTED";continue;}
            work.plan=candidate.plan;
            var intent=store.recoveryWorkResult(work.key);if(intent==null){lastStage="ACK_UNAVAILABLE";continue;}
            // A successful FutureTask.cancel can be observed independently of Ronova's executor. If
            // its linearized CANCEL event already settled the queued epoch, persist that observation
            // directly instead of replaying another cancellation attempt just to manufacture the same
            // NOT_EXECUTED fact. Running epochs remain independent and are handled by the exit path.
            if(intent.has(RecoveryWorkResult.Fact.NOT_EXECUTED)&&!intent.has(RecoveryWorkResult.Fact.CANCEL_REQUESTED))
                work.observedQueueSettlement=true;
            if(!work.invoked&&!work.observedQueueSettlement&&view.queuedEpochPrecluded()&&view.pendingSubmissions()==0
                &&!intent.has(RecoveryWorkResult.Fact.NOT_EXECUTED)) {
                work.observedQueueSettlement=true;work.facts.add(RecoveryWorkResult.Fact.NOT_EXECUTED);queuedSettled++;
                work.closurePending=!intent.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED);
                lastStage="OBSERVED_QUEUE_PRECLUDED";flush(work);settleClosure(work,view.registration());continue;
            }
            if(work.observedQueueSettlement) {
                work.closurePending|=!intent.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED);
                settleClosure(work,view.registration());continue;
            }
            // The in-memory latch is a cache of the obligation, never the obligation itself: a restored
            // intent that already recorded an exit or a queue settlement without successor closure
            // re-arms it, so losing the Pending object cannot silently drop the obligation.
            if(!intent.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED)
                &&(intent.has(RecoveryWorkResult.Fact.EXECUTION_EXITED)||intent.has(RecoveryWorkResult.Fact.NOT_EXECUTED)))
                work.closurePending=true;
            if(intent.has(RecoveryWorkResult.Fact.CANCEL_REQUESTED)||intent.has(RecoveryWorkResult.Fact.EFFECT_UNKNOWN)) {
                lastStage="QUERY_EXISTING_UNKNOWN";work.invoked=true;continue; // Resume by querying; never replay an ambiguous attempt here.
            }
            if(delayedTask.get()==candidate.task) {
                long now=System.nanoTime();if(delayStarted==0)delayStarted=now;
                if(now-delayStarted<invocationDelayNanos){lastStage="INVOCATION_DELAY_AFTER_ACK";continue;}
                delayedTask.clear();invocationDelayNanos=delayStarted=0;
            }
            try(var proof=enforcement.beginObservation(0x6000)) {
                if(proof==null||!proof.stillCurrent()||!candidate.bindingCurrent.getAsBoolean()){lastStage="PROOF_OR_BINDING_UNAVAILABLE";continue;}
                try(var lease=store.acquireTaskContinuation(candidate.plan,candidate.bindingCurrent.getAsBoolean())) {
                    if(lease==null||!lease.valid(candidate.plan)||!candidate.bindingCurrent.getAsBoolean()||!proof.stillCurrent()){lastStage="LEASE_OR_FINAL_BINDING_UNAVAILABLE";continue;}
                    int result=canceller.cancel(candidate.task,lease,candidate.plan);
                    if(result==0){lastStage="NATIVE_NOT_INVOKED";continue;}
                    lastStage="NATIVE_INVOKED";
                    work.invoked=true;attempts++;
                    store.retireTaskContinuation(candidate.plan);work.plan=null;
                    if(candidate.evidence!=null) {
                        long age=System.nanoTime()-candidate.evidence.observedAtNanos();
                        if(age>=0)maxInvocationWitnessAge=Math.max(maxInvocationWitnessAge,age);
                        if(age>RecoverySourceEvidence.MAX_AGE_NANOS)expiredWitnessContinuations++;
                    }
                    work.facts.add(RecoveryWorkResult.Fact.CANCEL_REQUESTED);
                    graph.cancellationRequested(view.registration(),intent.record().attemptId());
                    // The Graph snapshot taken before native invocation is never sufficient to mint
                    // NOT_EXECUTED: an ENTER/SUBMIT can have crossed the bootstrap hook but still be
                    // waiting in the observer inbox. BootstrapTaskHooks linearizes those publications
                    // against the FutureTask state CAS; after an accepted CAS, drain the bounded inbox
                    // through that boundary before deciding whether an epoch was still queued.
                    boolean didCancel=result==1;
                    boolean eventBoundaryCurrent=!didCancel||synchronizeCancellationEvents.getAsBoolean();
                    var settled=graph.view(view.registration());
                    if(didCancel&&(settled==null||settled.evidenceLost()))eventBoundaryCurrent=false;
                    boolean queuedEpochPrecluded=didCancel&&eventBoundaryCurrent&&settled!=null
                        &&settled.precludedSubmissions()>view.precludedSubmissions();
                    if(!proof.finish()||result==3) {work.facts.add(RecoveryWorkResult.Fact.EFFECT_UNKNOWN);unknown++;}
                    else {
                        work.facts.add(didCancel?RecoveryWorkResult.Fact.CANCEL_ACCEPTED:RecoveryWorkResult.Fact.CANCEL_REJECTED);
                        // Accepted state mutation and queue settlement are deliberately separate. PRECLUDED
                        // comes only from the CANCEL event emitted at the FutureTask CAS linearization point;
                        // the executor result itself never edits queue depth from a stale snapshot.
                        graph.cancellationReturned(view.registration(),intent.record().attemptId(),didCancel);
                        if(didCancel)accepted++;else rejected++;
                        if(didCancel&&!eventBoundaryCurrent) {
                            work.facts.add(RecoveryWorkResult.Fact.EFFECT_UNKNOWN);unknown++;
                            lastStage="NATIVE_INVOKED_EVENT_BOUNDARY_UNKNOWN";
                        } else if(didCancel&&queuedEpochPrecluded) {
                            // The synchronized stream proves an observed queued epoch had not entered at
                            // the CAS. It is PRECLUDED, while any separately running epoch may still
                            // later contribute EXECUTION_EXITED.
                            work.facts.add(RecoveryWorkResult.Fact.NOT_EXECUTED);queuedSettled++;
                            lastStage="NATIVE_INVOKED_QUEUED_SETTLED";
                            work.closurePending=true;
                        }
                    }
                }
            }
            flush(work);
            settleClosure(work,view.registration());
        }
    }
    /**
     * Attempts a latched successor-closure proof and clears the latch only once the closure fact is
     * recorded. A closure that is not yet provable stays latched and is retried by the bounded review
     * schedule even when only a descendant changed (the parent revision does not have to move).
     */
    private void settleClosure(Pending work,UUID registration) {
        if(work!=null&&work.closurePending&&recordSuccessorsClosed(work,registration))work.closurePending=false;
    }
    /**
     * Records successor closure for this task, but only against the exact same proof the queue
     * settlement uses: a complete observed subtree in which every descendant is settled. Facts are
     * flushed first because the store persists them one at a time by bit, so a partial write would
     * leave the intent half-recorded. Nothing here closes a resource or descendant obligation.
     *
     * @return true only when the store accepted the closure fact itself
     */
    private boolean recordSuccessorsClosed(Pending work,UUID registration) {
        if(work==null||registration==null)return false;
        if(!graph.successorsClosed(registration,64))return false;
        for(var iterator=work.facts.iterator();iterator.hasNext();) {
            var fact=iterator.next();if(store.observeRecoveryWork(work.key,fact))iterator.remove();
        }
        if(store.observeRecoveryWork(work.key,RecoveryWorkResult.Fact.SUCCESSORS_CLOSED)) {
            successorsClosed++;lastStage=lastStage+"_SUCCESSORS";return true;
        }
        return false;
    }
    private void flush(Pending work) {
        for(var iterator=work.facts.iterator();iterator.hasNext();) {
            var fact=iterator.next();if(!store.observeRecoveryWork(work.key,fact))break;iterator.remove();
        }
        if(work.invoked&&work.facts.isEmpty())store.releaseTaskContinuationGuard(work.key);
    }
    Map<String,Long> counters(){return Map.ofEntries(Map.entry("task_intents",(long)pending.size()),
        Map.entry("cancel_attempts",attempts),Map.entry("cancel_accepted",accepted),Map.entry("cancel_rejected",rejected),
        Map.entry("cancel_unknown",unknown),Map.entry("execution_exits",exits),Map.entry("queued_not_executed",queuedSettled),
        Map.entry("successors_closed",successorsClosed),Map.entry("expired_witness_continuations",expiredWitnessContinuations),
        Map.entry("max_invocation_witness_age_ns",maxInvocationWitnessAge));}
    String diagnostic(){var result=lastKey==null?null:store.recoveryWorkResult(lastKey);return lastStage+" result="+
        (result==null?"unavailable":result.record().state()+":"+result.record().evidenceRef());}
    @Override public void close() {
        if(closed)return;closed=true;
        delayedTask.clear();
        RuntimeException failure=null;
        for(Pending work:pending.values()) {
            try{flush(work);}catch(RuntimeException error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
            finally {
                work.task.clear();
                try{work.release.run();}catch(RuntimeException error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
            }
        }
        pending.clear(); // Root retains every unresolved intent; no exit or resource completion is invented.
        if(failure!=null)throw failure;
    }
}
