package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.obligation.*;
import dev.ronova.coremod.root.api.*;
import java.nio.file.*;
import java.util.*;

final class ObligationCheckpointScenarios {
    static void verify() throws Exception {
        Path world=Files.createTempDirectory("ronova-obligation-checkpoint-").toAbsolutePath().normalize();
        try {
            var store=LocalDominionStore.openWorld(world);UUID actor=new UUID(65,1),subject=new UUID(65,2),nonce=new UUID(65,3);
            var resources=new dev.ronova.coremod.budget.HostResourceGovernor();store.shadow().attachRuntimeResources(resources);
            store.commit(new DominionCommand.BindOwner(actor));
            var key=new SubjectKey(store.realmId(),subject);
            store.commit(new DominionCommand.TransitionSubjects(store.prepareTransitions(UUID.randomUUID(),actor,List.of(
                new SubjectTransitionBatch.Transition(key,SubjectTransitionBatch.Kind.REBIND,nonce,null,null)))));
            var registry=new ObligationRegistry(1);var q=store.subjectQualification(key);
            var alias=new SubjectKey(store.realmId(),new UUID(65,99));
            require(store.conflictingPhysicalBinding(alias.subjectId(),nonce),"unbound changed UUID inherited another subject's binding");
            var explicitAlias=store.commit(new DominionCommand.TransitionSubjects(store.prepareTransitions(UUID.randomUUID(),actor,List.of(
                new SubjectTransitionBatch.Transition(alias,SubjectTransitionBatch.Kind.REBIND,nonce,null,null)))));
            require(explicitAlias.committed()&&!store.conflictingPhysicalBinding(alias.subjectId(),nonce)
                &&store.subjectQualification(key).equals(q),"explicit subject-qualified binding changed another subject");
            var evidence=new CandidateEvidence(new ObligationKey(q.targetEpoch(),ObligationKey.Source.ADMISSION_GATE,
                ObligationKey.Edge.ADMISSION,ObligationKey.Result.CURRENT_QUALIFICATION),1,0,1,"event-a","basis-a",
                CandidateEvidence.Family.STALE_QUALIFICATION,5,205,true,false);
            registry.observe(evidence);
            var overflow=new CandidateEvidence(new ObligationKey(q.targetEpoch(),ObligationKey.Source.TERMINAL_PLAN,
                ObligationKey.Edge.EXECUTION,ObligationKey.Result.TERMINAL_SERVER_RESULT),1,0,1,"event-b","basis-b",
                CandidateEvidence.Family.STATE_RELATION,5,205,false,false);
            registry.observe(overflow);
            long index=store.commitIndex();var epoch=store.epoch();var request=new ObligationCheckpoint(1,10,registry.snapshot(),registry.coverage());
            var receipt=store.checkpointObligations(0,request);
            require(receipt.acknowledgedWrite()&&receipt.acknowledged().revision()==1&&store.commitIndex()==index&&store.epoch().equals(epoch),
                "checkpoint changed Authority transaction identity");
            Path file=world.resolve("ronova/durable-root-v1.bin");byte[] bytes=Files.readAllBytes(file);
            var reopened=LocalDominionStore.openWorld(world);
            require(reopened.obligationCheckpoint().equals(receipt.acknowledged())&&Arrays.equals(bytes,Files.readAllBytes(file)),
                "schema-6 checkpoint did not reopen without changing bytes");
            var restored=new ObligationRegistry(1);require(restored.restore(reopened.obligationCheckpoint()),"checkpoint restore failed");
            require(restored.coverage().overflowCount()==1&&restored.snapshot().get(0).state()==ObligationRegistry.State.NEEDS_REVALIDATION
                &&restored.snapshot().get(0).samples().stream().noneMatch(CandidateEvidence::observerVerified)
                &&restored.snapshot().get(0).candidate()==ObligationRegistry.Candidate.CANDIDATE,
                "restart lost historical obligation or treated restored observer proof as current");
            restored.observe(evidence);
            require(restored.snapshot().get(0).state()==ObligationRegistry.State.NEEDS_REVALIDATION,"old observer evidence reactivated after restart");
            var fresh=new CandidateEvidence(evidence.key(),1,0,1,"event-new","basis-new",evidence.family(),12,212,true,false);
            restored.observe(fresh);require(restored.snapshot().get(0).state()==ObligationRegistry.State.UNRESOLVED,"fresh evidence could not revalidate");
            var next=new ObligationCheckpoint(2,12,restored.snapshot(),restored.coverage());
            var backend=DurableRootFileBackend.at(file);store.shadow().attachDurableBackend(backend);
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(store.checkpointObligations(1,next).status()==CheckpointReceipt.Status.UNKNOWN&&store.obligationCheckpoint().revision()==1,
                "unacknowledged checkpoint became readable");
            var retry=store.checkpointObligations(1,next);
            require(retry.acknowledgedWrite()&&retry.acknowledged().revision()==2&&store.commitIndex()==index,"retry created a second checkpoint/Authority commit");
            var missing=new ObligationCheckpoint(3,13,List.of(),next.coverage());
            require(store.checkpointObligations(2,missing).status()==CheckpointReceipt.Status.REJECTED,"unresolved detail was forgotten without release");
            var conflict=new ObligationCheckpoint(3,13,next.entries(),next.coverage());
            require(store.checkpointObligations(0,conflict).status()==CheckpointReceipt.Status.CONFLICT,"stale checkpoint writer overwrote current revision");
            var image=backend.readIfPresent();require(image.getInt("schema")==6,"checkpoint did not activate schema 6");
            for(String mode:List.of("missing","schema","payload","downgrade")) {
                var bad=image.copy();
                switch(mode) {
                    case "missing" -> bad.remove("obligation_checkpoint");
                    case "schema" -> bad.getCompound("obligation_checkpoint").putInt("schema",9);
                    case "payload" -> bad.getCompound("obligation_checkpoint").putString("payload","{}");
                    case "downgrade" -> bad.putInt("schema",5);
                }
                require(ShadowDurableRootStore.load(bad,store.realmId()).readOnly(),"invalid checkpoint accepted: "+mode);
            }
            UUID staleNonce=new UUID(65,40);
            restored.observe(new CandidateEvidence(evidence.key(),1,q.revisions().judgmentRevision(),1,staleNonce.toString(),
                "root-binding-comparison/v1",CandidateEvidence.Family.STALE_QUALIFICATION,13,213,true,false));
            var restrictRequest=new ObligationCheckpoint(3,13,restored.snapshot(),restored.coverage());
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(store.checkpointObligations(2,restrictRequest).status()==CheckpointReceipt.Status.UNKNOWN
                &&!store.candidateAdmissionHeld(subject,staleNonce),"unacknowledged restriction became gate authority");
            require(store.checkpointObligations(2,restrictRequest).acknowledgedWrite()&&store.candidateAdmissionHeld(subject,staleNonce)
                &&!store.candidateAdmissionHeld(subject,nonce),"restriction ACK retry lost exact physical scope");
            var restrictedRoot=LocalDominionStore.openWorld(world);
            require(restrictedRoot.candidateAdmissionHeld(subject,staleNonce)&&restrictedRoot.obligationCheckpoint().restrictions().reasons().size()==1,
                "durable restriction failed to recover");
            var schema7=backend.readIfPresent();require(schema7.getInt("schema")==7,"restriction did not activate Root schema 7");
            var downgraded=schema7.copy();downgraded.putInt("schema",6);
            require(ShadowDurableRootStore.load(downgraded,store.realmId()).readOnly(),"restriction schema downgrade accepted");
            store.commit(new DominionCommand.TransitionSubjects(store.prepareTransitions(UUID.randomUUID(),actor,List.of(
                new SubjectTransitionBatch.Transition(key,SubjectTransitionBatch.Kind.REBIND,staleNonce,null,null)))));
            require(!store.candidateAdmissionHeld(subject,staleNonce),"legal rebind waited on candidate release debounce");
            require(store.checkpointObligations(3,new ObligationCheckpoint(4,14,restored.snapshot(),restored.coverage())).acknowledgedWrite()
                &&store.obligationCheckpoint().restrictions().reasons().stream().noneMatch(CandidateRestrictions.Reason::holds),
                "Root transition did not persist reason release");
            require(LocalDominionStore.openWorld(world).obligationCheckpoint().restrictions().equals(store.obligationCheckpoint().restrictions()),
                "release receipt did not survive restart");
            var descriptors=resources.snapshot().stream().filter(r->r.domain()==dev.ronova.coremod.budget.HostResourceDomain.FILE_DESCRIPTOR).findFirst().orElseThrow();
            require(descriptors.ownedUsage()==0&&descriptors.allocatedTotal()>0&&descriptors.allocatedTotal().equals(descriptors.releasedTotal()),
                "Root file handle permit leaked after successful/failed/ambiguous writes");
            verifyOverflow(world.resolve("overflow-fixture"));
            verifyCoverage(world.resolve("coverage-fixture"));
            System.out.println("ObligationCheckpointScenarios: PASS (same Root, schema6, no Authority advance, ACK ambiguity/retry, anti-forgetting, coverage, proof revalidation, corruption)");
        }finally{try(var paths=Files.walk(world)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList()){
            if(!p.toAbsolutePath().normalize().startsWith(world))throw new IllegalStateException("unsafe test cleanup");Files.deleteIfExists(p);
        }}}
    }
    private static void verifyOverflow(Path world) throws Exception {
        var store=LocalDominionStore.openWorld(world);UUID actor=new UUID(717,901),subject=new UUID(717,902),nonce=new UUID(717,903);
        store.commit(new DominionCommand.BindOwner(actor));var subjectKey=new SubjectKey(store.realmId(),subject);
        store.commit(new DominionCommand.TransitionSubjects(store.prepareTransitions(UUID.randomUUID(),actor,List.of(
            new SubjectTransitionBatch.Transition(subjectKey,SubjectTransitionBatch.Kind.REBIND,nonce,null,null)))));
        var q=store.subjectQualification(subjectKey);var registry=new ObligationRegistry(1);
        var primary=new ObligationKey(q.targetEpoch(),ObligationKey.Source.TERMINAL_PLAN,ObligationKey.Edge.EXECUTION,ObligationKey.Result.TERMINAL_SERVER_RESULT);
        registry.observe(new CandidateEvidence(primary,1,q.revisions().judgmentRevision(),1,"diagnostic","source",
            CandidateEvidence.Family.STATE_RELATION,1,2,false,false));
        UUID staleNonce=new UUID(717,904);
        var retryKey=new ObligationKey(q.targetEpoch(),ObligationKey.Source.ADMISSION_GATE,ObligationKey.Edge.ADMISSION,ObligationKey.Result.CURRENT_QUALIFICATION);
        registry.observe(new CandidateEvidence(retryKey,1,q.revisions().judgmentRevision(),1,staleNonce+"/1","root-binding-comparison/v1",
            CandidateEvidence.Family.STALE_QUALIFICATION,1,201,true,false));
        require(registry.retrySize()==1,"overflow fixture did not retain retry detail");
        var request=new ObligationCheckpoint(1,1,registry.snapshot(),registry.coverage(),CandidateRestrictions.EMPTY,registry.overflowCheckpoint());
        var accepted=store.checkpointObligations(0,request);
        require(accepted.acknowledgedWrite()&&store.candidateAdmissionHeld(subject,staleNonce),"retry candidate omitted from Root reason handoff");
        var backend=DurableRootFileBackend.at(world.resolve("ronova/durable-root-v1.bin"));
        var image=backend.readIfPresent();require(image.getInt("schema")==9&&image.getCompound("obligation_checkpoint").getInt("schema")==3,
            "structured overflow did not select the extended schemas");
        var reopened=LocalDominionStore.openWorld(world);
        require(reopened.obligationCheckpoint().equals(accepted.acknowledged())&&reopened.candidateAdmissionHeld(subject,staleNonce),
            "retry queue or restriction failed to reopen");
        var removed=new ObligationCheckpoint(2,2,request.entries(),request.coverage(),CandidateRestrictions.EMPTY,
            new OverflowCheckpoint(List.of(),request.overflow().scopes(),request.overflow().unscopedCount()));
        require(store.checkpointObligations(1,removed).status()==CheckpointReceipt.Status.REJECTED,"persisted retry Candidate was silently discarded");
        // Expired diagnostics may retire; an admitted retry record moves lanes without losing its identity.
        var promoted=new ObligationCheckpoint(2,3,request.overflow().retryEntries(),request.coverage(),CandidateRestrictions.EMPTY,
            new OverflowCheckpoint(List.of(),request.overflow().scopes(),request.overflow().unscopedCount()));
        require(store.checkpointObligations(1,promoted).acknowledgedWrite(),"Root rejected diagnostic retirement and retry promotion");
        for(int older:List.of(6,7,8)) {
            var bad=image.copy();bad.putInt("schema",older);
            require(ShadowDurableRootStore.load(bad,store.realmId()).readOnly(),"structured overflow accepted under older Root schema");
        }
        require(ObligationCheckpointCodec.decode(ObligationCheckpointCodec.encode(ObligationCheckpoint.EMPTY)).equals(ObligationCheckpoint.EMPTY),
            "source-only Root cannot round-trip its empty checkpoint");
        var gaps=new RestorationGapBuffer();gaps.missed();
        var gapRequest=gaps.prepare(store.obligationCheckpoint(),4);
        store.shadow().attachDurableBackend(backend);
        backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
        var uncertain=store.checkpointObligations(gaps.expectedRevision(),gapRequest);
        require(uncertain.status()==CheckpointReceipt.Status.UNKNOWN&&!gaps.acknowledge(uncertain),
            "unacknowledged gap checkpoint consumed pending observations");
        gaps.missed();
        require(store.retryPendingPersistence(),"gap image recovery failed");
        // Change Root-owned restriction inputs between the lost ACK and its retry.
        require(store.commit(new DominionCommand.BindOwner(subject)).committed(),"gap fixture Owner change failed");
        long commandIndex=store.commitIndex();byte[] written=Files.readAllBytes(world.resolve("ronova/durable-root-v1.bin"));
        var retried=store.checkpointObligations(gaps.expectedRevision(),gapRequest);
        require(retried.status()==CheckpointReceipt.Status.UNCHANGED&&gaps.acknowledge(retried)&&gaps.count()==1
            &&store.commitIndex()==commandIndex&&Arrays.equals(written,Files.readAllBytes(world.resolve("ronova/durable-root-v1.bin"))),
            "changed Root reasons duplicated or blocked the original gap ACK retry");
        var later=gaps.prepare(retried.acknowledged(),5);
        var laterReceipt=store.checkpointObligations(gaps.expectedRevision(),later);
        require(gaps.acknowledge(laterReceipt)&&!gaps.pending()
            &&laterReceipt.acknowledged().overflow().unscopedCount()==2,
            "observations arriving during an ambiguous write were lost or double-counted");
        require(LocalDominionStore.openWorld(world).obligationCheckpoint().equals(laterReceipt.acknowledged()),
            "restoration gap metadata failed to reopen");
    }
    private static void verifyCoverage(Path world) throws Exception {
        var store=LocalDominionStore.openWorld(world);UUID actor=new UUID(717,970),nonce=new UUID(717,971);
        store.commit(new DominionCommand.BindOwner(actor));
        var a=new SubjectKey(store.realmId(),new UUID(717,972));var b=new SubjectKey(store.realmId(),new UUID(717,973));
        require(store.commit(new DominionCommand.TransitionSubjects(store.prepareTransitions(UUID.randomUUID(),actor,List.of(
            new SubjectTransitionBatch.Transition(a,SubjectTransitionBatch.Kind.REBIND,nonce,null,null),
            new SubjectTransitionBatch.Transition(b,SubjectTransitionBatch.Kind.REBIND,new UUID(717,974),null,null))))).committed(),
            "coverage fixture binding failed");
        var target=store.subjectQualification(a).targetEpoch();var peer=store.subjectQualification(b).targetEpoch();
        var live=new ObligationRegistry();live.recordCoverageGap(target,ObligationKey.Source.SAVED_RECORD,5);
        var request=new ObligationCheckpoint(1,5,live.snapshot(),live.coverage(),CandidateRestrictions.EMPTY,live.overflowCheckpoint());
        require(store.checkpointObligations(0,request).acknowledgedWrite(),"discovery gap did not reach the existing Root writer");
        var restarted=LocalDominionStore.openWorld(world);
        require(restarted.obligationCheckpoint().pending(target)&&!restarted.obligationCheckpoint().pending(peer)
            &&restarted.obligationCheckpoint().overflow().scopes().get(0).sources().contains(ObligationKey.Source.SAVED_RECORD),
            "reopen lost source scope or widened it to every subject");
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
