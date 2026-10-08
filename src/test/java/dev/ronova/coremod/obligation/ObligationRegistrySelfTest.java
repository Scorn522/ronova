package dev.ronova.coremod.obligation;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.TargetEpoch;
import java.util.*;

public final class ObligationRegistrySelfTest {
    public static void main(String[] args) {
        var key=key(1);var registry=new ObligationRegistry(3);
        registry.observe(e(key,1,"event-1","basis-1",CandidateEvidence.Family.TIME_RELATION,true,false));
        registry.observe(e(key,1,"event-1","basis-2",CandidateEvidence.Family.STATE_RELATION,true,false));
        registry.observe(e(key,1,"event-2","basis-1",CandidateEvidence.Family.STATE_RELATION,true,false));
        require(registry.snapshot().get(0).candidate()==ObligationRegistry.Candidate.OBSERVED,"copied weak evidence became independent");
        registry.observe(e(key,1,"event-3","basis-3",CandidateEvidence.Family.CALLBACK_RELATION,true,false));
        require(registry.snapshot().get(0).candidate()==ObligationRegistry.Candidate.CANDIDATE,"independent weak families did not qualify");
        registry.nextReview(1000);
        require(registry.snapshot().get(0).state()==ObligationRegistry.State.NEEDS_REVALIDATION,"expiry silently closed an established obligation");
        registry.observe(e(key,2,"new-policy","new-basis",CandidateEvidence.Family.STATE_RELATION,true,false));
        require(registry.snapshot().get(0).candidate()==ObligationRegistry.Candidate.CANDIDATE&&registry.snapshot().get(0).state()==ObligationRegistry.State.NEEDS_REVALIDATION&&registry.snapshot().get(0).mixedContext(),"old proof was reused under another policy");
        registry.observe(e(key(2),1,"authorized","valid-operation",CandidateEvidence.Family.DIRECT_RESULT_CONFLICT,true,true));
        registry.observe(e(key(3),1,"unverified","self-report",CandidateEvidence.Family.STALE_QUALIFICATION,false,false));
        require(registry.snapshot().stream().noneMatch(v->v.candidate()==ObligationRegistry.Candidate.CANDIDATE&&v.state()==ObligationRegistry.State.UNRESOLVED),"authorized/unverified observation became Candidate");
        for(int i=0;i<100000;i++)registry.observe(e(key(3),1,"unverified","self-report",CandidateEvidence.Family.STALE_QUALIFICATION,false,false));
        require(registry.size()==3&&registry.snapshot().get(2).count()==100001&&registry.snapshot().get(2).samples().size()==1,"duplicate flood grew detail or fabricated evidence");
        Set<SubjectKey> reviewed=new HashSet<>();for(int i=0;i<3;i++)reviewed.add(registry.nextReview(10).key().targetEpoch().subjectKey());
        require(reviewed.size()==3,"hot subject starved unrelated subjects");
        registry.observe(e(key(4),1,"overflow","overflow",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        require(registry.size()==3&&registry.coverage().overflowCount()==1&&registry.coverage().needsRevalidation(),"overflow erased the coverage gap or grew detail");
        var strong=new ObligationRegistry();strong.observe(e(key,1,"strong","root-witness",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        require(strong.snapshot().get(0).candidate()==ObligationRegistry.Candidate.CANDIDATE,"verified strong evidence was not admitted");
        var durable=new ObligationRegistry();var seed=e(key,1,"durable","root-journal",CandidateEvidence.Family.STATE_RELATION,true,false);
        durable.observe(seed,"effect:one");durable.observe(seed,"effect:one");
        require(durable.snapshot().get(0).count()==1,"resampling one durable fact fabricated repeated events");
        revalidationDoesNotReplayEvidence();
        retirementFollowsQualification();
        coverageRemainsScoped();
        sourceFairness();
        durableContextChangesAreCheckpointed();
        boundedReclamation();
        contextRoundTripRejectsOldProof();
        overflowTiersRetainAndReviewCandidates();
        restoreRetirementAndBudgetRecovery();
        System.out.println("ObligationRegistrySelfTest: PASS (strong/two-independent-weak rules, authorized/unverified exclusion, context expiry, 100k merge, fair rotation, overflow, durable dedup)");
    }
    private static void revalidationDoesNotReplayEvidence() {
        var key=key(30);var live=new ObligationRegistry();
        var historical=e(key,1,"old","old-basis",CandidateEvidence.Family.STALE_QUALIFICATION,true,false);
        live.observe(historical);
        var restored=new ObligationRegistry();
        require(restored.restore(new ObligationCheckpoint(1,10,live.snapshot(),live.coverage())),"restore failed");
        require(restored.snapshot().get(0).samples().stream().noneMatch(CandidateEvidence::observerVerified)&&restored.pending(key.targetEpoch()),
            "restore replayed attestations or forgot an unresolved obligation");
        var repeated=new ObligationRegistry();
        require(repeated.restore(new ObligationCheckpoint(2,10,restored.snapshot(),restored.coverage()))
            &&repeated.snapshot().get(0).samples().stream().noneMatch(CandidateEvidence::observerVerified)&&repeated.pending(key.targetEpoch()),
            "revoked restored attestations violated the checkpoint contract or lost Candidate history");
        restored.observe(new CandidateEvidence(key,1,1,1,"fresh","fresh-basis",CandidateEvidence.Family.STALE_QUALIFICATION,11,211,true,false));
        restored.observe(historical);
        require(restored.snapshot().get(0).samples().stream().filter(CandidateEvidence::observerVerified).count()==1
            &&restored.snapshot().get(0).samples().stream().filter(CandidateEvidence::observerVerified).allMatch(sample->sample.eventRef().equals("fresh")),
            "old replay reentered live proof samples");
        restored.invalidate(key);
        long invalidated=restored.version();
        restored.observe(new CandidateEvidence(key,1,1,1,"same-tick-replay","fresh-basis",CandidateEvidence.Family.STALE_QUALIFICATION,11,211,true,false));
        require(restored.snapshot().get(0).state()==ObligationRegistry.State.NEEDS_REVALIDATION
            &&restored.snapshot().get(0).samples().stream().noneMatch(CandidateEvidence::observerVerified),"explicit invalidation reused an old attestation");
        restored.observe(new CandidateEvidence(key,1,1,1,"reobserved","fresh-basis",CandidateEvidence.Family.STALE_QUALIFICATION,12,212,true,false));
        require(restored.snapshot().get(0).state()==ObligationRegistry.State.UNRESOLVED&&restored.version()>invalidated,
            "new observer evidence could not revalidate");
        restored.observe(new CandidateEvidence(key,2,1,1,"new-policy","weak-basis",CandidateEvidence.Family.STATE_RELATION,13,213,true,false));
        require(restored.pending(key.targetEpoch()),"policy change silently removed unresolved coverage");
    }
    private static void sourceFairness() {
        var registry=new ObligationRegistry(64);var subject=key(31).targetEpoch().subjectKey();
        for(int i=1;i<=30;i++) {
            var flooded=new ObligationKey(new TargetEpoch(subject,i),ObligationKey.Source.ADMISSION_GATE,
                ObligationKey.Edge.ADMISSION,ObligationKey.Result.CURRENT_QUALIFICATION);
            registry.observe(e(flooded,1,"flood-"+i,"binding",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        }
        var source=new ObligationKey(new TargetEpoch(subject,31),ObligationKey.Source.SAVED_RECORD,
            ObligationKey.Edge.RECONSTRUCTION,ObligationKey.Result.RECOVERY_SOURCE_DISPOSAL);
        registry.observe(e(source,1,"source","copy",CandidateEvidence.Family.DIRECT_RECONSTRUCTION,true,false));
        require(registry.nextReview(1).key().source()==ObligationKey.Source.ADMISSION_GATE
            &&registry.nextReview(1).key().equals(source),"source flood delayed another source until all flood detail was reviewed");
        Set<ObligationKey> seen=new HashSet<>();
        for(int i=0;i<60;i++)seen.add(registry.nextReview(1).key());
        require(seen.size()==31,"source rotation starved individual detail");
    }
    private static void durableContextChangesAreCheckpointed() {
        var registry=new ObligationRegistry();var key=key(32);
        registry.observe(e(key,1,"effect","root",CandidateEvidence.Family.STATE_RELATION,true,false),"effect:fixed");
        long before=registry.version();
        registry.observe(e(key,2,"effect","root",CandidateEvidence.Family.STATE_RELATION,true,false),"effect:fixed");
        require(registry.version()>before&&registry.snapshot().get(0).policyRevision()==2
            &&registry.snapshot().get(0).count()==1&&registry.snapshot().get(0).samples().isEmpty(),
            "durable dedup hid a context mutation or fabricated a second effect");
        before=registry.version();registry.clear();
        require(registry.version()>before&&registry.size()==0,"cleared checkpoint content kept the old version");
    }
    private static void boundedReclamation() {
        var registry=new ObligationRegistry(2);
        var strong=key(40);var weak=key(41);var retry=key(42);
        registry.observe(e(strong,1,"strong","root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        registry.observe(e(weak,1,"weak","timing",CandidateEvidence.Family.TIME_RELATION,true,false));
        registry.observe(e(retry,1,"first-attempt","source",CandidateEvidence.Family.DIRECT_RECONSTRUCTION,true,false));
        require(registry.coverage().overflowCount()==1,"initial capacity gap was not recorded");
        registry.observe(new CandidateEvidence(retry,1,1,1,"retry","source",CandidateEvidence.Family.DIRECT_RECONSTRUCTION,201,401,true,false));
        require(registry.size()==2&&registry.snapshot().stream().anyMatch(view->view.key().equals(retry))
            &&registry.snapshot().stream().anyMatch(view->view.key().equals(strong)),"retry did not reclaim only expired diagnostic detail");
        require(registry.coverage().overflowCount()==1&&registry.coverage().needsRevalidation(),
            "successful retry erased an unproven historical coverage gap");
        var restored=new ObligationRegistry(2);
        require(restored.restore(new ObligationCheckpoint(1,201,registry.snapshot(),registry.coverage())),"reclaim fixture restore failed");
        restored.observe(new CandidateEvidence(key(43),1,1,1,"third","source",CandidateEvidence.Family.DIRECT_RECONSTRUCTION,500,700,true,false));
        require(restored.snapshot().stream().noneMatch(view->view.key().equals(key(43)))&&restored.pending(strong.targetEpoch()),
            "restart erased historical Candidate protection against eviction");
        var durable=new ObligationRegistry(1);
        durable.observe(e(weak,1,"effect","journal",CandidateEvidence.Family.STATE_RELATION,true,false),"durable:unknown");
        durable.observe(new CandidateEvidence(retry,1,1,1,"later","source",CandidateEvidence.Family.DIRECT_RECONSTRUCTION,500,700,true,false));
        require(durable.snapshot().get(0).key().equals(weak)&&durable.pending(weak.targetEpoch()),
            "weak sample expiry evicted or ignored an unresolved durable effect");
        var observed=new ObligationRegistry(1);
        observed.observe(e(weak,1,"unverified","timing",CandidateEvidence.Family.TIME_RELATION,false,false));
        observed.nextReview(201);
        require(!observed.pending(weak.targetEpoch()),"unverified expired diagnostic became a permanent pending obligation");
    }
    private static void contextRoundTripRejectsOldProof() {
        var registry=new ObligationRegistry();var key=key(50);
        var original=new CandidateEvidence(key,1,1,1,"policy-a","root-a",CandidateEvidence.Family.STALE_QUALIFICATION,1,201,true,false);
        registry.observe(original);
        registry.observe(new CandidateEvidence(key,2,1,1,"policy-b","root-b",CandidateEvidence.Family.STALE_QUALIFICATION,2,202,true,false));
        require(registry.snapshot().get(0).state()==ObligationRegistry.State.UNRESOLVED,"fresh B context did not establish live proof");
        registry.observe(original);
        require(registry.snapshot().get(0).state()==ObligationRegistry.State.NEEDS_REVALIDATION
            &&registry.snapshot().get(0).samples().isEmpty()&&registry.pending(key.targetEpoch()),
            "A -> B -> A reused delayed historical proof or dropped the old obligation");
        registry.observe(new CandidateEvidence(key,1,1,1,"fresh-a","root-a",CandidateEvidence.Family.STALE_QUALIFICATION,3,203,true,false));
        require(registry.snapshot().get(0).state()==ObligationRegistry.State.UNRESOLVED
            &&registry.snapshot().get(0).samples().size()==1,"fresh post-barrier observation could not revalidate");
    }
    private static void overflowTiersRetainAndReviewCandidates() {
        var registry=new ObligationRegistry(1);
        registry.observe(e(key(1000),1,"primary","root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        for(int i=1;i<=OverflowCheckpoint.MAX_RETRY_ENTRIES;i++)
            registry.observe(e(key(1000+i),1,"retry-"+i,"root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        for(int i=0;i<OverflowCheckpoint.MAX_SCOPES;i++)
            registry.observe(e(key(1129+i),1,"scope-"+i,"root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        var sameTarget=key(1129).targetEpoch();
        var secondSource=new ObligationKey(sameTarget,ObligationKey.Source.SAVED_RECORD,ObligationKey.Edge.RECONSTRUCTION,
            ObligationKey.Result.RECOVERY_SOURCE_DISPOSAL);
        registry.observe(e(secondSource,1,"same-scope-source","copy",CandidateEvidence.Family.DIRECT_RECONSTRUCTION,true,false));
        registry.observe(e(key(2000),1,"unscoped","root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        var overflow=registry.overflowCheckpoint();
        require(registry.size()==1&&registry.retrySize()==128&&overflow.scopes().size()==128&&overflow.unscopedCount()==1,
            "overflow tiers exceeded their bounds or forgot fully unscoped loss");
        require(overflow.scopes().stream().anyMatch(scope->scope.target().equals(sameTarget)&&scope.count()==2&&scope.sources().size()==2),
            "same target did not retain the independent source families to recheck");
        require(registry.pending(key(1001).targetEpoch())&&registry.pending(sameTarget),
            "retained overflow Candidate or known source recheck scope did not block completion");
        Set<ObligationKey> reviewed=new HashSet<>();
        for(int i=0;i<129;i++)reviewed.add(registry.nextReview(1).key());
        require(reviewed.size()==129,"overflow Candidates did not enter the actual fair review rotation");
        var checkpoint=new ObligationCheckpoint(1,1,registry.snapshot(),registry.coverage(),CandidateRestrictions.EMPTY,registry.overflowCheckpoint());
        require(checkpoint.allEntries().size()==129,"checkpoint merged only primary detail");
        var restored=new ObligationRegistry(1);require(restored.restore(checkpoint),"overflow restore failed");
        require(restored.retrySize()==128&&restored.pending(key(1001).targetEpoch())
            &&restored.overflowCheckpoint().retryEntries().stream().allMatch(view->view.samples().stream().noneMatch(CandidateEvidence::observerVerified)
                &&view.state()==ObligationRegistry.State.NEEDS_REVALIDATION),"restart replayed overflow proofs or lost Candidate history");
        require(restored.overflowCheckpoint().scopes().equals(checkpoint.overflow().scopes())
            &&restored.overflowCheckpoint().unscopedCount()==1,"restart lost source scopes or unknown coverage");
        var legacy=new ObligationCheckpoint(1,1,List.of(),new ObligationRegistry.Coverage(7,true,"legacy gap"));
        require(legacy.overflow().unscopedCount()==7,"legacy gap fabricated source coverage");
        try { new ObligationCheckpoint(1,1,registry.snapshot(),registry.coverage(),CandidateRestrictions.EMPTY,
            new OverflowCheckpoint(registry.snapshot(),List.of(),0));
            throw new AssertionError("same key was retained in both tiers");
        } catch(IllegalArgumentException expected) {}
    }
    private static void restoreRetirementAndBudgetRecovery() {
        var weak=key(3000);var replacement=key(3001);
        var original=new ObligationRegistry(1);
        original.observe(e(weak,1,"weak-id","clock",CandidateEvidence.Family.TIME_RELATION,true,false));
        var restored=new ObligationRegistry(1);
        require(restored.restore(new ObligationCheckpoint(1,0,original.snapshot(),original.coverage())),"diagnostic restore failed");
        restored.observe(new CandidateEvidence(replacement,1,1,1,"new-primary","root",CandidateEvidence.Family.STALE_QUALIFICATION,201,401,true,false));
        require(restored.snapshot().get(0).key().equals(replacement)&&restored.retrySize()==0,
            "restart discarded expiry evidence and permanently occupied a diagnostic slot");
        restored.observe(new CandidateEvidence(weak,1,1,1,"weak-id","clock",CandidateEvidence.Family.TIME_RELATION,202,402,true,false));
        var renewed=restored.overflowCheckpoint().retryEntries().get(0);
        require(renewed.key().equals(weak)&&renewed.firstSeen()==202&&renewed.count()==1,
            "retired key could not enter a new bounded observation lifetime");
        var freshId=new ObligationRegistry();freshId.restore(new ObligationCheckpoint(1,0,original.snapshot(),original.coverage()));
        freshId.observe(new CandidateEvidence(weak,1,1,1,"weak-id","clock",CandidateEvidence.Family.STALE_QUALIFICATION,1,201,true,false));
        require(freshId.snapshot().get(0).samples().size()==1&&freshId.snapshot().get(0).samples().get(0).observerVerified(),
            "post-restart observer event identifier could not replace its revoked history");

        var source=new ObligationRegistry();
        source.observe(e(key(3010),1,"one","root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        source.observe(e(key(3011),1,"two","root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        var checkpoint=new ObligationCheckpoint(1,0,source.snapshot(),source.coverage());
        var budgets=new EnumMap<dev.ronova.coremod.budget.HostResourceDomain,dev.ronova.coremod.budget.HostResourceGovernor.Budget>(
            dev.ronova.coremod.budget.HostResourceDomain.class);
        budgets.putAll(dev.ronova.coremod.budget.HostResourceGovernor.defaultBudgets());
        budgets.put(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP,new dev.ronova.coremod.budget.HostResourceGovernor.Budget(8192,8192,0));
        var resources=new dev.ronova.coremod.budget.HostResourceGovernor(budgets);
        var bounded=new ObligationRegistry(resources);
        var outside=resources.reserve(Map.of(dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP,4096L),
            dev.ronova.coremod.budget.HostResourceGovernor.Priority.NORMAL);
        require(outside.granted(),"restore budget fixture unavailable");
        require(!bounded.restore(checkpoint)&&bounded.restorePending()&&bounded.pending(key(3999).targetEpoch())&&heapUsage(resources)==4096,
            "partial restore leaked staged charges or falsely exposed no pending work");
        outside.reservation().close();
        require(bounded.restore(checkpoint)&&!bounded.restorePending()&&heapUsage(resources)==8192,"restore did not recover when budget returned");
        require(bounded.restore(checkpoint)&&heapUsage(resources)==8192,"same-key restore required a second full allocation");
        source.observe(e(key(3012),1,"three","root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        require(!bounded.restore(new ObligationCheckpoint(2,0,source.snapshot(),source.coverage()))
            &&bounded.size()==2&&heapUsage(resources)==8192&&bounded.pending(key(3999).targetEpoch()),
            "failed replacement restore damaged old obligations or reused reservations");
        require(bounded.restore(checkpoint,7)&&bounded.coverage().overflowCount()==7
            &&bounded.overflowCheckpoint().unscopedCount()==7,"failed replacement retry erased observations missed during restoration");
        bounded.clear();bounded.clear();require(heapUsage(resources)==0,"restore reservation transfer leaked or double released");
    }
    private static long heapUsage(dev.ronova.coremod.budget.HostResourceGovernor resources) {
        return resources.snapshot().stream().filter(row->row.domain()==dev.ronova.coremod.budget.HostResourceDomain.JAVA_HEAP)
            .findFirst().orElseThrow().ownedUsage();
    }
    private static ObligationKey key(long id) {return new ObligationKey(new TargetEpoch(new SubjectKey(new RealmId(new UUID(64,1)),new UUID(64,id)),1),ObligationKey.Source.ADMISSION_GATE,ObligationKey.Edge.ADMISSION,ObligationKey.Result.CURRENT_QUALIFICATION);}
    private static CandidateEvidence e(ObligationKey key,long policy,String event,String basis,CandidateEvidence.Family family,boolean verified,boolean authorized) {
        return new CandidateEvidence(key,policy,1,1,event,basis,family,0,200,verified,authorized);
    }
    private static void retirementFollowsQualification() {
        var oldKey=key(4001);var peer=key(4002);var registry=new ObligationRegistry(1);
        registry.observe(e(oldKey,1,"old","root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false),"unresolved-effect");
        registry.observe(e(peer,1,"peer","root",CandidateEvidence.Family.STALE_QUALIFICATION,true,false));
        var current=new dev.ronova.coremod.root.api.SubjectQualification(oldKey.targetEpoch(),true);
        var foreign=new dev.ronova.coremod.root.api.SubjectQualification(new TargetEpoch(
            new SubjectKey(new RealmId(new UUID(0,9000)),oldKey.targetEpoch().subjectKey().subjectId()),oldKey.targetEpoch().generation()+1),false);
        require(!registry.retireSuperseded(oldKey,current)&&!registry.retireSuperseded(oldKey,foreign),"current/foreign qualification retired an obligation");
        require(registry.retireSuperseded(oldKey,current.transition(false))&&registry.pending(peer.targetEpoch()),
            "retirement failed or removed another subject's pending work");
        registry.nextReview(10);require(registry.size()==1&&registry.retrySize()==0,"retirement did not return a slot to the retry queue");
        var scope=new OverflowCheckpoint.Scope(oldKey.targetEpoch(),Set.of(ObligationKey.Source.SAVED_RECORD),1,1,1);
        var checkpoint=new ObligationCheckpoint(1,1,List.of(),new ObligationRegistry.Coverage(1,true,"Root retained"),
            CandidateRestrictions.EMPTY,new OverflowCheckpoint(List.of(),List.of(scope),0));
        var scopes=new ObligationRegistry();require(scopes.restore(checkpoint),"scope fixture failed");
        require(!scopes.retireSupersededScope(scope.target(),current)&&scopes.nextOverflowScope().equals(scope),"active scope disappeared");
        require(scopes.retireSupersededScope(scope.target(),current.transition(false))&&scopes.nextOverflowScope()==null
            &&scopes.coverage().overflowCount()==1,"retirement erased historical overflow accounting");
    }
    private static void coverageRemainsScoped() {
        var a=key(5001).targetEpoch();var b=key(5002).targetEpoch();var registry=new ObligationRegistry();
        registry.recordCoverageGap(a,ObligationKey.Source.SAVED_RECORD,1);
        var checkpoint=new ObligationCheckpoint(1,1,registry.snapshot(),registry.coverage(),CandidateRestrictions.EMPTY,registry.overflowCheckpoint());
        require(registry.pending(a)&&checkpoint.pending(a)&&!registry.pending(b)&&!checkpoint.pending(b),
            "a known source gap became either invisible or a global live obligation");
        var emptyRuntime=new ObligationRegistry();
        require(!emptyRuntime.pending(a)&&checkpoint.pending(a),"durable pending state depended on live allocation");
        var legacy=new ObligationCheckpoint(1,1,List.of(),registry.coverage());
        require(legacy.pending(b),"legacy unscoped loss was silently assigned to one subject");
        var nonce=new UUID(717,5500);
        var reason=new CandidateRestrictions.Reason(CandidateRestrictions.reasonId(a,nonce),a,nonce,1,1,
            CandidateRestrictions.Decision.UNRESOLVED,CandidateRestrictions.Release.HELD,1,0,0,0,
            dev.ronova.coremod.root.api.EffectRecord.hash("retained-root-reason"));
        var reasonOnly=new ObligationCheckpoint(1,1,List.of(),ObligationCheckpoint.EMPTY.coverage(),
            new CandidateRestrictions(1,0,CandidateRestrictions.EMPTY.retiredDigest(),List.of(reason)));
        require(reasonOnly.pending(a)&&!reasonOnly.pending(b),"retained Root reason disappeared when runtime detail was absent");
        require(registry.retireSupersededScope(a,new dev.ronova.coremod.root.api.SubjectQualification(a.next(),false)),"scope retirement fixture failed");
        var retired=new ObligationCheckpoint(2,2,registry.snapshot(),registry.coverage(),CandidateRestrictions.EMPTY,registry.overflowCheckpoint());
        require(retired.coverage().overflowCount()==1&&!retired.pending(a)&&!retired.pending(b),"historical count prevented scoped retirement");
        var overflow=new ObligationRegistry();
        for(int i=0;i<=OverflowCheckpoint.MAX_SCOPES;i++)overflow.recordCoverageGap(key(5100+i).targetEpoch(),ObligationKey.Source.SAVED_RECORD,1);
        var lost=new ObligationCheckpoint(1,1,overflow.snapshot(),overflow.coverage(),CandidateRestrictions.EMPTY,overflow.overflowCheckpoint());
        require(overflow.pending(b)&&lost.pending(b)&&lost.overflow().unscopedCount()==1,
            "scope-capacity loss incorrectly permitted an unrelated scope to claim no pending work");
    }
    private static void require(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
