package dev.ronova.coremod.capability.protection;

import dev.ronova.coremod.capability.AuthorityRuntimeState;
import dev.ronova.coremod.capability.CapabilityContext;
import dev.ronova.coremod.capability.ServerCapabilityModule;
import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.observation.TopologyObservation;
import dev.ronova.coremod.mixin.EntityStateAccessor;
import dev.ronova.coremod.mixin.PersistentEntityManagerAccessor;
import dev.ronova.coremod.mixin.ServerLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

public final class TopologyContinuityModule implements ServerCapabilityModule {
    @Override public Capability capability() { return Capability.TOPOLOGY_CONTINUITY; }

    @Override public void repair(CapabilityContext context) {
        if (!(context.player() instanceof ServerPlayer player)
            || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        AuthorityRuntimeState state = context.authorityState();
        if (state == null) {
            return;
        }
        state.bindAnchor(player);
        context.attempt(() -> repairTopology(
            player, level, context.topologyBroken(), context.observation()
        ));
    }

    private static void repairTopology(
        ServerPlayer player,
        ServerLevel level,
        boolean topologyBroken,
        TopologyObservation observation
    ) {
        ServerLevelAccessor access = (ServerLevelAccessor) level;
        PersistentEntitySectionManager<Entity> manager = access.ronova$getEntityManager();
        Entity indexed = (Entity) manager.getEntityGetter().get(player.getUUID());
        EntityStateAccessor state = (EntityStateAccessor) player;
        boolean callbackMissing = state.ronova$getLevelCallback() == EntityInLevelCallback.NULL;
        if (topologyBroken || indexed != player || callbackMissing) {
            ((PersistentEntityManagerAccessor) manager).ronova$getKnownUuids().remove(player.getUUID());
            manager.addNewEntityWithoutEvent(player);
            state.ronova$setRemovalReason(null);
            state.ronova$setAddedToWorld(true);
        }
        if (!access.ronova$getEntityTickList().contains(player)) {
            access.ronova$getEntityTickList().add(player);
        }
        if (!access.ronova$getPlayers().contains(player)) {
            access.ronova$getPlayers().add(player);
        }
        if (observation != null) {
            observation.observe(player);
        }
    }
}
