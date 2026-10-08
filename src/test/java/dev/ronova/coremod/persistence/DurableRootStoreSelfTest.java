package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.root.api.JudgmentBatch;
import dev.ronova.coremod.policy.MechanismLevel;
import dev.ronova.coremod.policy.ProfilePresetMatrix;
import dev.ronova.coremod.policy.StrengthOptionKey;
import dev.ronova.coremod.policy.StrengthPolicyConfiguration;
import dev.ronova.coremod.policy.StrengthProfile;
import dev.ronova.coremod.policy.AttackAction;
import dev.ronova.coremod.policy.AttackActionLevel;
import dev.ronova.coremod.terminal.LifecycleIntegrationReceipt;
import dev.ronova.coremod.terminal.LifecycleClaimResult;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** 7.15 migration plus 7.16 Root writer, replay and strength-policy regression gate. */
public final class DurableRootStoreSelfTest {
    private static final String TERMINAL = "dev/ronova/coremod/engine/TerminalLifecycle";
    private static final String RUNTIME = "dev/ronova/coremod/runtime/DominionServerRuntime";
    private static final String LEDGER = "dev/ronova/coremod/persistence/DominionLedger";
    private static final String SHADOW = "dev/ronova/coremod/persistence/ShadowDurableRootStore";
    private static final String LOCAL_STORE = "dev/ronova/coremod/persistence/LocalDominionStore";
    private static final String RECEIPT = "dev/ronova/coremod/terminal/LifecycleIntegrationReceipt";

    private static final RealmId REALM = new RealmId(
        UUID.fromString("11111111-2222-3333-4444-555555555555")
    );
    private static final UUID OWNER =
        UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID SUBJECT_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000101");

    private DurableRootStoreSelfTest() {}

    public static void main(String[] args) throws Exception {
        if (args.length >= 2 && args[0].equals("--inspect-root")) {
            for (int i=1; i<args.length; i++) {
                CompoundTag image = DurableRootFileBackend.at(Path.of(args[i])).readIfPresent();
                require(image != null, "archived Root file is missing");
                ShadowDurableRootStore loaded = ShadowDurableRootStore.load(image);
                require(!loaded.readOnly(), "archived Root rejected: " + loaded.readOnlyReason());
                System.out.println("DurableRootProbe: VALID file=" + Path.of(args[i]).getFileName()
                    + " schema=" + image.getInt("schema") + " journal=" + image.getInt("command_journal_format")
                    + " epoch=" + loaded.epoch() + " owners=" + loaded.ownerCount() + " tombstones=" + loaded.tombstoneCount()
                    + " legacy_claims=" + loaded.lifecycleClaimCount() + " effects=" + loaded.effectRecordCount()
                    + " pending_upgrade_or_recovery=" + loaded.persistencePending());
            }
            return;
        }
        if (args.length == 3 && args[0].equals("--halt-effect-fixture")) {
            EffectJournalScenarios.crashChild(Path.of(args[1]), args[2]);
            return;
        }
        verifiesFreshRealmPersistsWithoutMutation();
        BootstrapPersistenceScenarios.verify();
        verifiesLegalRealmRecovery();
        verifiesAuthoritativeReceiptRoundTrip();
        verifiesLegacyRealmMigration();
        verifiesCorruptRealmFailsClosed();
        verifiesShadowReceiptMirrorAndRecovery();
        verifiesShadowFailpoints();
        verifiesAtomicDurableFileBackend();
        verifiesUnacknowledgedRootIsNotReadable();
        verifiesSubjectQualificationTransactions();
        QualificationPreconditionScenarios.verify();
        SubjectBindingScenarios.verify();
        AuthorityStampScenarios.verify();
        TerminalPlanScenarios.verify();
        RecoverySourceScenarios.verify();
        RecoveryMutationScenarios.verify();
        RecoveryWorkScenarios.verify();
        dev.ronova.coremod.observation.RecoveryTaskDispositionSelfTest.main(new String[0]);
        ObligationCheckpointScenarios.verify();
        verifiesLegacyQualificationMigration();
        EffectJournalScenarios.verifyIntentProtocol();
        EffectJournalScenarios.verifyStorageBackpressure();
        EffectJournalScenarios.verifyDelayedOpaqueCallback();
        EffectJournalScenarios.verifyProcessCrashRecovery();
        verifiesLifecycleClaimAmbiguity();
        verifiesLifecycleClaimDoesNotBypassPendingJudgment();
        verifiesConcurrentLifecycleClaims();
        verifiesCompactCommandJournal();
        verifies716CutoverAndCommitRecovery();
        verifiesStrengthPolicyPersistence();
        verifiesLegacyStrengthPolicyMatrixMigration();
        verifiesOrdinaryActionRestartGate();
        verifiesDeterministicCommandParity();
        verifiesSingleWriterWiring();
        System.out.println(
            "DurableRootStoreSelfTest: PASS "
                + "(fresh realm, legal recovery, migration, corruption fence, "
                + "Root cutover, COMMIT_UNKNOWN replay, atomic batch, strength policy, "
                + "legacy profile-matrix migration, "
                + "compact journal/action-domain conflict, checksummed atomic file, "
                + "acknowledged read-head isolation, per-subject qualification/clear/restart/migration, "
                + "effect journal/one-attempt/delayed callback/storage headroom/6 child-JVM crash windows, "
                + "lifecycle intent ambiguity/restart/concurrency, task-cancel observer linearization, 10k migration parity, wiring)"
        );
    }

    private static void verifiesFreshRealmPersistsWithoutMutation() throws Exception {
        DominionLedger ledger = new DominionLedger();
        require(ledger.isDirty(),
            "fresh authoritative realm was not scheduled for its first save");
        RealmId createdRealm = ledger.realmId();
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(createdRealm);
        ShadowVerification mirrored = shadow.mirror(
            createdRealm, ledger.snapshot(), ledger.lifecycleIntegrationReceipts()
        );
        require(mirrored.status() == ShadowVerification.Status.MIRRORED
                && shadow.initialized() && shadow.isDirty(),
            "fresh shadow was not initialized and scheduled beside the ledger");

        DominionLedger restartedLedger = DominionLedger.load(saveData(ledger));
        ShadowDurableRootStore restartedShadow = ShadowDurableRootStore.load(saveData(shadow));
        require(restartedLedger.realmId().equals(createdRealm),
            "fresh authoritative realm changed after a mutation-free restart");
        require(restartedShadow.realmId().equals(createdRealm),
            "fresh shadow realm changed after a mutation-free restart");
        require(restartedShadow.verify(
            restartedLedger.realmId(), restartedLedger.snapshot(),
            restartedLedger.lifecycleIntegrationReceipts()
        ).status() == ShadowVerification.Status.VERIFIED,
            "fresh ledger and shadow fenced after a mutation-free restart");
    }

    private static void verifiesLegalRealmRecovery() throws Exception {
        CompoundTag ledgerRoot = new CompoundTag();
        ledgerRoot.putString("realm_id", REALM.canonical());
        ledgerRoot.putLong("epoch_generation", 3L);
        ledgerRoot.putLong("sequence", 4L);

        DominionLedger ledger = DominionLedger.load(ledgerRoot);
        require(ledger.realmId().equals(REALM),
            "legal authoritative realm was not restored");
        require(!ledger.realmIdMigrated() && !ledger.isDirty(),
            "legal authoritative realm was treated as a migration");
        DominionLedger reloadedLedger = DominionLedger.load(saveData(ledger));
        require(reloadedLedger.realmId().equals(REALM) && !reloadedLedger.isDirty(),
            "legal authoritative realm did not survive a normal reload");

        ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
        ShadowDurableRootStore reloadedShadow = ShadowDurableRootStore.load(
            saveData(shadow), REALM
        );
        require(reloadedShadow.realmId().equals(REALM)
                && !reloadedShadow.realmIdMigrated()
                && !reloadedShadow.readOnly(),
            "legal shadow realm was not restored normally");
        require(reloadedShadow.verify(
            REALM, reloadedShadow.snapshot(), List.of()
        ).status() == ShadowVerification.Status.VERIFIED,
            "legal shadow realm did not reach VERIFIED");
    }

    private static void verifiesAuthoritativeReceiptRoundTrip() throws Exception {
        Fixture fixture = fixture();
        DominionLedger ledger = fixture.ledger();
        LifecycleIntegrationReceipt receipt = fixture.receipt();

        require(ledger.realmId().equals(REALM), "authoritative realm id changed");
        require(ledger.hasTombstone(SUBJECT_ID), "committed judgment did not create a tombstone");
        require(ledger.hasLifecycleIntegration(SUBJECT_ID),
            "authoritative lifecycle receipt was not retained");
        require(ledger.lifecycleIntegrationReceipt(SUBJECT_ID).equals(receipt),
            "authoritative lifecycle receipt changed after claim");
        require(ledger.claimLifecycleIntegration(SUBJECT_ID, "second-physical-object") == null,
            "duplicate lifecycle integration claim was accepted");

        CompoundTag persisted = saveData(ledger);
        DominionLedger restored = DominionLedger.load(persisted);
        require(restored.realmId().equals(REALM), "realm id was not durable");
        require(restored.snapshot().equals(ledger.snapshot()),
            "authoritative snapshot changed after NBT reload");
        require(restored.lifecycleIntegrationReceipt(SUBJECT_ID).equals(receipt),
            "lifecycle receipt was not durable across NBT reload");
        require(restored.claimLifecycleIntegration(SUBJECT_ID, "after-reload") == null,
            "reloaded ledger accepted a second lifecycle integration");

        require(restored.clearTombstone(SUBJECT_ID), "explicit tombstone clear did not remove judgment");
        require(!restored.hasTombstone(SUBJECT_ID), "cleared tombstone remained authoritative");
        require(restored.lifecycleIntegrationReceipts().isEmpty(),
            "tombstone clear did not revoke its lifecycle receipt");
        DominionLedger cleared = DominionLedger.load(saveData(restored));
        require(!cleared.hasTombstone(SUBJECT_ID)
                && cleared.lifecycleIntegrationReceipts().isEmpty(),
            "tombstone or receipt returned after clear and reload");
    }

