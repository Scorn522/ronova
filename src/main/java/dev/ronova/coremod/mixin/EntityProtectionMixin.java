package dev.ronova.coremod.mixin;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.engine.AuthorityEnforcement;
import dev.ronova.coremod.engine.SpawnFirewall;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityProtectionMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void ronova$observeConstruction(CallbackInfo callback) {
        SpawnFirewall.observeConstruction((Entity) (Object) this);
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void ronova$guardRemoval(Entity.RemovalReason reason, CallbackInfo callback) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player
            && enforcement(player).shouldProtectRemoval(self, reason)) {
            callback.cancel();
        }
    }

    private static AuthorityEnforcement enforcement(Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.server != null) {
            return ServerRuntimeRegistry.runtime(serverPlayer.server).authorityEnforcement();
        }
        return AuthorityEnforcement.client();
    }
}
