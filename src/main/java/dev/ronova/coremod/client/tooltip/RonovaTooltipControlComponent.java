package dev.ronova.coremod.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/** Draws controls in stable columns without relying on whitespace glyph widths. */
public final class RonovaTooltipControlComponent implements ClientTooltipComponent {
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final int LINE_HEIGHT = 10;
    private static final int INLINE_GAP = 4;
    private static final String WIDEST_INPUT = "[ Shift + 右键 ]  ";

    private final RonovaTooltipControlData data;

    public RonovaTooltipControlComponent(RonovaTooltipControlData data) {
        this.data = data;
    }

    @Override
    public int getHeight() {
        return data.descriptionBelow() ? LINE_HEIGHT * 2 : LINE_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        int keyColumn = font.width(WIDEST_INPUT);
        int firstLine = keyColumn + font.width(data.action());
        if (!data.descriptionBelow()) {
            firstLine += INLINE_GAP + font.width(data.description());
            return firstLine;
        }
        return Math.max(firstLine, keyColumn + font.width(data.description()));
    }

    @Override
    public void renderText(
        Font font,
        int x,
        int y,
        Matrix4f matrix,
        MultiBufferSource.BufferSource bufferSource
    ) {
        int keyColumn = font.width(WIDEST_INPUT);
        int actionX = x + keyColumn;
        draw(font, data.input(), x, y, matrix, bufferSource);
        draw(font, data.action(), actionX, y, matrix, bufferSource);
        if (data.descriptionBelow()) {
            draw(font, data.description(), actionX, y + LINE_HEIGHT, matrix, bufferSource);
        } else {
            int descriptionX = actionX + font.width(data.action()) + INLINE_GAP;
            draw(font, data.description(), descriptionX, y, matrix, bufferSource);
        }
    }

    private static void draw(
        Font font,
        Component text,
        int x,
        int y,
        Matrix4f matrix,
        MultiBufferSource.BufferSource bufferSource
    ) {
        font.drawInBatch(
            text, x, y, 0xFFFFFFFF, false, matrix, bufferSource,
            Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
    }
}