    private static void verifiesShadowReceiptMirrorAndRecovery() throws Exception {
        Fixture fixture = fixture();
        DominionLedger ledger = fixture.ledger();
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(
            REALM, fixture.beforeReceipt(), List.of()
        );

        ShadowVerification initial = shadow.verify(REALM, fixture.beforeReceipt(), List.of());
        require(initial.status() == ShadowVerification.Status.VERIFIED,
            "fresh shadow did not verify its initial state");

        ShadowVerification receiptMirror = shadow.mirror(
            REALM, fixture.withReceipt(), List.of(fixture.receipt())
        );
        require(receiptMirror.status() == ShadowVerification.Status.RECEIPT_MIRRORED,
            "same-epoch lifecycle receipt was not mirrored independently");
        require(shadow.lifecycleIntegrationReceipts().equals(List.of(fixture.receipt())),
            "shadow lifecycle receipt set differs from authority");
        require(shadow.verify(
            REALM, fixture.withReceipt(), List.of(fixture.receipt())
        ).status() == ShadowVerification.Status.VERIFIED,
            "mirrored lifecycle receipt did not verify");

        ShadowVerification drift = shadow.verify(REALM, fixture.withReceipt(), List.of());
        require(drift.status() == ShadowVerification.Status.RECEIPT_DRIFT,
            "receipt-only drift was not classified separately");
        String stateBeforeReceiptRollback = shadow.stateHash();
        ShadowVerification rollback = shadow.mirror(
            REALM, fixture.withReceipt(), List.of()
        );
        require(rollback.status() == ShadowVerification.Status.RECEIPT_DRIFT
                && shadow.stateHash().equals(stateBeforeReceiptRollback)
                && shadow.lifecycleIntegrationReceipts().equals(List.of(fixture.receipt())),
            "shadow accepted lifecycle receipt deletion or changed its retained evidence");
        LifecycleIntegrationReceipt rewritten = new LifecycleIntegrationReceipt(
            SUBJECT_ID, "rewritten-physical-object", fixture.receipt().integratedEpoch()
        );
        ShadowVerification rewrite = shadow.mirror(
            REALM, fixture.withReceipt(), List.of(rewritten)
        );
        require(rewrite.status() == ShadowVerification.Status.RECEIPT_DRIFT
                && shadow.stateHash().equals(stateBeforeReceiptRollback)
                && shadow.lifecycleIntegrationReceipts().equals(List.of(fixture.receipt())),
            "shadow accepted lifecycle receipt rewrite or replaced its retained evidence");
        require(shadow.verify(
            REALM, fixture.withReceipt(), List.of(fixture.receipt())
        ).status() == ShadowVerification.Status.VERIFIED,
            "receipt drift changed shadow state");

        ShadowVerification realmMismatch = shadow.mirror(
            new RealmId(UUID.fromString("99999999-8888-7777-6666-555555555555")),
            fixture.withReceipt(), List.of(fixture.receipt())
        );
        require(realmMismatch.status() == ShadowVerification.Status.REALM_MISMATCH,
            "shadow accepted a different realm");

        SubjectIdentity second = subject(
            UUID.fromString("00000000-0000-0000-0000-000000000202"),
            "minecraft:skeleton", "test.Skeleton"
        );
        DominionDecision.Judgment secondDecision = new DominionDecision.Judgment(
            second, ledger.nextDecisionEpoch(), true,
            DominionDecision.LineageScope.GLOBAL_LINEAGE, 0.0D, "second"
        );
        ledger.record(second, secondDecision);
        DominionSnapshot newer = ledger.snapshot();
        ShadowVerification newerMirror = shadow.mirror(
            REALM, newer, ledger.lifecycleIntegrationReceipts()
        );
        require(newerMirror.status() == ShadowVerification.Status.MIRRORED,
            "newer authoritative snapshot was not mirrored");
        require(shadow.mirror(
            REALM, fixture.withReceipt(), List.of(fixture.receipt())
        ).status() == ShadowVerification.Status.ROLLBACK_REJECTED,
            "shadow accepted an older authoritative snapshot");

        ShadowDurableRootStore reloaded = ShadowDurableRootStore.load(saveData(shadow));
        require(reloaded.realmId().equals(REALM), "shadow realm was not durable");
        require(reloaded.snapshot().equals(shadow.snapshot()),
            "shadow snapshot changed after NBT reload");
        require(reloaded.stateHash().equals(shadow.stateHash()),
            "shadow state hash changed after NBT reload");
        require(reloaded.lifecycleIntegrationReceipts().equals(
            shadow.lifecycleIntegrationReceipts()
        ), "shadow lifecycle receipts changed after NBT reload");
        require(reloaded.verify(
            REALM, shadow.snapshot(), shadow.lifecycleIntegrationReceipts()
        ).status() == ShadowVerification.Status.VERIFIED,
            "reloaded shadow did not verify against its persisted state");
    }

    private static void verifiesLegacyRealmMigration() throws Exception {
        CompoundTag legacy = new CompoundTag();
        legacy.putLong("epoch_generation", 2L);
        legacy.putLong("sequence", 7L);

        DominionLedger migrated = DominionLedger.load(legacy);
        require(migrated.isDirty(), "legacy realm migration did not mark the ledger dirty");
        require(migrated.realmIdMigrated(),
            "legacy realm migration was not recorded in the ledger state");
        RealmId stableRealm = migrated.realmId();
        CompoundTag saved = saveData(migrated);
        require(saved.contains("realm_id")
                && stableRealm.canonical().equals(saved.getString("realm_id"))
                && saved.getBoolean("realm_id_migrated"),
            "legacy realm migration marker was not persisted");

        DominionLedger restarted = DominionLedger.load(saved);
        require(restarted.realmId().equals(stableRealm)
                && restarted.realmIdMigrated(),
            "legacy realm id changed after save and restart");
        require(!restarted.isDirty(),
            "modernized realm NBT was treated as a second migration");

        ShadowDurableRootStore shadow = new ShadowDurableRootStore(
            stableRealm, migrated.snapshot(), migrated.lifecycleIntegrationReceipts()
        );
        CompoundTag legacyShadow = saveData(shadow);
        legacyShadow.remove("realm_id");
        legacyShadow.remove("realm_id_migrated");
        ShadowDurableRootStore migratedShadow = ShadowDurableRootStore.load(
            legacyShadow, stableRealm
        );
        require(migratedShadow.realmId().equals(stableRealm)
                && migratedShadow.realmIdMigrated()
                && migratedShadow.isDirty(),
            "legacy shadow realm did not migrate once and mark dirty");

        CompoundTag savedShadow = saveData(migratedShadow);
        require(savedShadow.contains("realm_id")
                && stableRealm.canonical().equals(savedShadow.getString("realm_id"))
                && savedShadow.getBoolean("realm_id_migrated"),
            "legacy shadow realm migration marker was not persisted");
        ShadowDurableRootStore restartedShadow = ShadowDurableRootStore.load(
            savedShadow, stableRealm
        );
        require(restartedShadow.realmId().equals(stableRealm)
                && restartedShadow.realmIdMigrated()
                && !restartedShadow.isDirty(),
            "modernized shadow realm was treated as a second migration");
        require(restartedShadow.verify(
            restarted.realmId(), restarted.snapshot(), restarted.lifecycleIntegrationReceipts()
        ).status() == ShadowVerification.Status.VERIFIED,
            "migrated shadow did not finish VERIFIED against the ledger");
    }

