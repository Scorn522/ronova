package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.root.api.DurableRootStore;
import dev.ronova.coremod.root.api.JudgmentBatch;
import dev.ronova.coremod.root.api.EffectClaim;
import dev.ronova.coremod.root.api.StorageCapacity;
import dev.ronova.coremod.policy.StrengthPolicyConfiguration;
import dev.ronova.coremod.policy.StrengthPolicySnapshot;
import dev.ronova.coremod.terminal.LifecycleIntegrationReceipt;
import dev.ronova.coremod.terminal.LifecycleClaimResult;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.nio.file.Path;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

/** 7.16 Java DurableRootStore writer with an r51 downgrade-only SavedData mirror. */
public final class LocalDominionStore implements DurableRootStore {
    private final DominionLedger legacyMirror;
    private final ShadowDurableRootStore shadow;
    private ShadowVerification lastShadowVerification;
    private long legacyMirrorFailures;
    private boolean mirrorPersistenceEnabled = true;
    private String legacyMirrorStatus;

    LocalDominionStore(DominionLedger ledger, ShadowDurableRootStore shadow) {
        this.shadow = Objects.requireNonNull(shadow, "shadow");
        if (shadow.authoritative()) {
            this.lastShadowVerification = shadow.promoteFromLegacy(null, null, List.of());
            this.legacyMirror = ledger != null && shadow.realmId().equals(ledger.realmId())
                ? ledger : new DominionLedger(shadow.realmId());
            this.legacyMirrorStatus = LegacyMirrorFacts.equivalent(shadow, ledger) ? "EQUIVALENT" : "REBUILT_FROM_ROOT";
        } else {
            this.legacyMirror = Objects.requireNonNull(ledger, "legacy migration source");
            if (ledger.isDurableMirror()) throw new DurableRootPersistenceException("authoritative Root is missing; Ledger is only a mirror");
            this.lastShadowVerification = shadow.promoteFromLegacy(
                ledger.realmId(), ledger.snapshot(), ledger.lifecycleIntegrationReceipts());
            this.legacyMirrorStatus = "INITIAL_CUTOVER";
        }
        synchronizeLegacyMirror();
    }

    public static LocalDominionStore open(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        LocalDominionStore store = openWorld(server.getWorldPath(LevelResource.ROOT));
        ShadowDurableRootStore.install(server, store.shadow, store.mirrorPersistenceEnabled);
        DominionLedger.install(server, store.legacyMirror, store.mirrorPersistenceEnabled);
        if (!"EQUIVALENT".equals(store.legacyMirrorStatus)) {
            dev.ronova.coremod.RonovaDominion.LOGGER.warn("Ronova SavedData mirror status={} (canonical Root retained)", store.legacyMirrorStatus);
        }
        return store;
    }

