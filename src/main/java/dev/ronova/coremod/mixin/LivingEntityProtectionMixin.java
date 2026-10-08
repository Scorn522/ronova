package dev.ronova.coremod.mixin;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.engine.AuthorityEnforcement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityProtectionMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ronova$guardHurt(
        DamageSource source,
        float amount,
        CallbackInfoReturnable<Boolean> callback
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && protect(player)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void ronova$guardDeath(DamageSource source, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && protect(player)) {
            callback.cancel();
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void ronova$guardHealth(float health, CallbackInfo callback) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (health <= 0.0F && self instanceof Player player && protect(player)) {
            callback.cancel();
        }
    }

    private static boolean protect(Player player) {
        AuthorityEnforcement enforcement = enforcement(player);
        if (!enforcement.view().isProtected(player)) {
            return false;
        }
        enforcement.repair(player);
        return true;
    }

    private static AuthorityEnforcement enforcement(Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.server != null) {
            return ServerRuntimeRegistry.runtime(serverPlayer.server).authorityEnforcement();
        }
        return AuthorityEnforcement.client();
    }
}
