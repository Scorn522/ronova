package dev.ronova.coremod.engine;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.util.WeakIdentitySet;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Owns the per-object terminal marker used by every execution lane. The weak
 * identity table is authoritative; Forge persistent data is only a diagnostic
 * serialization hint and can never recreate authority after a revocation.
 */
public final class JudgmentState {
    private static final String JUDGED_KEY = "ronova.judged";
    private static final WeakIdentitySet<Entity> EXECUTED = new WeakIdentitySet<>();

    private JudgmentState() {}

    public static boolean isJudged(Entity entity) {
        if (entity == null) {
            return false;
        }
        synchronized (EXECUTED) {
            return EXECUTED.contains(entity);
        }
    }

    /** Marks the object and returns true only for its first execution. */
    public static boolean mark(Entity entity) {
        if (entity == null) {
            return false;
        }
        synchronized (EXECUTED) {
            boolean first = !isJudged(entity);
            EXECUTED.add(entity);
            try {
                var data = entity.getPersistentData();
                if (data != null) {
                    data.putBoolean(JUDGED_KEY, true);
                }
            } catch (Throwable ignored) {
                // Persistence is optional; object identity is authoritative.
            }
            return first;
        }
    }

    /** Revokes both the in-memory terminal identity and its best-effort NBT hint. */
    public static void clearIdentity(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return;
        }
        synchronized (EXECUTED) {
            EXECUTED.removeIf(entity -> {
                if (!uuid.equals(entity.getUUID()) || !belongsTo(entity, server)) {
                    return false;
                }
                clearPersistentMarker(entity);
                return true;
            });
        }
        for (Entity entity : ServerRuntimeRegistry.runtime(server).observation().snapshot(uuid)) {
            if (belongsTo(entity, server)) {
                synchronized (EXECUTED) {
                    EXECUTED.remove(entity);
                }
                clearPersistentMarker(entity);
            }
        }
    }

    /** Clears every terminal object projection owned by one stopping or reset server. */
    public static void clearServer(MinecraftServer server) {
        if (server == null) {
            return;
        }
        synchronized (EXECUTED) {
            EXECUTED.removeIf(entity -> {
                if (!belongsTo(entity, server)) {
                    return false;
                }
                clearPersistentMarker(entity);
                return true;
            });
        }
    }

    private static void clearPersistentMarker(Entity entity) {
        try {
            var data = entity.getPersistentData();
            if (data != null) {
                data.remove(JUDGED_KEY);
            }
        } catch (Throwable ignored) {
            // The weak identity marker has already been revoked independently.
        }
    }

    private static boolean belongsTo(Entity entity, MinecraftServer server) {
        try {
            return entity != null && entity.level() instanceof ServerLevel level
                && level.getServer() == server;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
