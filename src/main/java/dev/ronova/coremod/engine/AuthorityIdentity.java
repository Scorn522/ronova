package dev.ronova.coremod.engine;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * Source-compatible bridge for the 7.14B move to
 * {@code authority.AuthorityIdentity}. It owns no state.
 */
@Deprecated(forRemoval = false)
public final class AuthorityIdentity {
    private AuthorityIdentity() {}

    public static void bind(UUID uuid) {
        dev.ronova.coremod.authority.AuthorityIdentity.bind(uuid);
    }

    public static void bind(MinecraftServer server, UUID uuid) {
        dev.ronova.coremod.authority.AuthorityIdentity.bind(server, uuid);
    }

    public static void unbind(UUID uuid) {
        dev.ronova.coremod.authority.AuthorityIdentity.unbind(uuid);
    }

    public static void unbind(MinecraftServer server, UUID uuid) {
        dev.ronova.coremod.authority.AuthorityIdentity.unbind(server, uuid);
    }

    public static boolean contains(UUID uuid) {
        return dev.ronova.coremod.authority.AuthorityIdentity.contains(uuid);
    }

    public static boolean contains(MinecraftServer server, UUID uuid) {
        return dev.ronova.coremod.authority.AuthorityIdentity.contains(server, uuid);
    }

    public static void replace(UUID uuid, boolean protectedState) {
        dev.ronova.coremod.authority.AuthorityIdentity.replace(uuid, protectedState);
    }

    public static void replace(
        MinecraftServer server,
        UUID uuid,
        boolean protectedState
    ) {
        dev.ronova.coremod.authority.AuthorityIdentity.replace(server, uuid, protectedState);
    }

    public static void clearServer(MinecraftServer server) {
        dev.ronova.coremod.authority.AuthorityIdentity.clearServer(server);
    }

    public static int serverCount() {
        return dev.ronova.coremod.authority.AuthorityIdentity.serverCount();
    }

    public static void clear() {
        dev.ronova.coremod.authority.AuthorityIdentity.clear();
    }
}
