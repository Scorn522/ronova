package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Exercises the complete filesystem bootstrap, including both optional mirrors. */
final class BootstrapPersistenceScenarios {
    private BootstrapPersistenceScenarios() {}
    static void verify() throws Exception {
        for (String mode : List.of("equivalent", "missing", "corrupt", "wrong-realm", "fact-drift", "stale")) {
            Path world=Files.createTempDirectory("ronova-bootstrap-");
            try {
                LocalDominionStore first=LocalDominionStore.openWorld(world);
                UUID owner=new UUID(58L,1L), subject=new UUID(58L,2L);
                first.commit(new DominionCommand.BindOwner(owner));
                DominionEpoch epoch=first.nextDecisionEpoch();
                JudgmentBatch batch=JudgmentBatch.of(new UUID(58L,3L),new DominionDecision.Judgment(
                    new SubjectIdentity(subject,"test:entity","test.Entity","test:world",1D,2D,3D,false),
                    epoch,true,DominionDecision.LineageScope.IDENTITY,0D,"bootstrap-fixture"));
                first.commit(batch);
                var effect=first.prepareLifecycleEffect(subject,"bootstrap-object");
                require(effect.granted()&&first.enterEffect(effect)&&first.finishEffectUnknown(effect),"fixture effect failed");
                saveMirrors(world,first);
                Path canonical=world.resolve("ronova/durable-root-v1.bin"), ledger=SavedDataMirrorFiles.path(world,SavedDataMirrorFiles.LEDGER);
                byte[] truth=Files.readAllBytes(canonical);
                CompoundTag original=SavedDataMirrorFiles.read(ledger);
                require(!first.snapshot().equals(first.legacyMirror().snapshot())
                    && LegacyMirrorFacts.equivalent(first.shadow(),first.legacyMirror()),"fixture did not reproduce reason-only mismatch");
                switch(mode) {
                    case "missing" -> Files.delete(ledger);
                    case "corrupt" -> Files.write(ledger,new byte[]{1,2,3,4});
                    case "wrong-realm" -> { original.putString("realm_id",new UUID(99L,99L).toString()); write(ledger,original); }
                    case "fact-drift" -> { original.put("owners_v2",new ListTag()); write(ledger,original); }
                    case "stale" -> { original.putLong("sequence",0L); write(ledger,original); }
                    default -> {}
                }
                byte[] mirrorBefore=Files.exists(ledger)?Files.readAllBytes(ledger):null;
                LocalDominionStore loaded=LocalDominionStore.openWorld(world);
                require(!loaded.readOnly() && loaded.realmId().equals(first.realmId())
                    && loaded.epoch().equals(first.epoch()) && loaded.commitIndex()==first.commitIndex()
                    && loaded.hasTombstone(subject) && loaded.shadow().ownerRegistry().contains(owner)
                    && loaded.effectUnresolvedCount()==1L && !loaded.prepareLifecycleEffect(subject,"reopen").granted(),
                    "mirror vetoed or changed authoritative Root: "+mode);
                require(Arrays.equals(truth,Files.readAllBytes(canonical)),"bootstrap changed canonical bytes: "+mode);
                require(LegacyMirrorFacts.equivalent(loaded.shadow(),loaded.legacyMirror()),"mirror facts were not reconstructed");
                if (mirrorBefore!=null) {
                    try(var backups=Files.list(world.resolve("ronova/mirror-backups"))) {
                        require(backups.anyMatch(p->{try{return Arrays.equals(mirrorBefore,Files.readAllBytes(p));}catch(Exception e){throw new RuntimeException(e);}}),"old mirror bytes were not preserved");
                    }
                }
                saveMirrors(world,loaded);
                LocalDominionStore twice=LocalDominionStore.openWorld(world);
                require(twice.snapshot().equals(first.snapshot()) && twice.effectUnresolvedCount()==1L
                    && Arrays.equals(truth,Files.readAllBytes(canonical)),"second bootstrap changed truth");
            } finally { cleanup(world); }
        }
        verifyRejectedTruth();
        verifyIncident();
    }

