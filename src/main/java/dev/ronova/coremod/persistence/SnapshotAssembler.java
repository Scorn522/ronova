package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Pure facts-to-snapshot assembler; persistence never asks the decision engine for policy. */
final class SnapshotAssembler {
    private SnapshotAssembler() {}

    static DominionSnapshot assemble(
        DominionEpoch epoch,
        Collection<UUID> owners,
        Collection<DominionLedger.Tombstone> tombstones
    ) {
        List<DominionDecision> decisions = new ArrayList<>(owners.size() + tombstones.size());
        for (UUID owner : owners) {
            SubjectIdentity subject = new SubjectIdentity(
                owner, "minecraft:player", "net.minecraft.server.level.ServerPlayer",
                "unknown:unknown", 0.0D, 0.0D, 0.0D, true
            );
            decisions.add(new DominionDecision.Authority(
                subject, epoch, true, CapabilitySet.OWNER_DEFAULT, "persisted-owner"
            ));
        }
        for (DominionLedger.Tombstone tombstone : tombstones) {
            decisions.add(tombstone.toDecision(
                new DominionEpoch(epoch.generation(), tombstone.sequence())
            ));
        }
        return new DominionSnapshot(epoch, decisions);
    }
}
