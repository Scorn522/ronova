package dev.ronova.coremod.obligation;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import java.util.*;

public final class CandidateRestrictionsSelfTest {
    public static void main(String[] args) {
        var key = new SubjectKey(new RealmId(new UUID(717,1)),new UUID(717,2));
        UUID valid=new UUID(717,3), stale=new UUID(717,4), second=new UUID(717,5);
        var q=QualificationPreconditions.baseline(key).rebind(valid);
        var registry=new ObligationRegistry(); observe(registry,q,stale,1,true);
        var overflowRegistry=new ObligationRegistry(1);
        var occupying=new ObligationKey(new TargetEpoch(new SubjectKey(key.realmId(),new UUID(900,1)),1),
            ObligationKey.Source.TERMINAL_PLAN,ObligationKey.Edge.EXECUTION,ObligationKey.Result.TERMINAL_SERVER_RESULT);
        overflowRegistry.observe(new CandidateEvidence(occupying,1,1,1,"occupying","root",CandidateEvidence.Family.DIRECT_RESULT_CONFLICT,0,200,true,false));
        observe(overflowRegistry,q,stale,1,true);
        var overflowCheckpoint=new ObligationCheckpoint(1,1,overflowRegistry.snapshot(),overflowRegistry.coverage(),
            CandidateRestrictions.EMPTY,overflowRegistry.overflowCheckpoint());
        require(overflowRegistry.retrySize()==1&&CandidateRestrictions.EMPTY.reconcile(overflowCheckpoint.allEntries(),Map.of(key,q),
            k->false,1,1).holds(q,stale),"retained overflow proof did not participate in Root-derived reason handoff");
        var held=CandidateRestrictions.EMPTY.reconcile(registry.snapshot(),Map.of(key,q),k->false,1,1);
        var restarted=new ObligationRegistry();
        require(restarted.restore(new ObligationCheckpoint(1,1,registry.snapshot(),registry.coverage())),"observation restore failed");
        require(CandidateRestrictions.EMPTY.reconcile(restarted.snapshot(),Map.of(key,q),k->false,1,2).reasons().isEmpty(),
            "historical proof created a new restriction before observer revalidation");
        observe(restarted,q,second,2,true);
        var revalidated=CandidateRestrictions.EMPTY.reconcile(restarted.snapshot(),Map.of(key,q),k->false,1,2);
        require(revalidated.holds(q,second)&&!revalidated.holds(q,stale),
            "one fresh nonce reauthorized another nonce from the historical checkpoint");
        require(held.holds(q,stale)&&!held.holds(q,valid),"hold escaped physical scope");
        require(held.equals(held.reconcile(registry.snapshot(),Map.of(key,q),k->false,1,2)),"duplicate evidence advanced restriction revision");
        observe(registry,q,second,2,true);
        held=held.reconcile(registry.snapshot(),Map.of(key,q),k->false,1,2);
        var reason=held.reasons().stream().filter(r->r.physicalNonce().equals(stale)).findFirst().orElseThrow();
        var pending=held.requestRelease(reason.id(),reason.revision(),3,EffectRecord.hash("verified-scope-proof"));
        var pendingReason=pending.reasons().stream().filter(r->r.id().equals(reason.id())).findFirst().orElseThrow();
        require(pendingReason.decision()==CandidateRestrictions.Decision.PROVEN_UNRELATED
            &&pendingReason.release()==CandidateRestrictions.Release.RELEASE_PENDING,"release debounce changed evidence decision");
        require(pending.requestRelease(reason.id(),pendingReason.revision(),99,EffectRecord.hash("duplicate-proof")).equals(pending),
            "repeated proof extended deadline");
        var released=pending.reconcile(registry.snapshot(),Map.of(key,q),k->false,1,103);
        require(!released.holds(q,stale)&&released.holds(q,second),"release cleared another valid reason");
        observe(registry,q,stale,104,true);
        var fresh=released.reconcile(registry.snapshot(),Map.of(key,q),k->false,1,104);
        require(fresh.holds(q,stale)&&fresh.revision()>released.revision(),"new valid conflict did not create a new decision");
        var rebound=q.rebind(stale);
        var legal=fresh.reconcile(registry.snapshot(),Map.of(key,rebound),k->false,1,105);
        require(legal.reasons().stream().noneMatch(CandidateRestrictions.Reason::holds),"legal qualification transition delayed by debounce");
        var unverified=new ObligationRegistry(); observe(unverified,q,stale,1,false);
        require(CandidateRestrictions.EMPTY.reconcile(unverified.snapshot(),Map.of(key,q),k->false,1,1).reasons().isEmpty(),
            "unverified observation created a durable restriction");
        require(CandidateRestrictions.EMPTY.reconcile(registry.snapshot(),Map.of(key,q),k->true,1,104).reasons().isEmpty(),
            "protected subject was held");
        var flood=new ObligationRegistry();var compact=CandidateRestrictions.EMPTY;
        for(int i=0;i<1000;i++) {
            observe(flood,q,new UUID(800,i),i+1,true);
            compact=compact.reconcile(flood.snapshot(),Map.of(key,q),k->false,1,i+1);
        }
        require(compact.reasons().size()==1&&compact.reasons().get(0).physicalNonce()==null&&compact.retiredCount()>=9,
            "different physical keys did not hand off to a bounded target outlet");
        require(compact.holds(q,stale)&&compact.holds(q,second),"compaction lost an old reason's scope");
        require(!compact.holds(q,valid),"summary denied the current legitimate incarnation");
        var summary=compact.reasons().get(0);
        try{compact.requestRelease(summary.id(),summary.revision(),1001,EffectRecord.hash("one unrelated sample"));
            throw new AssertionError("one sample released an entire summary");}catch(IllegalArgumentException expected){}
        require(compact.reconcile(flood.snapshot(),Map.of(key,q.rebind(second)),k->false,1,1001).reasons().stream()
            .noneMatch(CandidateRestrictions.Reason::holds),"summary did not release after whole-scope qualification transition");
        System.out.println("CandidateRestrictionsSelfTest: PASS (exact scope, independent reasons, fixed release deadline, duplicate immunity, new evidence, legal transition)");
    }
    private static void observe(ObligationRegistry registry,SubjectQualification q,UUID nonce,long clock,boolean verified) {
        registry.observe(new CandidateEvidence(new ObligationKey(q.targetEpoch(),ObligationKey.Source.ADMISSION_GATE,ObligationKey.Edge.ADMISSION,
            ObligationKey.Result.CURRENT_QUALIFICATION),1,q.revisions().judgmentRevision(),1,nonce + "/" + clock,"root-binding-comparison/v1",
            CandidateEvidence.Family.STALE_QUALIFICATION,clock,clock+200,verified,false));
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
