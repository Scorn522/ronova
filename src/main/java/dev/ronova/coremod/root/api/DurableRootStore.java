package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.contract.SubjectKey;
import dev.ronova.coremod.persistence.ShadowVerification;
import dev.ronova.coremod.policy.StrengthPolicyConfiguration;
import dev.ronova.coremod.terminal.LifecycleIntegrationReceipt;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Durable Root port. Since 7.16, authoritative mutations enter through this
 * port; the mirror methods remain for the reversible 7.15 migration window.
 */
public interface DurableRootStore {
    RealmId realmId();

    DominionEpoch epoch();

    DominionSnapshot snapshot();

    /** The acknowledged per-subject qualification, or null when no binding is known. */
    default TargetEpoch targetEpoch(SubjectKey key) { return null; }
    default SubjectQualification subjectQualification(SubjectKey key) { return null; }

    /** Captures one acknowledged qualification view without committing or changing the request epoch. */
    default JudgmentBatch qualify(JudgmentBatch batch) {
        throw new UnsupportedOperationException("qualification capture is unavailable");
    }

    String stateHash();

    String baseStateHash();

    List<LifecycleIntegrationReceipt> lifecycleIntegrationReceipts();

    default StrengthPolicyConfiguration strengthPolicyConfiguration() {
        return StrengthPolicyConfiguration.defaults();
    }

    /** Authoritative single-command commit. */
    default CommitReceipt commit(DominionCommand command) {
        throw new UnsupportedOperationException("store is not an authoritative writer");
    }

    /** Authoritative action-level batch commit. */
    default CommitReceipt commit(JudgmentBatch batch) {
        return commit(new DominionCommand.CommitJudgmentBatch(batch));
    }

    /** Returns the monotonic durable commit index, when supported. */
    default long commitIndex() {
        return 0L;
    }

    /** Recovers a batch receipt by its stable id and canonical payload hash. */
    default CommitReceipt recover(UUID commandId, String canonicalPayloadHash) {
        return null;
    }

    /** Recovers an already-resolved ingress command before rediscovering targets. */
    default CommitReceipt recover(UUID commandId) {
        return null;
    }

    default CommitReceipt recover(JudgmentBatch batch) {
        Objects.requireNonNull(batch, "batch");
        return recover(batch.commandId(), batch.canonicalPayloadHash());
    }

    default boolean authoritative() {
        return false;
    }

    ShadowVerification mirror(
        RealmId sourceRealm,
        DominionSnapshot snapshot,
        Collection<LifecycleIntegrationReceipt> receipts
    );

    ShadowVerification verify(
        RealmId sourceRealm,
        DominionSnapshot snapshot,
        Collection<LifecycleIntegrationReceipt> receipts
    );

    default ShadowVerification mirror(
        DominionSnapshot snapshot,
        Collection<LifecycleIntegrationReceipt> receipts
    ) {
        return mirror(realmId(), snapshot, receipts);
    }

    default ShadowVerification verify(
        DominionSnapshot snapshot,
        Collection<LifecycleIntegrationReceipt> receipts
    ) {
        return verify(realmId(), snapshot, receipts);
    }

    boolean readOnly();

    long mirrorCount();

    long verificationFailures();
}
