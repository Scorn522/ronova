package dev.ronova.coremod.root.local;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.persistence.LocalDominionStore;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.root.api.DominionRootGateway;
import java.util.Objects;
import java.util.UUID;

/** In-process gateway to the sole durable Root writer. */
public final class LocalDominionRootGateway implements DominionRootGateway {
    private final LocalDominionStore store;

    public LocalDominionRootGateway(LocalDominionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override public DominionSnapshot snapshot() { return store.snapshot(); }

    @Override public dev.ronova.coremod.root.api.JudgmentBatch qualify(
        dev.ronova.coremod.root.api.JudgmentBatch batch) { return store.qualify(batch); }

    @Override public dev.ronova.coremod.root.api.TargetEpoch targetEpoch(
        dev.ronova.coremod.contract.SubjectKey key) { return store.targetEpoch(key); }

    @Override public DominionEpoch epoch() { return store.epoch(); }
    @Override public dev.ronova.coremod.root.api.SubjectQualification subjectQualification(
        dev.ronova.coremod.contract.SubjectKey key) { return store.subjectQualification(key); }
    @Override public dev.ronova.coremod.root.api.SubjectTransitionBatch prepareTransitions(UUID commandId, UUID actor,
        java.util.List<dev.ronova.coremod.root.api.SubjectTransitionBatch.Transition> transitions) {
        return store.prepareTransitions(commandId, actor, transitions);
    }

    @Override public DominionEpoch nextDecisionEpoch() { return store.nextDecisionEpoch(); }

    @Override public CommitReceipt commit(DominionCommand command) {
        return store.commit(command);
    }

    @Override public CommitReceipt recover(UUID commandId, String canonicalPayloadHash) {
        return store.recover(commandId, canonicalPayloadHash);
    }

    @Override public CommitReceipt recover(UUID commandId) {
        return store.recover(commandId);
    }
}
