package dev.ronova.coremod.client;

import dev.ronova.coremod.authority.AuthorityIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiEvent;

public final class RonovaVisuals {
    private RonovaVisuals() {}

    public static void renderHud(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !AuthorityIdentity.contains(minecraft.player.getUUID())
            || RonovaClient.preferences().visualMode() == ClientPreferences.VisualMode.OFF) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int x = width - 154;
        graphics.fill(x, 10, width - 10, 37, 0xB814202A);
        graphics.fill(x, 10, x + 3, 37, 0xFF70E0D1);
        graphics.drawString(minecraft.font, Component.translatable("hud.ronova.authority"),
            x + 9, 15, 0xFF70E0D1, false);
        String result = ClientRuntime.feedbackTicks() > 0
            ? ClientRuntime.lastAction().name() + "  /  " + ClientRuntime.lastCount()
            : "64 · 64 · LOADED";
        graphics.drawString(minecraft.font, result, x + 9, 26, 0xFFD9E2EA, false);
    }

}
