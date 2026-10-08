package dev.ronova.coremod.authority;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;

/**
 * Ephemeral identity cache used only by the runtime binding adapters.
 *
 * <p>The client cache is process-local because a client has one active local
 * authority surface. Server caches are partitioned by {@link MinecraftServer}
 * so two servers in one process cannot observe each other's compatibility
 * markers. Neither cache is durable truth; Root-approved projections remain
 * authoritative.</p>
 */
public final class AuthorityIdentity {
    private static final Set<UUID> CLIENT = ConcurrentHashMap.newKeySet();
    private static final Map<MinecraftServer, Set<UUID>> SERVERS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private AuthorityIdentity() {}

    public static void bind(UUID uuid) {
        if (uuid != null) {
            CLIENT.add(uuid);
        }
    }

    public static void bind(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return;
        }
        synchronized (SERVERS) {
            SERVERS.computeIfAbsent(server, ignored -> ConcurrentHashMap.newKeySet())
                .add(uuid);
        }
    }

    public static void unbind(UUID uuid) {
        if (uuid != null) {
            CLIENT.remove(uuid);
        }
    }

    public static void unbind(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return;
        }
        synchronized (SERVERS) {
            Set<UUID> identities = SERVERS.get(server);
            if (identities == null) {
                return;
            }
            identities.remove(uuid);
            if (identities.isEmpty()) {
                SERVERS.remove(server);
            }
        }
    }

    public static boolean contains(UUID uuid) {
        return uuid != null && CLIENT.contains(uuid);
    }

    public static boolean contains(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return false;
        }
        synchronized (SERVERS) {
            Set<UUID> identities = SERVERS.get(server);
            return identities != null && identities.contains(uuid);
        }
    }

    public static void replace(UUID uuid, boolean protectedState) {
        if (protectedState) {
            bind(uuid);
        } else {
            unbind(uuid);
        }
    }

    public static void replace(
        MinecraftServer server,
        UUID uuid,
        boolean protectedState
    ) {
        if (protectedState) {
            bind(server, uuid);
        } else {
            unbind(server, uuid);
        }
    }

    public static void clearServer(MinecraftServer server) {
        if (server != null) {
            synchronized (SERVERS) {
                SERVERS.remove(server);
            }
        }
    }

    public static int serverCount() {
        synchronized (SERVERS) {
            return SERVERS.size();
        }
    }

    public static void clear() {
        CLIENT.clear();
        synchronized (SERVERS) {
            SERVERS.clear();
        }
    }
}
