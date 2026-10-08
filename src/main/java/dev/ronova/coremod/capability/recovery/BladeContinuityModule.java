package dev.ronova.coremod.capability.recovery;

import dev.ronova.coremod.capability.CapabilityContext;
import dev.ronova.coremod.capability.ServerCapabilityModule;
import dev.ronova.coremod.content.DominionContent;
import dev.ronova.coremod.contract.Capability;

public final class BladeContinuityModule implements ServerCapabilityModule {
    @Override public Capability capability() { return Capability.BLADE_CONTINUITY; }

    @Override public void repair(CapabilityContext context) {
        var player = context.player();
        if (player.tickCount % 20 != 0) {
            return;
        }
        context.attempt(() -> {
            var probe = DominionContent.RONOVA_BLADE.get().getDefaultInstance();
            if (!player.getInventory().contains(probe)) {
                player.getInventory().add(DominionContent.RONOVA_BLADE.get().getDefaultInstance());
            }
        });
    }
}
