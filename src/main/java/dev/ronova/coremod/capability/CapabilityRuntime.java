package dev.ronova.coremod.capability;

import dev.ronova.coremod.capability.protection.LifecycleContinuityModule;
import dev.ronova.coremod.capability.protection.TopologyContinuityModule;
import dev.ronova.coremod.capability.recovery.BladeContinuityModule;
import dev.ronova.coremod.capability.recovery.NegativeEffectCleanseModule;
import dev.ronova.coremod.capability.recovery.VitalsRecoveryModule;
import dev.ronova.coremod.capability.utility.FlightModule;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.observation.TopologyObservation;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Explicit module dispatcher. It is the only tick/repair owner for server capabilities. */
public final class CapabilityRuntime {
    private final AuthorityRuntimeState authorityState;
    private final TopologyObservation observation;
    private final List<ServerCapabilityModule> modules;
    private final CapabilityDiagnostics diagnostics = new CapabilityDiagnostics();
    private final ThreadLocal<Boolean> repairing = ThreadLocal.withInitial(() -> false);

    private CapabilityRuntime(
        AuthorityRuntimeState authorityState,
        TopologyObservation observation,
        List<ServerCapabilityModule> modules
    ) {
        this.authorityState = authorityState;
        this.observation = observation;
        this.modules = List.copyOf(modules);
    }

    public static CapabilityRuntime standard(AuthorityRuntimeState authorityState) {
        return standard(authorityState, null);
    }

    public static CapabilityRuntime standard(
        AuthorityRuntimeState authorityState,
        TopologyObservation observation
    ) {
        return new CapabilityRuntime(authorityState, observation, List.of(
            new LifecycleContinuityModule(),
            new VitalsRecoveryModule(),
            new NegativeEffectCleanseModule(),
            new FlightModule(),
            new TopologyContinuityModule(),
            new BladeContinuityModule()
        ));
    }

    public void repair(Player player, DominionDecision.Authority authority) {
        if (player == null || authority == null || !authority.authorized() || repairing.get()) {
            return;
        }
        int tick = tick(player);
        if (authorityState != null) {
            authorityState.markRepaired(player, tick);
        }
        boolean topologyBroken = topologyBroken(player);
        repairing.set(true);
        try {
            for (ServerCapabilityModule module : modules) {
                if (!authority.capabilities().contains(module.capability())) {
                    continue;
                }
                CapabilityContext context = new CapabilityContext(
                    player, module.capability(), authorityState, observation,
                    diagnostics, topologyBroken
                );
                try {
                    module.repair(context);
                } catch (Throwable failure) {
                    diagnostics.record(module.capability());
                }
            }
        } finally {
            repairing.set(false);
        }
    }

    public void maintain(Player player, DominionDecision.Authority authority) {
        if (player == null) {
            return;
        }
        if (authorityState == null
            || authorityState.claimRoutineRepair(player, tick(player))) {
            repair(player, authority);
        }
    }

    public void maintainAnchors(java.util.function.Function<Player, DominionDecision.Authority> resolver) {
        if (authorityState == null || authorityState.shuttingDown()) {
            return;
        }
        Objects.requireNonNull(resolver, "resolver");
        for (ServerPlayer player : authorityState.anchors()) {
            if (player.server == authorityState.server()) {
                try {
                var anchor=authorityState.anchorId(player);
                if(anchor==null)continue;
                dev.ronova.coremod.capability.recovery.RegisteredOwnerIdentity.repair(player,anchor);
                var authority=resolver.apply(player);
                if(!authorityState.shuttingDown()&&authorityState.anchor(anchor)==player)repair(player,authority);
                } catch(RuntimeException|LinkageError failure) {
                    diagnostics.record(dev.ronova.coremod.contract.Capability.TOPOLOGY_CONTINUITY);
                }
            }
        }
    }

    public CapabilityDiagnostics diagnostics() { return diagnostics; }

    private static int tick(Player player) {
        return player instanceof ServerPlayer serverPlayer && serverPlayer.server != null
            ? serverPlayer.server.getTickCount() : player.tickCount;
    }

    private static boolean topologyBroken(Player player) {
        try {
            return player.isRemoved() || !player.isAddedToWorld();
        } catch (Throwable ignored) {
            return true;
        }
    }
}
