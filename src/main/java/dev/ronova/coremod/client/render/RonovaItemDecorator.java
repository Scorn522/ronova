package dev.ronova.coremod.client.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemDecorator;

/** Small open-corner glints; intentionally avoids boxing in the 16px icon. */
public final class RonovaItemDecorator implements IItemDecorator {
    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        float breath = 0.5F + 0.5F * (float)Math.sin(System.currentTimeMillis() / 420.0D);
        int red = alpha(0xFFE43A5B, 0.48F + breath * 0.34F);
        int dark = alpha(0xFF7A1830, 0.48F + breath * 0.22F);
        int white = alpha(0xFFE7DEE1, 0.22F + breath * 0.20F);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 240.0F);
        corner(graphics, x, y, true, true, red);
        corner(graphics, x + 15, y, false, true, dark);
        corner(graphics, x, y + 15, true, false, dark);
        corner(graphics, x + 15, y + 15, false, false, red);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, white);
        if ((System.currentTimeMillis() / 210L) % 5L == 0L) {
            graphics.fill(x + 13, y + 4, x + 14, y + 5, white);
        }
        graphics.pose().popPose();
        return false;
    }

    private static void corner(
        GuiGraphics graphics,
        int x,
        int y,
        boolean right,
        boolean down,
        int color
    ) {
        int hx0 = right ? x : x - 2;
        int hx1 = right ? x + 3 : x + 1;
        int vy0 = down ? y : y - 2;
        int vy1 = down ? y + 3 : y + 1;
        graphics.fill(hx0, y, hx1, y + 1, color);
        graphics.fill(x, vy0, x + 1, vy1, color);
    }

    private static int alpha(int argb, float factor) {
        int value = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * factor)));
        return (value << 24) | (argb & 0x00FFFFFF);
    }
}
