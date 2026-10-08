package dev.ronova.coremod.runtime;

import dev.ronova.coremod.baseline.BaselineTelemetry;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.projection.ProjectionBundle;
import dev.ronova.coremod.projection.ProjectionCoordinator;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.root.api.DominionRootGateway;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** Serial Root commit lane: mutate once, then atomically publish the returned snapshot. */
public final class RootCommandBus {
    private final DominionRootGateway root;
    private final ProjectionCoordinator projections;
    private final AtomicLong blockedPublications = new AtomicLong();

    public RootCommandBus(DominionRootGateway root, ProjectionCoordinator projections) {
        this.root = Objects.requireNonNull(root, "root");
        this.projections = Objects.requireNonNull(projections, "projections");
    }

    public synchronized ProjectionBundle initialize() {
        return projections.publish(root.snapshot());
    }

    public synchronized CommitReceipt commit(DominionCommand command) {
        long baselineStarted = BaselineTelemetry.rootCommitStarted();
        DominionCommand requested = Objects.requireNonNull(command, "command");
        CommitReceipt receipt = root.commit(requested);
        if (receipt.unknown()) {
            CommitReceipt recovered = root.recover(
                DominionCommand.commandId(requested),
                DominionCommand.canonicalPayloadHash(requested)
            );
            if (recovered != null) {
                receipt = recovered;
            }
        }
        BaselineTelemetry.recordRootCommit(baselineStarted, receipt.changed());
        if (receipt.unknown()) {
            return receipt;
        }
        if (receipt.rejected()) {
            return receipt;
        }
        ProjectionBundle current = projections.published();
        int snapshotComparison = receipt.snapshot().epoch().compareTo(
            current.snapshot().epoch()
        );
        if (snapshotComparison < 0) {
            return receipt;
        }
        if (snapshotComparison == 0) {
            if (!current.snapshot().equals(receipt.snapshot())) {
                blockedPublications.incrementAndGet();
                throw new IllegalStateException(
                    "Root receipt conflicts with the current projection at "
                        + receipt.snapshot().epoch()
                );
            }
            return receipt;
        }
        ProjectionBundle published = projections.publish(receipt.snapshot());
        if (!published.snapshot().epoch().equals(receipt.snapshot().epoch())) {
            blockedPublications.incrementAndGet();
            throw new IllegalStateException(
                "Root commit was not published: committed=" + receipt.after()
                    + ", snapshot=" + receipt.snapshot().epoch()
                    + ", published=" + published.snapshot().epoch()
            );
        }
        return receipt;
    }

    /** Commits one action-level batch and publishes at most one projection epoch. */
    public synchronized CommitReceipt commit(dev.ronova.coremod.root.api.JudgmentBatch batch) {
        return commit(new DominionCommand.CommitJudgmentBatch(
            Objects.requireNonNull(batch, "batch")
        ));
    }

    public synchronized dev.ronova.coremod.root.api.JudgmentBatch qualify(
        dev.ronova.coremod.root.api.JudgmentBatch batch) {
        return root.qualify(Objects.requireNonNull(batch, "batch"));
    }

    /**
     * Replays a durable ingress receipt before target discovery.  A recovered
     * snapshot is published only when this runtime has not already published a
     * same-or-newer Root epoch.
     */
    public synchronized CommitReceipt recover(UUID commandId) {
        CommitReceipt receipt = root.recover(Objects.requireNonNull(commandId, "commandId"));
        if (receipt == null || receipt.unknown() || receipt.rejected()) {
            return receipt;
        }
        ProjectionBundle current = projections.published();
        int comparison = receipt.snapshot().epoch().compareTo(
            current.snapshot().epoch()
        );
        if (comparison < 0) {
            return receipt;
        }
        if (comparison == 0) {
            if (!current.snapshot().equals(receipt.snapshot())) {
                blockedPublications.incrementAndGet();
                throw new IllegalStateException(
                    "Recovered Root receipt conflicts with projection at "
                        + receipt.snapshot().epoch()
                );
            }
            return receipt;
        }
        ProjectionBundle published = projections.publish(receipt.snapshot());
        if (!published.snapshot().epoch().equals(receipt.snapshot().epoch())) {
            blockedPublications.incrementAndGet();
            throw new IllegalStateException(
                "Recovered Root receipt was not published: committed=" + receipt.after()
                    + ", snapshot=" + receipt.snapshot().epoch()
                    + ", published=" + published.snapshot().epoch()
            );
        }
        return receipt;
    }

    /** Retries a durable Root snapshot that could not be published during a prior commit. */
    public synchronized boolean reconcilePendingPublication() {
        DominionSnapshot rootSnapshot = root.snapshot();
        if (rootSnapshot.epoch().compareTo(projections.published().snapshot().epoch()) <= 0) {
            return false;
        }
        ProjectionBundle published = projections.publish(rootSnapshot);
        return published.snapshot().epoch().equals(rootSnapshot.epoch());
    }

    public boolean hasPendingPublication() {
        return root.epoch().compareTo(projections.published().snapshot().epoch()) > 0;
    }

    public long blockedPublications() { return blockedPublications.get(); }

    public DominionSnapshot snapshot() { return root.snapshot(); }

    public dev.ronova.coremod.root.api.TargetEpoch targetEpoch(
        dev.ronova.coremod.contract.SubjectKey key) { return root.targetEpoch(key); }

    public DominionEpoch epoch() { return root.epoch(); }
    public dev.ronova.coremod.root.api.SubjectQualification subjectQualification(
        dev.ronova.coremod.contract.SubjectKey key) { return root.subjectQualification(key); }
    public synchronized dev.ronova.coremod.root.api.SubjectTransitionBatch prepareTransitions(UUID commandId, UUID actor,
        java.util.List<dev.ronova.coremod.root.api.SubjectTransitionBatch.Transition> transitions) {
        return root.prepareTransitions(commandId, actor, transitions);
    }

    public DominionEpoch nextDecisionEpoch() { return root.nextDecisionEpoch(); }
}
