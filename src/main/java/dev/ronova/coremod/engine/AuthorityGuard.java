package dev.ronova.coremod.engine;

import dev.ronova.coremod.authority.AuthorityBinder;
import dev.ronova.coremod.authority.AuthorityIdentity;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.contract.Capability;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Source/Agent compatibility facade for the split Authority API.
 *
 * <p>Production event, Mixin, discovery, capability and action paths use the
 * composed {@link AuthorityView}, {@link AuthorityEnforcement}, and
 * {@link AuthorityBinder} directly.</p>
 */
@Deprecated(forRemoval = false)
public final class AuthorityGuard {
    public static final String BOUND_TAG = AuthorityView.BOUND_TAG;

    private AuthorityGuard() {}

    public static void grant(ServerPlayer player) {
        AuthorityBinder.grant(player);
    }

    public static boolean restore(ServerPlayer player) {
        return AuthorityBinder.restore(player);
    }

    public static void unbind(ServerPlayer player) {
        AuthorityBinder.unbind(player);
    }

    public static void releaseObject(ServerPlayer player) {
        AuthorityBinder.releaseObject(player);
    }

    /** Pure compatibility query used by the bootstrap-safe Agent predicate. */
    public static boolean isProtectedObject(Object value) {
        return value instanceof Player player && view(player).isProtected(player);
    }

    /** Pure compatibility query. */
    public static boolean isProtected(Entity entity) {
        return entity instanceof Player player && view(player).isProtected(player);
    }

    /** Pure compatibility capability query. */
    public static boolean hasCapability(Player player, Capability capability) {
        return player != null && enforcement(player).view().hasCapability(player, capability);
    }

    public static boolean shouldProtectRemoval(Object value, Object rawReason) {
        return value instanceof Player player
            && enforcement(player).shouldProtectRemoval(value, rawReason);
    }

    public static boolean shouldRejectDisconnect(ServerPlayer player) {
        return player != null && player.server != null
            && ServerRuntimeRegistry.runtime(player.server).authorityEnforcement()
                .shouldRejectDisconnect(player);
    }

    public static boolean shouldRejectConnectionDisconnect(Connection connection) {
        if (connection == null) {
            return false;
        }
        ServerPlayer player = ServerRuntimeRegistry.protectedPlayer(connection);
        return player != null && player.server != null
            && ServerRuntimeRegistry.runtime(player.server).authorityEnforcement()
                .shouldRejectConnectionDisconnect(connection, player);
    }

    public static void beginServerShutdown(MinecraftServer server) {
        if (server != null) {
            ServerRuntimeRegistry.runtime(server).authorityState().beginShutdown();
        }
    }

    public static void releaseServer(MinecraftServer server) {
        if (server != null) {
            AuthorityIdentity.clearServer(server);
            ServerRuntimeRegistry.runtime(server).authorityState().release();
        }
    }

    public static void repair(Player player) {
        enforcement(player).repair(player);
    }

    public static void maintain(Player player) {
        enforcement(player).maintain(player);
    }

    public static void maintainAnchors(MinecraftServer server) {
        if (server != null) {
            ServerRuntimeRegistry.runtime(server).authorityEnforcement().maintainAnchors();
        }
    }

    public static int anchorCount() {
        return ServerRuntimeRegistry.anchorCount();
    }

    public static int connectionCount() {
        return ServerRuntimeRegistry.connectionCount();
    }

    private static AuthorityView view(Player player) {
        return enforcement(player).view();
    }

    private static AuthorityEnforcement enforcement(Player player) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.server != null) {
            return ServerRuntimeRegistry.runtime(serverPlayer.server).authorityEnforcement();
        }
        return AuthorityEnforcement.client();
    }
}
