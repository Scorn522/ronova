package dev.ronova.coremod.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ronova.coremod.RonovaDominion;
import dev.ronova.coremod.client.model.RonovaItemModel;
import dev.ronova.coremod.client.model.RonovaMaskedLayerModelLoader;
import dev.ronova.coremod.client.render.RonovaItemDecorator;
import dev.ronova.coremod.client.render.RonovaMaskedLayerShaders;
import dev.ronova.coremod.client.render.RonovaShaders;
import dev.ronova.coremod.client.tooltip.RonovaTooltipControlComponent;
import dev.ronova.coremod.client.tooltip.RonovaTooltipControlData;
import dev.ronova.coremod.client.tooltip.RonovaTooltipLineComponent;
import dev.ronova.coremod.client.tooltip.RonovaTooltipLineData;
import dev.ronova.coremod.content.DominionContent;
import java.io.IOException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import dev.ronova.coremod.engine.QuarantineView;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = RonovaDominion.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RonovaClient {
    private static final RonovaItemDecorator ITEM_DECORATOR = new RonovaItemDecorator();
    private static ClientPreferences preferences = ClientPreferences.load(
        FMLPaths.GAMEDIR.get().resolve("config").resolve("ronova-client.properties"));
    public static final KeyMapping OPEN_CONTROL = new KeyMapping(
        "key.ronova.open_control",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_O,
        "key.categories.ronova"
    );

    private RonovaClient() {}

    public static ClientPreferences preferences() {
        return preferences;
    }

    public static void setPreferences(ClientPreferences next) {
        preferences = next == null ? ClientPreferences.defaults() : next;
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONTROL);
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        event.register(DominionContent.RONOVA_BLADE.get(), ITEM_DECORATOR);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        RonovaShaders.register(event);
        RonovaMaskedLayerShaders.register(event);
    }

    @SubscribeEvent
    public static void registerTooltipComponents(
        RegisterClientTooltipComponentFactoriesEvent event
    ) {
        event.register(RonovaTooltipLineData.class, RonovaTooltipLineComponent::new);
        event.register(RonovaTooltipControlData.class, RonovaTooltipControlComponent::new);
    }

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("masked_layer_loader", RonovaMaskedLayerModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void wrapBladeModel(ModelEvent.ModifyBakingResult event) {
        RonovaItemModel.wrapInventoryModel(event, DominionContent.RONOVA_BLADE.getId());
    }

    @Mod.EventBusSubscriber(modid = RonovaDominion.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        private ForgeEvents() {}

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            ClientRuntime.pulse(minecraft);
            while (OPEN_CONTROL.consumeClick()) {
                minecraft.setScreen(new RonovaStrengthScreen(minecraft.screen));
            }
        }

        @SubscribeEvent
        public static void renderHud(RenderGuiEvent.Post event) {
            ClientRuntime.framePulse(Minecraft.getInstance());
            RonovaVisuals.renderHud(event);
        }

        @SubscribeEvent
        public static void renderLevel(RenderLevelStageEvent event) {
            ClientRuntime.framePulse(Minecraft.getInstance());
        }

        @SubscribeEvent
        public static void suppressQuarantinedRender(RenderLivingEvent.Pre<?, ?> event) {
            if (QuarantineView.matches(event.getEntity())) {
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void clearQuarantine(ClientPlayerNetworkEvent.LoggingOut event) {
            QuarantineView.clearClientMarks();
            ClientRuntime.suspendSession();
        }
    }
}