    /** Shared production/test bootstrap. Read canonical authority before any optional mirror. */
    static LocalDominionStore openWorld(Path world) {
        Path ledgerFile = SavedDataMirrorFiles.path(world, SavedDataMirrorFiles.LEDGER);
        Path shadowFile = SavedDataMirrorFiles.path(world, SavedDataMirrorFiles.SHADOW);
        DurableRootFileBackend backend = DurableRootFileBackend.at(world.resolve("ronova/durable-root-v1.bin"));
        CompoundTag canonical = backend.readIfPresent();
        ShadowDurableRootStore root;
        DominionLedger ledger;
        boolean invalidMirror = false;
        if (canonical != null) {
            root = ShadowDurableRootStore.load(canonical);
            if (!root.authoritative() || root.readOnly()) {
                throw new DurableRootPersistenceException("canonical Root validation failed: " + root.readOnlyReason());
            }
            root.attachDurableBackend(backend);
            try {
                CompoundTag mirror = SavedDataMirrorFiles.read(ledgerFile);
                ledger = mirror == null ? null : DominionLedger.load(mirror);
            } catch (IOException | RuntimeException damaged) {
                ledger = null;
                invalidMirror = true;
            }
        } else {
            try {
                CompoundTag shadowData = SavedDataMirrorFiles.read(shadowFile);
                CompoundTag ledgerData = SavedDataMirrorFiles.read(ledgerFile);
                rejectMissingRootMarker(shadowData, "authoritative_7_16");
                rejectMissingRootMarker(ledgerData, SavedDataMirrorFiles.MIRROR_ONLY);
                ledger = ledgerData == null ? new DominionLedger() : DominionLedger.load(ledgerData);
                root = shadowData == null ? new ShadowDurableRootStore(ledger.realmId())
                    : ShadowDurableRootStore.load(shadowData, ledger.realmId());
                root.attachDurableBackend(backend);
            } catch (IOException unreadable) {
                throw new DurableRootPersistenceException("cannot validate the legacy migration source", unreadable);
            }
        }
        boolean preserveSucceeded = true;
        try {
            SavedDataMirrorFiles.preserve(world, ledgerFile);
            SavedDataMirrorFiles.preserve(world, shadowFile);
        } catch (IOException | RuntimeException unavailable) { preserveSucceeded = false; }
        LocalDominionStore result = new LocalDominionStore(ledger, root);
        result.mirrorPersistenceEnabled = preserveSucceeded;
        if (!preserveSucceeded) {
            result.legacyMirrorFailures++;
            result.legacyMirrorStatus = "PRESERVATION_BLOCKED_MIRRORS_NOT_WRITTEN";
        } else if (invalidMirror) result.legacyMirrorStatus = "REBUILT_INVALID_MIRROR";
        return result;
    }

    private static void rejectMissingRootMarker(CompoundTag data, String name) {
        if (data == null || !data.contains(name)) return;
        if (!data.contains(name, Tag.TAG_BYTE) || data.getByte(name) != 0) {
            throw new DurableRootPersistenceException("durable Root file is missing after cutover (" + name + ")");
        }
    }

    DominionLedger legacyMirror() { return legacyMirror; }
    boolean mirrorPersistenceEnabled() { return mirrorPersistenceEnabled; }
    public synchronized String legacyMirrorStatus() { return legacyMirrorStatus; }

