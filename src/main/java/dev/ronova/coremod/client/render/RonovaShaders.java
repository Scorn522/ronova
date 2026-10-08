package dev.ronova.coremod.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

/** Owns the independent Tooltip shader. Sword rendering is model-driven. */
public final class RonovaShaders {
    private static final ResourceLocation TOOLTIP_ID =
        ResourceLocation.fromNamespaceAndPath("ronova", "ronova_tooltip");

    private static ShaderInstance tooltip;
    private static long lastTooltipFrameNanos;
    private static float shiftOpenBlend;

    private RonovaShaders() {}

    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(
            new ShaderInstance(event.getResourceProvider(), TOOLTIP_ID, DefaultVertexFormat.POSITION_TEX),
            shader -> tooltip = shader
        );
    }

    public static ShaderInstance tooltip() {
        return tooltip;
    }

    public static void updateTooltip(float width, float height, boolean expanded) {
        ShaderInstance shader = tooltip;
        if (shader == null) return;
        long now = System.nanoTime();
        set(shader, "Time", renderSeconds(now));
        set(shader, "TooltipSize", width, height);
        set(shader, "ShiftOpen", updateShiftBlend(expanded, now));
    }

    private static float renderSeconds(long now) {
        return (float)((now / 1_000_000_000.0D) % 4096.0D);
    }

    private static float updateShiftBlend(boolean expanded, long now) {
        long elapsed = now - lastTooltipFrameNanos;
        if (lastTooltipFrameNanos == 0L || elapsed < 0L || elapsed > 500_000_000L) {
            shiftOpenBlend = 0.0F;
            elapsed = 0L;
        }
        lastTooltipFrameNanos = now;

        float target = expanded ? 1.0F : 0.0F;
        float seconds = Math.min(elapsed / 1_000_000_000.0F, 0.05F);
        float rate = expanded ? 1.85F : 5.0F;
        float step = Math.min(seconds * rate, 1.0F);
        shiftOpenBlend += (target - shiftOpenBlend) * step;
        if (Math.abs(target - shiftOpenBlend) < 0.001F) {
            shiftOpenBlend = target;
        }
        return shiftOpenBlend;
    }

    private static void set(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void set(ShaderInstance shader, String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }
}
