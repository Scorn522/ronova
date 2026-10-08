package dev.ronova.coremod.engine;

import dev.ronova.coremod.authority.AuthorityBinder;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.capability.AuthorityRuntimeState;
import dev.ronova.coremod.capability.CapabilityRuntime;
import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.contract.DominionDecision;
import java.util.Objects;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Per-server physical enforcement for decisions obtained from one AuthorityView. */
public final class AuthorityEnforcement {
    private static final AuthorityEnforcement CLIENT = new AuthorityEnforcement(
        null, AuthorityView.client(), null, CapabilityRuntime.standard(null)
    );

    private final MinecraftServer server;
    private final AuthorityView view;
    private final AuthorityRuntimeState authorityState;
    private final CapabilityRuntime capabilities;

    public AuthorityEnforcement(
        MinecraftServer server,
        AuthorityView view,
        AuthorityRuntimeState authorityState,
        CapabilityRuntime capabilities
    ) {
        this.server = server;
        this.view = Objects.requireNonNull(view, "view");
        this.authorityState = authorityState;
        this.capabilities = Objects.requireNonNull(capabilities, "capabilities");
        if (server != null && authorityState == null) {
            throw new IllegalArgumentException("server enforcement requires authority state");
        }
    }

    /** Returns the process-local client enforcement adapter. */
    public static AuthorityEnforcement client() {
        return CLIENT;
    }

    public AuthorityView view() {
        return view;
    }

    public void repair(Player player) {
        if (!belongsToRuntime(player)) {
            return;
        }
        DominionDecision.Authority authority = view.resolve(player);
        if (authority != null && authority.authorized()) {
            repair(player, authority);
        }
    }

    public void maintain(Player player) {
        if (!belongsToRuntime(player)) {
            return;
        }
        DominionDecision.Authority authority = view.resolve(player);
        if (authority == null || !authority.authorized()) {
            return;
        }
        capabilities.maintain(player, authority);
    }

    public void maintainAnchors() {
        if (server != null) {
            capabilities.maintainAnchors(view::resolve);
        }
    }

    public boolean shouldProtectRemoval(Object value, Object rawReason) {
        if (!(value instanceof Player player) || !belongsToRuntime(player)) {
            return false;
        }
        DominionDecision.Authority authority = view.resolve(player);
        if (!allows(authority, Capability.LIFECYCLE_CONTINUITY)) {
            return false;
        }
        Entity.RemovalReason reason = rawReason instanceof Entity.RemovalReason candidate
            ? candidate : null;
        if (reason == Entity.RemovalReason.CHANGED_DIMENSION
            || reason == Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer && isClosing(serverPlayer)) {
            AuthorityBinder.releaseObject(serverPlayer);
            return false;
        }
        repair(player, authority);
        return true;
    }

    public boolean shouldRejectDisconnect(ServerPlayer player) {
        if (!(belongsToRuntime(player)) || server == null) {
            return false;
        }
        DominionDecision.Authority authority = view.resolve(player);
        if (!server.isRunning() || authorityState.shuttingDown()
            || !allows(authority, Capability.CONNECTION_CONTINUITY)) {
            return false;
        }
        repair(player, authority);
        return true;
    }

    public boolean shouldRejectConnectionDisconnect(
        Connection connection,
        ServerPlayer player
    ) {
        if (connection == null || !belongsToRuntime(player) || server == null) {
            return false;
        }
        DominionDecision.Authority authority = view.resolve(player);
        if (!connection.isConnected() || !server.isRunning() || authorityState.shuttingDown()
            || !allows(authority, Capability.CONNECTION_CONTINUITY)) {
            return false;
        }
        repair(player, authority);
        return true;
    }

    public boolean isClosing(ServerPlayer player) {
        if (player == null || server == null || player.server != server) {
            return true;
        }
        try {
            return !server.isRunning() || authorityState.shuttingDown()
                || player.connection == null || player.connection.connection == null
                || !player.connection.connection.isConnected();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void repair(Player player, DominionDecision.Authority authority) {
        capabilities.repair(player, authority);
    }

    private boolean belongsToRuntime(Player player) {
        if (player == null) {
            return false;
        }
        if (server == null) {
            return !(player instanceof ServerPlayer serverPlayer)
                || serverPlayer.server == null;
        }
        return player instanceof ServerPlayer serverPlayer && serverPlayer.server == server;
    }

    private static boolean allows(
        DominionDecision.Authority authority,
        Capability capability
    ) {
        return authority != null && authority.authorized()
            && capability != null && authority.capabilities().contains(capability);
    }
}