    @Override
    public synchronized RealmId realmId() {
        return shadow.realmId();
    }
    public synchronized dev.ronova.coremod.root.api.AuthorityStamp authorityStamp(UUID subject) {
        return shadow.authorityStamp(subject);
    }
    public synchronized dev.ronova.coremod.root.api.TerminalOperationPlan approveTerminalPlan(UUID subject, UUID nonce) {
        return shadow.approveTerminalPlan(subject, nonce);
    }
    public synchronized boolean validateTerminalPlan(dev.ronova.coremod.root.api.TerminalOperationPlan plan) { return shadow.validateTerminalPlan(plan); }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireOwnerWrite(dev.ronova.coremod.root.api.AuthorityStamp expected) {
        return shadow.acquireOwnerWrite(expected);
    }
    public synchronized dev.ronova.coremod.root.api.RecoverySourcePlan qualifyRecoverySource(
        dev.ronova.coremod.root.api.TerminalOperationPlan plan, dev.ronova.coremod.root.api.RecoverySourceEvidence evidence) {
        return shadow.qualifyRecoverySource(plan, evidence);
    }
    public synchronized boolean recoverySourceHeld(dev.ronova.coremod.root.api.RecoverySourcePlan plan, int depth, UUID proposedSubject) {
        return shadow.recoverySourceHeld(plan, depth, proposedSubject);
    }
    public synchronized dev.ronova.coremod.root.api.RecoverySnapshotPlan approveStaticSnapshotPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, long sourceVersion, boolean bindingVerified) {
        return shadow.approveStaticSnapshotPlan(terminal, evidence, sourceVersion, bindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RecoveryRecordPlan approveStaticRecordPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, long sourceVersion, boolean directBindingVerified) {
        return shadow.approveStaticRecordPlan(terminal, evidence, sourceVersion, directBindingVerified);
    }
    public synchronized boolean prepareRecoveryMutation(dev.ronova.coremod.root.api.RecoveryMutationIntent intent,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean bindingVerified) {
        return shadow.prepareRecoveryMutation(intent, evidence, bindingVerified);
    }
    public synchronized boolean prepareRecoveryMutation(dev.ronova.coremod.root.api.RecoveryMutationIntent intent,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean bindingVerified,
        dev.ronova.coremod.root.api.RecoverySourceLocation location) {
        return shadow.prepareRecoveryMutation(intent, evidence, bindingVerified, location);
    }
    public synchronized dev.ronova.coremod.root.api.RecoverySourceLocation recoveryMutationLocation(
        dev.ronova.coremod.root.api.EffectKey key) { return shadow.recoveryMutationLocation(key); }
    public synchronized dev.ronova.coremod.root.api.RecoveryRecordPlan approveLoadedRecordPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, long sourceVersion, boolean directBindingVerified) {
        return shadow.approveLoadedRecordPlan(terminal, evidence, sourceVersion, directBindingVerified);
    }
    public synchronized boolean recoveryMutationPrepared(dev.ronova.coremod.root.api.RecoveryMutationIntent intent) {
        return shadow.recoveryMutationPrepared(intent);
    }
    public synchronized dev.ronova.coremod.root.api.RecoveryTaskPlan approveRecoveryTaskPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,
        dev.ronova.coremod.root.api.RecoveryWorkDescriptor description,boolean bindingVerified) {
        return shadow.approveRecoveryTaskPlan(terminal,evidence,description,bindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RecoveryStoragePlan approveRecoveryStoragePlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,
        dev.ronova.coremod.root.api.RecoveryWorkDescriptor description,boolean bindingVerified) {
        return shadow.approveRecoveryStoragePlan(terminal,evidence,description,bindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RecoveryStoragePlan approveStorageAfterMemory(
        dev.ronova.coremod.root.api.RecoveryMutationIntent memory,dev.ronova.coremod.root.api.RecoveryWorkDescriptor description,boolean bindingVerified) {
        return shadow.approveStorageAfterMemory(memory,description,bindingVerified);
    }
    public synchronized boolean prepareStorageAfterMemory(dev.ronova.coremod.root.api.RecoveryStoragePlan plan,
        dev.ronova.coremod.root.api.RecoveryMutationIntent memory,boolean bindingVerified) {
        return shadow.prepareStorageAfterMemory(plan,memory,bindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireStorageAfterMemory(
        dev.ronova.coremod.root.api.RecoveryStoragePlan plan,dev.ronova.coremod.root.api.RecoveryMutationIntent memory,boolean bindingVerified) {
        return shadow.acquireStorageAfterMemory(plan,memory,bindingVerified);
    }
    public synchronized boolean prepareRecoveryTask(dev.ronova.coremod.root.api.RecoveryTaskPlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,boolean bindingVerified) {return shadow.prepareRecoveryTask(plan,evidence,bindingVerified);}
    public synchronized boolean recoveryTaskContinuationCurrent(dev.ronova.coremod.root.api.RecoveryTaskPlan plan,boolean bindingVerified) {
        return shadow.recoveryTaskContinuationCurrent(plan,bindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireTaskContinuation(dev.ronova.coremod.root.api.RecoveryTaskPlan plan,boolean bindingVerified) {
        return shadow.acquireTaskContinuation(plan,bindingVerified);
    }
    public synchronized void retireTaskContinuation(dev.ronova.coremod.root.api.RecoveryTaskPlan plan){shadow.retireTaskContinuation(plan);}
    public synchronized void releaseTaskContinuationGuard(dev.ronova.coremod.root.api.EffectKey key){shadow.releaseTaskContinuationGuard(key);}
    public synchronized boolean prepareRecoveryStorage(dev.ronova.coremod.root.api.RecoveryStoragePlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,boolean bindingVerified) {return shadow.prepareRecoveryStorage(plan,evidence,bindingVerified);}
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireRecoveryTaskWrite(
        dev.ronova.coremod.root.api.RecoveryTaskPlan plan,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,boolean bindingVerified) {
        return shadow.acquireRecoveryTaskWrite(plan,evidence,bindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireRecoveryStorageWrite(
        dev.ronova.coremod.root.api.RecoveryStoragePlan plan,dev.ronova.coremod.root.api.RecoverySourceEvidence evidence,boolean bindingVerified) {
        return shadow.acquireRecoveryStorageWrite(plan,evidence,bindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RecoveryWorkResult recoveryWorkResult(dev.ronova.coremod.root.api.EffectKey key){return shadow.recoveryWorkResult(key);}
    public synchronized dev.ronova.coremod.root.api.EffectRecord recoveryMutationResult(dev.ronova.coremod.root.api.RecoveryMutationIntent intent){return shadow.recoveryMutationResult(intent);}
    public synchronized dev.ronova.coremod.root.api.EffectClaim claimRecoveryStorage(dev.ronova.coremod.root.api.RecoveryStoragePlan plan){return shadow.claimRecoveryStorage(plan);}
    public synchronized boolean recoveryStorageClaimCurrent(dev.ronova.coremod.root.api.EffectClaim claim,dev.ronova.coremod.root.api.RecoveryStoragePlan plan){return shadow.recoveryStorageClaimCurrent(claim,plan);}
    public synchronized boolean enterRecoveryStorageClaim(dev.ronova.coremod.root.api.EffectClaim claim,dev.ronova.coremod.root.api.RecoveryStoragePlan plan,dev.ronova.coremod.root.api.RootExecutionFence.Lease lease){return shadow.enterRecoveryStorageClaim(claim,plan,lease);}
    public synchronized void releaseRecoveryStorageClaim(dev.ronova.coremod.root.api.EffectClaim claim){shadow.releaseRecoveryStorageClaim(claim);}
    public synchronized boolean observeRecoveryWork(dev.ronova.coremod.root.api.EffectKey key,dev.ronova.coremod.root.api.RecoveryWorkResult.Fact fact){return shadow.observeRecoveryWork(key,fact);}
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireRecoveryMutationWrite(
        dev.ronova.coremod.root.api.RecoveryMutationIntent intent,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean bindingVerified) {
        return shadow.acquireRecoveryMutationWrite(intent, evidence, bindingVerified);
    }
    public synchronized boolean observeRecoveryMutation(dev.ronova.coremod.root.api.RecoveryMutationIntent intent,
        dev.ronova.coremod.root.api.RecoveryMutationIntent.Observation observation) {
        return shadow.observeRecoveryMutation(intent, observation);
    }
    public synchronized boolean observeRecoveryMutations(
        java.util.List<dev.ronova.coremod.root.api.RecoveryMutationIntent.Receipt> receipts) {
        return shadow.observeRecoveryMutations(receipts);
    }
    public synchronized dev.ronova.coremod.root.api.RecoverySnapshotPlan approveLoadedSnapshotPlan(
        dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, long sourceVersion, boolean loadedBindingVerified) {
        return shadow.approveLoadedSnapshotPlan(terminal, evidence, sourceVersion, loadedBindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireLoadedSnapshotWrite(
        dev.ronova.coremod.root.api.RecoverySnapshotPlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean loadedBindingVerified) {
        return shadow.acquireLoadedSnapshotWrite(plan, evidence, loadedBindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireStaticRecordWrite(
        dev.ronova.coremod.root.api.RecoveryRecordPlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean directBindingVerified) {
        return shadow.acquireStaticRecordWrite(plan, evidence, directBindingVerified);
    }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireStaticSnapshotWrite(
        dev.ronova.coremod.root.api.RecoverySnapshotPlan plan,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, boolean bindingVerified) {
        return shadow.acquireStaticSnapshotWrite(plan, evidence, bindingVerified);
    }
    public synchronized boolean recoverySourceHeld(dev.ronova.coremod.root.api.TerminalOperationPlan terminal,
        dev.ronova.coremod.root.api.RecoverySourceEvidence evidence, UUID proposedSubject) {
        return shadow.recoverySourceHeld(terminal, evidence, proposedSubject);
    }
    public synchronized boolean acceptOperationReport(dev.ronova.coremod.root.api.OperationReport report) { return shadow.acceptOperationReport(report); }
    public synchronized boolean terminalEffectResolved(dev.ronova.coremod.root.api.TerminalOperationPlan plan) { return shadow.terminalEffectResolved(plan); }
    public synchronized dev.ronova.coremod.root.api.EffectClaim prepareTerminalEffect(
        dev.ronova.coremod.root.api.TerminalOperationPlan plan, String identity) {
        var claim = shadow.prepareTerminalEffect(plan, identity);
        if (claim.granted()) synchronizeLegacyMirror();
        return claim;
    }

    @Override
    public synchronized DominionSnapshot snapshot() {
        return shadow.snapshot();
    }

    @Override
    public synchronized dev.ronova.coremod.root.api.TargetEpoch targetEpoch(
        dev.ronova.coremod.contract.SubjectKey key) { return shadow.targetEpoch(key); }

    @Override public synchronized JudgmentBatch qualify(JudgmentBatch batch) {
        return shadow.qualify(batch);
    }
    @Override public synchronized dev.ronova.coremod.root.api.SubjectQualification subjectQualification(
        dev.ronova.coremod.contract.SubjectKey key) { return shadow.subjectQualification(key); }

    public synchronized dev.ronova.coremod.root.api.SubjectTransitionBatch prepareTransitions(UUID commandId, UUID actor,
        java.util.List<dev.ronova.coremod.root.api.SubjectTransitionBatch.Transition> transitions) {
        return shadow.prepareTransitions(commandId, actor, transitions);
    }

    @Override
    public synchronized DominionEpoch epoch() {
        return shadow.epoch();
    }

    public synchronized DominionEpoch nextDecisionEpoch() {
        return shadow.epoch().nextSequence();
    }

    @Override
    public synchronized String stateHash() {
        return shadow.stateHash();
    }

    @Override
    public synchronized String baseStateHash() {
        return shadow.baseStateHash();
    }

    @Override
    public synchronized List<LifecycleIntegrationReceipt> lifecycleIntegrationReceipts() {
        return shadow.lifecycleIntegrationReceipts();
    }

    @Override
    public synchronized StrengthPolicyConfiguration strengthPolicyConfiguration() {
        return shadow.strengthPolicyConfiguration();
    }

    public synchronized StrengthPolicySnapshot strengthPolicySnapshot() {
        return shadow.strengthPolicySnapshot();
    }

    @Override
    public synchronized ShadowVerification mirror(
        RealmId sourceRealm,
        DominionSnapshot sourceSnapshot,
        Collection<LifecycleIntegrationReceipt> receipts
    ) {
        lastShadowVerification = shadow.verify(sourceRealm, sourceSnapshot, receipts);
        return lastShadowVerification;
    }

    @Override
    public synchronized ShadowVerification verify(
        RealmId sourceRealm,
        DominionSnapshot sourceSnapshot,
        Collection<LifecycleIntegrationReceipt> receipts
    ) {
        lastShadowVerification = shadow.verify(sourceRealm, sourceSnapshot, receipts);
        return lastShadowVerification;
    }

    /** Reports the shadow fence without making the authoritative ledger read-only. */
    @Override
    public synchronized boolean readOnly() {
        return shadow.readOnly();
    }

    @Override
    public synchronized long mirrorCount() {
        return shadow.mirrorCount();
    }

    @Override
    public synchronized long verificationFailures() {
        return shadow.verificationFailures();
    }

    @Override
    public synchronized long commitIndex() {
        return shadow.commitIndex();
    }

    @Override
    public synchronized CommitReceipt recover(UUID commandId, String canonicalPayloadHash) {
        CommitReceipt recovered = shadow.recover(commandId, canonicalPayloadHash);
        if (recovered != null && recovered.committed()) {
            synchronizeLegacyMirror();
        }
        return recovered;
    }

    @Override
    public synchronized CommitReceipt recover(UUID commandId) {
        CommitReceipt recovered = shadow.recover(commandId);
        if (recovered != null && recovered.committed()) {
            synchronizeLegacyMirror();
        }
        return recovered;
    }

    @Override
    public synchronized boolean authoritative() {
        return shadow.authoritative();
    }

    public synchronized ShadowDurableRootStore shadow() {
        return shadow;
    }

    public synchronized boolean persistencePending() {
        return shadow.persistencePending();
    }
    public synchronized boolean retryPendingPersistence() {
        boolean pending=shadow.persistencePending();
        boolean acknowledged=shadow.retryPendingPersistence();
        if(pending&&acknowledged)synchronizeLegacyMirror();
        return acknowledged;
    }

    public synchronized String persistenceFailure() {
        return shadow.persistenceFailure();
    }

    public synchronized boolean durableFilePresent() {
        return shadow.durableFilePresent();
    }

    public synchronized ShadowVerification lastShadowVerification() {
        return lastShadowVerification;
    }

    /** Compatibility owner mutation; the command bus is preferred by runtime callers. */
    public synchronized void bindOwner(UUID uuid) {
        commit(new DominionCommand.BindOwner(Objects.requireNonNull(uuid, "uuid")));
    }

    /** Compatibility owner mutation; the command bus is preferred by runtime callers. */
    public synchronized void unbindOwner(UUID uuid) {
        commit(new DominionCommand.UnbindOwner(Objects.requireNonNull(uuid, "uuid")));
    }

    public synchronized DominionLedger.Tombstone record(
        Entity entity, DominionDecision.Judgment decision
    ) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(decision, "decision");
        CommitReceipt receipt = commit(new DominionCommand.CommitJudgment(
            decision.subject(), decision
        ));
        if (!receipt.committed()) {
            throw new IllegalStateException("judgment command was not committed");
        }
        return toLegacyTombstone(shadow.tombstone(decision.subject().uuid()));
    }

    public synchronized void clearTombstones() {
        commit(new DominionCommand.ClearTombstones());
    }

    public synchronized boolean clearTombstone(UUID uuid) {
        return commit(new DominionCommand.ClearTombstone(
            Objects.requireNonNull(uuid, "uuid")
        )).changed();
    }

    public synchronized CommitReceipt configureStrengthPolicy(
        StrengthPolicyConfiguration configuration
    ) {
        return commit(new DominionCommand.ConfigureStrengthPolicy(
            Objects.requireNonNull(configuration, "configuration")
        ));
    }

    public synchronized boolean hasTombstone(UUID uuid) {
        return shadow.hasTombstone(uuid);
    }

    public synchronized boolean hasLifecycleIntegration(UUID judgmentId) {
        return shadow.hasLifecycleIntegration(judgmentId);
    }

    public synchronized int lifecycleClaimCount() {
        return shadow.lifecycleClaimCount();
    }

    public synchronized LifecycleIntegrationReceipt lifecycleIntegrationReceipt(UUID judgmentId) {
        return shadow.lifecycleIntegrationReceipt(judgmentId);
    }

    /** Compatibility adapter for the existing runtime death entry. */
    public synchronized LifecycleIntegrationReceipt claimLifecycleIntegration(
        UUID judgmentId, String firstPhysicalIdentity
    ) {
        LifecycleClaimResult result = prepareLifecycleIntegration(judgmentId, firstPhysicalIdentity);
        return result.mayExecute() ? result.receipt() : null;
    }

    /** The Root owns claim creation and ambiguous persistence recovery. */
    public synchronized LifecycleClaimResult prepareLifecycleIntegration(
        UUID judgmentId, String firstPhysicalIdentity
    ) {
        LifecycleClaimResult result = shadow.prepareLifecycleIntegration(
            judgmentId, firstPhysicalIdentity
        );
        if (result.status() == LifecycleClaimResult.Status.GRANTED
            || result.status() == LifecycleClaimResult.Status.ALREADY_CLAIMED_EFFECT_UNKNOWN) {
            synchronizeLegacyMirror();
        }
        return result;
    }

    public synchronized EffectClaim prepareLifecycleEffect(UUID subjectId, String physicalIdentity) {
        EffectClaim claim = shadow.prepareLifecycleEffect(subjectId, physicalIdentity);
        if (claim.granted()) synchronizeLegacyMirror();
        return claim;
    }

    public synchronized boolean enterEffect(EffectClaim claim) { return shadow.enterEffect(claim); }
    public synchronized dev.ronova.coremod.root.api.RootExecutionFence.Lease acquireTerminalWrite(
        dev.ronova.coremod.root.api.TerminalOperationPlan plan) { return shadow.acquireTerminalWrite(plan); }
    public synchronized boolean registerWriteSetReceipt(dev.ronova.coremod.root.api.TerminalOperationPlan plan,
        dev.ronova.coremod.root.api.RootExecutionFence.Lease lease,dev.ronova.coremod.root.api.WriteSetReceipt receipt) {
        return shadow.registerWriteSetReceipt(plan,lease,receipt);
    }
    public synchronized boolean finishEffectUnknown(EffectClaim claim) { return shadow.finishEffectUnknown(claim); }
    public synchronized int effectRecordCount() { return shadow.effectRecordCount(); }
    public synchronized java.util.List<dev.ronova.coremod.root.api.EffectRecord> effectPage(int offset,int limit) { return shadow.effectPage(offset,limit); }
    public synchronized dev.ronova.coremod.obligation.ObligationCheckpoint obligationCheckpoint() { return shadow.obligationCheckpoint(); }
    public synchronized boolean candidateAdmissionHeld(UUID subject, UUID nonce) { return shadow.candidateAdmissionHeld(subject, nonce); }
    public synchronized dev.ronova.coremod.root.api.ClientParticipationContract approveClientParticipation(
        dev.ronova.coremod.root.api.TerminalOperationPlan plan,java.util.List<dev.ronova.coremod.root.api.ClientParticipationContract.Endpoint> endpoints) {
        return shadow.approveClientParticipation(plan,endpoints);
    }
    public synchronized boolean conflictingPhysicalBinding(UUID subject, UUID nonce) { return shadow.conflictingPhysicalBinding(subject, nonce); }
    public synchronized dev.ronova.coremod.root.api.ClientParticipationContract refreshClientParticipation(
        dev.ronova.coremod.root.api.ClientParticipationContract previous,dev.ronova.coremod.root.api.TerminalOperationPlan plan) {
        return shadow.refreshClientParticipation(previous,plan);
    }
    public synchronized dev.ronova.coremod.root.api.CheckpointReceipt checkpointObligations(long expected,
        dev.ronova.coremod.obligation.ObligationCheckpoint requested) { return shadow.checkpointObligations(expected,requested); }
    public synchronized long effectUnresolvedCount() { return shadow.effectUnresolvedCount(); }
    public synchronized long effectReservedGrowthBytes() { return shadow.effectReservedGrowthBytes(); }
    public synchronized long storageRejections() { return shadow.storageRejections(); }
    public synchronized StorageCapacity storageCapacity() { return shadow.storageCapacity(); }

    /** The only authoritative mutation entry used by the composed runtime. */
    @Override
    public synchronized CommitReceipt commit(DominionCommand command) {
        Objects.requireNonNull(command, "command");
        CommitReceipt receipt = shadow.commit(command);
        if (receipt.committed()) {
            synchronizeLegacyMirror();
        }
        return receipt;
    }

    @Override
    public synchronized CommitReceipt commit(JudgmentBatch batch) {
        Objects.requireNonNull(batch, "batch");
        return commit(new DominionCommand.CommitJudgmentBatch(batch));
    }

    private synchronized void synchronizeLegacyMirror() {
        try {
            legacyMirror.mirrorFromDurableRoot(
                shadow.realmId(), shadow.snapshot(), shadow.lifecycleIntegrationReceipts(),
                shadow.commandJournalSnapshot()
            );
        } catch (RuntimeException failure) {
            // The rollback mirror is diagnostic only; it may never fence or undo Root truth.
            legacyMirrorFailures++;
        }
    }

    public synchronized long legacyMirrorFailures() {
        return legacyMirrorFailures;
    }

    private static DominionLedger.Tombstone toLegacyTombstone(
        TombstoneStore.Record record
    ) {
        return record == null ? null : new DominionLedger.Tombstone(
            record.uuid(), record.type(), record.className(), record.dimension(),
            record.x(), record.y(), record.z(), record.radius(), record.scope(),
            record.sequence()
        );
    }

    public static void release(MinecraftServer server) {
        DominionLedger.release(server);
        ShadowDurableRootStore.release(server);
    }
}
