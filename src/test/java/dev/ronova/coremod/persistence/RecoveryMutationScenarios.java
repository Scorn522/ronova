package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.policy.*;
import dev.ronova.coremod.root.api.*;
import java.nio.file.*;
import java.util.*;

/** Durable memory intents do not consume lifecycle claims or manufacture external save receipts. */
final class RecoveryMutationScenarios {
    static void verify() throws Exception {
        Path world = Files.createTempDirectory("ronova-recovery-mutation-").toAbsolutePath().normalize();
        try {
            var store = LocalDominionStore.openWorld(world);
            require(store.commit(new DominionCommand.ConfigureStrengthPolicy(store.strengthPolicyConfiguration()
                .next(StrengthProfile.ENHANCED, Map.of()))).committed(), "mutation fixture policy failed");
            store = LocalDominionStore.openWorld(world);
            UUID subject = new UUID(717, 901), nonce = new UUID(717, 902);
            var judgment = new DominionDecision.Judgment(new SubjectIdentity(subject, "test:mutation", "test.Mutation",
                "minecraft:overworld", 0, 0, 0, false), store.nextDecisionEpoch(), true,
                DominionDecision.LineageScope.IDENTITY, 0, "mutation-fixture");
            require(store.commit(store.qualify(JudgmentBatch.of(UUID.randomUUID(), judgment))
                .withIncarnations(Map.of(subject, nonce))).committed(), "mutation fixture judgment failed");
            var terminal = store.approveTerminalPlan(subject, nonce);
            String locator = EffectRecord.hash("fixture:mutation/exact-slot");
            var evidence = evidence(store, subject, locator);
            var snapshot = store.approveStaticSnapshotPlan(terminal, evidence, 1, true);
            var incarnation = UUID.randomUUID();
            var intent = new RecoveryMutationIntent(snapshot, incarnation, RecoveryMutationIntent.Kind.STATIC_RECORD);
            var cell = new RecoveryMutationIntent(snapshot, incarnation, RecoveryMutationIntent.Kind.STATIC_SNAPSHOT);
            var loaded = new RecoveryMutationIntent(store.approveLoadedSnapshotPlan(terminal, evidence, 1, true),
                incarnation, RecoveryMutationIntent.Kind.LOADED_SNAPSHOT);
            require(!intent.effectKey().equals(cell.effectKey()) && !cell.effectKey().equals(loaded.effectKey()),
                "memory effects shared a semantic slot");
            require(!store.prepareRecoveryMutation(intent, evidence, false) && store.effectRecordCount() == 0,
                "unbound source created durable intent");
            require(store.acquireRecoveryMutationWrite(intent, evidence, true) == null,
                "memory writer obtained a lease before durable intent ACK");
            long commit = store.commitIndex(); var epoch = store.epoch();
            var backend = DurableRootFileBackend.at(world.resolve("ronova/durable-root-v1.bin"));
            store.shadow().attachDurableBackend(backend);
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(!store.prepareRecoveryMutation(intent, evidence(store, subject, locator), true)
                && store.persistencePending() && !store.recoveryMutationPrepared(intent),
                "ambiguous intent authorized a memory write");
            var original = store.shadow().effectRecords().get(0);
            require(store.retryPendingPersistence() && store.recoveryMutationPrepared(intent)
                && store.shadow().effectRecords().get(0).equals(original), "intent retry changed the original attempt");
            try (var lease = store.acquireRecoveryMutationWrite(intent, evidence(store, subject, locator), true)) {
                require(lease != null && lease.valid(new RecoveryRecordPlan(snapshot))
                    && !store.prepareRecoveryMutation(cell, evidence(store, subject, locator), true)
                    && store.commit(new DominionCommand.ClearTombstone(subject)).rejected(),
                    "acknowledged intent lost its exact write scope or allowed concurrent Root mutation");
            }
            require(store.prepareRecoveryMutation(intent, evidence(store, subject, locator), true)
                && store.effectRecordCount() == 1 && store.commitIndex() == commit && store.epoch().equals(epoch),
                "duplicate intent changed Judgment or duplicated its record");
            require(backend.readIfPresent().getInt("schema") == 10, "mutation intent lacks its schema gate");
            var downgraded = backend.readIfPresent(); downgraded.putInt("schema", 9);
            require(ShadowDurableRootStore.load(downgraded, store.realmId()).readOnly(), "mutation accepted schema downgrade");
            require(store.prepareRecoveryMutation(cell, evidence(store, subject, locator), true)
                && store.prepareRecoveryMutation(loaded, evidence(store, subject, locator), true)
                && store.effectRecordCount() == 3, "independent mutation kinds were not persisted");
            var lifecycle = store.prepareTerminalEffect(terminal, "mutation-fixture-body");
            require(lifecycle.granted(), "memory intent consumed lifecycle ONCE");
            store.finishEffectUnknown(lifecycle);
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(!store.observeRecoveryMutation(intent, RecoveryMutationIntent.Observation.WRITE_OBSERVED)
                && store.persistencePending() && !store.recoveryMutationPrepared(intent),
                "ambiguous memory observation was acknowledged or left its write gate open");
            require(store.recoveryMutationResult(intent)==null,"unacknowledged memory result exposed to storage driver");
            require(store.retryPendingPersistence()
                && store.observeRecoveryMutation(intent, RecoveryMutationIntent.Observation.WRITE_OBSERVED),
                "memory observation was not recovered through the original pending image");
            var observed = store.shadow().effectRecords().stream().filter(row -> row.key().equals(intent.effectKey()))
                .findFirst().orElseThrow();
            require(intent.acknowledgedWrite(store.recoveryMutationResult(intent)),"original acknowledged memory write not queryable");
            var uncertain=observed.unknown(RecoveryMutationIntent.observations(observed,RecoveryMutationIntent.Observation.WRITE_UNVERIFIED));
            require(!intent.acknowledgedWrite(uncertain),"observed write erased an uncertain effect");
            require(observed.state() == EffectRecord.State.EFFECT_UNKNOWN
                && observed.evidenceRef().equals("memory-observations/1;task-and-save-unproven"),
                "memory observation claimed complete disposition");
            require(store.observeRecoveryMutation(intent, RecoveryMutationIntent.Observation.NOT_EXECUTED),
                "later rejected attempt was not recorded");
            var accumulated = store.shadow().effectRecords().stream().filter(row -> row.key().equals(intent.effectKey()))
                .findFirst().orElseThrow();
            require(accumulated.evidenceRef().equals("memory-observations/9;task-and-save-unproven")
                && accumulated.attemptId().equals(original.attemptId()), "later retry erased the earlier write");
            evidenceMonotonicity(original, accumulated);
            store.observeRecoveryMutation(intent, RecoveryMutationIntent.Observation.NOT_EXECUTED);
            require(store.shadow().effectRecords().contains(accumulated), "duplicate observation advanced its revision");
            store = LocalDominionStore.openWorld(world);
            require(!store.readOnly() && store.effectUnresolvedCount() == 4
                && store.shadow().effectRecords().contains(accumulated), "reopen lost unresolved memory or lifecycle facts");
            require(store.shadow().effectRecords().stream().noneMatch(row -> row.state() == EffectRecord.State.PREPARED),
                "restart retained a live prepared attempt");
            var newIncarnation = new RecoveryMutationIntent(snapshot, UUID.randomUUID(), RecoveryMutationIntent.Kind.STATIC_RECORD);
            require(!store.recoveryMutationPrepared(newIncarnation), "new physical source reused an old intent ACK");
            var beforeBatch = store.shadow().effectRecords();
            require(!store.observeRecoveryMutations(List.of(
                new RecoveryMutationIntent.Receipt(cell, RecoveryMutationIntent.Observation.WRITE_OBSERVED),
                new RecoveryMutationIntent.Receipt(newIncarnation, RecoveryMutationIntent.Observation.NOT_CURRENT)))
                && store.shadow().effectRecords().equals(beforeBatch), "invalid batch partially changed the effect journal");
            var batch = List.of(
                new RecoveryMutationIntent.Receipt(cell, RecoveryMutationIntent.Observation.WRITE_OBSERVED),
                new RecoveryMutationIntent.Receipt(cell, RecoveryMutationIntent.Observation.NOT_EXECUTED),
                new RecoveryMutationIntent.Receipt(loaded, RecoveryMutationIntent.Observation.WRITE_UNVERIFIED));
            store.shadow().attachDurableBackend(backend);
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(!store.observeRecoveryMutations(batch) && store.persistencePending(), "ambiguous batch was acknowledged");
            var stagedBatch = store.shadow().effectRecords();
            require(store.observeRecoveryMutations(batch) && store.shadow().effectRecords().equals(stagedBatch),
                "batch recovery duplicated observations or changed attempts");
            require(stagedBatch.stream().filter(row -> row.key().equals(cell.effectKey())).findFirst().orElseThrow()
                .evidenceRef().equals("memory-observations/9;task-and-save-unproven"),
                "two observations in one batch did not preserve both results");
            store = LocalDominionStore.openWorld(world);
            require(!store.readOnly() && new HashSet<>(store.shadow().effectRecords()).equals(new HashSet<>(stagedBatch))
                && store.effectUnresolvedCount() == 4, "batch reopen lost history or invented external completion");
            var loadedRecord = store.approveLoadedRecordPlan(terminal, evidence(store, subject, locator), 1, true);
            require(loadedRecord != null && store.approveLoadedRecordPlan(terminal, evidence(store, subject, locator), 1, false) == null
                && store.acquireStaticRecordWrite(loadedRecord, evidence(store, subject, locator), true) == null,
                "loaded direct record crossed the static storage scope or accepted an unbound location");
            var loadedRecordIntent = new RecoveryMutationIntent(loadedRecord.source(), incarnation, RecoveryMutationIntent.Kind.LOADED_RECORD);
            require(!loadedRecordIntent.effectKey().equals(loaded.effectKey()) && !loadedRecordIntent.effectKey().equals(intent.effectKey())
                && store.acquireRecoveryMutationWrite(loadedRecordIntent, evidence(store, subject, locator), true) == null,
                "loaded record reused a snapshot or static-record intent");
            require(store.prepareRecoveryMutation(loadedRecordIntent, evidence(store, subject, locator), true),
                "loaded record intent was not durably accepted");
            try (var lease = store.acquireRecoveryMutationWrite(loadedRecordIntent, evidence(store, subject, locator), true)) {
                require(lease != null && lease.valid(loadedRecord), "loaded record intent received a snapshot lease");
                var records = new java.util.concurrent.ConcurrentHashMap<UUID, Object>();
                Object image = new Object(), other = new Object(); var otherSubject = UUID.randomUUID();
                records.put(subject, image); records.put(otherSubject, other);
                require(dev.ronova.coremod.observation.AtomicSnapshotRecord.remove(records, subject, image,
                    () -> lease.valid(loadedRecord)) == dev.ronova.coremod.observation.AtomicSnapshotRecord.Result.REMOVED
                    && records.get(otherSubject) == other, "loaded direct-record primitive touched another entry");
                require(!lease.valid(loadedRecord.source()), "record-removal lease authorized snapshot-cell mutation");
            }
            boolean scopeRejected = false;
            try { new RecoveryMutationIntent(snapshot, incarnation, RecoveryMutationIntent.Kind.LOADED_RECORD); }
            catch (IllegalArgumentException expected) { scopeRejected = true; }
            require(scopeRejected, "static source minted a loaded-record intent");
            store = LocalDominionStore.openWorld(world);
            require(!store.readOnly() && store.effectUnresolvedCount() == 5,
                "loaded record intent did not reopen as unresolved work");
            var location = new RecoverySourceLocation(RecoverySnapshotPlan.StorageScope.LOADED_SAVED_DATA,
                "minecraft:overworld", "fixture/location", "test.Data", "records", "", "");
            var locationEvidence = evidence(store, subject, location.locatorHash());
            var locatedPlan = store.approveLoadedRecordPlan(terminal, locationEvidence, 1, true);
            var locatedIntent = new RecoveryMutationIntent(locatedPlan.source(), UUID.randomUUID(), RecoveryMutationIntent.Kind.LOADED_RECORD);
            store.shadow().attachDurableBackend(backend);
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(!store.prepareRecoveryMutation(locatedIntent, locationEvidence, true, location)
                && store.persistencePending() && !store.recoveryMutationPrepared(locatedIntent),
                "unacknowledged source metadata authorized mutation");
            require(store.retryPendingPersistence() && store.recoveryMutationLocation(locatedIntent.effectKey()).equals(location),
                "source metadata did not survive original-image retry");
            var locatedRecords = store.shadow().effectRecords();
            require(store.prepareRecoveryMutation(locatedIntent, evidence(store, subject, location.locatorHash()), true, location)
                && store.shadow().effectRecords().equals(locatedRecords), "duplicate source metadata replaced an attempt");
            var wrongLocation = new RecoverySourceLocation(location.scope(), location.dimension(), location.storageName(),
                location.mapOwner(), "otherField", "", "");
            require(!store.prepareRecoveryMutation(locatedIntent, evidence(store, subject, location.locatorHash()), true, wrongLocation)
                && store.recoveryMutationLocation(locatedIntent.effectKey()).equals(location), "conflicting location changed old metadata");
            var locatedImage = backend.readIfPresent();
            require(locatedImage.getInt("schema") == 11 && locatedImage.getCompound("effect_journal").getInt("schema") == 2,
                "source metadata lacks its schema version");
            var oldSchema = locatedImage.copy(); oldSchema.putInt("schema", 10);
            require(ShadowDurableRootStore.load(oldSchema, store.realmId()).readOnly(), "location metadata survived Root schema downgrade");
            var corrupted = locatedImage.copy();
            for (var raw : corrupted.getCompound("effect_journal").getList("records", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                var row = (net.minecraft.nbt.CompoundTag) raw;
                if (row.contains("source_location")) row.putString("source_location", row.getString("source_location") + "trailing");
            }
            require(ShadowDurableRootStore.load(corrupted, store.realmId()).readOnly(), "corrupted location metadata was trusted");
            store = LocalDominionStore.openWorld(world);
            require(!store.readOnly() && location.equals(store.recoveryMutationLocation(locatedIntent.effectKey()))
                && store.effectUnresolvedCount() == 6, "reopen lost location metadata or resolved its external effect");
            require(store.commit(new DominionCommand.ClearTombstone(subject)).committed()
                && !store.prepareRecoveryMutation(intent, evidence(store, subject, locator), true)
                && store.acquireRecoveryMutationWrite(intent, evidence(store, subject, locator), true) == null,
                "retired qualification prepared another mutation");
            require(store.acquireRecoveryMutationWrite(loadedRecordIntent, evidence(store, subject, locator), true) == null,
                "retired qualification retained loaded-record permission");
            require(!LocalDominionStore.openWorld(world).readOnly(), "retired mutation history did not reopen");
            System.out.println("RecoveryMutationScenarios PASS");
        } finally {
            try (var paths = Files.walk(world)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    if (!path.toAbsolutePath().normalize().startsWith(world)) throw new IllegalStateException("unsafe fixture cleanup");
                    Files.deleteIfExists(path);
                }
            }
        }
    }
    private static RecoverySourceEvidence evidence(LocalDominionStore store, UUID subject, String locator) {
        return new RecoverySourceEvidence(locator, store.authorityStamp(subject), 1, 1, true, System.nanoTime());
    }
    private static void evidenceMonotonicity(EffectRecord prepared, EffectRecord accumulated) {
        var journal = new EffectJournal(); journal.put(accumulated);
        for (String invalid : List.of("memory-observations/8;task-and-save-unproven",
            "memory-observations/1;task-and-save-unproven", "restart-prepared-effect-unknown",
            "memory-observations/0;task-and-save-unproven", "memory-observations/09;task-and-save-unproven",
            "memory-observations/16;task-and-save-unproven", "unrecognized-query-result")) {
            boolean rejected = false;
            try { journal.put(accumulated.unknown(invalid)); }
            catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected && journal.get(accumulated.key()).equals(accumulated) && journal.unresolved() == 1,
                "invalid or weaker observation replaced prior evidence: " + invalid);
        }
        var extended = accumulated.unknown(RecoveryMutationIntent.observations(accumulated,
            RecoveryMutationIntent.Observation.WRITE_UNVERIFIED));
        journal.put(extended);
        require(RecoveryMutationIntent.observationMask(extended) == 11 && journal.unresolved() == 1,
            "adding an observation lost history or resolved external work");
        var fresh = new EffectJournal(); fresh.put(prepared);
        require(fresh.recoverPrepared() && RecoveryMutationIntent.observationMask(fresh.get(prepared.key())) == 0,
            "restart recovery invented an observation");
        for (String invalid : List.of("memory-observations/0;task-and-save-unproven", "unknown-arbitrary-text")) {
            boolean rejected = false;
            try { new EffectJournal().put(prepared.unknown(invalid)); }
            catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected, "initial journal import accepted malformed memory evidence");
        }
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
