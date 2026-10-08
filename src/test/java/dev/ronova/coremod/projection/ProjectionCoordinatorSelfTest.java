package dev.ronova.coremod.projection;

import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.util.List;
import java.util.UUID;

/** Golden checks for atomic, monotonic R2 projection publication. */
public final class ProjectionCoordinatorSelfTest {
    private ProjectionCoordinatorSelfTest() {}

    public static void main(String[] args) {
        UUID ownerUuid = UUID.randomUUID();
        SubjectIdentity owner = subject(
            ownerUuid, "minecraft:player", "test.Player", true
        );
        SubjectIdentity zombie = subject(
            UUID.randomUUID(), "minecraft:zombie", "test.Zombie", false
        );
        DominionEpoch firstEpoch = new DominionEpoch(1L, 4L);
        DominionDecision.Authority authority = new DominionDecision.Authority(
            owner, firstEpoch, true, CapabilitySet.OWNER_DEFAULT, "test-owner"
        );
        DominionDecision.Judgment judgment = new DominionDecision.Judgment(
            zombie, firstEpoch, true, DominionDecision.LineageScope.GLOBAL_LINEAGE,
            0.0D, "test-judgment"
        );

        ProjectionCoordinator coordinator = new ProjectionCoordinator();
        ProjectionBundle first = coordinator.publish(
            new DominionSnapshot(firstEpoch, List.of(authority, judgment))
        );
        require(first.authorityTopology().owns(ownerUuid), "owner projection missing");
        require(first.capabilities().allows(ownerUuid, Capability.FLIGHT),
            "capability projection missing flight");
        require(first.admission().matches(zombie), "registry lineage projection missing");
        require(!first.admission().matches(subject(
            UUID.randomUUID(), "minecraft:pig", "test.Zombie", false
        )), "an unverified serialized class name authorized lineage rejection");

        first.admission().recordObjectRejection();
        first.admission().recordNbtRejection();
        require(first.admission().rejectedAdmissions() == 2L,
            "admission counters must cover object and NBT boundaries");

        DominionEpoch secondEpoch = firstEpoch.nextSequence();
        DominionDecision.Authority secondAuthority = new DominionDecision.Authority(
            owner, secondEpoch, true, CapabilitySet.OWNER_DEFAULT, "test-owner"
        );
        ProjectionBundle second = coordinator.publish(
            new DominionSnapshot(secondEpoch, List.of(secondAuthority))
        );
        require(second.snapshot().epoch().equals(secondEpoch), "new snapshot not published");
        require(second.admission().tombstoneCount() == 0, "cleared lineage survived rebuild");
        require(second.admission().rejectedAdmissions() == 2L,
            "server-lifetime metrics reset during rebuild");

        ProjectionBundle afterStale = coordinator.publish(
            new DominionSnapshot(firstEpoch, List.of(authority, judgment))
        );
        require(afterStale == second, "stale snapshot replaced the published bundle");
        require(coordinator.publications() == 2L, "unexpected publication count");
        require(coordinator.staleRejections() == 1L, "stale publication was not counted");

        System.out.println(
            "ProjectionCoordinatorSelfTest: PASS "
                + "(atomic epoch, authority, capability, admission, rollback)"
        );
    }

    private static SubjectIdentity subject(
        UUID uuid, String type, String runtimeClass, boolean player
    ) {
        return new SubjectIdentity(
            uuid, type, runtimeClass, "minecraft:overworld",
            1.0D, 2.0D, 3.0D, player
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
