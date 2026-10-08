package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.policy.*;
import dev.ronova.coremod.root.api.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Root work migration/ACK tests; synthetic binding evidence is not a production cancellation or storage proof. */
final class RecoveryWorkScenarios {
    static void verify() throws Exception {
        Path world=Files.createTempDirectory("ronova-recovery-work-").toAbsolutePath().normalize();
        try {
            var store=LocalDominionStore.openWorld(world);
            require(store.commit(new DominionCommand.ConfigureStrengthPolicy(store.strengthPolicyConfiguration().next(StrengthProfile.EXTREME,Map.of()))).committed(),"work policy failed");
            store=LocalDominionStore.openWorld(world);
            UUID subject=new UUID(717,1201),nonce=new UUID(717,1202);
            var judgment=new DominionDecision.Judgment(new SubjectIdentity(subject,"test:work","test.Work","minecraft:overworld",0,0,0,false),
                store.nextDecisionEpoch(),true,DominionDecision.LineageScope.IDENTITY,0,"work-fixture");
            require(store.commit(store.qualify(JudgmentBatch.of(UUID.randomUUID(),judgment)).withIncarnations(Map.of(subject,nonce))).committed(),"work judgment failed");
            var terminal=store.approveTerminalPlan(subject,nonce);
            var location=new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.STATIC_MANAGER,"","","test.Holder","records","","");
            var snapshot=store.approveStaticSnapshotPlan(terminal,evidence(store,subject,location),1,true);
            var memory=new RecoveryMutationIntent(snapshot,UUID.randomUUID(),RecoveryMutationIntent.Kind.STATIC_RECORD);
            require(store.prepareRecoveryMutation(memory,evidence(store,subject,location),true,location)
                &&store.observeRecoveryMutation(memory,RecoveryMutationIntent.Observation.NOT_EXECUTED),"old memory fixture failed");
            var lifecycle=store.prepareTerminalEffect(terminal,"work-fixture-lifecycle");require(lifecycle.granted(),"old ONCE claim failed");store.finishEffectUnknown(lifecycle);
            var oldRecords=store.shadow().effectRecords();
            var backend=DurableRootFileBackend.at(world.resolve("ronova/durable-root-v1.bin"));store.shadow().attachDurableBackend(backend);
            CompoundTag oldImage=backend.readIfPresent();require(oldImage.getInt("schema")==11,"old fixture did not exercise schema 11");
            Tag oldCommands=Objects.requireNonNull(oldImage.get("command_journal_v1")).copy();long oldCommit=store.commitIndex();
            var description=new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.TASK_CANCEL,location,UUID.randomUUID(),1,1,
                EffectRecord.hash("registered-task-adapter"),EffectRecord.hash("baseline"),EffectRecord.hash("expected"));
            require(RecoveryWorkDescriptor.parse(description.canonical()).equals(description),"work description did not round trip");
            var descendant=new RecoveryWorkDescriptor(description.kind(),location,description.registration(),1,1,description.adapterHash(),description.baselineHash(),description.expectedHash(),true);
            require(RecoveryWorkDescriptor.parse(descendant.canonical()).equals(descendant)&&descendant.permissionDepth()==2
                &&!descendant.effectKey(terminal).equals(description.effectKey(terminal)),"descendant task reused a shallow intent or lost its wire flag");
            var plan=store.approveRecoveryTaskPlan(terminal,evidence(store,subject,location),description,true);
            require(plan!=null&&store.approveRecoveryTaskPlan(terminal,evidence(store,subject,location),description,false)==null,"task binding gate failed");
            require(store.acquireRecoveryTaskWrite(plan,evidence(store,subject,location),true)==null,"task writer obtained permission before ACK");
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(!store.prepareRecoveryTask(plan,evidence(store,subject,location),true)&&store.persistencePending()
                &&store.recoveryWorkResult(plan.effectKey())==null&&store.acquireRecoveryTaskWrite(plan,evidence(store,subject,location),true)==null,"lost ACK left a write/read gate open");
            require(store.retryPendingPersistence(),"work ACK recovery failed");
            var prepared=store.recoveryWorkResult(plan.effectKey()).record();
            require(store.prepareRecoveryTask(plan,evidence(store,subject,location),true)
                &&prepared.equals(store.recoveryWorkResult(plan.effectKey()).record()),"work retry changed original attempt");
            var expiredTaskWitness=evidence(store,subject,location);
            expiredTaskWitness=new RecoverySourceEvidence(expiredTaskWitness.sourceLocator(),expiredTaskWitness.authority(),
                expiredTaskWitness.sourceRevision(),expiredTaskWitness.copyDepth(),true,System.nanoTime()-RecoverySourceEvidence.MAX_AGE_NANOS-1);
            require(store.acquireRecoveryTaskWrite(plan,expiredTaskWitness,true)==null,"expired task witness was refreshed");
            require(store.acquireTaskContinuation(plan,false)==null
                &&store.acquireTaskContinuation(new RecoveryTaskPlan(plan.source(),plan.descriptor()),true)==null,
                "unverified binding or reconstructed task plan acquired a continuation");
            try(var lease=store.acquireTaskContinuation(plan,true)) {
                require(lease!=null&&lease.valid(plan),"original acknowledged task did not continue after witness expiration");
            }
            try(var lease=store.acquireRecoveryTaskWrite(plan,evidence(store,subject,location),true)) {
                require(lease!=null&&lease.valid(plan)&&!lease.valid(snapshot)&&store.commit(new DominionCommand.ClearTombstone(subject)).rejected(),"task fence did not preserve exact scope");
            }
            store.retireTaskContinuation(plan);store.releaseTaskContinuationGuard(plan.effectKey());
            require(store.acquireTaskContinuation(plan,true)==null&&store.acquireRecoveryTaskWrite(plan,evidence(store,subject,location),true)==null
                &&!store.prepareRecoveryTask(plan,evidence(store,subject,location),true),"consumed task scope replayed before receipt ACK");
            require(store.observeRecoveryWork(plan.effectKey(),RecoveryWorkResult.Fact.CANCEL_REQUESTED)
                &&store.observeRecoveryWork(plan.effectKey(),RecoveryWorkResult.Fact.CANCEL_ACCEPTED),"task observations failed");
            var cancellation=store.recoveryWorkResult(plan.effectKey());
            store.releaseTaskContinuationGuard(plan.effectKey());
            require(store.acquireTaskContinuation(plan,true)==null,"previous task effects became replayable");
            require(!store.observeRecoveryWork(plan.effectKey(),RecoveryWorkResult.Fact.FILE_COMMITTED)
                &&store.recoveryWorkResult(plan.effectKey()).equals(cancellation),"storage evidence entered a task intent");
            require(cancellation.has(RecoveryWorkResult.Fact.CANCEL_ACCEPTED)&&!cancellation.has(RecoveryWorkResult.Fact.EXECUTION_EXITED)
                &&cancellation.record().state()==EffectRecord.State.EFFECT_UNKNOWN,"cancel return implied exit or completion");
            // Queue settlement is a distinct durable fact: a wrapper proven never to have run is
            // recorded as NOT_EXECUTED and must never be discharged as EXECUTION_EXITED.
            require(store.observeRecoveryWork(plan.effectKey(),RecoveryWorkResult.Fact.NOT_EXECUTED),"queued settlement was rejected");
            var queued=store.recoveryWorkResult(plan.effectKey());
            require(queued.has(RecoveryWorkResult.Fact.NOT_EXECUTED)&&queued.has(RecoveryWorkResult.Fact.CANCEL_ACCEPTED)
                &&!queued.has(RecoveryWorkResult.Fact.EXECUTION_EXITED),"queued settlement was merged into an execution exit");
            require(store.recoveryWorkResult(plan.effectKey()).equals(queued),"repeated queued settlement was not idempotent");
            // Successor closure is its own fact: recording it must neither mint an exit nor
            // discharge the queued settlement, and it belongs to task work only.
            require(store.observeRecoveryWork(plan.effectKey(),RecoveryWorkResult.Fact.SUCCESSORS_CLOSED),"successor closure was rejected");
            var closed=store.recoveryWorkResult(plan.effectKey());
            require(closed.has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED)&&closed.has(RecoveryWorkResult.Fact.NOT_EXECUTED)
                &&!closed.has(RecoveryWorkResult.Fact.EXECUTION_EXITED)&&!closed.has(RecoveryWorkResult.Fact.RESOURCES_RELEASED),
                "successor closure discharged an execution or resource obligation");
            var storageDescription=new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.STORAGE_COMMIT,location,UUID.randomUUID(),1,1,
                EffectRecord.hash("registered-storage-adapter"),EffectRecord.hash("baseline"),EffectRecord.hash("expected"));
            var storage=store.approveRecoveryStoragePlan(terminal,evidence(store,subject,location),storageDescription,true);
            require(storage!=null&&store.prepareRecoveryStorage(storage,evidence(store,subject,location),true),"storage intent failed");
            // The queue settlement fact belongs to task work only; a storage commit must reject it.
            require(!store.observeRecoveryWork(storage.effectKey(),RecoveryWorkResult.Fact.NOT_EXECUTED)
                &&!store.observeRecoveryWork(storage.effectKey(),RecoveryWorkResult.Fact.SUCCESSORS_CLOSED)
                &&!store.recoveryWorkResult(storage.effectKey()).has(RecoveryWorkResult.Fact.NOT_EXECUTED)
                &&!store.recoveryWorkResult(storage.effectKey()).has(RecoveryWorkResult.Fact.SUCCESSORS_CLOSED),
                "task queue/successor settlement entered a storage intent");
            verifyReadbackQuery(store,storage,plan.effectKey());
            require(store.acquireRecoveryStorageWrite(storage,evidence(store,subject,location),false)==null,"unverified storage binding obtained lease");
            try(var lease=store.acquireRecoveryStorageWrite(storage,evidence(store,subject,location),true)) {
                require(lease!=null&&lease.valid(storage)&&!lease.valid(plan),"task/storage lease scopes aliased");
            }
            CompoundTag image=backend.readIfPresent();
            require(image.getInt("schema")==12&&image.getCompound("effect_journal").getInt("schema")==3,"work metadata lacks unified version gate");
            require(store.commitIndex()==oldCommit&&oldCommands.equals(image.get("command_journal_v1"))&&store.shadow().effectRecords().containsAll(oldRecords),"work changed old commands, hashes or opaque effects");
            for(int version:new int[]{11,13}) {
                CompoundTag bad=image.copy();bad.putInt("schema",version);require(ShadowDurableRootStore.load(bad,store.realmId()).readOnly(),"Root version mismatch was accepted");
            }
            CompoundTag future=image.copy();future.getCompound("effect_journal").putInt("schema",4);
            require(ShadowDurableRootStore.load(future,store.realmId()).readOnly(),"future effect schema was silently downgraded");
            CompoundTag changed=image.copy();
            for(Tag raw:changed.getCompound("effect_journal").getList("records",Tag.TAG_COMPOUND)) {
                var row=(CompoundTag)raw;if(row.contains("work_description")){row.putString("work_description",row.getString("work_description")+"\n");break;}
            }
            require(ShadowDurableRootStore.load(changed,store.realmId()).readOnly(),"tampered work description was accepted");
            var check=new EffectJournal();check.put(cancellation.record());check.describeWork(plan.effectKey(),description);
            boolean rollback=false;try{check.put(cancellation.record().unknown("work-facts/1"));}catch(IllegalArgumentException expected){rollback=true;}
            require(rollback&&check.get(plan.effectKey()).equals(cancellation.record()),"task observation rollback was accepted");
            store=LocalDominionStore.openWorld(world);
            require(!store.readOnly()&&store.shadow().effectRecords().containsAll(oldRecords),"work migration damaged old history");
            require(store.recoveryWorkResult(plan.effectKey()).record().equals(closed.record()),"restart changed observed task result");
            require(store.recoveryWorkResult(storage.effectKey()).has(RecoveryWorkResult.Fact.EFFECT_UNKNOWN),"restart treated unobserved storage as success");
            verifyReadbackQuery(store,storage,plan.effectKey());
            var legacyResult=store.recoveryWorkResult(storage.effectKey());
            var identifiedDescription=new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.STORAGE_COMMIT,location,UUID.randomUUID(),1,1,
                storageDescription.adapterHash(),storageDescription.baselineHash(),storageDescription.expectedHash(),false,"win32-file-id/1:1:2:3");
            require(RecoveryWorkDescriptor.parse(identifiedDescription.canonical()).equals(identifiedDescription),"identity description did not roundtrip");
            for(String invalidIdentity:new String[]{"win32-file-id/1:01:2:3","win32-file-id/1:1:ABC:3","win32-file-id/1:1:2:","win32-file-id/1:1:2:10000000000000000"}) {
                boolean refused=false;
                try {new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.STORAGE_COMMIT,location,UUID.randomUUID(),1,1,
                    storageDescription.adapterHash(),storageDescription.baselineHash(),storageDescription.expectedHash(),false,invalidIdentity);}
                catch(IllegalArgumentException expected){refused=true;}
                require(refused,"noncanonical file identity accepted");
            }
            var identified=store.approveRecoveryStoragePlan(terminal,evidence(store,subject,location),identifiedDescription,true);
            require(identified!=null&&store.prepareRecoveryStorage(identified,evidence(store,subject,location),true),"identified storage intent failed");
            var identifiedImage=backend.readIfPresent();
            require(identifiedImage.getInt("schema")==13&&identifiedImage.getCompound("effect_journal").getInt("schema")==4,"identity versions not unified");
            require(legacyResult.equals(store.recoveryWorkResult(storage.effectKey())),"identity upgrade rewrote old work hash or state");
            var futureRoot=identifiedImage.copy();futureRoot.putInt("schema",14);
            require(ShadowDurableRootStore.load(futureRoot,store.realmId()).readOnly(),"future Root accepted");
            var futureEffects=identifiedImage.copy();futureEffects.getCompound("effect_journal").putInt("schema",5);
            require(ShadowDurableRootStore.load(futureEffects,store.realmId()).readOnly(),"future effect journal accepted");
            var downgraded=identifiedImage.copy();downgraded.putInt("schema",12);
            require(ShadowDurableRootStore.load(downgraded,store.realmId()).readOnly(),"identity metadata downgraded to legacy Root");
            store=LocalDominionStore.openWorld(world);
            require(!store.readOnly()&&store.recoveryWorkResult(identified.effectKey()).descriptor().equals(identifiedDescription),"persisted identity lost on reopen");
            require(legacyResult.equals(store.recoveryWorkResult(storage.effectKey())),"reopen changed legacy opaque effects");
            verifyReadbackQuery(store,identified,plan.effectKey());
            UUID saveIncarnation=UUID.randomUUID();
            var generation=new RecoveryStorageGeneration(RecoveryStorageGeneration.WINDOWS_NTFS_TXF,saveIncarnation,1);
            var generatedDescription=new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.STORAGE_COMMIT,location,
                identifiedDescription.registration(),1,1,storageDescription.adapterHash(),storageDescription.baselineHash(),
                storageDescription.expectedHash(),false,identifiedDescription.fileIdentity(),generation);
            require(RecoveryWorkDescriptor.parse(generatedDescription.canonical()).equals(generatedDescription),"save generation did not roundtrip");
            var generated=store.approveRecoveryStoragePlan(terminal,evidence(store,subject,location),generatedDescription,true);
            require(generated!=null&&store.prepareRecoveryStorage(generated,evidence(store,subject,location),true),"generation intent not acknowledged");
            var firstGeneration=store.recoveryWorkResult(generated.effectKey());
            require(store.prepareRecoveryStorage(generated,evidence(store,subject,location),true)
                &&firstGeneration.equals(store.recoveryWorkResult(generated.effectKey())),"same save generation changed attempt");
            var nextDescription=new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.STORAGE_COMMIT,location,
                identifiedDescription.registration(),1,1,storageDescription.adapterHash(),storageDescription.baselineHash(),
                EffectRecord.hash("next-save-image"),false,identifiedDescription.fileIdentity(),
                new RecoveryStorageGeneration(RecoveryStorageGeneration.WINDOWS_NTFS_TXF,saveIncarnation,2));
            var nextGeneration=store.approveRecoveryStoragePlan(terminal,evidence(store,subject,location),nextDescription,true);
            require(nextGeneration!=null&&!nextGeneration.effectKey().equals(generated.effectKey())
                &&store.prepareRecoveryStorage(nextGeneration,evidence(store,subject,location),true),"new save reused old work slot");
            var disposed=new RecoveryMutationIntent(snapshot,UUID.randomUUID(),RecoveryMutationIntent.Kind.STATIC_RECORD);
            var continuationDescription=new RecoveryWorkDescriptor(RecoveryWorkDescriptor.Kind.STORAGE_COMMIT,location,
                disposed.incarnation(),1,1,storageDescription.adapterHash(),storageDescription.baselineHash(),
                storageDescription.expectedHash(),false,identifiedDescription.fileIdentity(),generation);
            require(store.prepareRecoveryMutation(disposed,evidence(store,subject,location),true,location),"continuation memory intent failed");
            require(store.approveStorageAfterMemory(disposed,continuationDescription,true)==null,"unobserved memory enabled storage continuation");
            require(store.observeRecoveryMutation(disposed,RecoveryMutationIntent.Observation.WRITE_OBSERVED),"memory observation failed");
            var stale=evidence(store,subject,location);
            stale=new RecoverySourceEvidence(stale.sourceLocator(),stale.authority(),stale.sourceRevision(),stale.copyDepth(),true,
                System.nanoTime()-RecoverySourceEvidence.MAX_AGE_NANOS-1);
            require(store.approveRecoveryStoragePlan(terminal,stale,continuationDescription,true)==null,"expired witness became fresh");
            var continuation=store.approveStorageAfterMemory(disposed,continuationDescription,true);
            require(continuation!=null&&store.prepareStorageAfterMemory(continuation,disposed,true),"acknowledged memory did not continue storage");
            require(store.approveStorageAfterMemory(new RecoveryMutationIntent(disposed.source(),disposed.incarnation(),disposed.kind()),continuationDescription,true)==null,
                "reconstructed memory metadata acquired live observer permission");
            require(LocalDominionStore.openWorld(world).approveStorageAfterMemory(disposed,continuationDescription,true)==null,
                "historical memory record acquired write permission on reopen");
            var continuationAck=store.recoveryWorkResult(continuation.effectKey());
            require(store.prepareStorageAfterMemory(continuation,disposed,true)
                &&continuationAck.equals(store.recoveryWorkResult(continuation.effectKey())),"continuation changed original attempt");
            require(store.approveStorageAfterMemory(disposed,generatedDescription,true)==null
                &&store.acquireStorageAfterMemory(continuation,disposed,false)==null,"continuation accepted wrong incarnation or lost binding");
            try(var owned=store.acquireStorageAfterMemory(continuation,disposed,true)) {
                require(owned!=null&&owned.valid(continuation),"continuation final fence unavailable");
            }
            require(store.observeRecoveryMutation(disposed,RecoveryMutationIntent.Observation.WRITE_UNVERIFIED)
                &&store.acquireStorageAfterMemory(continuation,disposed,true)==null,"unknown memory write enabled continuation");
            var storageClaim=store.claimRecoveryStorage(generated);
            require(storageClaim!=null&&storageClaim.granted()&&store.claimRecoveryStorage(generated)==null,
                "original storage ACK issued duplicate execution claims");
            require(!store.enterEffect(storageClaim),"storage claim entered the lifecycle executor");
            var foreignFence=new RootExecutionFence();
            try(var foreign=foreignFence.tryAcquireStorage(generated)) {
                require(!store.enterRecoveryStorageClaim(storageClaim,generated,foreign),"foreign Root fence authorized storage entry");
            }
            try(var owned=store.acquireRecoveryStorageWrite(generated,evidence(store,subject,location),true)) {
                require(owned!=null&&store.enterRecoveryStorageClaim(storageClaim,generated,owned)
                    &&!store.enterRecoveryStorageClaim(storageClaim,generated,owned),"storage execution claim was not one-use");
            }
            store.releaseRecoveryStorageClaim(storageClaim);
            require(store.recoveryWorkResult(generated.effectKey()).has(RecoveryWorkResult.Fact.EFFECT_UNKNOWN)
                &&store.claimRecoveryStorage(generated)==null,"ambiguous storage entry became replayable");
            var generatedImage=backend.readIfPresent();
            require(generatedImage.getInt("schema")==14&&generatedImage.getCompound("effect_journal").getInt("schema")==5,"generation schema not unified");
            futureRoot=generatedImage.copy();futureRoot.putInt("schema",15);
            require(ShadowDurableRootStore.load(futureRoot,store.realmId()).readOnly(),"future generation Root accepted");
            futureEffects=generatedImage.copy();futureEffects.getCompound("effect_journal").putInt("schema",6);
            require(ShadowDurableRootStore.load(futureEffects,store.realmId()).readOnly(),"future generation effects accepted");
            downgraded=generatedImage.copy();downgraded.putInt("schema",13);
            require(ShadowDurableRootStore.load(downgraded,store.realmId()).readOnly(),"generation metadata downgraded");
            store=LocalDominionStore.openWorld(world);
            require(!store.readOnly()&&store.recoveryWorkResult(generated.effectKey()).descriptor().equals(generatedDescription)
                &&store.recoveryWorkResult(nextGeneration.effectKey()).descriptor().equals(nextDescription),"save generation lost on reopen");
            require(legacyResult.equals(store.recoveryWorkResult(storage.effectKey())),"generation migration changed legacy effects");
            require(store.commit(new DominionCommand.ClearTombstone(subject)).committed()
                &&store.acquireRecoveryTaskWrite(plan,evidence(store,subject,location),true)==null
                &&!store.prepareRecoveryTask(plan,evidence(store,subject,location),true),"clear retained old task permission");
            require(!LocalDominionStore.openWorld(world).readOnly(),"retired work history could not reopen");
            System.out.println("RecoveryWorkScenarios PASS (Root schemas 11-14, effect schemas 3-5, save-generation isolation, ACK loss, scoped task/storage fences, old hashes/ONCE, unknown restart, clear)");
        } finally {
            try(var paths=Files.walk(world)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList()) {
                if(!path.toAbsolutePath().normalize().startsWith(world))throw new IllegalStateException("unsafe work fixture cleanup");Files.deleteIfExists(path);
            }}
        }
    }
    private static void verifyReadbackQuery(LocalDominionStore store,RecoveryStoragePlan storage,EffectKey taskKey) {
        var original=store.recoveryWorkResult(storage.effectKey());
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.Function<RecoveryWorkDescriptor,dev.ronova.coremod.observation.RecoveryFileReadback.Result> reader=description->{
            calls.incrementAndGet();require(description.equals(original.descriptor()),"query changed persisted storage description");
            return new dev.ronova.coremod.observation.RecoveryFileReadback.Result(
                dev.ronova.coremod.observation.RecoveryFileReadback.Status.OBSERVED_MATCH,
                dev.ronova.coremod.observation.RecoveryFileReadback.Reason.NONE,1,description.expectedHash(),
                description.fileIdentity().isEmpty()?"fixture-file-identity":description.fileIdentity());
        };
        var result=dev.ronova.coremod.observation.RecoveryStorageReadbackQuery.query(storage.effectKey(),store::recoveryWorkResult,reader);
        require(result.status()==dev.ronova.coremod.observation.RecoveryFileReadback.Status.OBSERVED_MATCH&&calls.get()==1,"original storage query failed");
        require(original.equals(store.recoveryWorkResult(storage.effectKey()))&&!store.recoveryWorkResult(storage.effectKey()).has(RecoveryWorkResult.Fact.READBACK_VERIFIED),"read query invented a durable receipt");
        calls.set(0);
        result=dev.ronova.coremod.observation.RecoveryStorageReadbackQuery.query(storage.effectKey(),key->null,reader);
        require(result.status()==dev.ronova.coremod.observation.RecoveryFileReadback.Status.UNAVAILABLE&&calls.get()==0,"missing ACK allowed external reading");
        result=dev.ronova.coremod.observation.RecoveryStorageReadbackQuery.query(taskKey,store::recoveryWorkResult,reader);
        require(result.status()==dev.ronova.coremod.observation.RecoveryFileReadback.Status.UNAVAILABLE&&calls.get()==0,"task intent entered storage reader");
        var reads=new java.util.concurrent.atomic.AtomicInteger();
        result=dev.ronova.coremod.observation.RecoveryStorageReadbackQuery.query(storage.effectKey(),key->reads.incrementAndGet()==1?original:null,reader);
        require(result.status()==dev.ronova.coremod.observation.RecoveryFileReadback.Status.UNAVAILABLE,"lost post-read intent was accepted");
        result=dev.ronova.coremod.observation.RecoveryStorageReadbackQuery.query(storage.effectKey(),store::recoveryWorkResult,description->
            new dev.ronova.coremod.observation.RecoveryFileReadback.Result(dev.ronova.coremod.observation.RecoveryFileReadback.Status.OBSERVED_MATCH,
                dev.ronova.coremod.observation.RecoveryFileReadback.Reason.NONE,1,EffectRecord.hash("wrong-image"),"fixture-file-identity"));
        require(result.status()==dev.ronova.coremod.observation.RecoveryFileReadback.Status.UNAVAILABLE,"reader bypassed the original expected hash");
        if(!original.descriptor().fileIdentity().isEmpty()) {
            result=dev.ronova.coremod.observation.RecoveryStorageReadbackQuery.query(storage.effectKey(),store::recoveryWorkResult,description->
                new dev.ronova.coremod.observation.RecoveryFileReadback.Result(dev.ronova.coremod.observation.RecoveryFileReadback.Status.OBSERVED_MATCH,
                    dev.ronova.coremod.observation.RecoveryFileReadback.Reason.NONE,1,description.expectedHash(),"win32-file-id/1:1:2:4"));
            require(result.status()==dev.ronova.coremod.observation.RecoveryFileReadback.Status.UNAVAILABLE,"same bytes on a different file accepted");
        }
        System.out.println("RecoveryStorageReadbackQuery Root test PASS (synthetic reader; no production storage receipt)");
    }
    private static RecoverySourceEvidence evidence(LocalDominionStore store,UUID subject,RecoverySourceLocation location) {
        return new RecoverySourceEvidence(location.locatorHash(),store.authorityStamp(subject),1,1,true,System.nanoTime());
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
