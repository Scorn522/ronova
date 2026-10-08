package dev.ronova.coremod.authority;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.network.RonovaNetwork;
import dev.ronova.coremod.root.api.DominionCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Explicit Authority mutation and binding boundary.
 *
 * <p>All operations in this class may change runtime state, entity metadata,
 * anchors, connections, client projection, or durable ownership. It must never
 * be called from a high-frequency query predicate; callers that only need an
 * answer must use {@link AuthorityView}.</p>
 */
public final class AuthorityBinder {
    private AuthorityBinder() {}

    /** Performs the explicit administrative owner mutation and activates its projection. */
    public static boolean grant(ServerPlayer player) {
        if (!validServerPlayer(player)) {
            return false;
        }
        var runtime = ServerRuntimeRegistry.runtime(player.server);
        runtime.commands().commit(new DominionCommand.BindOwner(player.getUUID()));
        DominionDecision.Authority authority = runtime.authorityView().resolve(player);
        if (authority == null || !authority.authorized()) {
            clearProjection(player, false);
            return false;
        }
        activate(player, authority, true);
        return true;
    }

    /** Restores runtime bindings only when the Root-approved projection owns the player. */
    public static boolean restore(ServerPlayer player) {
        if (!validServerPlayer(player)) {
            return false;
        }
        var runtime = ServerRuntimeRegistry.runtime(player.server);
        DominionDecision.Authority authority = runtime.authorityView().resolve(player);
        if (authority == null || !authority.authorized()) {
            clearProjection(player, false);
            return false;
        }
        activate(player, authority, false);
        return true;
    }

    /** Performs the explicit durable unbind and removes the runtime projection. */
    public static boolean unbind(ServerPlayer player) {
        if (!validServerPlayer(player)) {
            return false;
        }
        var runtime = ServerRuntimeRegistry.runtime(player.server);
        runtime.commands().commit(new DominionCommand.UnbindOwner(player.getUUID()));
        clearProjection(player, true);
        return true;
    }

    /** Restores pre-14B runtime markers for the explicit LEGACY rollback mode. */
    static void restoreLegacyProjection(
        ServerPlayer player,
        DominionDecision.Authority authority
    ) {
        if (validServerPlayer(player) && authority != null && authority.authorized()) {
            synchronizeRuntime(player, authority, false, false);
        }
    }

    /** Releases only the current physical object anchor; durable ownership remains. */
    public static void releaseObject(ServerPlayer player) {
        if (validServerPlayer(player)) {
            ServerRuntimeRegistry.runtime(player.server).authorityState().releaseObject(player);
        }
    }

    /** Removes an invalid runtime projection without submitting a durable command. */
    public static void clearProjection(ServerPlayer player, boolean notifyClient) {
        if (player == null) {
            return;
        }
        AuthorityIdentity.unbind(player.server, player.getUUID());
        writeBoundTag(player, false);
        if (player.server != null) {
            ServerRuntimeRegistry.runtime(player.server).authorityState().clearProjection(player);
        }
        if (notifyClient) {
            RonovaNetwork.sendOwnerState(player, false);
        }
    }

    private static void activate(
        ServerPlayer player,
        DominionDecision.Authority authority,
        boolean forceRepair
    ) {
        synchronizeRuntime(player, authority, forceRepair, true);
    }

    private static void synchronizeRuntime(
        ServerPlayer player,
        DominionDecision.Authority authority,
        boolean forceRepair,
        boolean maintainCapabilities
    ) {
        var runtime = ServerRuntimeRegistry.runtime(player.server);
        AuthorityIdentity.bind(player.server, player.getUUID());
        writeBoundTag(player, true);
        ServerPlayer previous = runtime.authorityState().bindAnchor(player);
        runtime.authorityState().bindConnection(player);
        runtime.observation().observe(player);
        if (previous != player) {
            RonovaNetwork.sendOwnerState(player, true);
        }
        if (!maintainCapabilities) {
            return;
        }
        if (forceRepair || previous != player) {
            runtime.capabilities().repair(player, authority);
        } else {
            runtime.capabilities().maintain(player, authority);
        }
    }

    private static boolean validServerPlayer(ServerPlayer player) {
        return player != null && player.server != null;
    }

    private static void writeBoundTag(Player player, boolean value) {
        try {
            var data = player == null ? null : player.getPersistentData();
            if (data == null) {
                return;
            }
            if (value) {
                data.putBoolean(AuthorityView.BOUND_TAG, true);
            } else {
                data.remove(AuthorityView.BOUND_TAG);
            }
        } catch (Throwable ignored) {
            // Root ownership and the immutable projection remain authoritative.
        }
    }
}
