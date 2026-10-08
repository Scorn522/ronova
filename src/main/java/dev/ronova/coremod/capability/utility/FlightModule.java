package dev.ronova.coremod.capability.utility;

import dev.ronova.coremod.capability.CapabilityContext;
import dev.ronova.coremod.capability.ServerCapabilityModule;
import dev.ronova.coremod.contract.Capability;
import net.minecraft.server.level.ServerPlayer;

public final class FlightModule implements ServerCapabilityModule {
    @Override public Capability capability() { return Capability.FLIGHT; }

    @Override public void repair(CapabilityContext context) {
        var player = context.player();
        context.attempt(() -> {
            var abilities = player.getAbilities();
            boolean changed = !abilities.invulnerable || !abilities.mayfly || !abilities.flying;
            abilities.invulnerable = true;
            abilities.mayfly = true;
            abilities.flying = true;
            if (changed && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.onUpdateAbilities();
            }
        });
    }
}
