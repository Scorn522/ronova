package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.policy.*;
import dev.ronova.coremod.root.api.*;
import java.nio.file.*;
import java.util.*;

/** Real Root image scenarios; factory/copy integrity is checked separately in the integrated stage. */
final class RecoverySourceScenarios {
    static void verify() throws Exception {
        Path world = Files.createTempDirectory("ronova-source-qualification-").toAbsolutePath().normalize();
        try {
            var store = LocalDominionStore.openWorld(world);
            require(store.commit(new DominionCommand.ConfigureStrengthPolicy(store.strengthPolicyConfiguration()
                .next(StrengthProfile.ENHANCED, Map.of()))).committed(), "fixture policy failed");
            store = LocalDominionStore.openWorld(world);
            UUID subject = new UUID(717, 801), nonce = new UUID(717, 802), owner = new UUID(717, 803), replacement = new UUID(717, 804);
            store.commit(new DominionCommand.BindOwner(owner));
            var decision = new DominionDecision.Judgment(new SubjectIdentity(subject, "test:source", "test.Source",
                "minecraft:overworld", 0, 0, 0, false), store.nextDecisionEpoch(), true,
                DominionDecision.LineageScope.IDENTITY, 0, "source-fixture");
            var receipt = store.commit(store.qualify(JudgmentBatch.of(UUID.randomUUID(), decision)).withIncarnations(Map.of(subject, nonce)));
            require(receipt.committed(), "fixture terminal command failed");
            var plan = store.approveTerminalPlan(subject, nonce);
            String locator = EffectRecord.hash("fixture:loaded-owner/exact-record");
            var evidence = evidence(store, subject, locator, 1, true);
            var file = world.resolve("ronova/durable-root-v1.bin");
            byte[] before = Files.readAllBytes(file);
            require(store.recoverySourceHeld(plan, evidence, replacement)
                && !store.recoverySourceHeld(plan, evidence, owner)
                && Arrays.equals(before, Files.readAllBytes(file)), "read-only outlet changed Root or ignored Owner protection");
            require(!store.recoverySourceHeld(plan, evidence(store, subject, locator, 2, true), replacement), "enhanced widened to derived copies");
            require(!store.recoverySourceHeld(plan, evidence(store, subject, locator, 1, false), replacement), "unverified observer obtained permission");
            var expired = new RecoverySourceEvidence(locator, store.authorityStamp(subject), 1, 1, true,
                System.nanoTime() - RecoverySourceEvidence.MAX_AGE_NANOS - 1);
            require(!store.recoverySourceHeld(plan, expired, replacement), "expired source evidence remained executable");
            var snapshotPlan = store.approveStaticSnapshotPlan(plan, evidence, 1, true);
            require(snapshotPlan != null && store.effectRecordCount() == 0
                && store.approveStaticSnapshotPlan(plan, evidence, 1, false) == null
                && store.approveStaticSnapshotPlan(plan, expired, 1, true) == null
                && store.approveStaticSnapshotPlan(plan, evidence, 2, true) == null
                && store.approveStaticSnapshotPlan(plan, evidence(store, subject, locator, 1, false), 1, true) == null,
                "snapshot permission bypassed source evidence or minted a durable effect");
            try (var snapshotLease = store.acquireStaticSnapshotWrite(snapshotPlan, evidence, true)) {
                require(snapshotLease != null && snapshotLease.valid(snapshotPlan) && store.acquireTerminalWrite(plan) == null
                    && store.commit(new DominionCommand.ClearTombstone(subject)).rejected(),
                    "snapshot write did not exclude concurrent Root mutation");
                Object image = new Object();
                var slot = new java.util.concurrent.atomic.AtomicReference<>(image);
                require(dev.ronova.coremod.observation.AtomicSnapshotReference.clear(slot, image,
                    () -> snapshotLease.valid(snapshotPlan)) == dev.ronova.coremod.observation.AtomicSnapshotReference.ClearResult.CLEARED
                    && slot.get() == null && store.effectRecordCount() == 0,
                    "Root-approved snapshot primitive failed or claimed a durable disposition receipt");
            }
            var wrongSource = new RecoverySnapshotPlan(snapshotPlan.terminal(), EffectRecord.hash("another-source"), 1, 1, 1);
            var loadedPlan = store.approveLoadedSnapshotPlan(plan, evidence, 1, true);
            require(loadedPlan != null && loadedPlan.storageScope() == RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA
                && store.approveLoadedSnapshotPlan(plan, expired, 1, true) == null
                && store.approveLoadedSnapshotPlan(plan, evidence, 1, false) == null
                && store.acquireStaticSnapshotWrite(loadedPlan, evidence, true) == null
                && store.acquireLoadedSnapshotWrite(snapshotPlan, evidence, true) == null,
                "loaded snapshot permission accepted stale evidence or crossed storage scopes");
            try (var loadedLease = store.acquireLoadedSnapshotWrite(loadedPlan, evidence, true)) {
                require(loadedLease != null && loadedLease.valid(loadedPlan) && store.acquireTerminalWrite(plan) == null
                    && store.commit(new DominionCommand.ClearTombstone(subject)).rejected(),
                    "loaded snapshot lease failed to exclude a Root transition");
                Object image = new Object(); var cell = new java.util.concurrent.atomic.AtomicReference<>(image);
                require(dev.ronova.coremod.observation.AtomicSnapshotReference.clear(cell, image,
                    () -> loadedLease.valid(loadedPlan)) == dev.ronova.coremod.observation.AtomicSnapshotReference.ClearResult.CLEARED
                    && cell.get() == null, "loaded snapshot permission did not execute its exact cell operation");
            }
            require(store.acquireStaticSnapshotWrite(wrongSource, evidence, true) == null
                && store.acquireStaticSnapshotWrite(snapshotPlan, evidence(store, subject, locator, 0, true), true) == null
                && store.acquireStaticSnapshotWrite(snapshotPlan, evidence, false) == null
                && Arrays.equals(before, Files.readAllBytes(file)), "snapshot approval changed Root or accepted another source");
            long index = store.commitIndex(); var epoch = store.epoch();
            var recordPlan = store.approveStaticRecordPlan(plan, evidence, 1, true);
            require(recordPlan != null && store.approveStaticRecordPlan(plan, expired, 1, true) == null
                && store.approveStaticRecordPlan(plan, evidence, 1, false) == null
                && store.acquireStaticRecordWrite(new RecoveryRecordPlan(wrongSource), evidence, true) == null,
                "record permission accepted an expired, unbound or different source");
            try (var recordLease = store.acquireStaticRecordWrite(recordPlan, evidence, true)) {
                require(recordLease != null && recordLease.valid(recordPlan)
                    && store.acquireStaticSnapshotWrite(snapshotPlan, evidence, true) == null
                    && store.commit(new DominionCommand.ClearTombstone(subject)).rejected(),
                    "record lease allowed a conflicting Root mutation");
                var records = new java.util.concurrent.ConcurrentHashMap<UUID,Object>();
                Object image = new Object(), other = new Object(); records.put(subject, image); records.put(owner, other);
                require(dev.ronova.coremod.observation.AtomicSnapshotRecord.remove(records, subject, image,
                    () -> recordLease.valid(recordPlan)) == dev.ronova.coremod.observation.AtomicSnapshotRecord.Result.REMOVED
                    && !records.containsKey(subject) && records.get(owner) == other,
                    "record write failed or affected an unrelated entry");
            }
            require(store.effectRecordCount() == 0 && Arrays.equals(before, Files.readAllBytes(file)),
                "record removal invented a persistent disposition receipt");
            var backend = DurableRootFileBackend.at(file);store.shadow().attachDurableBackend(backend);
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(store.qualifyRecoverySource(plan,evidence(store,subject,locator,1,true))==null&&store.persistencePending()
                &&store.obligationCheckpoint().revision()==0,"ambiguous source image was acknowledged or invented an obligation checkpoint");
            var attempted=store.shadow().effectRecords().get(0);
            require(!store.recoverySourceHeld(plan,evidence(store,subject,locator,1,true),replacement),"pending Root permitted source execution");
            require(store.acquireStaticSnapshotWrite(snapshotPlan, evidence(store, subject, locator, 1, true), true) == null,
                "pending Root permitted snapshot mutation");
            require(store.acquireStaticRecordWrite(recordPlan, evidence(store, subject, locator, 1, true), true) == null,
                "pending Root permitted record mutation");
            require(store.acquireLoadedSnapshotWrite(loadedPlan, evidence(store, subject, locator, 1, true), true) == null,
                "pending Root permitted loaded snapshot mutation");
            require(store.retryPendingPersistence()&&!store.persistencePending()&&store.commitIndex()==index
                &&store.epoch().equals(epoch)&&store.shadow().effectRecords().get(0).equals(attempted),
                "maintenance retry changed the source attempt or canonical command");
            byte[] acknowledged=Files.readAllBytes(file);
            require(store.retryPendingPersistence()&&Arrays.equals(acknowledged,Files.readAllBytes(file)),
                "idle persistence retry rewrote an already acknowledged image");
            var source = store.qualifyRecoverySource(plan, evidence(store, subject, locator, 1, true));
            require(source != null && store.commitIndex() == index && store.epoch().equals(epoch), "source metadata issued another Judgment");
            var original = store.shadow().effectRecords().stream().filter(e -> e.key().equals(source.effectKey())).findFirst().orElseThrow();
            require(original.sideEffectClass() == EffectRecord.SideEffectClass.RONOVA_CONTROLLED
                && original.evidenceRef().contains("external-disposition-unproven"), "source qualification claimed foreign record disposal");
            require(backend.readIfPresent().getInt("schema") == 8, "source qualification did not select Root schema 8");
            var replay = store.qualifyRecoverySource(plan, evidence(store, subject, locator, 1, true));
            require(replay != null && store.effectRecordCount() == 1
                && store.shadow().effectRecords().get(0).equals(original), "source replan rewrote the stable semantic slot");
            var opaque = store.prepareTerminalEffect(plan, "source-fixture-body");
            require(opaque.granted(), "source metadata consumed the lifecycle once slot");
            store.finishEffectUnknown(opaque);
            require(store.effectRecordCount() == 2 && store.effectUnresolvedCount() == 1, "source metadata resolved an opaque effect");
            store = LocalDominionStore.openWorld(world);
            require(!store.readOnly() && store.recoverySourceHeld(source, 1, replacement)
                && store.effectUnresolvedCount() == 1, "reopen lost source qualification or unknown lifecycle fact");
            store.commit(new DominionCommand.BindOwner(new UUID(717, 805)));
            require(store.recoverySourceHeld(source, 1, replacement), "unrelated command erased a scoped source qualification");
            var current = store.approveTerminalPlan(subject, nonce);
            var duplicate = store.qualifyRecoverySource(current, evidence(store, subject, locator, 1, true));
            require(duplicate != null && duplicate.effectKey().equals(source.effectKey()), "new global plan revision created another source effect");
            var downgraded = backend.readIfPresent(); downgraded.putInt("schema", 7);
            require(ShadowDurableRootStore.load(downgraded, store.realmId()).readOnly(), "source qualification survived a schema downgrade");
            require(store.commit(new DominionCommand.ClearTombstone(subject)).committed(), "clear failed");
            require(!store.recoverySourceHeld(source, 1, replacement)
                && store.qualifyRecoverySource(current, evidence) == null, "clear retained old source permission");
            require(store.acquireStaticSnapshotWrite(snapshotPlan, evidence(store, subject, locator, 1, true), true) == null,
                "clear retained snapshot write permission");
            require(store.acquireStaticRecordWrite(recordPlan, evidence(store, subject, locator, 1, true), true) == null,
                "clear retained record write permission");
            require(store.acquireLoadedSnapshotWrite(loadedPlan, evidence(store, subject, locator, 1, true), true) == null,
                "clear retained loaded snapshot write permission");
            require(!LocalDominionStore.openWorld(world).readOnly(), "cleared source history failed to reopen");
            System.out.println("RecoverySourceScenarios PASS");
        } finally {
            try (var paths = Files.walk(world)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    if (!path.toAbsolutePath().normalize().startsWith(world)) throw new IllegalStateException("unsafe fixture cleanup");
                    Files.deleteIfExists(path);
                }
            }
        }
    }
    private static RecoverySourceEvidence evidence(LocalDominionStore store, UUID subject, String locator, int depth, boolean verified) {
        return new RecoverySourceEvidence(locator, store.authorityStamp(subject), 1, depth, verified, System.nanoTime());
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