    private static void verifiesCorruptRealmFailsClosed() {
        CompoundTag invalidLedger = new CompoundTag();
        invalidLedger.putString("realm_id", "not-a-uuid");
        requireRealmRejected(
            () -> DominionLedger.load(invalidLedger), "authoritative invalid realm"
        );
        require("not-a-uuid".equals(invalidLedger.getString("realm_id")),
            "invalid authoritative realm was replaced in the source tag");

        CompoundTag invalidShadow = new CompoundTag();
        invalidShadow.putString("realm_id", "not-a-uuid");
        requireRealmRejected(
            () -> ShadowDurableRootStore.load(invalidShadow, REALM), "shadow invalid realm"
        );
        require("not-a-uuid".equals(invalidShadow.getString("realm_id")),
            "invalid shadow realm was replaced in the source tag");

        CompoundTag wrongType = new CompoundTag();
        wrongType.putLong("realm_id", 42L);
        requireRealmRejected(
            () -> DominionLedger.load(wrongType), "invalid realm NBT type"
        );
        require(wrongType.getLong("realm_id") == 42L,
            "invalid realm NBT value was replaced");

        CompoundTag missingAfterMigration = new CompoundTag();
        missingAfterMigration.putBoolean("realm_id_migrated", true);
        requireRealmRejected(
            () -> DominionLedger.load(missingAfterMigration), "missing after migration"
        );

        CompoundTag invalidMarker = new CompoundTag();
        invalidMarker.putString("realm_id_migrated", "true");
        requireRealmRejected(
            () -> ShadowDurableRootStore.load(invalidMarker, REALM),
            "realm_id_migrated has an invalid NBT type"
        );

        CompoundTag shadowMissingAfterMigration = new CompoundTag();
        shadowMissingAfterMigration.putBoolean("realm_id_migrated", true);
        requireRealmRejected(
            () -> ShadowDurableRootStore.load(shadowMissingAfterMigration, REALM),
            "shadow realm missing after migration"
        );

        CompoundTag mismatchedShadow = new CompoundTag();
        mismatchedShadow.putString(
            "realm_id", "99999999-8888-7777-6666-555555555555"
        );
        ShadowDurableRootStore fenced = ShadowDurableRootStore.load(
            mismatchedShadow, REALM
        );
        require(fenced.readOnly()
                && fenced.readOnlyReason().contains("does not match authoritative ledger realm"),
            "legal but mismatched shadow realm was not fenced");
    }

    private static void verifiesShadowFailpoints() {
        for (CrashFailpoint point : List.of(
            CrashFailpoint.BEFORE_APPLY,
            CrashFailpoint.AFTER_APPLY_BEFORE_DIRTY,
            CrashFailpoint.AFTER_DIRTY_BEFORE_ACK
        )) {
            Fixture fixture = fixture();
            ShadowDurableRootStore shadow = new ShadowDurableRootStore(
                REALM, fixture.beforeReceipt(), List.of()
            );
            shadow.armFailpoint(point);
            boolean interrupted = false;
            try {
                shadow.mirror(REALM, fixture.withReceipt(), List.of(fixture.receipt()));
            } catch (ShadowCrashException expected) {
                interrupted = true;
            }
            require(interrupted, "failpoint did not interrupt " + point);
            require(shadow.failpoint() == CrashFailpoint.NONE,
                "failpoint was not consumed: " + point);

            if (point == CrashFailpoint.BEFORE_APPLY) {
                require(shadow.lifecycleIntegrationReceipts().isEmpty()
                        && !shadow.persistencePending(),
                    "before-apply failure changed shadow state");
            } else {
                require(shadow.lifecycleIntegrationReceipts().equals(List.of(fixture.receipt())),
                    "mid-mirror failure lost the applied lifecycle receipt: " + point);
                require(shadow.persistencePending()
                        == (point == CrashFailpoint.AFTER_APPLY_BEFORE_DIRTY),
                    "pending persistence state is incorrect: " + point);
            }

            ShadowVerification retry = shadow.mirror(
                REALM, fixture.withReceipt(), List.of(fixture.receipt())
            );
            ShadowVerification.Status expected = switch (point) {
                case BEFORE_APPLY -> ShadowVerification.Status.RECEIPT_MIRRORED;
                case AFTER_APPLY_BEFORE_DIRTY -> ShadowVerification.Status.MIRRORED;
                case AFTER_DIRTY_BEFORE_ACK -> ShadowVerification.Status.VERIFIED;
                case NONE -> throw new AssertionError("unexpected NONE failpoint");
            };
            require(retry.status() == expected,
                "retry status is wrong for " + point + ": " + retry.status());
            require(!shadow.persistencePending(),
                "successful recovery left persistence pending: " + point);
            require(shadow.verify(
                REALM, fixture.withReceipt(), List.of(fixture.receipt())
            ).status() == ShadowVerification.Status.VERIFIED,
                "recovered shadow did not verify: " + point);
        }
    }

    private static void verifiesAtomicDurableFileBackend() throws Exception {
        Path directory = Files.createTempDirectory("ronova-durable-root-");
        Path target = directory.resolve("durable-root-v1.bin");
        DurableRootFileBackend backend = DurableRootFileBackend.at(target);
        try {
            DominionLedger ledger = new DominionLedger(REALM);
            ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
            shadow.attachDurableBackend(backend);
            LocalDominionStore store = new LocalDominionStore(ledger, shadow);
            require(backend.exists() && shadow.durableFilePresent()
                    && !shadow.persistencePending(),
                "7.16 cutover returned before its first Root image was durable");

            DominionCommand.BindOwner command = new DominionCommand.BindOwner(
                UUID.fromString("00000000-0000-0000-0000-000000000404"), OWNER
            );
            shadow.armCommitFailpoint(RootCommitFailpoint.AFTER_APPLY_BEFORE_ACK);
            CommitReceipt ambiguous = store.commit(command);
            require(ambiguous.unknown(),
                "after-apply failpoint did not expose COMMIT_UNKNOWN");

            CompoundTag durableImage = backend.readIfPresent();
            require(durableImage != null,
                "durable backend did not return its committed Root image");
            ShadowDurableRootStore restarted = ShadowDurableRootStore.load(
                durableImage, REALM
            );
            CommitReceipt recovered = restarted.recover(
                DominionCommand.commandId(command),
                DominionCommand.canonicalPayloadHash(command)
            );
            require(recovered != null && recovered.committed()
                    && restarted.ownerRegistry().contains(OWNER),
                "durable file did not recover state+journal after lost acknowledgement");
            require(recovered.equals(restarted.recover(
                DominionCommand.commandId(command)
            )), "durable file did not support pre-discovery ingress replay");
            require(!Files.exists(backend.temporaryPath()),
                "atomic durable replacement left its staging file behind");

            byte[] corrupted = Files.readAllBytes(target);
            corrupted[corrupted.length - 1] ^= 0x5a;
            Files.write(target, corrupted);
            boolean rejected = false;
            try {
                backend.readIfPresent();
            } catch (DurableRootPersistenceException expected) {
                rejected = expected.getMessage().contains("checksum mismatch");
            }
            require(rejected,
                "corrupted durable Root file was accepted or silently replaced");

            Path blockedTarget = directory.resolve("blocked-target");
            Files.createDirectory(blockedTarget);
            ShadowDurableRootStore blocked = new ShadowDurableRootStore(REALM);
            blocked.attachDurableBackend(DurableRootFileBackend.at(blockedTarget));
            boolean cutoverRejected = false;
            try {
                new LocalDominionStore(new DominionLedger(REALM), blocked);
            } catch (DurableRootPersistenceException expected) {
                cutoverRejected = true;
            }
            require(cutoverRejected && !blocked.cutoverComplete(),
                "failed durable replacement still published the Root cutover");
            Files.deleteIfExists(blockedTarget.resolveSibling("blocked-target.next"));
            Files.deleteIfExists(blockedTarget);
        } finally {
            Files.deleteIfExists(backend.temporaryPath());
            Files.deleteIfExists(target);
            Files.deleteIfExists(directory);
        }
    }

