package dev.ronova.coremod.client.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ronova.coremod.client.render.RonovaShaders;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Draws the complete animated frame in one GUI shader pass. */
public final class RonovaTooltipRenderer {
    private static final ResourceLocation EYE_ATLAS =
        ResourceLocation.fromNamespaceAndPath(
            "ronova", "textures/gui/ronova_eye_atlas.png");

    private RonovaTooltipRenderer() {}

    public static void render(
        GuiGraphics graphics,
        List<ClientTooltipComponent> components,
        int x,
        int y,
        Font font,
        boolean expanded
    ) {
        int contentWidth = 0;
        for (ClientTooltipComponent component : components) {
            contentWidth = Math.max(contentWidth, component.getWidth(font));
        }
        int contentHeight = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent component : components) {
            contentHeight += component.getHeight();
        }
        if (contentWidth <= 0 || contentHeight <= 0) return;

        int left = x - 6;
        int top = y - 7;
        int right = x + contentWidth + 6;
        int bottom = y + contentHeight + 7;
        int width = right - left;
        int height = bottom - top;

        ShaderInstance shader = RonovaShaders.tooltip();
        if (shader == null) {
            fallback(graphics, left, top, right, bottom);
            return;
        }

        graphics.flush();
        RonovaShaders.updateTooltip(width, height, expanded);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, EYE_ATLAS);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, left, bottom, 400.0F).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(matrix, right, bottom, 400.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, right, top, 400.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, left, top, 400.0F).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void fallback(GuiGraphics graphics, int left, int top, int right, int bottom) {
        graphics.fill(left, top, right, bottom, 0xF5130912);
        graphics.fill(left, top, right, top + 1, 0xF0681028);
        graphics.fill(left, bottom - 1, right, bottom, 0xF04A0B1D);
        graphics.fill(left, top, left + 1, bottom, 0xF05B0D24);
        graphics.fill(right - 1, top, right, bottom, 0xF05B0D24);
        graphics.fill(left + 1, top + 1, right - 1, top + 2, 0x704F1C2A);
    }
}
