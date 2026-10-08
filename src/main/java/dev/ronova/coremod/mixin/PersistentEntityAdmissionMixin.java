package dev.ronova.coremod.mixin;

import dev.ronova.coremod.engine.SpawnFirewall;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Final entity-manager sink used even by admissions that bypass Forge events. */
@Mixin(PersistentEntitySectionManager.class)
public abstract class PersistentEntityAdmissionMixin {
    @Inject(
        method = "addEntity(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void ronova$rejectManagerCommit(
        EntityAccess access,
        boolean existing,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (denies(access, SpawnFirewall.Boundary.ENTITY_MANAGER)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "addEntityWithoutEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private void ronova$rejectTombstonedSink(
        EntityAccess access,
        boolean existing,
        CallbackInfoReturnable<Boolean> callback
    ) {
        if (denies(access, SpawnFirewall.Boundary.ENTITY_MANAGER)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "startTracking", at = @At("HEAD"), cancellable = true)
    private void ronova$rejectTracking(EntityAccess access, CallbackInfo callback) {
        if (denies(access, SpawnFirewall.Boundary.ENTITY_MANAGER)) {
            callback.cancel();
        }
    }

    @Inject(method = "startTicking", at = @At("HEAD"), cancellable = true)
    private void ronova$rejectTicking(EntityAccess access, CallbackInfo callback) {
        // Raw scheduling membership must survive HELD so a later Root rebind can resume actual ticks.
        if (denies(access, SpawnFirewall.Boundary.ENTITY_MANAGER)) {
            callback.cancel();
        }
    }

    private static boolean denies(EntityAccess access, SpawnFirewall.Boundary boundary) {
        return access instanceof Entity entity
            && entity.level() instanceof ServerLevel level
            && SpawnFirewall.reject(level, entity, boundary);
    }
}
