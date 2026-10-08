package dev.ronova.coremod.contract;

import dev.ronova.coremod.core.DominionDecisionSource;
import dev.ronova.coremod.core.LocalDominionCore;
import java.util.List;
import java.util.UUID;

/** Golden state-machine checks for the loader-neutral R1 contract. */
public final class DominionContractSelfTest {
    private DominionContractSelfTest() {}

    public static void main(String[] args) {
        SubjectIdentity mob = subject(false);
        SubjectIdentity player = subject(true);

        DominionEpoch first = DominionEpoch.LOCAL_INITIAL.nextSequence();
        DominionEpoch second = first.nextSequence();
        require(second.isAfter(first), "decision sequence must be monotonic");
        require(!first.isAfter(second), "older epoch must never outrank a newer epoch");

        CapabilitySet ownerCapabilities = CapabilitySet.OWNER_DEFAULT;
        require(ownerCapabilities.contains(Capability.FLIGHT), "owner must retain flight");
        require(ownerCapabilities.contains(Capability.INSTANT_MINING),
            "owner must retain instant mining");
        require(!CapabilitySet.NONE.contains(Capability.FLIGHT),
            "empty capability set must deny every capability");

        DominionDecision.Authority denied = authority(player, first, false);
        DominionDecision.Authority allowed = authority(player, first, true);
        require(!denied.authorized() && denied.capabilities().equals(CapabilitySet.NONE),
            "unrecorded owner must be denied without capabilities");
        require(allowed.authorized() && allowed.capabilities().contains(Capability.FLIGHT),
            "recorded owner must receive the frozen owner capability set");

        DominionDecision.Judgment mobJudgment = judgment(mob, first, true, false, 8.0D);
        require(mobJudgment.committed()
                && mobJudgment.scope() == DominionDecision.LineageScope.LOCAL_LINEAGE
                && mobJudgment.radius() == 0.0D,
            "non-player judgment widened beyond its requested scope");

        DominionDecision.Judgment playerJudgment = judgment(player, second, true, false, 8.0D);
        require(playerJudgment.committed()
                && playerJudgment.scope() == DominionDecision.LineageScope.IDENTITY
                && playerJudgment.radius() == 8.0D,
            "player judgment must remain exact identity with its requested radius");
        require(!judgment(mob, second, false, false, 8.0D).committed(),
            "unauthorized actor must not commit a judgment");
        require(!judgment(mob, second, true, true, 8.0D).committed(),
            "terminal object must not create a second judgment");

        DominionDecision.Admission tombstone = admission(mob, second, true, false, false, true);
        DominionDecision.Admission worldResult = admission(mob, second, false, false, false, true);
        require(!tombstone.admitted(), "tombstone match must be rejected");
        require(worldResult.admitted(), "world result must remain outside combat admission");

        DominionDecision.Presence hidden = (DominionDecision.Presence) LocalDominionCore.decide(
            new DominionRequest.Presence(mob, second, true)
        );
        require(!hidden.present(), "quarantined subject must be absent from Presence");

        DominionSnapshot snapshot = new DominionSnapshot(
            second, List.of(allowed, mobJudgment, playerJudgment, tombstone, hidden)
        );
        require(snapshot.authorities().size() == 1 && snapshot.judgments().size() == 2,
            "snapshot must preserve typed domain views");
        require(snapshot.isNewerThan(new DominionSnapshot(first, List.of())),
            "snapshot comparison must use epoch and sequence");
        require(snapshot.decisions().getClass() != java.util.ArrayList.class,
            "snapshot decisions must be immutable copies");

        DominionDecisionSource previous = LocalDominionCore.install(request ->
            new DominionDecision.Presence(request.subject(), request.epoch(), true, "test-source")
        );
        try {
            DominionDecision replaced = LocalDominionCore.decide(
                new DominionRequest.Presence(mob, second, true)
            );
            require(replaced instanceof DominionDecision.Presence presence && presence.present(),
                "decision source cutover seam must be replaceable");
        } finally {
            LocalDominionCore.install(previous);
            LocalDominionCore.resetLocal();
        }

        previous = LocalDominionCore.install(request -> new DominionDecision.Judgment(request.subject(), request.epoch(),
            true, DominionDecision.LineageScope.GLOBAL_LINEAGE, 999D, "legacy-native-proposal"));
        try {
            var bounded = (DominionDecision.Judgment) LocalDominionCore.decide(new DominionRequest.Judgment(
                mob, second, true, false, DominionDecision.LineageScope.IDENTITY, 0D));
            require(bounded.scope() == DominionDecision.LineageScope.IDENTITY && bounded.radius() == 0D,
                "legacy provider bypassed the Root scope ceiling");
            require(!judgment(mob, second, false, false, 0D).committed()
                && !judgment(mob, second, true, true, 0D).committed(), "provider bypassed actor or terminal guard");
        } finally { LocalDominionCore.install(previous); }
        System.out.println(
            "DominionContractSelfTest: PASS "
                + "(Subject, Decision, Snapshot, Epoch, Capability)"
        );
    }

    private static SubjectIdentity subject(boolean player) {
        return new SubjectIdentity(
            UUID.randomUUID(), player ? "minecraft:player" : "minecraft:zombie",
            player ? "test.Player" : "test.Zombie", "minecraft:overworld",
            1.0D, 2.0D, 3.0D, player
        );
    }

    private static DominionDecision.Authority authority(
        SubjectIdentity subject, DominionEpoch epoch, boolean owner
    ) {
        return (DominionDecision.Authority) LocalDominionCore.decide(
            new DominionRequest.Authority(subject, epoch, owner)
        );
    }

    private static DominionDecision.Judgment judgment(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean actor,
        boolean terminal,
        double radius
    ) {
        return (DominionDecision.Judgment) LocalDominionCore.decide(
            new DominionRequest.Judgment(
                subject, epoch, actor, terminal,
                DominionDecision.LineageScope.LOCAL_LINEAGE, radius
            )
        );
    }

    private static DominionDecision.Admission admission(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean combat,
        boolean protectedSubject,
        boolean terminal,
        boolean tombstone
    ) {
        return (DominionDecision.Admission) LocalDominionCore.decide(
            new DominionRequest.Admission(
                subject, epoch, combat, protectedSubject, terminal,
                tombstone, false, true
            )
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
