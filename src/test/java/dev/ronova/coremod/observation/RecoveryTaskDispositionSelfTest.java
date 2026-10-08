package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.integrity.*;
import dev.ronova.coremod.persistence.LocalDominionStore;
import dev.ronova.coremod.persistence.LocalStoreTestAccess;
import dev.ronova.coremod.policy.StrengthProfile;
import dev.ronova.coremod.root.api.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Store/disposition regression for the cancellation-observer linearization boundary. */
public final class RecoveryTaskDispositionSelfTest {
    public static void main(String[] args) throws Exception {
        RecoveryTaskObserverLaneSelfTest.run();
        verifySynchronizedCancelSettlesQueuedEpoch();
        verifyPeriodicOverlapWaitsForRunningExit();
        verifyUnsynchronizedCancelStaysUnknown();
        verifyObservedExternalCancelSettlesWithoutReplay();
        verifyExternalCancelAfterRejectedAttemptStillSettlesQueue();
        System.out.println("RecoveryTaskDispositionSelfTest PASS");
    }

    private static void verifySynchronizedCancelSettlesQueuedEpoch() throws Exception {
        try(var fixture=Fixture.open(true)) {
            fixture.run();
            var result=fixture.result();
            require(result!=null&&result.has(RecoveryWorkResult.Fact.CANCEL_REQUESTED)
                &&result.has(RecoveryWorkResult.Fact.CANCEL_ACCEPTED)
                &&result.has(RecoveryWorkResult.Fact.NOT_EXECUTED)
                &&result.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED)
                &&!result.has(RecoveryWorkResult.Fact.EFFECT_UNKNOWN),
                "synchronized accepted cancel did not settle the queued epoch and closure");
            var view=fixture.graph.view(fixture.registration);
            require(view!=null&&view.cancellationAccepted()&&view.pendingSubmissions()==0
                &&view.precludedSubmissions()==1&&view.queuedEpochPrecluded(),
                "synchronized accepted cancel did not move the observed queue epoch to PRECLUDED");
        }
    }


    private static void verifyPeriodicOverlapWaitsForRunningExit() throws Exception {
        try(var fixture=Fixture.open(true,true)) {
            fixture.run(1);
            var first=fixture.result();
            require(first!=null&&first.has(RecoveryWorkResult.Fact.CANCEL_ACCEPTED)
                &&first.has(RecoveryWorkResult.Fact.NOT_EXECUTED)
                &&!first.has(RecoveryWorkResult.Fact.EXECUTION_EXITED)
                &&!first.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED),
                "periodic overlap closed before the running epoch exited");
            var beforeExit=fixture.graph.view(fixture.registration);
            require(beforeExit!=null&&beforeExit.activeExecutions()==1&&beforeExit.precludedSubmissions()==1
                &&!fixture.graph.successorsClosed(fixture.registration,64),
                "queued preclusion discharged a distinct running epoch");
            fixture.graph.exited(fixture.task,1);
            fixture.run(2);
            var settled=fixture.result();
            require(settled!=null&&settled.has(RecoveryWorkResult.Fact.NOT_EXECUTED)
                &&settled.has(RecoveryWorkResult.Fact.EXECUTION_EXITED)
                &&settled.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED),
                "periodic overlap did not retain both epoch facts before closing");
            require(fixture.cancelCalls.get()==1,"periodic overlap replayed the cancellation attempt");
        }
    }

    private static void verifyUnsynchronizedCancelStaysUnknown() throws Exception {
        try(var fixture=Fixture.open(false)) {
            fixture.run();
            var result=fixture.result();
            require(result!=null&&result.has(RecoveryWorkResult.Fact.CANCEL_REQUESTED)
                &&result.has(RecoveryWorkResult.Fact.CANCEL_ACCEPTED)
                &&result.has(RecoveryWorkResult.Fact.EFFECT_UNKNOWN)
                &&!result.has(RecoveryWorkResult.Fact.NOT_EXECUTED)
                &&!result.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED),
                "unsynchronized accepted cancel invented a queue settlement or closure");
            var view=fixture.graph.view(fixture.registration);
            require(view!=null&&view.cancellationAccepted()&&view.pendingSubmissions()==1
                &&view.precludedSubmissions()==0&&!view.queuedEpochPrecluded(),
                "unsynchronized accepted cancel mutated the stale Graph into PRECLUDED");
        }
    }

    private static void verifyObservedExternalCancelSettlesWithoutReplay() throws Exception {
        try(var fixture=Fixture.open(true)) {
            fixture.graph.queuePrecluded(fixture.task);
            fixture.run(1,0);
            var result=fixture.result();
            require(result!=null&&result.has(RecoveryWorkResult.Fact.NOT_EXECUTED)
                &&result.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED)
                &&!result.has(RecoveryWorkResult.Fact.CANCEL_REQUESTED)
                &&!result.has(RecoveryWorkResult.Fact.CANCEL_ACCEPTED)
                &&!result.has(RecoveryWorkResult.Fact.CANCEL_REJECTED)
                &&!result.has(RecoveryWorkResult.Fact.EFFECT_UNKNOWN),
                "an observed external cancel was replayed or failed to settle durably");
            require(fixture.cancelCalls.get()==0,"an already-precluded queue epoch replayed cancellation");
        }
    }

    private static void verifyExternalCancelAfterRejectedAttemptStillSettlesQueue() throws Exception {
        try(var fixture=Fixture.open(true,false,2)) {
            fixture.run(1,1);
            var rejected=fixture.result();
            require(rejected!=null&&rejected.has(RecoveryWorkResult.Fact.CANCEL_REQUESTED)
                &&rejected.has(RecoveryWorkResult.Fact.CANCEL_REJECTED)
                &&!rejected.has(RecoveryWorkResult.Fact.NOT_EXECUTED),
                "rejected attempt unexpectedly settled the queue before external cancellation");
            fixture.graph.queuePrecluded(fixture.task);
            fixture.run(2,1);
            var settled=fixture.result();
            require(settled!=null&&settled.has(RecoveryWorkResult.Fact.CANCEL_REJECTED)
                &&settled.has(RecoveryWorkResult.Fact.NOT_EXECUTED)
                &&settled.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED),
                "a later observed external cancellation was hidden by an earlier rejected attempt");
        }
    }

    private static final class Fixture implements AutoCloseable {
        final Path world;
        final LocalDominionStore store;
        final TaskExecutionGraph graph;
        final Object task,scheduler;
        final UUID registration;
        final RecoveryTaskPlan plan;
        final RecoverySourceEvidence evidence;
        final RecoveryTaskDisposition disposition;
        final AtomicInteger cancelCalls=new AtomicInteger();
        final FrameBudgetGovernor governor=new FrameBudgetGovernor(false);

        private Fixture(Path world,LocalDominionStore store,TaskExecutionGraph graph,Object task,Object scheduler,UUID registration,
                        RecoveryTaskPlan plan,RecoverySourceEvidence evidence,boolean synchronizedEvents,int cancelResult) {
            this.world=world;this.store=store;this.graph=graph;this.task=task;this.scheduler=scheduler;
            this.registration=registration;this.plan=plan;this.evidence=evidence;
            var proof=new EnforcementProof(true,true,true,0x6000,0x6000,1,1,0,1,0,1,0,0);
            var code=new CodeIntegrityProof(true,1,1,1,0);
            var enforcement=new EnforcementRuntime(new EnforcementProofSource() {
                @Override public EnforcementProof read(){return proof;}
                @Override public CodeIntegrityProof codeIntegrity(){return code;}
            });
            disposition=new RecoveryTaskDisposition(store,enforcement,graph,
                (view,prior)->view.registration().equals(registration)
                    ?new RecoveryTaskDisposition.Candidate(task,plan,evidence,()->true):null,
                bytes->()->{},()->{
                    if(!synchronizedEvents)return false;
                    // Production receives this from BootstrapTaskHooks.CANCEL after the CAS. The fixture
                    // injects the same observed queue settlement before the boundary reports current.
                    graph.queuePrecluded(task);return true;
                },
                (target,lease,candidate)->{cancelCalls.incrementAndGet();return cancelResult;});
        }

        static Fixture open(boolean synchronizedEvents) throws Exception { return open(synchronizedEvents,false,1); }
        static Fixture open(boolean synchronizedEvents,boolean overlap) throws Exception { return open(synchronizedEvents,overlap,1); }
        static Fixture open(boolean synchronizedEvents,boolean overlap,int cancelResult) throws Exception {
            Path world=Files.createTempDirectory("ronova-task-disposition-").toAbsolutePath().normalize();
            LocalDominionStore store=LocalStoreTestAccess.openWorld(world);
            require(store.commit(new DominionCommand.ConfigureStrengthPolicy(
                store.strengthPolicyConfiguration().next(StrengthProfile.EXTREME,Map.of()))).committed(),"fixture policy failed");
            store=LocalStoreTestAccess.openWorld(world);
            UUID subject=UUID.randomUUID(),nonce=UUID.randomUUID();
            var judgment=new DominionDecision.Judgment(
                new SubjectIdentity(subject,"test:task-disposition","test.TaskDisposition","minecraft:overworld",0,0,0,false),
                store.nextDecisionEpoch(),true,DominionDecision.LineageScope.IDENTITY,0,"task-disposition-fixture");
            require(store.commit(store.qualify(JudgmentBatch.of(UUID.randomUUID(),judgment))
                .withIncarnations(Map.of(subject,nonce))).committed(),"fixture judgment failed");
            var terminal=store.approveTerminalPlan(subject,nonce);require(terminal!=null,"fixture terminal plan unavailable");
            var location=new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.STATIC_MANAGER,"","","test.Holder","records","","");
            var evidence=new RecoverySourceEvidence(location.locatorHash(),store.authorityStamp(subject),1,1,true,System.nanoTime());
            var graph=new TaskExecutionGraph(8);Object task=new Object(),scheduler=new Object();
            UUID registration=graph.submitted(task,scheduler,null);require(registration!=null,"fixture task registration failed");
            if(overlap) {graph.entered(task,1,null);graph.submitted(task,scheduler,null);}
            graph.sourceUsed(task,new TaskExecutionGraph.SourceUse(terminal.qualification(),evidence.sourceLocator(),1,1));
            var description=new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.TASK_CANCEL,location,registration,1,1,
                EffectRecord.hash("registered-task-adapter"),EffectRecord.hash("baseline"),EffectRecord.hash("expected"));
            var plan=store.approveRecoveryTaskPlan(terminal,evidence,description,true);require(plan!=null,"fixture task plan unavailable");
            return new Fixture(world,store,graph,task,scheduler,registration,plan,evidence,synchronizedEvents,cancelResult);
        }

        void run(){run(1,1);}
        void run(long tick){run(tick,1);}
        void run(long tick,int expectedCancelCalls) {
            governor.beginTick(tick);
            disposition.maintain(governor.budget(BudgetCategory.MAINTENANCE));
            require(cancelCalls.get()==expectedCancelCalls,"fixture cancellation-attempt count changed: "+cancelCalls.get());
        }
        RecoveryWorkResult result(){return store.recoveryWorkResult(plan.effectKey());}
        @Override public void close() throws Exception {
            disposition.close();
            try(var paths=Files.walk(world)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
        }
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
