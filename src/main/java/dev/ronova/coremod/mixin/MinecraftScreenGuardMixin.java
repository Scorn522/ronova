package dev.ronova.coremod.mixin;

import dev.ronova.coremod.client.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftScreenGuardMixin {
    @Inject(method = {"setScreen", "forceSetScreen"}, at = @At("HEAD"), cancellable = true)
    private void ronova$rejectForcedScreenDuringRecovery(Screen requested, CallbackInfo callback) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (ClientRuntime.shouldRejectScreen(minecraft, requested)) {
            callback.cancel();
        }
    }

    @Inject(method = "setOverlay", at = @At("HEAD"), cancellable = true)
    private void ronova$rejectForcedOverlayDuringRecovery(Overlay requested, CallbackInfo callback) {
        if (ClientRuntime.shouldRejectOverlay((Minecraft) (Object) this, requested)) {
            callback.cancel();
        }
    }
}
