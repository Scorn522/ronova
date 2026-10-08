package dev.ronova.coremod.obligation;

import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.root.api.*;
import dev.ronova.coremod.runtime.DominionServerRuntime;
import java.util.*;

/** Reviews bounded detail fairly; durable unresolved effects stay in the one Root journal. */
public final class ObligationCoordinator {
    private final DominionServerRuntime runtime;
    private final ObligationRegistry registry;
    private int effectCursor;
    private int scopeTurn;
    private boolean reviewTurn;
    private long eventClock, reviews;
    private final RestorationGapBuffer restorationGaps=new RestorationGapBuffer();
    private long checkpointRevision,lastPersistedVersion=-1,checkpointFailures;
    private boolean restored;
    public ObligationCoordinator(DominionServerRuntime runtime) {
        this.runtime=Objects.requireNonNull(runtime);registry=new ObligationRegistry(runtime.resources());
        var checkpoint=runtime.durableStore()==null?ObligationCheckpoint.EMPTY:runtime.durableStore().obligationCheckpoint();
        checkpointRevision=checkpoint.revision();eventClock=checkpoint.clock();restored=registry.restore(checkpoint);
    }
    public synchronized void admission(AuthorityStamp stamp, UUID physicalNonce) {
        if(stamp==null||physicalNonce==null||!stamp.settled()||stamp.qualification()==null)return;
        if(!restored){restorationGaps.missed();return;}
        var q=stamp.qualification();long now=clock();
        var acknowledged = runtime.durableStore()==null ? null : runtime.durableStore().authorityStamp(stamp.subjectKey().subjectId());
        if(acknowledged==null||!stamp.sameAuthority(acknowledged))return;
        boolean bindingConflict = !stamp.owner() && !q.terminal()
            && q.revisions()!=null && q.revisions().activeIncarnation()!=null
            && !q.revisions().activeIncarnation().nonce().equals(physicalNonce);
        registry.observe(new CandidateEvidence(new ObligationKey(q.targetEpoch(),ObligationKey.Source.ADMISSION_GATE,
            ObligationKey.Edge.ADMISSION,ObligationKey.Result.CURRENT_QUALIFICATION),stamp.policyRevision(),judgment(q),1,
            physicalNonce + "/" + now,"root-binding-comparison/v1",CandidateEvidence.Family.STALE_QUALIFICATION,now,deadline(now),
            bindingConflict,false)); // Attests only the Root binding mismatch, not arbitrary observer/Native integrity.
    }
    public synchronized void plan(OperationReport report, boolean accepted) {
        if(report==null)return;
        var current=runtime.durableStore()==null?null:runtime.durableStore().subjectQualification(report.plan().subjectKey());
        if(current==null||!current.targetEpoch().equals(report.plan().qualification().targetEpoch()))return;
        if(!restored){restorationGaps.missed();return;}
        var p=report.plan();long now=clock();
        registry.observe(new CandidateEvidence(new ObligationKey(p.qualification().targetEpoch(),ObligationKey.Source.TERMINAL_PLAN,
            ObligationKey.Edge.EXECUTION,ObligationKey.Result.TERMINAL_SERVER_RESULT),p.configuredPolicyRevision(),judgment(p.qualification()),1,
            EffectRecord.hash(p.causalityKey()+report.state()),"root-plan-report",CandidateEvidence.Family.STATE_RELATION,now,deadline(now),
            accepted,false));
    }
    public synchronized void reconstruction(dev.ronova.coremod.observation.RecoverySourceTracker.Witness witness) {
        if (witness == null || runtime.durableStore() == null) return;
        var stamp = witness.authority(); var q = stamp.qualification();
        var current = runtime.durableStore().authorityStamp(stamp.subjectKey().subjectId());
        if (q == null || !q.terminal() || stamp.owner() || !stamp.sameAuthority(current)
            || !witness.source().targetEpoch().equals(q.targetEpoch())) return;
        if(!restored){restorationGaps.missed();return;}
        long now = clock();
        String event = EffectRecord.hash(witness.source().locator() + "/" + witness.source().observedVersion()
            + "/" + witness.revision());
        registry.observe(new CandidateEvidence(new ObligationKey(q.targetEpoch(), ObligationKey.Source.SAVED_RECORD,
            ObligationKey.Edge.RECONSTRUCTION, ObligationKey.Result.RECOVERY_SOURCE_DISPOSAL),
            stamp.policyRevision(), judgment(q), 1, event, "observed-nbt-record-factory/v1",
            CandidateEvidence.Family.DIRECT_RECONSTRUCTION, now, deadline(now), witness.observerVerified(), false));
    }
    public synchronized void discoveryGap(TargetEpoch target) {
        if(runtime.durableStore()==null)return;
        var current=runtime.durableStore().subjectQualification(target.subjectKey());
        if(current==null||!current.targetEpoch().equals(target))return;
        if(!restored){restorationGaps.missed();return;}
        registry.recordCoverageGap(target,ObligationKey.Source.SAVED_RECORD,eventClock);
    }
    public synchronized void pulse(WorkBudget budget) {
        eventClock=increment(eventClock);
        if(runtime.durableStore()==null)return;
        // Retry the existing Root image independently of registry allocation or whether any obligations exist.
        if(runtime.durableStore().persistencePending()
            &&(eventClock%20!=0||!runtime.durableStore().retryPendingPersistence()))return;
        if(!restored) {
            // A failed allocation keeps completion blocked and retries without restaging 640 entries every tick.
            if(eventClock%20!=0)return;
            if(!flushRestorationGap())return;
            var checkpoint=runtime.durableStore().obligationCheckpoint();
            restored=registry.restore(checkpoint);
            if(!restored)return;
            checkpointRevision=checkpoint.revision();eventClock=Math.max(eventClock,checkpoint.clock());lastPersistedVersion=-1;
        }
        long operations=0;
        try { for(int i=0;i<8&&budget.tryConsumeCount(1);i++) {
            operations++;
            reviewTurn=!reviewTurn;
            if(reviewTurn) {
                var entry=registry.nextReview(clock());if(entry==null)continue;
                var current=runtime.durableStore().subjectQualification(entry.key().targetEpoch().subjectKey());
                if(registry.retireSuperseded(entry.key(),current)){reviews=increment(reviews);continue;}
                if(current==null||!current.targetEpoch().equals(entry.key().targetEpoch())
                    ||runtime.durableStore().strengthPolicyConfiguration().revision().value()!=entry.policyRevision())registry.invalidate(entry.key());
                reviews=increment(reviews);
            }else{
                if((++scopeTurn&7)==0) {
                    var scope=registry.nextOverflowScope();
                    if(scope!=null)registry.retireSupersededScope(scope.target(),runtime.durableStore().subjectQualification(scope.target().subjectKey()));
                    continue;
                }
                var page=runtime.durableStore().effectPage(effectCursor,1);
                if(page.isEmpty()){effectCursor=0;continue;}effectCursor++;
                var effect=page.get(0);if(!effect.unresolved())continue;long now=clock();
                var current=runtime.durableStore().subjectQualification(effect.key().targetEpoch().subjectKey());
                // Historical unknown effects remain in Root's journal. A closed qualification must not
                // continuously recreate a live diagnostic entry that was retired for that old epoch.
                if(current==null||!current.targetEpoch().equals(effect.key().targetEpoch()))continue;
                var key=new ObligationKey(effect.key().targetEpoch(),ObligationKey.Source.ROOT_EFFECT_JOURNAL,
                    ObligationKey.Edge.OPAQUE_EFFECT,ObligationKey.Result.EFFECT_RESOLUTION);
                String ref=EffectRecord.hash(effect.key().canonical());
                registry.observe(new CandidateEvidence(key,Math.max(1,effect.policyRevision()),0,1,ref,"root-effect-journal",
                    CandidateEvidence.Family.STATE_RELATION,now,deadline(now),true,false),ref);
            }
        }} finally { if(operations>0)budget.finish(operations,0); }
        if(eventClock%20==0)flushCheckpoint();
    }
    public synchronized boolean flushCheckpoint() {
        if(runtime.durableStore()==null)return false;
        if(runtime.durableStore().persistencePending()&&!runtime.durableStore().retryPendingPersistence())return false;
        if(!restored||registry.restorePending())return flushRestorationGap();
        if(registry.version()==lastPersistedVersion)return true;
        if(checkpointRevision==0&&registry.size()==0&&registry.coverage().overflowCount()==0)return true;
        var requested=new ObligationCheckpoint(Math.addExact(checkpointRevision,1),eventClock,registry.snapshot(),registry.coverage(),CandidateRestrictions.EMPTY,registry.overflowCheckpoint());
        var result=runtime.durableStore().checkpointObligations(checkpointRevision,requested);
        if(result.status()!=CheckpointReceipt.Status.UNKNOWN)checkpointRevision=result.acknowledged().revision();
        if(result.acknowledgedWrite()){lastPersistedVersion=registry.version();return true;}
        checkpointFailures=increment(checkpointFailures);return false;
    }
    private boolean flushRestorationGap() {
        var store=runtime.durableStore();
        var requested=restorationGaps.prepare(store.obligationCheckpoint(),eventClock);
        if(requested==null)return true;
        var receipt=store.checkpointObligations(restorationGaps.expectedRevision(),requested);
        if(restorationGaps.acknowledge(receipt)) {
            checkpointRevision=receipt.acknowledged().revision();lastPersistedVersion=-1;
            return !restorationGaps.pending();
        }
        checkpointFailures=increment(checkpointFailures);return false;
    }
    public synchronized long checkpointRevision() {return checkpointRevision;}
    public synchronized long checkpointFailures() {return checkpointFailures;}
    public ObligationRegistry registry() { return registry; }
    public synchronized long reviews() { return reviews; }
    public synchronized void clear() { registry.clear();effectCursor=0;restorationGaps.clear(); }
    private long clock() { return eventClock; }
    private static long deadline(long now) { return now>Long.MAX_VALUE-200?Long.MAX_VALUE:now+200; }
    private static long judgment(SubjectQualification q) { return q.revisions()==null?0:q.revisions().judgmentRevision(); }
    private static long increment(long n) { return n==Long.MAX_VALUE?n:n+1; }
}
