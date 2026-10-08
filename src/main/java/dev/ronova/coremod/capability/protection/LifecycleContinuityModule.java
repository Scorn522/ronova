package dev.ronova.coremod.capability.protection;

import dev.ronova.coremod.capability.CapabilityContext;
import dev.ronova.coremod.capability.ServerCapabilityModule;
import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.mixin.EntityStateAccessor;
import dev.ronova.coremod.mixin.LivingEntityStateAccessor;

public final class LifecycleContinuityModule implements ServerCapabilityModule {
    @Override public Capability capability() { return Capability.LIFECYCLE_CONTINUITY; }

    @Override public void repair(CapabilityContext context) {
        var player = context.player();
        context.attempt(() -> {
            if (player.isRemoved()) player.revive();
        });
        context.attempt(() -> {
            EntityStateAccessor state = (EntityStateAccessor) player;
            state.ronova$setRemovalReason(null);
            state.ronova$setAddedToWorld(true);
        });
        context.attempt(() -> {
            ((LivingEntityStateAccessor) player).ronova$setDead(false);
            player.deathTime = 0;
            player.hurtTime = 0;
            player.hurtDuration = 0;
        });
    }
}