    private static void verifyRejectedTruth() throws Exception {
        for(String mode:List.of("checksum","missing","schema","realm","journal","marker")) {
            Path world=Files.createTempDirectory("ronova-bootstrap-");
            try {
                LocalDominionStore store=LocalDominionStore.openWorld(world);saveMirrors(world,store);
                Path file=world.resolve("ronova/durable-root-v1.bin");
                DurableRootFileBackend backend=DurableRootFileBackend.at(file);
                CompoundTag data=backend.readIfPresent();
                switch(mode) {
                    case "checksum" -> { byte[] b=Files.readAllBytes(file);b[b.length-1]^=1;Files.write(file,b); }
                    case "missing" -> Files.delete(file);
                    case "schema" -> {data.putInt("schema",999);backend.write(data);}
                    case "realm" -> {data.putString("realm_id","invalid-realm");backend.write(data);}
                    case "journal" -> {data.putInt("command_journal_format",999);backend.write(data);}
                    case "marker" -> {data.putBoolean("authoritative_7_16",false);backend.write(data);}
                }
                byte[] before=Files.exists(file)?Files.readAllBytes(file):null;
                boolean rejected=false;
                try {LocalDominionStore.openWorld(world);}catch(RuntimeException expected){rejected=true;}
                require(rejected,"invalid/missing canonical truth silently fell back: "+mode);
                require(before==null?!Files.exists(file):Arrays.equals(before,Files.readAllBytes(file)),"invalid Root was overwritten");
            } finally {cleanup(world);}
        }
    }

    private static void verifyIncident() throws Exception {
        Path source=Path.of("validation/7.17/r57-startup-failure-20260907");
        if(!Files.exists(source.resolve("root.bin"))) return;
        Path world=Files.createTempDirectory("ronova-bootstrap-");
        try {
            Files.createDirectories(world.resolve("ronova"));Files.createDirectories(world.resolve("data"));
            Files.copy(source.resolve("root.bin"),world.resolve("ronova/durable-root-v1.bin"));
            Files.copy(source.resolve("ledger.dat"),SavedDataMirrorFiles.path(world,SavedDataMirrorFiles.LEDGER));
            Files.copy(source.resolve("shadow.dat"),SavedDataMirrorFiles.path(world,SavedDataMirrorFiles.SHADOW));
            byte[] before=Files.readAllBytes(world.resolve("ronova/durable-root-v1.bin"));
            for(int pass=0;pass<2;pass++) {
                LocalDominionStore store=LocalDominionStore.openWorld(world);
                require(store.authoritative()&&!store.readOnly()&&store.shadow().ownerCount()==1&&store.shadow().tombstoneCount()==1,
                    "real incident failed complete LocalDominionStore bootstrap");
                for(var claim:store.lifecycleIntegrationReceipts()) require(!store.prepareLifecycleIntegration(claim.judgmentId(),"incident-reopen").mayExecute(),"incident replayed lifecycle effect");
                saveMirrors(world,store);
            }
            require(Arrays.equals(before,Files.readAllBytes(world.resolve("ronova/durable-root-v1.bin"))),"incident canonical file changed");
            System.out.println("BootstrapIncident: PASS real Root/Ledger/shadow, save/reopen twice, canonical bytes unchanged");
        } finally {cleanup(world);}
    }

    static void saveMirrors(Path world,LocalDominionStore store) throws Exception {
        if(!store.mirrorPersistenceEnabled())return;
        write(SavedDataMirrorFiles.path(world,SavedDataMirrorFiles.LEDGER),store.legacyMirror().save(new CompoundTag()));
        write(SavedDataMirrorFiles.path(world,SavedDataMirrorFiles.SHADOW),store.shadow().save(new CompoundTag()));
    }
    private static void write(Path file,CompoundTag data) throws Exception {
        Files.createDirectories(file.getParent());CompoundTag wrapper=new CompoundTag();wrapper.put("data",data);wrapper.putInt("DataVersion",3465);
        NbtIo.writeCompressed(wrapper,file.toFile());
    }
    private static void cleanup(Path world) throws Exception {
        Path target=world.toRealPath(), base=Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
        require(target.startsWith(base)&&target.getFileName().toString().startsWith("ronova-bootstrap-"),"unsafe fixture cleanup");
        try(var paths=Files.walk(target)){for(Path p:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
