package dev.ronova.coremod.mixin;

import dev.ronova.coremod.client.ClientRuntime;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerInputMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void ronova$captureMouseEdge(
        long window,
        int button,
        int action,
        int modifiers,
        CallbackInfo callback
    ) {
        ClientRuntime.onMouseButton(window, button, action, modifiers);
    }
}
