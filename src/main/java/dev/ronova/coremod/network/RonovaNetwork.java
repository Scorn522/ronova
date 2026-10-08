package dev.ronova.coremod.network;

import dev.ronova.coremod.client.ClientRuntime;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Stable network facade; channel, packets and services are independently owned. */
public final class RonovaNetwork {
    private RonovaNetwork() {}

    public static void initialize() {
        RonovaChannel.initialize();
    }

    public static void request(RonovaAction action) {
        ClientRuntime.queueAction(action);
    }

    public static void sendAction(
        UUID realmId,
        long sessionId,
        long sequence,
        RonovaAction action
    ) {
        RonovaChannel.sendToServer(new RonovaPackets.ActionRequest(
            realmId, sessionId, sequence, action
        ));
    }

    public static void requestBreak(BlockPos position) {
        if (position != null) {
            RonovaChannel.sendToServer(new RonovaPackets.BlockBreakRequest(position));
        }
    }

    public static void sendOwnerState(ServerPlayer player, boolean protectedState) {
        QuarantineSyncService.sendOwnerState(player);
    }

    public static void registerPlayer(ServerPlayer player) {
        QuarantineSyncService.registerPlayer(player);
    }

    public static int executeServerAction(
        ServerPlayer player,
        RonovaAction action,
        String origin
    ) {
        return ServerActionService.execute(player, action, origin);
    }

    public static int executeTargeted(ServerPlayer player, Entity target, String origin) {
        return ServerActionService.executeTargeted(player, target, origin);
    }

    public static void releasePlayer(ServerPlayer player) {
        ServerActionService.releasePlayer(player);
        QuarantineSyncService.releasePlayer(player);
    }

    public static void clearServerState(MinecraftServer server) {
        Set<UUID> released = QuarantineSyncService.clearServerState(server);
        ServerActionService.clearPlayers(server,released);
    }

    public static void broadcastQuarantineRemoval(MinecraftServer server, UUID uuid) {
        QuarantineSyncService.broadcastRemoval(server, uuid);
    }

    public static void broadcastQuarantineClear(MinecraftServer server) {
        QuarantineSyncService.broadcastClear(server);
    }
}
