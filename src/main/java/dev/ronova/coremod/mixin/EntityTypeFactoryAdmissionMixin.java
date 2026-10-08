package dev.ronova.coremod.mixin;

import dev.ronova.coremod.engine.SpawnFirewall;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Retained ABI boundary that observes factory results. Rejection happens at
 * JOIN/LEVEL/manager gates: returning null here makes NaturalSpawner emit one
 * WARN for every intentionally suppressed spawn.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeFactoryAdmissionMixin {
    @Inject(method = "create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;",
        at = @At("RETURN"))
    private void ronova$observeFactoryResult(
        Level level,
        CallbackInfoReturnable<Entity> callback
    ) {
        Entity entity = callback.getReturnValue();
        if (level instanceof ServerLevel && entity != null) {
            SpawnFirewall.observeFactoryResult(entity);
        }
    }
}
