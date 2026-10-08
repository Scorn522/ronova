package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Real file/owned-child-process protocol fixtures; never operates on a Minecraft instance. */
public final class EffectJournalScenarios {
    private static final RealmId REALM = new RealmId(new UUID(57L, 1L));
    private static final UUID SUBJECT = new UUID(57L, 2L);
    private static final UUID COMMAND = new UUID(57L, 3L);
    private static final List<String> CRASH_PHASES = List.of("BEFORE_INTENT", "AFTER_INTENT",
        "AFTER_EFFECT", "BEFORE_RECEIPT_WRITE", "AFTER_RECEIPT_REPLACE", "AFTER_RECEIPT_ACK");
    private EffectJournalScenarios() {}

    private static void verifyImmutableAttempt() {
        var subject=new SubjectKey(REALM,SUBJECT);
        var key=new EffectKey(COMMAND,new TargetEpoch(subject,1),EffectRecord.TERMINAL_LIFECYCLE,EffectRecord.ONCE_SLOT);
        var original=EffectRecord.terminalLifecycle(key,EffectRecord.hash("immutable-attempt"),1,1,1);
        for(int change=0;change<8;change++) {
            var journal=new EffectJournal();journal.put(original);
            var changed=new EffectRecord(key,original.authorityPayloadHash(),original.authorityCommitIndex(),original.effectPayloadHash(),
                change==0?2:original.policyRevision(),change==1?new UUID(57,500):original.planId(),
                change==2?2:original.planRevision(),change==3?"substituted-step":original.stepId(),
                change==4?new UUID(57,501):original.attemptId(),original.sideEffectClass(),original.recoveryCapability(),
                change==6?EffectRecord.State.PREPARED:EffectRecord.State.EFFECT_UNKNOWN,
                change==5?3:change==7?1:2,"attempt-update");
            boolean rejected=false;
            try {journal.put(changed);}catch(IllegalArgumentException expected){rejected=true;}
            require(rejected&&journal.get(key).equals(original)&&journal.unresolved()==1,
                "effect update changed retained attempt identity or revision: "+change);
        }
        var journal=new EffectJournal();journal.put(original);
        var unknown=original.unknown("first-unknown");journal.put(unknown);
        var refreshed=unknown.unknown("later-query-still-unknown");journal.put(refreshed);
        require(journal.get(key).equals(refreshed)&&journal.size()==1&&journal.unresolved()==1,
            "valid evidence progression changed effect identity or unresolved accounting");
    }

    static void verifyIntentProtocol() throws Exception {
        verifyImmutableAttempt();
        for (DurableWriteFailpoint point : DurableWriteFailpoint.values()) {
            Path dir = Files.createTempDirectory("ronova-effect-intent-");
            try {
                Fixture f = fixture(dir);
                DominionEpoch epoch = f.store.epoch();
                long index = f.store.commitIndex();
                f.backend.armWriteFailpoint(point);
                EffectClaim claim = f.root.prepareLifecycleEffect(SUBJECT, "physical-a");
                require(claim.granted() == (point == DurableWriteFailpoint.NONE), "unacknowledged intent granted execution");
                require(f.root.effectRecordCount() == 1 && f.root.effectUnresolvedCount() == 1
                    && f.root.effectReservedGrowthBytes() == EffectRecord.RECOVERY_GROWTH_BYTES,
                    "intent or its recovery headroom was not retained");
                EffectKey key = claim.record().key();
                CompoundTag disk = f.backend.readIfPresent();
                int diskCount = disk.getCompound("effect_journal").getList("records", Tag.TAG_COMPOUND).size();
                require(diskCount == (point == DurableWriteFailpoint.BEFORE_WRITE ? 0 : 1),
                    "fault did not bracket the actual intent replacement");
                if (claim.granted()) {
                    require(f.root.enterEffect(claim) && !f.root.enterEffect(claim), "attempt was not one-use");
                    EffectClaim forged = new EffectClaim(EffectClaim.Status.GRANTED, claim.record());
                    require(!f.root.enterEffect(forged), "copied record forged an active attempt handle");
                    require(!f.root.prepareLifecycleEffect(SUBJECT, "other-object").granted(), "duplicate physical object got a second attempt");
                    require(f.root.finishEffectUnknown(claim), "opaque completion state did not persist");
                    require(!f.root.finishEffectUnknown(claim), "finished handle was accepted twice");
                } else {
                    require(!f.root.enterEffect(claim), "unknown claim entered callback");
                    f.backend.armWriteFailpoint(DurableWriteFailpoint.BEFORE_WRITE);
                    require(f.root.recoverEffect(key).status() == EffectClaim.Status.PERSISTENCE_UNKNOWN,
                        "failed effect recovery returned durable acknowledgement");
                    require(f.root.recoverEffect(key).status() == EffectClaim.Status.ALREADY_RECORDED,
                        "same-key effect recovery failed");
                }
                require(f.store.epoch().equals(epoch) && f.store.commitIndex() == index,
                    "effect progress advanced Judgment or command commit index");
                ShadowDurableRootStore restarted = ShadowDurableRootStore.load(f.backend.readIfPresent(), REALM);
                restarted.attachDurableBackend(f.backend);
                require(!restarted.readOnly() && !restarted.prepareLifecycleEffect(SUBJECT, "restart-object").granted(),
                    "restart reissued an opaque attempt or fenced a valid journal");
                EffectRecord record = restarted.effectRecords().get(0);
                require(record.state() == EffectRecord.State.EFFECT_UNKNOWN
                    && record.key().equals(key) && record.attemptId().equals(claim.record().attemptId()),
                    "restart lost original effect identity or guessed completion");
                CompoundTag tampered = f.backend.readIfPresent();
                tampered.getCompound("effect_journal").getList("records", Tag.TAG_COMPOUND)
                    .getCompound(0).putString("payload_hash", EffectRecord.hash("different-payload"));
                require(ShadowDurableRootStore.load(tampered, REALM).readOnly(), "effect payload tampering was accepted");
            } finally { cleanup(dir); }
        }
    }

