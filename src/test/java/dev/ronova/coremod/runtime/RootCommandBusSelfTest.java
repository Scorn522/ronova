package dev.ronova.coremod.runtime;

import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.projection.ProjectionCoordinator;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import dev.ronova.coremod.root.api.DominionRootGateway;
import java.util.List;
import java.util.UUID;

/** Proves that a Root commit and projection publication are one explicit runtime lane. */
public final class RootCommandBusSelfTest {
    private RootCommandBusSelfTest() {}

    public static void main(String[] args) {
        FakeRoot root = new FakeRoot();
        ProjectionCoordinator projections = new ProjectionCoordinator();
        RootCommandBus bus = new RootCommandBus(root, projections);
        bus.initialize();
        require(projections.published().snapshot().epoch().equals(new DominionEpoch(1L, 0L)),
            "initial Root snapshot was not published");

        UUID owner = UUID.randomUUID();
        CommitReceipt receipt = bus.commit(new DominionCommand.BindOwner(owner));
        require(receipt.changed(), "bind did not advance Root epoch");
        require(projections.published().authorityTopology().owns(owner),
            "commit receipt was not projected");
        require(projections.published().snapshot().epoch().equals(receipt.after()),
            "published epoch differs from commit receipt");
        require(!bus.hasPendingPublication(), "successful commit left a pending publication");
        require(bus.blockedPublications() == 0L, "successful commit counted a failure");

        long publications = projections.publications();
        CommitReceipt noOp = bus.commit(new DominionCommand.BindOwner(owner));
        require(!noOp.changed(), "duplicate bind advanced the Root epoch");
        require(projections.publications() == publications,
            "no-op command republished an unchanged snapshot");
        require(projections.staleRejections() == 0L,
            "no-op command produced a stale projection rejection");

        verifiesCommitUnknownRecovery();

        System.out.println(
            "RootCommandBusSelfTest: PASS "
                + "(commit, atomic publish, no-op, COMMIT_UNKNOWN + ingress replay)"
        );
    }

    private static void verifiesCommitUnknownRecovery() {
        UUID owner = UUID.fromString("00000000-0000-0000-0000-000000000716");
        DominionCommand.BindOwner command = new DominionCommand.BindOwner(
            UUID.fromString("00000000-0000-0000-0000-000000000717"), owner
        );

        AmbiguousRoot afterApply = new AmbiguousRoot(true);
        ProjectionCoordinator recoveredProjection = new ProjectionCoordinator();
        RootCommandBus recoveredBus = new RootCommandBus(afterApply, recoveredProjection);
        recoveredBus.initialize();
        CommitReceipt recovered = recoveredBus.commit(command);
        require(recovered.committed() && recovered.changed()
                && afterApply.commitCalls == 1 && afterApply.recoverCalls == 1,
            "after-apply ambiguity did not resolve through the original command id");
        require(recoveredProjection.published().authorityTopology().owns(owner),
            "recovered durable receipt was not published");
        long publications = recoveredProjection.publications();
        CommitReceipt ingressReplay = recoveredBus.recover(
            DominionCommand.commandId(command)
        );
        require(recovered.equals(ingressReplay)
                && afterApply.identityRecoverCalls == 1
                && recoveredProjection.publications() == publications,
            "stable ingress replay did not return the durable receipt exactly once");

        AmbiguousRoot beforeApply = new AmbiguousRoot(false);
        ProjectionCoordinator unresolvedProjection = new ProjectionCoordinator();
        RootCommandBus unresolvedBus = new RootCommandBus(beforeApply, unresolvedProjection);
        unresolvedBus.initialize();
        CommitReceipt unresolved = unresolvedBus.commit(command);
        require(unresolved.unknown() && beforeApply.recoverCalls == 1,
            "before-apply ambiguity was falsely resolved");
        require(!unresolvedProjection.published().authorityTopology().owns(owner)
                && unresolvedProjection.publications() == 1L,
            "unresolved command published an irreversible projection");
    }

