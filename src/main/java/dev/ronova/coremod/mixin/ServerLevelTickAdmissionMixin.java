package dev.ronova.coremod.mixin;

import dev.ronova.coremod.engine.SpawnFirewall;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Current qualification assertion; raw held incarnations stay saved but cannot resume ticking. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelTickAdmissionMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void ronova$rejectFirstTick(Entity entity, CallbackInfo callback) {
        if (SpawnFirewall.reject(
            (ServerLevel) (Object) this, entity, SpawnFirewall.Boundary.FIRST_TICK
        )) {
            callback.cancel();
        }
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void ronova$rejectPassengerTick(Entity vehicle, Entity passenger, CallbackInfo callback) {
        if (SpawnFirewall.reject((ServerLevel) (Object) this, passenger, SpawnFirewall.Boundary.FIRST_TICK)) callback.cancel();
    }
}
