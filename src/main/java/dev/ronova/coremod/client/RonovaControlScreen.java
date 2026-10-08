package dev.ronova.coremod.client;

import dev.ronova.coremod.agent.AgentHooks;
import dev.ronova.coremod.agent.StartupPolicy;
import dev.ronova.coremod.nativebridge.NativeBridge;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

public final class RonovaControlScreen extends Screen {
    private static final int PANEL_WIDTH = 520;
    private static final int PANEL_HEIGHT = 372;
    private static final int PANEL = 0xF21A222B;
    private static final int CARD = 0xFF24323E;
    private static final int ACCENT = 0xFF9B7CFF;
    private static final int CYAN = 0xFF70E0D1;
    private static final int MUTED = 0xFF8FA1B2;
    private final Screen parent;
    private StartupPolicy policy;
    private Component status = Component.empty();
    private Button modeButton;
    private Button containButton;
    private Button intervalButton;
    private Button visualButton;
    private ClientPreferences preferences;

    public RonovaControlScreen(Screen parent) {
        super(Component.translatable("screen.ronova.title"));
        this.parent = parent;
        this.policy = StartupPolicy.load(policyPath());
        this.preferences = ClientPreferences.load(preferencesPath());
    }

    @Override
    protected void init() {
        int left = (width - Math.min(PANEL_WIDTH, width - 24)) / 2;
        int top = (height - Math.min(PANEL_HEIGHT, height - 24)) / 2;
        int buttonX = left + Math.min(PANEL_WIDTH, width - 24) - 154;

        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> {
            StartupPolicy.ProtectionMode next = policy.protectionMode() == StartupPolicy.ProtectionMode.DUAL_AGENT
                ? StartupPolicy.ProtectionMode.MIXIN : StartupPolicy.ProtectionMode.DUAL_AGENT;
            policy = new StartupPolicy(next, policy.containExternalRetransform(), policy.reassertMillis());
            button.setMessage(modeLabel());
            status = Component.empty();
        }).bounds(buttonX, top + 78, 126, 24).build());

        containButton = addRenderableWidget(Button.builder(toggleLabel(), button -> {
            policy = new StartupPolicy(policy.protectionMode(), !policy.containExternalRetransform(), policy.reassertMillis());
            button.setMessage(toggleLabel());
            status = Component.empty();
        }).bounds(buttonX, top + 132, 126, 24).build());

        intervalButton = addRenderableWidget(Button.builder(intervalLabel(), button -> {
            int next = policy.reassertMillis() < 30_000 ? 30_000
                : policy.reassertMillis() < 60_000 ? 60_000 : 15_000;
            policy = new StartupPolicy(policy.protectionMode(), policy.containExternalRetransform(), next);
            button.setMessage(intervalLabel());
            status = Component.empty();
        }).bounds(buttonX, top + 186, 126, 24).build());

        visualButton = addRenderableWidget(Button.builder(visualLabel(), button -> {
            preferences = preferences.nextVisualMode();
            RonovaClient.setPreferences(preferences);
            button.setMessage(visualLabel());
            status = Component.empty();
        }).bounds(buttonX, top + 240, 126, 24).build());

        addRenderableWidget(Button.builder(Component.translatable("screen.ronova.save"), button -> save())
            .bounds(left + 24, top + 294, 140, 28).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(left + 172, top + 294, 96, 28).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, width, height, 0xFF111820, 0xFF17242D);
        for (int y = 12; y < height; y += 24) {
            for (int x = ((y / 24) & 1) * 12; x < width; x += 24) {
                graphics.fill(x, y, x + 1, y + 1, 0x334A7180);
            }
        }

        int panelWidth = Math.min(PANEL_WIDTH, width - 24);
        int panelHeight = Math.min(PANEL_HEIGHT, height - 24);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left - 6, top + 8, left + panelWidth + 6, top + panelHeight + 12, 0x66000000);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fillGradient(left, top, left + panelWidth, top + 3, ACCENT, CYAN);

        graphics.drawString(font, title, left + 24, top + 18, 0xFFF0F3F7, false);
        graphics.drawString(font, Component.translatable("screen.ronova.subtitle"), left + 24, top + 35, MUTED, false);
        drawCard(graphics, left + 20, top + 65, panelWidth - 40,
            Component.translatable("screen.ronova.mode"),
            Component.translatable("screen.ronova.mode.desc"));
        drawCard(graphics, left + 20, top + 119, panelWidth - 40,
            Component.translatable("screen.ronova.contain"),
            Component.translatable("screen.ronova.contain.desc"));
        drawCard(graphics, left + 20, top + 173, panelWidth - 40,
            Component.translatable("screen.ronova.interval"),
            Component.translatable("screen.ronova.interval.desc"));
        drawCard(graphics, left + 20, top + 227, panelWidth - 40,
            Component.translatable("screen.ronova.visual"),
            Component.translatable("screen.ronova.visual.desc"));

        Component runtime = Component.literal(
            "Native=" + NativeBridge.state() + "  Agent=" + AgentHooks.isAgentActive() + "/" + AgentHooks.isLateGuardActive()
        ).withStyle(NativeBridge.isActive() || AgentHooks.isLateGuardActive() ? ChatFormatting.AQUA : ChatFormatting.YELLOW);
        graphics.drawString(font, runtime, left + panelWidth - 24 - font.width(runtime), top + 306, 0xFFFFFFFF, false);
        Component scope = Component.literal("Primary 64  ·  Area 64  ·  Loaded chunks only")
            .withStyle(ChatFormatting.LIGHT_PURPLE);
        graphics.drawString(font, scope, left + 24, top + 334, 0xFFFFFFFF, false);
        if (!status.getString().isEmpty()) {
            graphics.drawString(font, status, left + 24, top + 351, CYAN, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawCard(GuiGraphics graphics, int x, int y, int cardWidth, Component label, Component description) {
        graphics.fill(x, y, x + cardWidth, y + 43, CARD);
        graphics.fill(x, y + 8, x + 3, y + 35, ACCENT);
        graphics.drawString(font, label, x + 14, y + 8, 0xFFE8EDF2, false);
        graphics.drawString(font, description, x + 14, y + 25, MUTED, false);
    }

    private Component modeLabel() {
        return Component.translatable("screen.ronova.mode." + policy.protectionMode().name().toLowerCase());
    }

    private Component toggleLabel() {
        return Component.translatable(policy.containExternalRetransform() ? "options.on" : "options.off");
    }

    private Component intervalLabel() {
        return Component.literal(policy.reassertMillis() + " ms");
    }

    private Component visualLabel() {
        return Component.translatable("screen.ronova.visual."
            + preferences.visualMode().name().toLowerCase());
    }

    private void save() {
        try {
            policy.save(policyPath());
            preferences.save(preferencesPath());
            RonovaClient.setPreferences(preferences);
            status = Component.translatable("screen.ronova.saved");
        } catch (IOException exception) {
            status = Component.translatable("screen.ronova.save_failed");
        }
    }

    private static Path policyPath() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("ronova-startup.properties");
    }

    private static Path preferencesPath() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("ronova-client.properties");
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
