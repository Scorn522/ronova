package dev.ronova.coremod.projection;

import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.util.List;
import java.util.UUID;

/** Proves routine Authority, Admission and Presence decisions need no Root call. */
public final class HotPathProjectionSelfTest {
    private HotPathProjectionSelfTest() {}

    public static void main(String[] args) {
        DominionEpoch epoch = new DominionEpoch(1L, 9L);
        SubjectIdentity owner = subject(UUID.randomUUID(), "minecraft:player", true);
        SubjectIdentity slime = subject(UUID.randomUUID(), "minecraft:slime", false);
        DominionSnapshot snapshot = new DominionSnapshot(epoch, List.of(
            new DominionDecision.Authority(
                owner, epoch, true, CapabilitySet.OWNER_DEFAULT, "test-owner"
            ),
            new DominionDecision.Judgment(
                slime, epoch, true, DominionDecision.LineageScope.GLOBAL_LINEAGE,
                0.0D, "test-lineage"
            )
        ));

        AuthorityTopologyProjection authority = new AuthorityTopologyProjection(snapshot);
        require(authority.resolve(owner).authorized(), "projected owner was denied");
        require(!authority.resolve(subject(UUID.randomUUID(), "minecraft:player", true))
            .authorized(), "unknown owner was authorized");

        AdmissionProjection admission = new AdmissionProjection(
            snapshot, new AdmissionProjection.AdmissionMetrics()
        );
        require(!admission.decide(slime, true, false, false, false, true).admitted(),
            "global lineage spawn was admitted");
        require(admission.decide(
            subject(UUID.randomUUID(), "minecraft:pig", false),
            true, false, false, false, true
        ).admitted(), "unmatched spawn was rejected");

        PresenceProjection presence = new PresenceProjection();
        require(!presence.decide(slime, epoch, true).present(),
            "quarantined entity remained present");
        require(presence.decide(slime, DominionEpoch.ZERO, false).present(),
            "client-local visible entity was hidden");
        require(presence.evaluations() == 2L, "presence evaluations were not counted");

        System.out.println(
            "HotPathProjectionSelfTest: PASS (Authority, Admission, Presence stay local)"
        );
    }

    private static SubjectIdentity subject(UUID uuid, String type, boolean player) {
        return new SubjectIdentity(
            uuid, type, "test." + type.substring(type.indexOf(':') + 1),
            "minecraft:overworld", 1.0D, 2.0D, 3.0D, player
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