    static void verifyStorageBackpressure() throws Exception {
        Path dir = Files.createTempDirectory("ronova-effect-capacity-");
        try {
            Fixture f = fixture(dir);
            byte[] before = Files.readAllBytes(f.backend.target());
            DominionSnapshot snapshot = f.store.snapshot();
            DominionCommand.BindOwner owner = new DominionCommand.BindOwner(new UUID(57L, 4L), new UUID(57L, 5L));
            f.backend.setUsableBytesForTest(0L);
            require(f.store.commit(owner).rejected() && f.store.snapshot().equals(snapshot)
                && !f.root.persistencePending() && !f.root.readOnly(), "capacity rejection changed or fenced Root truth");
            EffectClaim blocked = f.root.prepareLifecycleEffect(SUBJECT, "capacity-blocked");
            require(blocked.status() == EffectClaim.Status.STORAGE_BLOCKED && f.root.effectRecordCount() == 0
                && f.root.lifecycleClaimCount() == 0 && Arrays.equals(before, Files.readAllBytes(f.backend.target())),
                "capacity preflight accepted an intent or touched the prior image");
            require(f.root.storageRejections() == 2L && f.root.hasTombstone(SUBJECT), "backpressure lost existing protection or diagnostics");
            f.backend.setUsableBytesForTest(null);
            require(f.store.commit(owner).committed(), "same request did not resume after capacity recovered");
            EffectClaim first = f.root.prepareLifecycleEffect(SUBJECT, "capacity-restored");
            require(first.granted() && f.root.enterEffect(first), "restored capacity did not admit the original subject effect");
            writeEffect(dir.resolve("side-effect.txt"));
            f.backend.setUsableBytesForTest(0L);
            require(!f.root.finishEffectUnknown(first) && f.root.persistencePending(), "unwritable outcome receipt was acknowledged");
            require(f.root.effectRecords().get(0).state() == EffectRecord.State.EFFECT_UNKNOWN,
                "failed outcome persistence forgot uncertainty");
            f.backend.setUsableBytesForTest(null);
            require(f.root.recoverEffect(first.record().key()).status() == EffectClaim.Status.ALREADY_RECORDED
                && readEffect(dir.resolve("side-effect.txt")) == 1, "recovery replayed physical effect");
            long beforeClearIndex = f.store.commitIndex();
            f.store.commit(new DominionCommand.ClearTombstone(SUBJECT));
            require(f.root.effectRecordCount() == 1 && !f.root.hasTombstone(SUBJECT), "clear erased unresolved effect history");
            f.store.commit(JudgmentBatch.of(new UUID(57L, 6L), judgment(f.store.nextDecisionEpoch())));
            EffectClaim second = f.root.prepareLifecycleEffect(SUBJECT, "new-authorized-generation");
            require(second.granted() && !second.record().key().equals(first.record().key())
                && f.root.effectRecordCount() == 2 && f.store.commitIndex() == beforeClearIndex + 2L,
                "new qualification overwrote prior effect or effect writes advanced Root counters");
            f.store.commit(new DominionCommand.ClearTombstone(SUBJECT));
            require(!f.root.enterEffect(second), "clear between prepare and entry did not invalidate the grant");
            require(f.root.finishEffectUnknown(second), "unused grant was not retired conservatively");
            ShadowDurableRootStore restarted = ShadowDurableRootStore.load(f.backend.readIfPresent(), REALM);
            require(!restarted.readOnly() && restarted.effectRecordCount() == 2
                && restarted.recoverEffect(first.record().key()).status() == EffectClaim.Status.ALREADY_RECORDED,
                "historical effect authority depended on the latest subject generation");
        } finally { cleanup(dir); }
    }

