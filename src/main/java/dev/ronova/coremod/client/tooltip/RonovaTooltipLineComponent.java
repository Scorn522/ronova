package dev.ronova.coremod.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/** Single-pass, shadow-free text and fixed-width divider rendering. */
public final class RonovaTooltipLineComponent implements ClientTooltipComponent {
    private static final int FULL_BRIGHT = 0x00F000F0;

    private final RonovaTooltipLineData data;

    public RonovaTooltipLineComponent(RonovaTooltipLineData data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return data.height();
    }

    @Override
    public int getWidth(Font font) {
        return switch (data.kind()) {
            case TEXT -> font.width(data.text());
            case SPACER -> 0;
            case DIVIDER -> data.width();
        };
    }

    @Override
    public void renderText(
        Font font,
        int x,
        int y,
        Matrix4f matrix,
        MultiBufferSource.BufferSource bufferSource
    ) {
        if (data.kind() != RonovaTooltipLineData.Kind.TEXT) return;
        font.drawInBatch(
            data.text(), x, y, 0xFFFFFFFF, false, matrix, bufferSource,
            Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        if (data.kind() != RonovaTooltipLineData.Kind.DIVIDER) return;
        int centerY = y + data.height() / 2;
        int width = data.width();
        graphics.fill(x, centerY, x + width, centerY + 1, 0x604A2631);
        graphics.fill(
            x + width / 5,
            centerY,
            x + width - width / 5,
            centerY + 1,
            data.decorationArgb()
        );
    }
}