    private static void verifiesUnacknowledgedRootIsNotReadable() throws Exception {
        for (DurableWriteFailpoint point : List.of(DurableWriteFailpoint.BEFORE_WRITE,
            DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK)) {
            for (int mutation = 0; mutation < 3; mutation++) {
                Path directory = Files.createTempDirectory("ronova-committed-read-head-");
                DurableRootFileBackend backend = DurableRootFileBackend.at(directory.resolve("root.bin"));
                try {
                    ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
                    shadow.attachDurableBackend(backend);
                    LocalDominionStore store = new LocalDominionStore(new DominionLedger(REALM), shadow);
                    store.commit(new DominionCommand.BindOwner(OWNER));
                    store.commit(JudgmentBatch.of(UUID.randomUUID(), judgment(SUBJECT_ID, store.nextDecisionEpoch())));
                    DominionSnapshot before = store.snapshot();
                    String beforeHash = store.stateHash();
                    long beforeIndex = store.commitIndex();
                    StrengthPolicyConfiguration beforePolicy = store.strengthPolicyConfiguration();
                    DominionCommand command = switch (mutation) {
                        case 0 -> new DominionCommand.ClearTombstone(SUBJECT_ID);
                        case 1 -> new DominionCommand.UnbindOwner(OWNER);
                        default -> new DominionCommand.CommitJudgmentBatch(JudgmentBatch.of(
                            UUID.randomUUID(), judgment(new UUID(0L, 999L), store.nextDecisionEpoch())));
                    };
                    var projections = new dev.ronova.coremod.projection.ProjectionCoordinator();
                    var gateway = new dev.ronova.coremod.root.local.LocalDominionRootGateway(store);
                    var bus = new dev.ronova.coremod.runtime.RootCommandBus(gateway, projections);
                    bus.initialize();
                    long publications = projections.publications();
                    backend.armWriteFailpoint(point);
                    CommitReceipt unknown = store.commit(command);
                    require(unknown.unknown() && store.persistencePending(), "write failure was acknowledged");
                    require(store.snapshot().equals(before) && store.epoch().equals(before.epoch())
                        && store.stateHash().equals(beforeHash) && store.commitIndex() == beforeIndex,
                        "unacknowledged working image leaked through Root reads: " + point + "/" + mutation);
                    require(shadow.ownerRegistry().contains(OWNER) && store.hasTombstone(SUBJECT_ID)
                        && shadow.tombstoneCount() == 1 && shadow.tombstone(SUBJECT_ID) != null
                        && shadow.epochClock().current().equals(before.epoch())
                        && store.strengthPolicyConfiguration().equals(beforePolicy),
                        "pending mutation changed a public component reader");
                    // Mutable compatibility components must not be a second Root writer.
                    shadow.ownerRegistry().clear();
                    shadow.tombstoneStore().clear();
                    require(shadow.ownerRegistry().contains(OWNER) && store.hasTombstone(SUBJECT_ID),
                        "a component reader mutated committed Root state");
                    require(!bus.hasPendingPublication() && !bus.reconcilePendingPublication()
                        && projections.publications() == publications,
                        "background reconciliation published an unresolved Root image");
                    var freshProjections = new dev.ronova.coremod.projection.ProjectionCoordinator();
                    new dev.ronova.coremod.runtime.RootCommandBus(gateway, freshProjections).initialize();
                    require(freshProjections.published().snapshot().equals(before),
                        "runtime initialization published an unresolved image");
                    ShadowDurableRootStore disk = ShadowDurableRootStore.load(backend.readIfPresent(), REALM);
                    require(disk.snapshot().equals(before) == (point == DurableWriteFailpoint.BEFORE_WRITE),
                        "disk failure injection did not bracket replacement");
                    backend.armWriteFailpoint(DurableWriteFailpoint.BEFORE_WRITE);
                    CommitReceipt stillUnknown = store.recover(DominionCommand.commandId(command));
                    require(stillUnknown.unknown() && stillUnknown.snapshot().equals(before)
                        && store.snapshot().equals(before) && !bus.reconcilePendingPublication(),
                        "failed recovery leaked the unacknowledged head");
                    CommitReceipt recovered = store.recover(DominionCommand.commandId(command));
                    require(recovered.committed() && recovered.commitIndex() == beforeIndex + 1
                        && recovered.after().equals(before.epoch().nextSequence())
                        && store.snapshot().equals(recovered.snapshot()),
                        "recovery did not acknowledge the original transaction exactly once");
                    require(bus.reconcilePendingPublication() && !bus.reconcilePendingPublication()
                        && projections.publications() == publications + 1,
                        "resolved image was not published exactly once");
                    ShadowDurableRootStore restarted = ShadowDurableRootStore.load(backend.readIfPresent(), REALM);
                    require(restarted.snapshot().equals(store.snapshot())
                        && restarted.recover(DominionCommand.commandId(command)).after().equals(recovered.after()),
                        "committed read head changed after restart");
                } finally {
                    Files.deleteIfExists(backend.temporaryPath());
                    Files.deleteIfExists(backend.target());
                    Files.deleteIfExists(directory);
                }
            }
        }
    }

    private static void verifiesSubjectQualificationTransactions() throws Exception {
        Path directory = Files.createTempDirectory("ronova-subject-qualifications-");
        DurableRootFileBackend backend = DurableRootFileBackend.at(directory.resolve("root.bin"));
        try {
            ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
            shadow.attachDurableBackend(backend);
            LocalDominionStore store = new LocalDominionStore(new DominionLedger(REALM), shadow);
            store.commit(new DominionCommand.BindOwner(OWNER));
            var a = new dev.ronova.coremod.contract.SubjectKey(REALM, SUBJECT_ID);
            var b = new dev.ronova.coremod.contract.SubjectKey(REALM, new UUID(0L, 17002L));
            DominionEpoch shared = store.nextDecisionEpoch();
            JudgmentBatch batch = new JudgmentBatch(UUID.randomUUID(), List.of(
                judgment(a.subjectId(), shared), judgment(b.subjectId(), shared)));
            CommitReceipt receipt = store.commit(batch);
            require(receipt.qualifications().size() == 2 && receipt.targetEpoch(a).generation() == 1L
                && receipt.targetEpoch(b).generation() == 1L && receipt.commitIndex() == 2L,
                "multi-target qualification did not commit as one transaction");
            var oldGate = dev.ronova.coremod.root.api.CommittedJudgmentGate.from(receipt);
            DominionDecision.Judgment decisionA = batch.judgments().stream()
                .filter(value -> value.subject().uuid().equals(a.subjectId())).findFirst().orElseThrow();
            DominionDecision.Judgment decisionB = batch.judgments().stream()
                .filter(value -> value.subject().uuid().equals(b.subjectId())).findFirst().orElseThrow();
            DominionCommand.ClearTombstone clear = new DominionCommand.ClearTombstone(a.subjectId());
            backend.armWriteFailpoint(DurableWriteFailpoint.AFTER_REPLACE_BEFORE_ACK);
            require(store.commit(clear).unknown() && store.targetEpoch(a).generation() == 1L
                && store.hasTombstone(a.subjectId()), "unacknowledged clear published a new qualification");
            CommitReceipt clearReceipt = store.recover(DominionCommand.commandId(clear));
            require(clearReceipt.committed() && clearReceipt.targetEpoch(a).generation() == 2L
                && store.targetEpoch(b).generation() == 1L && !store.hasTombstone(a.subjectId()),
                "clear lost the old generation or advanced an unrelated subject");
            int[] effects = {0};
            require(!oldGate.run(decisionA, store::targetEpoch, () -> effects[0]++), "clear did not revoke old receipt");
            require(oldGate.run(decisionB, store::targetEpoch, () -> effects[0]++), "unrelated subject was revoked");
            require(effects[0] == 1, "stale qualification executed an effect");
            CommitReceipt historical = store.recover(batch.commandId());
            require(historical.targetEpoch(a).generation() == 1L && historical.targetEpoch(b).generation() == 1L,
                "recovery relabeled an old receipt with current target generations");
            CommitReceipt next = store.commit(JudgmentBatch.of(UUID.randomUUID(), judgment(a.subjectId(), store.nextDecisionEpoch())));
            require(next.targetEpoch(a).generation() == 3L && store.targetEpoch(b).generation() == 1L,
                "re-terminalization reused an old qualification or advanced another subject");
            require(!oldGate.run(decisionA, store::targetEpoch, () -> effects[0]++), "later terminal state renewed an old receipt");
            long priorIndex = store.commitIndex();
            require(store.commit(batch).targetEpoch(a).generation() == 1L && store.commitIndex() == priorIndex,
                "same-command replay advanced subject or global counters");
            DominionSnapshot beforeConflict = store.snapshot();
            JudgmentBatch protectedBatch = new JudgmentBatch(UUID.randomUUID(), List.of(
                judgment(a.subjectId(), store.nextDecisionEpoch()), judgment(OWNER, store.nextDecisionEpoch())));
            boolean rejected = false;
            try { store.commit(protectedBatch); } catch (IllegalArgumentException expected) { rejected = true; }
            require(rejected && store.snapshot().equals(beforeConflict) && store.targetEpoch(a).generation() == 3L,
                "protected member left a partially committed qualification batch");
            store.commit(new DominionCommand.ClearTombstones());
            require(store.targetEpoch(a).generation() == 4L && store.targetEpoch(b).generation() == 2L,
                "clear-all forgot qualification history");
            ShadowDurableRootStore restarted = ShadowDurableRootStore.load(backend.readIfPresent(), REALM);
            require(!restarted.readOnly() && restarted.targetEpoch(a).equals(store.targetEpoch(a))
                && restarted.targetEpoch(b).equals(store.targetEpoch(b))
                && restarted.recover(batch.commandId()).targetEpoch(a).generation() == 1L,
                "qualification state or original receipt changed after restart");
            CompoundTag tampered = backend.readIfPresent();
            tampered.getCompound("subject_qualifications").getList("subjects", Tag.TAG_COMPOUND)
                .getCompound(0).putLong("target_generation", 999L);
            require(ShadowDurableRootStore.load(tampered, REALM).readOnly(), "qualification tampering was accepted");
        } finally {
            Files.deleteIfExists(backend.temporaryPath());
            Files.deleteIfExists(backend.target());
            Files.deleteIfExists(directory);
        }
    }

