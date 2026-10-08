package dev.ronova.coremod.mixin;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Protects the server-side network sink while leaving client socket closure untouched. */
@Mixin(Connection.class)
public abstract class ConnectionProtectionMixin {
    @Inject(method = "disconnect", at = @At("HEAD"), cancellable = true)
    private void ronova$rejectRegisteredDisconnect(Component reason, CallbackInfo callback) {
        Connection connection = (Connection) (Object) this;
        ServerPlayer player = ServerRuntimeRegistry.protectedPlayer(connection);
        if (player != null && player.server != null
            && ServerRuntimeRegistry.runtime(player.server).authorityEnforcement()
                .shouldRejectConnectionDisconnect(connection, player)) {
            callback.cancel();
        }
    }
}