    static void verifyDelayedOpaqueCallback() throws Exception {
        Path dir = Files.createTempDirectory("ronova-effect-delayed-");
        var release = new java.util.concurrent.CountDownLatch(1);
        var workers = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            Fixture f = fixture(dir);
            EffectClaim claim = f.root.prepareLifecycleEffect(SUBJECT, "async-producer");
            require(claim.granted() && f.root.enterEffect(claim), "delayed fixture was not admitted");
            var late = workers.submit(() -> {
                try {
                    if (!release.await(10L, TimeUnit.SECONDS)) throw new AssertionError("delayed callback was not released");
                    writeEffect(dir.resolve("side-effect.txt"));
                } catch (Exception failed) { throw new RuntimeException(failed); }
            });
            require(f.root.finishEffectUnknown(claim) && readEffect(dir.resolve("side-effect.txt")) == 0L,
                "delayed fixture did not leave an unresolved absent effect");
            require(!f.root.prepareLifecycleEffect(SUBJECT, "retry-before-late-callback").granted(),
                "temporary absence caused opaque effect replay");
            release.countDown();
            late.get(10L, TimeUnit.SECONDS);
            require(readEffect(dir.resolve("side-effect.txt")) == 1L
                && !f.root.prepareLifecycleEffect(SUBJECT, "retry-after-late-callback").granted(),
                "late completion duplicated the effect");
            require(f.root.effectRecords().get(0).state() == EffectRecord.State.EFFECT_UNKNOWN,
                "a late callback alone fabricated completion evidence");
        } finally {
            release.countDown();
            workers.shutdownNow();
            require(workers.awaitTermination(10L, TimeUnit.SECONDS), "delayed fixture worker did not stop");
            cleanup(dir);
        }
    }

    static void verifyProcessCrashRecovery() throws Exception {
        for (String phase : CRASH_PHASES) {
            Path dir = Files.createTempDirectory("ronova-effect-crash-");
            try {
                Path classpathJar = dir.resolve("classpath.jar");
                Manifest manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                StringBuilder classpath = new StringBuilder();
                for (String entry : System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(java.io.File.pathSeparator))) {
                    if (!entry.isBlank()) classpath.append(Path.of(entry).toAbsolutePath().toUri().toASCIIString()).append(' ');
                }
                manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, classpath.toString());
                try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(classpathJar), manifest)) {}
                String java = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
                Process child = new ProcessBuilder(java, "-Dfile.encoding=UTF-8", "-cp", classpathJar.toString(),
                    EffectJournalScenarios.class.getName(), dir.toString(), phase)
                    .directory(dir.toFile()).redirectErrorStream(true).redirectOutput(dir.resolve("child.log").toFile()).start();
                boolean exited = child.waitFor(30L, TimeUnit.SECONDS);
                if (!exited) { child.destroyForcibly(); child.waitFor(10L, TimeUnit.SECONDS); }
                require(exited && child.exitValue() == 71, "owned crash child failed at " + phase
                    + ": " + Files.readString(dir.resolve("child.log")));
                DurableRootFileBackend backend = DurableRootFileBackend.at(dir.resolve("root.bin"));
                ShadowDurableRootStore recovered = ShadowDurableRootStore.load(backend.readIfPresent(), REALM);
                recovered.attachDurableBackend(backend);
                require(!recovered.readOnly() && recovered.hasTombstone(SUBJECT), "process crash corrupted Root at " + phase);
                long expectedEffects = List.of("BEFORE_INTENT", "AFTER_INTENT").contains(phase) ? 0L : 1L;
                require(readEffect(dir.resolve("side-effect.txt")) == expectedEffects, "wrong crash boundary at " + phase);
                EffectClaim retry = recovered.prepareLifecycleEffect(SUBJECT, "parent-after-crash");
                if (phase.equals("BEFORE_INTENT")) {
                    require(retry.granted() && recovered.enterEffect(retry), "absent intent incorrectly blocked first execution");
                    writeEffect(dir.resolve("side-effect.txt"));
                    require(recovered.finishEffectUnknown(retry), "resumed outcome did not persist");
                } else {
                    require(!retry.granted() && retry.status() == EffectClaim.Status.ALREADY_RECORDED
                        && retry.record().state() == EffectRecord.State.EFFECT_UNKNOWN,
                        "prepared/unknown effect replayed after process crash at " + phase);
                }
                require(readEffect(dir.resolve("side-effect.txt")) == (phase.equals("BEFORE_INTENT") ? 1L : expectedEffects),
                    "physical effect was duplicated on recovery at " + phase);
                require(recovered.commitIndex() == 1L && recovered.targetEpoch(new SubjectKey(REALM, SUBJECT)).generation() == 1L,
                    "crash recovery advanced qualification/commit index");
                System.out.println("EffectCrashScenario: PASS " + phase + " physical_effects=" + readEffect(dir.resolve("side-effect.txt")));
            } finally { cleanup(dir); }
        }
    }

    public static void main(String[] args) throws Exception {
        require(args.length == 2, "crash fixture arguments required");
        crashChild(Path.of(args[0]), args[1]);
    }

    static void crashChild(Path dir, String phase) throws Exception {
        require(CRASH_PHASES.contains(phase), "unknown crash phase");
        require(dir.toAbsolutePath().normalize().startsWith(Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize())
            && dir.getFileName().toString().startsWith("ronova-effect-crash-"), "fixture path is outside its owned temporary directory");
        Fixture f = fixture(dir);
        if (phase.equals("BEFORE_INTENT")) Runtime.getRuntime().halt(71);
        EffectClaim claim = f.root.prepareLifecycleEffect(SUBJECT, "owned-child-process");
        require(claim.granted(), "child intent rejected");
        if (phase.equals("AFTER_INTENT")) Runtime.getRuntime().halt(71);
        require(f.root.enterEffect(claim), "child attempt rejected");
        writeEffect(dir.resolve("side-effect.txt"));
        if (phase.equals("AFTER_EFFECT")) Runtime.getRuntime().halt(71);
        if (phase.equals("BEFORE_RECEIPT_WRITE")) f.backend.armWriteFailpoint(DurableWriteFailpoint.BEFORE_WRITE);
        if (phase.equals("AFTER_RECEIPT_REPLACE")) f.backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
        boolean saved = f.root.finishEffectUnknown(claim);
        require(saved == phase.equals("AFTER_RECEIPT_ACK"), "outcome fault was not triggered");
        Runtime.getRuntime().halt(71);
    }

    private static Fixture fixture(Path dir) {
        DurableRootFileBackend backend = DurableRootFileBackend.at(dir.resolve("root.bin"));
        ShadowDurableRootStore root = new ShadowDurableRootStore(REALM);
        root.attachDurableBackend(backend);
        LocalDominionStore store = new LocalDominionStore(new DominionLedger(REALM), root);
        require(store.commit(JudgmentBatch.of(COMMAND, judgment(store.nextDecisionEpoch()))).committed(), "fixture Judgment failed");
        return new Fixture(root, store, backend);
    }

    private static DominionDecision.Judgment judgment(DominionEpoch epoch) {
        return new DominionDecision.Judgment(new SubjectIdentity(SUBJECT, "test:opaque", "test.Opaque", "test:world", 0D,0D,0D,false),
            epoch,true,DominionDecision.LineageScope.IDENTITY,0D,"opaque-effect-fixture");
    }

    private static void writeEffect(Path path) throws Exception {
        byte[] next = Long.toString(readEffect(path)+1L).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            ByteBuffer buffer = ByteBuffer.wrap(next);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
    }

    private static long readEffect(Path path) throws Exception { return Files.exists(path) ? Long.parseLong(Files.readString(path)) : 0L; }

    private static void cleanup(Path dir) throws Exception {
        Path target = dir.toRealPath();
        Path temporaryRoot = Path.of(System.getProperty("java.io.tmpdir")).toRealPath();
        require(!target.equals(temporaryRoot) && target.startsWith(temporaryRoot)
            && target.getFileName().toString().startsWith("ronova-effect-"), "unsafe fixture cleanup target");
        try (var paths = Files.walk(target)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                require(path.toAbsolutePath().normalize().startsWith(target), "fixture cleanup escaped its directory");
                Files.deleteIfExists(path);
            }
        }
    }

    private record Fixture(ShadowDurableRootStore root, LocalDominionStore store, DurableRootFileBackend backend) {}
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