    private static void verifiesLegacyQualificationMigration() throws Exception {
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
        LocalDominionStore store = new LocalDominionStore(new DominionLedger(REALM), shadow);
        JudgmentBatch batch = JudgmentBatch.of(UUID.randomUUID(), judgment(SUBJECT_ID, store.nextDecisionEpoch()));
        CommitReceipt original = store.commit(batch);
        store.prepareLifecycleIntegration(SUBJECT_ID, "legacy-opaque-claim");
        CompoundTag old = saveData(shadow);
        old.putInt("schema", 3);
        old.putInt("command_journal_format", 2);
        old.remove("subject_qualifications");
        old.remove("effect_journal");
        for (Tag raw : old.getList("command_journal_v1", Tag.TAG_COMPOUND)) ((CompoundTag) raw).remove("qualifications");
        var key = new dev.ronova.coremod.contract.SubjectKey(REALM, SUBJECT_ID);
        ShadowDurableRootStore migrated = ShadowDurableRootStore.load(old, REALM);
        require(!migrated.readOnly() && migrated.persistencePending()
            && migrated.targetEpoch(key).generation() == 0L && migrated.hasTombstone(SUBJECT_ID),
            "legacy UUID/Realm migration changed truth or fabricated a generation");
        CommitReceipt recovered = migrated.recover(batch.commandId());
        require(recovered.committed() && recovered.qualifications().isEmpty()
            && recovered.after().equals(original.after())
            && recovered.canonicalPayloadHash().equals(original.canonicalPayloadHash()),
            "legacy command/hash was rewritten or upgraded to new execution proof");
        require(!migrated.prepareLifecycleIntegration(SUBJECT_ID, "new-object").mayExecute(),
            "migration replayed opaque lifecycle effect");
        CompoundTag current = saveData(migrated);
        require(current.getInt("schema") == 5 && current.getInt("command_journal_format") == 3,
            "migration did not persist its explicit extension versions");
        ShadowDurableRootStore restarted = ShadowDurableRootStore.load(current, REALM);
        require(!restarted.readOnly() && !restarted.persistencePending()
            && restarted.targetEpoch(key).generation() == 0L, "legacy mapping was not stable across reload");
        CommitReceipt clear = restarted.commit(new DominionCommand.ClearTombstone(SUBJECT_ID));
        require(clear.targetEpoch(key).generation() == 1L && !restarted.hasTombstone(SUBJECT_ID),
            "first post-migration transition did not allocate one subject generation");
        CompoundTag wrongRealm = saveData(restarted);
        wrongRealm.put("subject_qualifications", QualificationNbtCodec.encode(List.of(
            new dev.ronova.coremod.root.api.SubjectQualification(new dev.ronova.coremod.root.api.TargetEpoch(
                new dev.ronova.coremod.contract.SubjectKey(new RealmId(new UUID(99L, 99L)), SUBJECT_ID), 1L), false))));
        require(ShadowDurableRootStore.load(wrongRealm, REALM).readOnly(),
            "individually valid foreign-Realm qualification was accepted");
    }

    private static void verifiesLifecycleClaimAmbiguity() throws Exception {
        for (DurableWriteFailpoint point : DurableWriteFailpoint.values()) {
            Path directory = Files.createTempDirectory("ronova-lifecycle-intent-");
            DurableRootFileBackend backend = DurableRootFileBackend.at(directory.resolve("root.bin"));
            try {
                ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
                shadow.attachDurableBackend(backend);
                LocalDominionStore store = new LocalDominionStore(new DominionLedger(REALM), shadow);
                JudgmentBatch batch = JudgmentBatch.of(UUID.randomUUID(),
                    judgment(SUBJECT_ID, store.nextDecisionEpoch()));
                CommitReceipt committed = store.commit(batch);
                require(committed.committed(), "claim fixture did not commit Judgment");
                DominionEpoch epoch = store.epoch();
                long commitIndex = store.commitIndex();
                backend.armWriteFailpoint(point);
                LifecycleClaimResult first = store.prepareLifecycleIntegration(SUBJECT_ID, "first-object");
                require(first.status() == (point == DurableWriteFailpoint.NONE
                    ? LifecycleClaimResult.Status.GRANTED
                    : LifecycleClaimResult.Status.CLAIM_PERSISTENCE_UNKNOWN),
                    "incorrect intent result at " + point);
                require(shadow.hasLifecycleIntegration(SUBJECT_ID),
                    "uncertain intent was forgotten at " + point);
                require(shadow.persistencePending() == (point != DurableWriteFailpoint.NONE),
                    "uncertain intent lost pending persistence at " + point);

                // Inspect the actual replaced file, not save() of the live fixture.
                ShadowDurableRootStore disk = ShadowDurableRootStore.load(backend.readIfPresent(), REALM);
                require(!disk.readOnly(), "intent disk image is invalid at " + point);
                require(disk.hasLifecycleIntegration(SUBJECT_ID)
                    == (point != DurableWriteFailpoint.BEFORE_WRITE),
                    "file failpoint did not bracket the atomic replace at " + point);
                if (disk.hasLifecycleIntegration(SUBJECT_ID)) {
                    require(disk.prepareLifecycleIntegration(SUBJECT_ID, "new-incarnation").status()
                        == LifecycleClaimResult.Status.ALREADY_CLAIMED_EFFECT_UNKNOWN,
                        "restart reissued an opaque effect after intent-only recovery");
                }

                // Unknown persistence must remain unknown while writes still fail.
                if (point != DurableWriteFailpoint.NONE) {
                    backend.armWriteFailpoint(DurableWriteFailpoint.BEFORE_WRITE);
                    require(shadow.recoverLifecycleIntegration(SUBJECT_ID).status()
                        == LifecycleClaimResult.Status.CLAIM_PERSISTENCE_UNKNOWN,
                        "failed intent recovery was acknowledged");
                }
                LifecycleClaimResult resolved = shadow.recoverLifecycleIntegration(SUBJECT_ID);
                require(resolved.status() == LifecycleClaimResult.Status.ALREADY_CLAIMED_EFFECT_UNKNOWN
                    && !resolved.mayExecute() && resolved.receipt().equals(first.receipt()),
                    "intent recovery granted a second effect or changed its identity");
                require(!shadow.persistencePending(), "successful recovery left intent pending");
                require(store.claimLifecycleIntegration(SUBJECT_ID, "different-object") == null,
                    "compatibility entry replayed an already claimed effect");
                require(store.epoch().equals(epoch) && store.commitIndex() == commitIndex,
                    "effect persistence advanced Judgment epoch or commit index");
                require(store.recover(batch.commandId()).after().equals(committed.after()),
                    "effect persistence changed the original command receipt");
                ShadowDurableRootStore restarted = ShadowDurableRootStore.load(backend.readIfPresent(), REALM);
                restarted.attachDurableBackend(backend);
                require(restarted.claimLifecycleIntegration(SUBJECT_ID, "restart-object") == null
                    && !restarted.readOnly(), "opaque effect repeated or hash drifted after restart");
                require(first.mayExecute() == (point == DurableWriteFailpoint.NONE),
                    "an unacknowledged claim authorized the death entry");
                restarted.fenceReadOnly("test fence");
                require(restarted.recoverLifecycleIntegration(SUBJECT_ID).status()
                    == LifecycleClaimResult.Status.REJECTED,
                    "fenced writer acknowledged effect recovery");
            } finally {
                Files.deleteIfExists(backend.temporaryPath());
                Files.deleteIfExists(backend.target());
                Files.deleteIfExists(directory);
            }
        }
    }

    private static void verifiesLifecycleClaimDoesNotBypassPendingJudgment() throws Exception {
        Path directory = Files.createTempDirectory("ronova-pending-judgment-");
        DurableRootFileBackend backend = DurableRootFileBackend.at(directory.resolve("root.bin"));
        try {
            ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
            shadow.attachDurableBackend(backend);
            LocalDominionStore store = new LocalDominionStore(new DominionLedger(REALM), shadow);
            require(store.prepareLifecycleIntegration(SUBJECT_ID, "unjudged").status()
                == LifecycleClaimResult.Status.REJECTED, "unjudged subject obtained an intent");
            backend.armWriteFailpoint(DurableWriteFailpoint.BEFORE_WRITE);
            JudgmentBatch batch = JudgmentBatch.of(UUID.randomUUID(),
                judgment(SUBJECT_ID, store.nextDecisionEpoch()));
            require(store.commit(batch).unknown(), "failed Judgment write was acknowledged");
            backend.armWriteFailpoint(DurableWriteFailpoint.BEFORE_WRITE);
            LifecycleClaimResult blocked = store.prepareLifecycleIntegration(SUBJECT_ID, "blocked");
            require(blocked.status() == LifecycleClaimResult.Status.CLAIM_PERSISTENCE_UNKNOWN
                && blocked.receipt() == null && !shadow.hasLifecycleIntegration(SUBJECT_ID),
                "unresolved Judgment created or authorized an effect intent");
            require(!ShadowDurableRootStore.load(backend.readIfPresent(), REALM).hasTombstone(SUBJECT_ID),
                "before-write failure leaked a durable Judgment");
            require(store.recover(batch.commandId()).committed(), "original Judgment did not recover");
            require(store.prepareLifecycleIntegration(SUBJECT_ID, "resolved").mayExecute(),
                "resolved Judgment could not prepare its first effect");
        } finally {
            Files.deleteIfExists(backend.temporaryPath());
            Files.deleteIfExists(backend.target());
            Files.deleteIfExists(directory);
        }
    }

