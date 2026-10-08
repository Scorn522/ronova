package dev.ronova.coremod.mixin;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks server-initiated owner kicks without interfering with socket close or server stop. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerConnectionProtectionMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void ronova$rejectForcedDisconnect(Component reason, CallbackInfo callback) {
        if (player != null && player.server != null
            && ServerRuntimeRegistry.runtime(player.server).authorityEnforcement()
                .shouldRejectDisconnect(player)) {
            callback.cancel();
        }
    }
}
