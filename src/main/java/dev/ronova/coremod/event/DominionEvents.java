package dev.ronova.coremod.event;

import dev.ronova.coremod.RonovaDominion;
import dev.ronova.coremod.authority.AuthorityBinder;
import dev.ronova.coremod.baseline.BaselineTelemetry;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.command.DominionCommands;
import dev.ronova.coremod.engine.AuthorityEnforcement;
import dev.ronova.coremod.engine.JudgmentEngine;
import dev.ronova.coremod.engine.QuarantineView;
import dev.ronova.coremod.engine.SpawnFirewall;
import dev.ronova.coremod.network.RonovaNetwork;
import dev.ronova.coremod.runtime.RuntimeCoordinator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RonovaDominion.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DominionEvents {
    private DominionEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onAttack(LivingAttackEvent event) {
        recordEvent(event.getEntity(),RuntimeEventLedger.Kind.ATTACK,false);
        if (QuarantineView.matchesObject(event.getSource().getEntity())
            || QuarantineView.matchesObject(event.getSource().getDirectEntity())) {
            event.setCanceled(true);
            recordEvent(event.getEntity(),RuntimeEventLedger.Kind.ATTACK,true);
            return;
        }
        if (event.getEntity() instanceof Player player) {
            AuthorityEnforcement enforcement = enforcement(player);
            if (enforcement.view().isProtected(player)) {
                event.setCanceled(true);
                recordEvent(event.getEntity(),RuntimeEventLedger.Kind.ATTACK,true);
                enforcement.repair(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onHurt(LivingHurtEvent event) {
        recordEvent(event.getEntity(),RuntimeEventLedger.Kind.HURT,false);
        if (event.getEntity() instanceof Player player) {
            AuthorityEnforcement enforcement = enforcement(player);
            if (enforcement.view().isProtected(player)) {
                event.setAmount(0.0F);
                event.setCanceled(true);
                recordEvent(event.getEntity(),RuntimeEventLedger.Kind.HURT,true);
                enforcement.repair(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onDamage(LivingDamageEvent event) {
        recordEvent(event.getEntity(),RuntimeEventLedger.Kind.DAMAGE,false);
        if (event.getEntity() instanceof Player player) {
            AuthorityEnforcement enforcement = enforcement(player);
            if (enforcement.view().isProtected(player)) {
                event.setAmount(0.0F);
                event.setCanceled(true);
                recordEvent(event.getEntity(),RuntimeEventLedger.Kind.DAMAGE,true);
                enforcement.repair(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onDeath(LivingDeathEvent event) {
        recordEvent(event.getEntity(),RuntimeEventLedger.Kind.DEATH,false);
        if (event.getEntity() instanceof Player player) {
            AuthorityEnforcement enforcement = enforcement(player);
            if (enforcement.view().isProtected(player)) {
                event.setCanceled(true);
                recordEvent(event.getEntity(),RuntimeEventLedger.Kind.DEATH,true);
                enforcement.repair(player);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        recordEvent(event.getEntity(),RuntimeEventLedger.Kind.JOIN,false);
        if (event.getLevel() instanceof ServerLevel level
            && SpawnFirewall.reject(level, event.getEntity(), SpawnFirewall.Boundary.JOIN)) {
            event.setCanceled(true);
            recordEvent(event.getEntity(),RuntimeEventLedger.Kind.JOIN,true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.player instanceof ServerPlayer player) {
            AuthorityEnforcement enforcement = ServerRuntimeRegistry.runtime(player.server)
                .authorityEnforcement();
            if (enforcement.view().isProtected(player)) {
                enforcement.maintain(player);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RuntimeCoordinator.serverTick(event.getServer());
        }
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLevelUnload(net.minecraftforge.event.level.LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            var runtime = ServerRuntimeRegistry.getIfPresent(level.getServer());
            if (runtime != null) runtime.recoverySources().worldUnloading(level);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original
            && event.getEntity() instanceof ServerPlayer replacement) {
            RonovaNetwork.registerPlayer(replacement);
            var runtime = ServerRuntimeRegistry.runtime(original.server);
            if (runtime.authorityView().isProtected(original)) {
                AuthorityBinder.restore(replacement);
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RonovaNetwork.registerPlayer(player);
            if (ServerRuntimeRegistry.runtime(player.server).authorityView()
                .owns(player.getUUID())) {
                AuthorityBinder.restore(player);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AuthorityBinder.releaseObject(player);
            RonovaNetwork.releasePlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        try {
            BaselineTelemetry.serverStopped(event.getServer());
            try { JudgmentEngine.clearServer(event.getServer()); }
            finally {
                try { SpawnFirewall.clearServer(event.getServer()); }
                finally {
                    try { QuarantineView.clearServer(event.getServer()); }
                    finally { RonovaNetwork.clearServerState(event.getServer()); }
                }
            }
        } finally { ServerRuntimeRegistry.release(event.getServer()); }
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        DominionCommands.register(event.getDispatcher());
    }

    private static AuthorityEnforcement enforcement(Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.server != null) {
            return ServerRuntimeRegistry.runtime(serverPlayer.server).authorityEnforcement();
        }
        return AuthorityEnforcement.client();
    }
    private static void recordEvent(net.minecraft.world.entity.Entity entity,RuntimeEventLedger.Kind kind,boolean cancellation) {
        if(entity!=null&&entity.level() instanceof ServerLevel level){var runtime=ServerRuntimeRegistry.getIfPresent(level.getServer());
            if(runtime!=null){if(cancellation)runtime.eventLedger().cancellationRequested(kind);else runtime.eventLedger().entered(kind);}}
    }
}