    private static void verifiesConcurrentLifecycleClaims() throws Exception {
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
        LocalDominionStore store = new LocalDominionStore(new DominionLedger(REALM), shadow);
        store.commit(JudgmentBatch.of(UUID.randomUUID(), judgment(SUBJECT_ID, store.nextDecisionEpoch())));
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            java.util.List<java.util.concurrent.Callable<LifecycleClaimResult>> calls = new java.util.ArrayList<>();
            for (int i = 0; i < 16; i++) {
                final String identity = "concurrent-" + i;
                calls.add(() -> store.prepareLifecycleIntegration(SUBJECT_ID, identity));
            }
            int granted = 0;
            for (java.util.concurrent.Future<LifecycleClaimResult> future : workers.invokeAll(
                calls, 10L, java.util.concurrent.TimeUnit.SECONDS)) {
                if (future.get().mayExecute()) granted++;
            }
            require(granted == 1 && shadow.lifecycleIntegrationReceipts().size() == 1,
                "concurrent incarnations obtained multiple death grants");
        } finally {
            workers.shutdownNow();
        }
    }

    private static void verifies716CutoverAndCommitRecovery() throws Exception {
        DominionLedger ledger = new DominionLedger(REALM);
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
        LocalDominionStore store = new LocalDominionStore(ledger, shadow);
        require(store.authoritative() && shadow.cutoverComplete(),
            "7.15 shadow was not promoted to the 7.16 sole writer");

        UUID ownerCommandId = UUID.fromString(
            "00000000-0000-0000-0000-000000000401"
        );
        DominionCommand.BindOwner bind = new DominionCommand.BindOwner(
            ownerCommandId, OWNER
        );
        shadow.armCommitFailpoint(RootCommitFailpoint.BEFORE_APPLY);
        CommitReceipt unresolvedBefore = store.commit(bind);
        require(unresolvedBefore.unknown()
                && !shadow.ownerRegistry().contains(OWNER)
                && store.recover(
                    ownerCommandId, DominionCommand.canonicalPayloadHash(bind)
                ) == null,
            "before-apply ambiguity leaked a partial Root transition");

        CommitReceipt ownerCommit = store.commit(bind);
        require(ownerCommit.changed() && shadow.ownerRegistry().contains(OWNER)
                && store.commitIndex() == 1L,
            "same-id replay did not commit the original owner command exactly once");

        DominionEpoch batchEpoch = store.nextDecisionEpoch();
        List<DominionDecision.Judgment> judgments = List.of(
            judgment(new UUID(0L, 0x501L), batchEpoch),
            judgment(new UUID(0L, 0x502L), batchEpoch),
            judgment(new UUID(0L, 0x503L), batchEpoch)
        );
        JudgmentBatch batch = new JudgmentBatch(
            UUID.fromString("00000000-0000-0000-0000-000000000402"), judgments
        );
        DominionCommand.CommitJudgmentBatch command =
            new DominionCommand.CommitJudgmentBatch(batch);
        shadow.armCommitFailpoint(RootCommitFailpoint.AFTER_APPLY_BEFORE_ACK);
        CommitReceipt ambiguous = store.commit(command);
        require(ambiguous.unknown()
                && shadow.tombstoneCount() == 3
                && shadow.snapshot().judgments().size() == 3,
            "after-apply ambiguity exposed a partial JudgmentBatch");

        CommitReceipt recovered = store.recover(
            batch.commandId(), batch.canonicalPayloadHash()
        );
        require(recovered != null && recovered.committed()
                && recovered.commitIndex() == 2L
                && recovered.admissionRevision().equals(batchEpoch)
                && recovered.revokeThroughEpoch().equals(ownerCommit.after()),
            "COMMIT_UNKNOWN did not recover the original atomic receipt");
        for (DominionDecision.Judgment judgment : judgments) {
            require(recovered.targetEpoch(new dev.ronova.coremod.contract.SubjectKey(
                REALM, judgment.subject().uuid())).generation() == 1L,
                "first subject generation incorrectly reused the global batch epoch");
        }
        CommitReceipt replay = store.commit(command);
        require(replay.equals(recovered) && store.commitIndex() == 2L
                && shadow.epoch().equals(batchEpoch),
            "same-id batch replay advanced epoch or commit index twice");

        JudgmentBatch emptyBatch = JudgmentBatch.empty(UUID.fromString(
            "00000000-0000-0000-0000-000000000404"
        ));
        CommitReceipt emptyReceipt = store.commit(emptyBatch);
        require(emptyReceipt.committed() && !emptyReceipt.changed()
                && store.commitIndex() == 2L,
            "empty sequenced action did not create a durable no-op receipt");

        ShadowDurableRootStore restarted = ShadowDurableRootStore.load(
            saveData(shadow), REALM
        );
        CommitReceipt recoveredAfterRestart = restarted.recover(
            batch.commandId(), batch.canonicalPayloadHash()
        );
        require(restarted.authoritative() && recovered.equals(recoveredAfterRestart)
                && restarted.tombstoneCount() == 3,
            "durable batch receipt or all-or-nothing state was lost after restart");
        require(recovered.equals(restarted.recover(batch.commandId())),
            "restart replay could not recover a committed action before rediscovery");
        require(emptyReceipt.equals(restarted.recover(emptyBatch.commandId())),
            "restart replay lost the durable no-op action receipt");
    }

    private static void verifiesCompactCommandJournal() throws Exception {
        DominionLedger ledger = new DominionLedger(REALM);
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
        LocalDominionStore store = new LocalDominionStore(ledger, shadow);

        UUID ownerCommandId = UUID.fromString(
            "00000000-0000-0000-0000-000000000601"
        );
        CommitReceipt ownerReceipt = store.commit(new DominionCommand.BindOwner(
            ownerCommandId, OWNER
        ));
        DominionEpoch judgmentEpoch = store.nextDecisionEpoch();
        JudgmentBatch batch = new JudgmentBatch(
            UUID.fromString("00000000-0000-0000-0000-000000000602"),
            "ronova.action.v1.area",
            List.of(judgment(new UUID(0L, 0x603L), judgmentEpoch))
        );
        CommitReceipt judgmentReceipt = store.commit(batch);
        boolean changedActionRejected = false;
        try {
            store.commit(new JudgmentBatch(
                batch.commandId(), "ronova.action.v1.primary", batch.judgments()
            ));
        } catch (CommandIdConflictException expected) {
            changedActionRejected = true;
        }
        require(changedActionRejected,
            "same ingress sequence accepted a second action domain");
        CommitReceipt inMemoryOld = store.recover(
            ownerCommandId, DominionCommand.canonicalPayloadHash(
                new DominionCommand.BindOwner(ownerCommandId, OWNER)
            )
        );
        require(inMemoryOld != null && !inMemoryOld.exactSnapshot()
                && inMemoryOld.snapshot().equals(store.snapshot()),
            "in-memory journal retained a full historical Root snapshot");
        long journalSnapshotFields = 0L;
        for (Field field : RootCommandJournal.class.getDeclaredFields()) {
            journalSnapshotFields += field.getType() == DominionSnapshot.class ? 1L : 0L;
        }
        require(journalSnapshotFields == 1L,
            "command journal must hold exactly one current Root snapshot");
        for (Class<?> nested : RootCommandJournal.class.getDeclaredClasses()) {
            if (!nested.getSimpleName().equals("Entry")) {
                continue;
            }
            for (Field field : nested.getDeclaredFields()) {
                require(field.getType() != DominionSnapshot.class
                        && field.getType() != CommitReceipt.class,
                    "command journal entry retained a historical snapshot receipt");
            }
        }

        CompoundTag compact = saveData(shadow);
        require(compact.getInt("command_journal_format") == 3,
            "compact command journal format was not persisted");
        ListTag compactRows = compact.getList(
            "command_journal_v1", Tag.TAG_COMPOUND
        );
        require(compactRows.size() == 2,
            "compact command journal lost a committed row");
        for (Tag raw : compactRows) {
            CompoundTag row = (CompoundTag) raw;
            require(!row.contains("snapshot_payload"),
                "command journal row duplicated the full Root snapshot");
            if (row.getCompound("command").getString("kind")
                .equals("commit_judgment_batch")) {
                require(row.getCompound("command").getString("command_domain")
                        .equals("ronova.action.v1.area"),
                    "compact journal lost the immutable action domain");
            }
        }

        ShadowDurableRootStore restarted = ShadowDurableRootStore.load(
            compact, REALM
        );
        CommitReceipt oldRecovered = restarted.recover(ownerCommandId);
        require(oldRecovered != null && oldRecovered.committed()
                && oldRecovered.after().equals(ownerReceipt.after())
                && !oldRecovered.exactSnapshot()
                && oldRecovered.snapshot().equals(restarted.snapshot()),
            "compact journal did not rehydrate an older receipt against current truth");
        CommitReceipt latestRecovered = restarted.recover(batch.commandId());
        require(latestRecovered != null && latestRecovered.committed()
                && latestRecovered.after().equals(judgmentReceipt.after())
                && latestRecovered.command().equals(judgmentReceipt.command())
                && latestRecovered.exactSnapshot(),
            "compact journal changed the latest exact receipt or action domain");

        DominionLedger legacyLedger = new DominionLedger(REALM);
        ShadowDurableRootStore legacyShadow = new ShadowDurableRootStore(REALM);
        LocalDominionStore legacyStore = new LocalDominionStore(
            legacyLedger, legacyShadow
        );
        UUID legacyCommandId = UUID.fromString(
            "00000000-0000-0000-0000-000000000604"
        );
        CommitReceipt legacyReceipt = legacyStore.commit(
            new DominionCommand.BindOwner(legacyCommandId, OWNER)
        );
        CompoundTag legacy = saveData(legacyShadow);
        legacy.remove("command_journal_format");
        ListTag legacyRows = legacy.getList(
            "command_journal_v1", Tag.TAG_COMPOUND
        );
        require(legacyRows.size() == 1,
            "legacy journal fixture has an unexpected row count");
        ((CompoundTag) legacyRows.get(0)).putByteArray(
            "snapshot_payload", legacy.getByteArray("snapshot_payload")
        );
        ((CompoundTag) legacyRows.get(0)).remove("qualifications");
        ShadowDurableRootStore migrated = ShadowDurableRootStore.load(
            legacy, REALM
        );
        CommitReceipt migratedReceipt = migrated.recover(legacyCommandId);
        require(legacyReceipt.equals(migratedReceipt) && !migrated.readOnly(),
            "format-1 command journal did not migrate without losing its receipt");
        CompoundTag migratedImage = saveData(migrated);
        ListTag migratedRows = migratedImage.getList(
            "command_journal_v1", Tag.TAG_COMPOUND
        );
        require(migratedImage.getInt("command_journal_format") == 3
                && !((CompoundTag) migratedRows.get(0)).contains("snapshot_payload"),
            "legacy journal was not rewritten into the compact format");
    }

    private static void verifiesStrengthPolicyPersistence() throws Exception {
        DominionLedger ledger = new DominionLedger(REALM);
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
        LocalDominionStore store = new LocalDominionStore(ledger, shadow);
        StrengthPolicyConfiguration requested = store.strengthPolicyConfiguration().next(
            StrengthProfile.EXTREME,
            Map.of(
                StrengthOptionKey.INTERCEPT_MIXIN, MechanismLevel.STRICT_ALL,
                StrengthOptionKey.PROTECT_LIFE, MechanismLevel.STRICT_ALL
            )
        );
        UUID commandId = UUID.fromString(
            "00000000-0000-0000-0000-000000000403"
        );
        DominionCommand.ConfigureStrengthPolicy command =
            new DominionCommand.ConfigureStrengthPolicy(commandId, requested);
        CommitReceipt committed = store.commit(command);
        require(committed.changed()
                && store.strengthPolicyConfiguration().equals(requested)
                && store.strengthPolicySnapshot().activeProfile()
                    == StrengthProfile.COMPATIBLE
                && store.strengthPolicySnapshot().pendingRestart(),
            "Root did not persist the requested image behind a restart boundary");

        CompoundTag persisted = saveData(shadow);
        ShadowDurableRootStore restarted = ShadowDurableRootStore.load(persisted, REALM);
        require(restarted.strengthPolicyConfiguration().equals(requested)
                && restarted.strengthPolicySnapshot().activeProfile()
                    == StrengthProfile.EXTREME
                && restarted.strengthPolicySnapshot().activeMatrixRevision()
                    == ProfilePresetMatrix.CURRENT_REVISION
                && !restarted.readOnly(),
            "strength policy did not survive a clean restart");
        CommitReceipt replay = restarted.commit(command);
        require(replay.equals(committed)
                && restarted.strengthPolicyConfiguration().revision().value() == 2L,
            "strength configuration replay was not idempotent");

        CompoundTag corrupted = persisted.copy();
        corrupted.putString("strength_policy_hash", "00");
        ShadowDurableRootStore fenced = ShadowDurableRootStore.load(corrupted, REALM);
        require(fenced.readOnly()
                && fenced.readOnlyReason().contains("strength policy hash mismatch"),
            "tampered strength policy was accepted as Root truth");

        CompoundTag incomplete = persisted.copy();
        incomplete.getCompound("strength_policy")
            .getList("options", Tag.TAG_COMPOUND).remove(0);
        ShadowDurableRootStore incompleteStore = ShadowDurableRootStore.load(
            incomplete, REALM
        );
        require(incompleteStore.readOnly()
                && incompleteStore.readOnlyReason().contains("exactly 54"),
            "incomplete profile matrix payload was accepted");

        CompoundTag unknownRevision = persisted.copy();
        unknownRevision.getCompound("strength_policy")
            .putInt("profile_matrix_revision", 2);
        ShadowDurableRootStore unknownRevisionStore = ShadowDurableRootStore.load(
            unknownRevision, REALM
        );
        require(unknownRevisionStore.readOnly()
                && unknownRevisionStore.readOnlyReason()
                    .contains("unsupported profile preset matrix revision"),
            "unknown profile matrix revision was accepted");
    }

    private static void verifiesLegacyStrengthPolicyMatrixMigration() throws Exception {
        DominionLedger ledger = new DominionLedger(REALM);
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
        LocalDominionStore store = new LocalDominionStore(ledger, shadow);
        StrengthPolicyConfiguration requested = store.strengthPolicyConfiguration().next(
            StrengthProfile.ENHANCED,
            Map.of(
                StrengthOptionKey.ATTACK_DYNAMIC_CODE, MechanismLevel.MONITOR,
                StrengthOptionKey.PROTECT_LOOP_GUARD, MechanismLevel.OFF
            )
        );
        UUID commandId = UUID.fromString(
            "00000000-0000-0000-0000-000000000417"
        );
        DominionCommand.ConfigureStrengthPolicy command =
            new DominionCommand.ConfigureStrengthPolicy(commandId, requested);
        store.commit(command);

        CompoundTag legacyImage = saveData(shadow);
        CompoundTag legacyPolicy = legacyImage.getCompound("strength_policy");
        legacyPolicy.putInt("schema", 1);
        legacyPolicy.remove("profile_matrix_revision");
        legacyPolicy.remove("attack_actions");
        legacyPolicy.remove("action_matrix_revision");
        legacyImage.putString(
            "strength_policy_hash",
            ShadowDurableRootStore.policyHashForSchema(requested, 1)
        );

        ListTag journalRows = legacyImage.getList(
            "command_journal_v1", Tag.TAG_COMPOUND
        );
        require(journalRows.size() == 1,
            "legacy matrix fixture has an unexpected journal size");
        CompoundTag journalRow = (CompoundTag) journalRows.get(0);
        CompoundTag commandTag = journalRow.getCompound("command");
        require("configure_strength_policy".equals(commandTag.getString("kind")),
            "legacy matrix fixture did not persist its configure command");
        CompoundTag commandPolicy = commandTag.getCompound("configuration");
        commandPolicy.putInt("schema", 1);
        commandPolicy.remove("profile_matrix_revision");
        commandPolicy.remove("attack_actions");
        commandPolicy.remove("action_matrix_revision");
        journalRow.putString(
            "payload_hash", DominionCommand.canonicalPayloadHash(command, 1)
        );

        ShadowDurableRootStore migrated = ShadowDurableRootStore.load(
            legacyImage, REALM
        );
        require(!migrated.readOnly()
                && migrated.persistencePending()
                && migrated.strengthPolicyConfiguration().profileMatrixRevision()
                    == ProfilePresetMatrix.CURRENT_REVISION
                && migrated.strengthPolicySnapshot().activeMatrixRevision()
                    == ProfilePresetMatrix.CURRENT_REVISION,
            "schema-1 strength policy did not migrate to the current matrix");
        CommitReceipt recovered = migrated.recover(commandId);
        require(recovered != null
                && recovered.status() == CommitReceipt.Status.COMMITTED,
            "legacy strength command receipt was lost during matrix migration");

        CompoundTag migratedImage = saveData(migrated);
        CompoundTag migratedPolicy = migratedImage.getCompound("strength_policy");
        require(migratedPolicy.getInt("schema") == 3
                && migratedPolicy.getInt("profile_matrix_revision")
                    == ProfilePresetMatrix.CURRENT_REVISION,
            "migrated strength policy was not rewritten as schema 3");
        ListTag migratedRows = migratedImage.getList(
            "command_journal_v1", Tag.TAG_COMPOUND
        );
        CompoundTag migratedRow = (CompoundTag) migratedRows.get(0);
        require(migratedRow.getString("payload_hash").equals(
                DominionCommand.canonicalPayloadHash(command, 1)
            ) && migratedRow.getCompound("command")
                .getCompound("configuration").getInt("schema") == 1,
            "legacy strength command tag/hash was not preserved");
        ShadowDurableRootStore reloaded = ShadowDurableRootStore.load(
            migratedImage, REALM
        );
        require(!reloaded.readOnly()
                && reloaded.strengthPolicyConfiguration().equals(requested),
            "schema-3 strength policy failed after migration rewrite");
    }

    private static void verifiesOrdinaryActionRestartGate() throws Exception {
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
        LocalDominionStore store = new LocalDominionStore(new DominionLedger(REALM), shadow);
        StrengthPolicyConfiguration disabled = store.strengthPolicyConfiguration().next(
            StrengthProfile.CUSTOM, Map.of(),
            Map.of(AttackAction.TERMINALIZE, AttackActionLevel.DISABLED));
        require(store.commit(new DominionCommand.ConfigureStrengthPolicy(disabled)).committed(),
            "ordinary-action configuration did not commit");
        JudgmentBatch existing = new JudgmentBatch(UUID.randomUUID(),
            List.of(judgment(SUBJECT_ID, store.nextDecisionEpoch())));
        require(store.commit(existing).committed(),
            "pending action policy changed the active image before restart");
        ShadowDurableRootStore restarted = ShadowDurableRootStore.load(saveData(shadow), REALM);
        require(!restarted.readOnly() && !restarted.activeStrengthPolicyConfiguration()
            .permitsBaselineAction(AttackAction.TERMINALIZE), "saved OFF did not activate on reload");
        DominionSnapshot before = restarted.snapshot();
        JudgmentBatch fresh = new JudgmentBatch(UUID.randomUUID(), List.of(judgment(
            UUID.randomUUID(), restarted.epoch().nextSequence())));
        CommitReceipt rejected = restarted.commit(new DominionCommand.CommitJudgmentBatch(fresh));
        require(rejected.status() == CommitReceipt.Status.REJECTED
            && restarted.snapshot().equals(before), "disabled terminal action mutated Root");
        require(restarted.commit(new DominionCommand.CommitJudgmentBatch(existing)).committed(),
            "turning OFF invalidated an already committed receipt");
        require(restarted.snapshot().equals(before), "replay altered existing terminal obligations");
        CompoundTag corrupted = saveData(restarted);
        for (Tag raw : corrupted.getCompound("strength_policy").getList("attack_actions", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) raw;
            if (row.getString("key").equals(AttackAction.TERMINALIZE.canonical()))
                row.putString("level", AttackActionLevel.EXTREME.name());
        }
        require(ShadowDurableRootStore.load(corrupted, REALM).readOnly(),
            "live policy action tamper bypassed the stored hash");
    }

    private static void verifiesDeterministicCommandParity() {
        DominionLedger ledger = new DominionLedger(REALM);
        ShadowDurableRootStore shadow = new ShadowDurableRootStore(REALM);
        long random = 0x6a09e667f3bcc909L;
        for (int index = 0; index < 10_000; index++) {
            random = mix(random + index + 0x9e3779b97f4a7c15L);
            int operation = (int) (random & 7L);
            UUID owner = new UUID(0L, 0x100L + (random & 7L));
            switch (operation) {
                case 0, 1 -> {
                    if (operation == 0) {
                        ledger.bindOwner(owner);
                    } else {
                        ledger.unbindOwner(owner);
                    }
                }
                case 2, 3 -> {
                    UUID subjectId = new UUID(1L, 0x200L + (random & 31L));
                    SubjectIdentity subject = subject(
                        subjectId, "minecraft:zombie", "test.StreamZombie"
                    );
                    DominionDecision.Judgment decision = new DominionDecision.Judgment(
                        subject, ledger.nextDecisionEpoch(), true,
                        DominionDecision.LineageScope.GLOBAL_LINEAGE, 0.0D, "stream"
                    );
                    ledger.record(subject, decision);
                    if (operation == 3) {
                        ledger.claimLifecycleIntegration(subjectId, "stream-" + index);
                    }
                }
                case 4 -> ledger.clearTombstones();
                default -> { }
            }
            DominionSnapshot snapshot = ledger.snapshot();
            List<LifecycleIntegrationReceipt> receipts = ledger.lifecycleIntegrationReceipts();
            ShadowVerification mirrored = shadow.mirror(REALM, snapshot, receipts);
            require(mirrored.accepted(),
                "deterministic command parity rejected operation " + index + ": "
                    + mirrored.status());
            require(shadow.stateHash().equals(
                ShadowDurableRootStore.stateHash(REALM, snapshot, receipts)
            ), "shadow hash diverged at deterministic operation " + index);
        }
        require(shadow.verify(
            REALM, ledger.snapshot(), ledger.lifecycleIntegrationReceipts()
        ).status() == ShadowVerification.Status.VERIFIED,
            "10,000-command shadow parity did not finish verified");
    }

    private static void verifiesSingleWriterWiring() throws Exception {
        ClassNode terminal = read(TERMINAL);
        ClassNode runtime = read(RUNTIME);
        ClassNode ledger = read(LEDGER);
        ClassNode shadow = read(SHADOW);
        ClassNode localStore = read(LOCAL_STORE);

        require(countCalls(terminal, RUNTIME, "integrateLifecycleOnce") == 1,
            "TerminalLifecycle does not use exactly one runtime claim boundary");
        require(countCalls(terminal, LOCAL_STORE, "claimLifecycleIntegration") == 0,
            "TerminalLifecycle bypasses the runtime claim boundary");
        require(countCalls(terminal, SHADOW, null) == 0,
            "TerminalLifecycle depends directly on the 7.15 shadow");
        require(countCalls(runtime, LOCAL_STORE, "claimLifecycleIntegration") == 1,
            "DominionServerRuntime does not delegate the claim to LocalDominionStore");
        require(countCalls(shadow, RECEIPT, "<init>") >= 1 && countCalls(runtime, RECEIPT, "<init>") == 0,
            "7.16 Root does not own lifecycle receipt creation");
        require(countCalls(runtime, LOCAL_STORE, "prepareLifecycleEffect") == 1
            && countCalls(runtime, LOCAL_STORE, "enterEffect") == 1
            && countCalls(runtime, LOCAL_STORE, "finishEffectUnknown") >= 1,
            "runtime bypasses durable effect preparation, entry or unknown-outcome recording");
        require(countCalls(localStore, RECEIPT, "<init>") == 0,
            "LocalDominionStore became a lifecycle receipt writer");
        require(countCalls(localStore, SHADOW, "commit") == 1,
            "LocalDominionStore does not have exactly one Root mutation delegation");
        require(countCalls(ledger, SHADOW, "commit") == 0,
            "legacy rollback mirror can still mutate the 7.16 Root");
    }

    private static Fixture fixture() {
        DominionLedger ledger = new DominionLedger(REALM);
        ledger.bindOwner(OWNER);
        SubjectIdentity subject = subject(SUBJECT_ID, "minecraft:zombie", "test.Zombie");
        DominionDecision.Judgment decision = new DominionDecision.Judgment(
            subject, ledger.nextDecisionEpoch(), true,
            DominionDecision.LineageScope.GLOBAL_LINEAGE, 0.0D, "fixture"
        );
        ledger.record(subject, decision);
        DominionSnapshot beforeReceipt = ledger.snapshot();
        LifecycleIntegrationReceipt receipt = ledger.claimLifecycleIntegration(
            SUBJECT_ID, "fixture-physical-object"
        );
        require(receipt != null, "fixture lifecycle receipt was not created");
        require(receipt.integratedEpoch().equals(beforeReceipt.epoch()),
            "fixture receipt epoch does not match authoritative epoch");
        return new Fixture(
            ledger, subject, beforeReceipt, receipt, ledger.snapshot()
        );
    }

    private static SubjectIdentity subject(UUID uuid, String type, String className) {
        return new SubjectIdentity(
            uuid, type, className, "minecraft:overworld",
            1.0D, 64.0D, -2.0D, false
        );
    }

    private static DominionDecision.Judgment judgment(
        UUID uuid, DominionEpoch epoch
    ) {
        SubjectIdentity target = subject(uuid, "minecraft:zombie", "test.BatchZombie");
        return new DominionDecision.Judgment(
            target, epoch, true, DominionDecision.LineageScope.GLOBAL_LINEAGE,
            0.0D, "7.16-atomic-batch"
        );
    }

    private static long mix(long value) {
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static CompoundTag saveData(Object savedData) throws Exception {
        for (Method method : savedData.getClass().getDeclaredMethods()) {
            if (method.getParameterCount() != 1
                || method.getParameterTypes()[0] != CompoundTag.class
                || method.getReturnType() != CompoundTag.class) {
                continue;
            }
            method.trySetAccessible();
            try {
                return (CompoundTag) method.invoke(savedData, new CompoundTag());
            } catch (InvocationTargetException wrapped) {
                Throwable cause = wrapped.getCause();
                if (cause instanceof Exception exception) {
                    throw exception;
                }
                if (cause instanceof Error error) {
                    throw error;
                }
                throw new AssertionError("unexpected SavedData failure", cause);
            }
        }
        throw new AssertionError("missing CompoundTag SavedData override on "
            + savedData.getClass().getName());
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = DurableRootStoreSelfTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            require(input != null, "missing production class: " + internalName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static int countCalls(ClassNode node, String owner, String name) {
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && owner.equals(call.owner)
                    && (name == null || name.equals(call.name))) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void requireRealmRejected(Runnable operation, String scenario) {
        boolean rejected = false;
        try {
            operation.run();
        } catch (RealmIdLoadException expected) {
            rejected = true;
            require(expected.getMessage().contains("realm_id load rejected"),
                scenario + " did not expose an explicit realm rejection");
        }
        require(rejected, scenario + " was silently accepted or replaced");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Fixture(
        DominionLedger ledger,
        SubjectIdentity subject,
        DominionSnapshot beforeReceipt,
        LifecycleIntegrationReceipt receipt,
        DominionSnapshot withReceipt
    ) {}
}
