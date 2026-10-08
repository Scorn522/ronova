package dev.ronova.coremod.capability.recovery;

import dev.ronova.coremod.capability.CapabilityContext;
import dev.ronova.coremod.capability.ServerCapabilityModule;
import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.mixin.LivingEntityStateAccessor;

public final class VitalsRecoveryModule implements ServerCapabilityModule {
    @Override public Capability capability() { return Capability.VITALS_RECOVERY; }

    @Override public void repair(CapabilityContext context) {
        var player = context.player();
        float health;
        try {
            health = Math.max(20.0F, player.getMaxHealth());
        } catch (Throwable failure) {
            health = 20.0F;
        }
        if(!Float.isFinite(health))health=20.0F;
        float restoredHealth = health;
        context.attempt(() -> player.setHealth(restoredHealth));
        context.attempt(() -> player.getEntityData().set(
            LivingEntityStateAccessor.ronova$getHealthDataAccessor(), restoredHealth
        ));
        if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            context.attempt(()->RegisteredOwnerVitals.repair(serverPlayer,restoredHealth));
        context.attempt(() -> {
            player.invulnerableTime = 20;
            player.clearFire();
            player.setAirSupply(player.getMaxAirSupply());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
        });
    }
}
