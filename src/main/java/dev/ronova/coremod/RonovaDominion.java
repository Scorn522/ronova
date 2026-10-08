package dev.ronova.coremod;

import com.mojang.logging.LogUtils;
import dev.ronova.coremod.agent.AgentHooks;
import dev.ronova.coremod.agent.AutoAttach;
import dev.ronova.coremod.content.DominionContent;
import dev.ronova.coremod.engine.AuthorityGuard;
import dev.ronova.coremod.engine.QuarantineView;
import dev.ronova.coremod.nativebridge.NativeBridge;
import dev.ronova.coremod.network.RonovaNetwork;
import dev.ronova.coremod.agent.StartupPolicy;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

@Mod(RonovaDominion.MOD_ID)
public final class RonovaDominion {
    public static final String MOD_ID = "ronova";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final NativeBridge.State NATIVE_STATE =
        NativeBridge.load(FMLPaths.GAMEDIR.get().toAbsolutePath());

    public RonovaDominion(FMLJavaModLoadingContext context) {
        AutoAttach.Result attachResult = NativeBridge.isActive()
            ? AutoAttach.Result.NATIVE_ACTIVE
            : AutoAttach.ensureAttached(ModList.get().getModFileById(MOD_ID).getFile().getFilePath());
        DominionContent.register(context.getModEventBus());
        RonovaNetwork.initialize();
        context.getModEventBus().addListener(this::onLoadComplete);
        AgentHooks.installPredicate(AuthorityGuard::isProtectedObject);
        AgentHooks.installRemovalPredicate(AuthorityGuard::shouldProtectRemoval);
        AgentHooks.installQuarantinePredicate(QuarantineView::matchesObject);
        AgentHooks.installEntityPredicate(Entity.class::isInstance);
        LOGGER.info("Ronova initialized (native: {}, agent attach: {}, early agent: {})",
            NATIVE_STATE + "/0x" + Integer.toHexString(NativeBridge.capabilityMask()),
            attachResult, AgentHooks.isAgentActive());
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        if (NativeBridge.isActive()) {
            NativeBridge.startIntegrityWatchdog(StartupPolicy.load().reassertMillis());
        } else {
            AgentHooks.markRuntimeReady();
        }
        LOGGER.info("Ronova runtime integrity reassertion armed (late agent: {})",
            AgentHooks.isLateGuardActive());
    }
}
