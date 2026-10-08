package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import dev.ronova.coremod.obligation.*;
import java.nio.file.*;
import java.util.*;

final class TerminalPlanScenarios {
    static void verify() throws Exception {
        Path world = Files.createTempDirectory("ronova-terminal-plan-").toAbsolutePath().normalize();
        try {
            var store = LocalDominionStore.openWorld(world);
            UUID actor = new UUID(63, 10), a = new UUID(63, 11), b = new UUID(63, 12), nonce = new UUID(63, 13);
            store.commit(new DominionCommand.BindOwner(actor));
            var batch = store.qualify(new JudgmentBatch(UUID.randomUUID(), List.of(judgment(a, store.nextDecisionEpoch()), judgment(b, store.nextDecisionEpoch()))))
                .withIncarnations(Map.of(a, nonce, b, nonce));
            var receipt = store.commit(batch); long index = store.commitIndex();
            Path root = world.resolve("ronova/durable-root-v1.bin"); byte[] initial = Files.readAllBytes(root);
            var plan = store.approveTerminalPlan(a, nonce);
            var ownerStamp=store.authorityStamp(actor);
            try(var ownerLease=store.acquireOwnerWrite(ownerStamp)) {
                require(ownerLease!=null&&ownerLease.valid(ownerStamp)&&store.acquireTerminalWrite(plan)==null,
                    "current Owner did not acquire an exclusive Root write scope");
                require(store.commit(new DominionCommand.ClearTombstone(a)).rejected()&&store.validateTerminalPlan(plan),
                    "new canonical command crossed an Owner repair scope");
            }
            require(store.acquireOwnerWrite(store.authorityStamp(a))==null,"terminal target acquired Owner write scope");
            var participant=store.approveClientParticipation(plan,List.of(new ClientParticipationContract.Endpoint(actor,new UUID(63,900),1,1),
                new ClientParticipationContract.Endpoint(new UUID(63,901),new UUID(63,902),1,1)));
            require(participant!=null&&participant.members().stream().filter(ClientParticipationContract.Member::required).count()==1
                &&participant.members().stream().filter(ClientParticipationContract.Member::required).findFirst().orElseThrow().endpoint().client().equals(actor),
                "Root did not distinguish required protected owner from optional client");
            require(plan != null && plan.commandId().equals(receipt.commandId()) && plan.authorityCommitIndex() == receipt.commitIndex()
                && store.approveTerminalPlan(a, UUID.randomUUID()) == null && store.approveTerminalPlan(actor, nonce) == null
                && Arrays.equals(initial, Files.readAllBytes(root)), "plan approval changed Truth or accepted an unbound subject");
            var reopened = LocalDominionStore.openWorld(world);
            require(plan.equals(reopened.approveTerminalPlan(a, nonce)), "plan reconstruction changed revision/identity");
            var bundle = store.prepareTerminalEffect(plan, "fixture-physical-a");
            require(bundle.granted() && store.enterEffect(bundle) && !store.enterEffect(bundle), "bundle entry not exactly one attempt");
            store.finishEffectUnknown(bundle);
            require(!store.prepareLifecycleEffect(a, "legacy-name").granted() && store.effectRecordCount() == 1,
                "renaming bundle to legacy public-death created a second intent");
            var legacy = store.prepareLifecycleEffect(b, "fixture-physical-b");
            require(legacy.granted(), "legacy effect fixture failed"); store.finishEffectUnknown(legacy);
            var legacyPlan = store.approveTerminalPlan(b, nonce);
            require(!store.prepareTerminalEffect(legacyPlan, "new-name").granted() && store.effectRecordCount() == 2,
                "new bundle bypassed a retained legacy unknown effect");
            store.commit(new DominionCommand.BindOwner(new UUID(63, 99)));
            var next = store.approveTerminalPlan(a, nonce);
            require(!store.validateTerminalPlan(plan) && next.planRevision() > plan.planRevision()
                && next.effectKey().equals(plan.effectKey()) && !store.prepareTerminalEffect(next, "replanned").granted(),
                "replan renewed an opaque effect identity");
            var after = LocalDominionStore.openWorld(world);
            require(!after.readOnly() && after.effectUnresolvedCount() == 2 && after.commitIndex() == index + 1
                && !after.prepareTerminalEffect(after.approveTerminalPlan(a, nonce), "restarted").granted(),
                "restart replayed or rewrote bundle authority");
            var detail=new ObligationRegistry();
            var detailKey=new ObligationKey(next.qualification().targetEpoch(),ObligationKey.Source.ROOT_EFFECT_JOURNAL,
                ObligationKey.Edge.OPAQUE_EFFECT,ObligationKey.Result.EFFECT_RESOLUTION);
            String effectRef=EffectRecord.hash(bundle.record().key().canonical());
            detail.observe(new CandidateEvidence(detailKey,1,0,1,effectRef,"root-effect-journal",CandidateEvidence.Family.STATE_RELATION,
                10,210,true,false),effectRef);
            var recorded=new ObligationCheckpoint(1,10,detail.snapshot(),detail.coverage());
            require(store.checkpointObligations(0,recorded).acknowledgedWrite(),"effect detail fixture not persisted");
            require(store.checkpointObligations(1,new ObligationCheckpoint(2,11,List.of(),detail.coverage())).status()==CheckpointReceipt.Status.REJECTED,
                "current qualification lost unresolved detail without retirement proof");
            var unknownEffects=store.shadow().effectRecords();
            var clear=new DominionCommand.ClearTombstone(a);
            try(var lease=store.acquireTerminalWrite(next)) {
                require(lease!=null&&lease.valid(next)&&store.acquireTerminalWrite(plan)==null,"stale plan acquired a write fence");
                var result=new java.util.concurrent.atomic.AtomicReference<CommitReceipt>();
                var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();
                Thread concurrent=new Thread(()->{
                    try{result.set(store.commit(clear));}catch(Throwable failure){error.set(failure);}
                },"ronova-root-fence-commit");
                concurrent.setDaemon(true);concurrent.start();concurrent.join(2000);
                require(!concurrent.isAlive()&&error.get()==null&&result.get()!=null&&result.get().rejected()
                    &&store.validateTerminalPlan(next)&&store.commitIndex()==index+1,
                    "canonical qualification changed during a registered write step");
                require(store.commit(batch).committed(),"write fence broke recovery of an existing canonical receipt");
            }
            require(store.commit(clear).committed(),"clear did not become available after the write step left");
            require(store.approveTerminalPlan(a, nonce) == null && !store.validateTerminalPlan(next), "cleared subject kept a terminal plan");
            require(store.acquireTerminalWrite(next)==null,"clear left a reusable write fence");
            require(detail.retireSuperseded(detailKey,store.subjectQualification(next.subjectKey()))&&detail.size()==0,
                "superseded execution detail retained its live slot");
            var retired=new ObligationCheckpoint(2,11,detail.snapshot(),detail.coverage());
            require(store.checkpointObligations(1,retired).acknowledgedWrite()&&store.shadow().effectRecords().equals(unknownEffects)
                &&store.effectUnresolvedCount()==2,"retirement changed or resolved an opaque effect");
            require(store.checkpointObligations(2,new ObligationCheckpoint(3,12,recorded.entries(),recorded.coverage())).status()==CheckpointReceipt.Status.REJECTED,
                "old observation recreated retired execution detail");
            var retiredReload=LocalDominionStore.openWorld(world);
            require(retiredReload.obligationCheckpoint().entries().isEmpty()&&retiredReload.shadow().effectRecords().equals(unknownEffects),
                "restart lost retirement or the historical effect journal");
            UUID independentSubject=new UUID(63,140);
            var independentBatch=retiredReload.qualify(new JudgmentBatch(UUID.randomUUID(),
                List.of(judgment(independentSubject,retiredReload.nextDecisionEpoch()))))
                .withIncarnations(Map.of(independentSubject,nonce));
            require(retiredReload.commit(independentBatch).committed(),"independent action fixture did not commit");
            var oldIndependentPlan=retiredReload.approveTerminalPlan(independentSubject,nonce);
            var policy=retiredReload.strengthPolicyConfiguration().next(dev.ronova.coremod.policy.StrengthProfile.EXTREME,Map.of(),
                Map.of(dev.ronova.coremod.policy.AttackAction.TERMINALIZE,dev.ronova.coremod.policy.AttackActionLevel.DISABLED));
            require(retiredReload.configureStrengthPolicy(policy).committed(),"ordinary action switch did not persist");
            var independentReload=LocalDominionStore.openWorld(world);
            var independentPlan=independentReload.approveTerminalPlan(independentSubject,nonce);
            require(independentPlan!=null&&!independentReload.validateTerminalPlan(oldIndependentPlan)
                &&independentPlan.effectKey().equals(oldIndependentPlan.effectKey()),"disabled lifecycle invalidated unrelated retained authority");
            require(!independentReload.prepareTerminalEffect(independentPlan,"disabled-lifecycle").granted()
                &&independentReload.effectRecordCount()==2,"disabled lifecycle created a new opaque intent");
            var missingWriteSetSteps=independentPlan.orderedSteps().stream().map(step->new OperationReport.StepReceipt(step,
                step==TerminalOperationPlan.Step.TERMINAL_LIFECYCLE?OperationReport.Status.UNAVAILABLE:OperationReport.Status.DISPATCHED,
                "fixture-independent-action")).toList();
            require(!independentReload.acceptOperationReport(new OperationReport(independentPlan,missingWriteSetSteps,false,false,"FENCED")),
                "Root accepted a successful terminal-state report without typed write-set evidence");
            var fixtureSink=new WriteSinkId("ronova:terminal-state/life");
            WriteSetReceipt forgedReceipt;
            try(var forgedLease=WriteSetLease.forPlan(independentPlan,()->true,fixtureSink)) {
                require(forgedLease.consume(fixtureSink),"fixture forged write-set sink unavailable");
                forgedReceipt=forgedLease.receipt();
            }
            var forgedSteps=independentPlan.orderedSteps().stream().map(step->step==TerminalOperationPlan.Step.TERMINAL_STATE
                ?new OperationReport.StepReceipt(step,OperationReport.Status.DISPATCHED,"fixture-independent-action",forgedReceipt)
                :new OperationReport.StepReceipt(step,
                    step==TerminalOperationPlan.Step.TERMINAL_LIFECYCLE?OperationReport.Status.UNAVAILABLE:OperationReport.Status.DISPATCHED,
                    "fixture-independent-action")).toList();
            require(!independentReload.acceptOperationReport(new OperationReport(independentPlan,forgedSteps,false,false,"FENCED")),
                "Root trusted an unregistered caller-constructed write-set receipt");
            WriteSetReceipt fixtureReceipt;
            try(var rootLease=independentReload.acquireTerminalWrite(independentPlan)) {
                require(rootLease!=null,"fixture terminal Root lease unavailable");
                try(var fixtureLease=WriteSetLease.forPlan(independentPlan,()->rootLease.valid(independentPlan),fixtureSink)) {
                    require(fixtureLease.consume(fixtureSink),"fixture write-set sink unavailable");
                    fixtureReceipt=fixtureLease.receipt();
                    require(independentReload.registerWriteSetReceipt(independentPlan,rootLease,fixtureReceipt),
                        "Root did not register a receipt minted under its active write fence");
                }
            }
            var worldSink=new WriteSinkId("ronova:world-records/entity-lookup-id");
            WriteSetReceipt forgedWorldReceipt;
            try(var forgedWorldLease=WriteSetLease.forPlan(independentPlan,()->true,worldSink)) {
                require(forgedWorldLease.consume(worldSink),"fixture forged world-record sink unavailable");
                forgedWorldReceipt=forgedWorldLease.receipt();
            }
            var forgedWorldSteps=independentPlan.orderedSteps().stream().map(step->
                step==TerminalOperationPlan.Step.TERMINAL_STATE
                    ?new OperationReport.StepReceipt(step,OperationReport.Status.DISPATCHED,"fixture-independent-action",fixtureReceipt)
                :step==TerminalOperationPlan.Step.WORLD_RECORDS
                    ?new OperationReport.StepReceipt(step,OperationReport.Status.DISPATCHED,"fixture-world-record-action",forgedWorldReceipt)
                :new OperationReport.StepReceipt(step,
                    step==TerminalOperationPlan.Step.TERMINAL_LIFECYCLE?OperationReport.Status.UNAVAILABLE:OperationReport.Status.DISPATCHED,
                    "fixture-independent-action")).toList();
            require(!independentReload.acceptOperationReport(new OperationReport(independentPlan,forgedWorldSteps,false,false,"FENCED")),
                "Root trusted an unregistered caller-constructed WORLD_RECORDS write-set receipt");
            WriteSetReceipt worldReceipt;
            try(var rootLease=independentReload.acquireTerminalWrite(independentPlan)) {
                require(rootLease!=null,"fixture terminal Root lease unavailable after rejected report");
                require(independentReload.registerWriteSetReceipt(independentPlan,rootLease,fixtureReceipt),
                    "Root did not re-register terminal-state provenance after rejected report consumed it");
                try(var worldLease=WriteSetLease.forPlan(independentPlan,()->rootLease.valid(independentPlan),worldSink)) {
                    require(worldLease.consume(worldSink),"fixture world-record sink unavailable");
                    worldReceipt=worldLease.receipt();
                    require(independentReload.registerWriteSetReceipt(independentPlan,rootLease,worldReceipt),
                        "Root did not register WORLD_RECORDS receipt under active write fence");
                }
            }
            var independentSteps=independentPlan.orderedSteps().stream().map(step->
                step==TerminalOperationPlan.Step.TERMINAL_STATE
                    ?new OperationReport.StepReceipt(step,OperationReport.Status.DISPATCHED,"fixture-independent-action",fixtureReceipt)
                :step==TerminalOperationPlan.Step.WORLD_RECORDS
                    ?new OperationReport.StepReceipt(step,OperationReport.Status.DISPATCHED,"fixture-world-record-action",worldReceipt)
                :new OperationReport.StepReceipt(step,
                    step==TerminalOperationPlan.Step.TERMINAL_LIFECYCLE?OperationReport.Status.UNAVAILABLE:OperationReport.Status.DISPATCHED,
                    "fixture-independent-action")).toList();
            require(independentReload.acceptOperationReport(new OperationReport(independentPlan,independentSteps,false,false,"FENCED")),
                "Root rejected registered typed write-set evidence across terminal-state and WORLD_RECORDS steps");
            require(!independentReload.acceptOperationReport(new OperationReport(independentPlan,independentSteps,false,false,"FENCED")),
                "Root accepted replay of one-use cross-step write-set receipt provenance");
            require(independentReload.shadow().effectRecords().equals(unknownEffects),"ordinary action switch changed historical unknown effects");
            System.out.println("TerminalPlanScenarios: PASS (Root approval/reconstruction, one-use cross-step write-set provenance, exact attempt, bidirectional legacy alias fence, stable effect across replan/restart, revocation)");
        } finally { try (var paths = Files.walk(world)) { for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
            if (!p.toAbsolutePath().normalize().startsWith(world)) throw new IllegalStateException("unsafe test cleanup"); Files.deleteIfExists(p);
        } } }
    }
    private static DominionDecision.Judgment judgment(UUID id, DominionEpoch epoch) {
        return new DominionDecision.Judgment(new SubjectIdentity(id, "test:entity", "test.Entity", "minecraft:overworld", 0, 0, 0, false),
            epoch, true, DominionDecision.LineageScope.IDENTITY, 0, "plan-fixture");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
