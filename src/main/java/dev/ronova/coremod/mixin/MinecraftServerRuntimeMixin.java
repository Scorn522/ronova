package dev.ronova.coremod.mixin;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.runtime.RuntimeCoordinator;
import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerRuntimeMixin {
    @Inject(method = "stopServer", at = @At("HEAD"))
    private void ronova$beginShutdown(CallbackInfo callback) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        var runtime = ServerRuntimeRegistry.getIfPresent(server);
        if (runtime != null) runtime.authorityState().beginShutdown();
    }

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void ronova$runtimeHead(BooleanSupplier hasTimeLeft, CallbackInfo callback) {
        RuntimeCoordinator.serverTickHead((MinecraftServer) (Object) this);
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void ronova$runtimePulse(BooleanSupplier hasTimeLeft, CallbackInfo callback) {
        RuntimeCoordinator.serverTickTail((MinecraftServer) (Object) this);
    }
}
