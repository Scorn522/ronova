package dev.ronova.coremod.mixin;

import dev.ronova.coremod.client.ClientRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientRuntimeMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ronova$clientPulse(CallbackInfo callback) {
        ClientRuntime.pulse((Minecraft) (Object) this);
    }
}