    private static final class FakeRoot implements DominionRootGateway {
        private DominionSnapshot snapshot = new DominionSnapshot(
            new DominionEpoch(1L, 0L), List.of()
        );
        private UUID owner;

        @Override public DominionSnapshot snapshot() { return snapshot; }
        @Override public DominionEpoch epoch() { return snapshot.epoch(); }
        @Override public DominionEpoch nextDecisionEpoch() { return epoch().nextSequence(); }

        @Override public CommitReceipt commit(DominionCommand command) {
            DominionEpoch before = epoch();
            if (!(command instanceof DominionCommand.BindOwner bind)) {
                throw new IllegalArgumentException("unexpected test command");
            }
            if (bind.owner().equals(owner)) {
                return new CommitReceipt(command, before, before, snapshot);
            }
            owner = bind.owner();
            DominionEpoch after = before.nextSequence();
            SubjectIdentity subject = new SubjectIdentity(
                bind.owner(), "minecraft:player", "test.Player", "test:level",
                0.0D, 0.0D, 0.0D, true
            );
            snapshot = new DominionSnapshot(after, List.of(new DominionDecision.Authority(
                subject, after, true, CapabilitySet.OWNER_DEFAULT, "test-owner"
            )));
            return new CommitReceipt(command, before, after, snapshot);
        }
    }

    private static final class AmbiguousRoot implements DominionRootGateway {
        private DominionSnapshot snapshot = new DominionSnapshot(
            new DominionEpoch(1L, 0L), List.of()
        );
        private final boolean applyBeforeAck;
        private CommitReceipt durableReceipt;
        private int commitCalls;
        private int recoverCalls;
        private int identityRecoverCalls;

        private AmbiguousRoot(boolean applyBeforeAck) {
            this.applyBeforeAck = applyBeforeAck;
        }

        @Override public DominionSnapshot snapshot() { return snapshot; }
        @Override public DominionEpoch epoch() { return snapshot.epoch(); }
        @Override public DominionEpoch nextDecisionEpoch() { return epoch().nextSequence(); }

        @Override
        public CommitReceipt commit(DominionCommand command) {
            commitCalls++;
            DominionEpoch before = epoch();
            DominionSnapshot beforeSnapshot = snapshot;
            if (applyBeforeAck) {
                DominionCommand.BindOwner bind = (DominionCommand.BindOwner) command;
                DominionEpoch after = before.nextSequence();
                SubjectIdentity subject = new SubjectIdentity(
                    bind.owner(), "minecraft:player", "test.Player", "test:level",
                    0.0D, 0.0D, 0.0D, true
                );
                snapshot = new DominionSnapshot(after, List.of(
                    new DominionDecision.Authority(
                        subject, after, true, CapabilitySet.OWNER_DEFAULT,
                        "ambiguous-test-owner"
                    )
                ));
                durableReceipt = new CommitReceipt(
                    command, before, after, snapshot,
                    DominionCommand.commandId(command),
                    DominionCommand.canonicalPayloadHash(command),
                    1L, CommitReceipt.Status.COMMITTED
                );
            }
            return CommitReceipt.unknown(command, before, beforeSnapshot, 0L);
        }

        @Override
        public CommitReceipt recover(UUID commandId, String canonicalPayloadHash) {
            recoverCalls++;
            if (durableReceipt == null) {
                return null;
            }
            require(durableReceipt.commandId().equals(commandId)
                    && durableReceipt.canonicalPayloadHash()
                        .equals(canonicalPayloadHash),
                "recovery did not use the original command identity");
            return durableReceipt;
        }

        @Override
        public CommitReceipt recover(UUID commandId) {
            identityRecoverCalls++;
            if (durableReceipt == null) {
                return null;
            }
            require(durableReceipt.commandId().equals(commandId),
                "ingress recovery changed the durable command identity");
            return durableReceipt;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
