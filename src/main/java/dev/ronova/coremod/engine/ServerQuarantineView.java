package dev.ronova.coremod.engine;

import dev.ronova.coremod.adapter.MinecraftSubjectAdapter;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.projection.ProjectionBundle;
import dev.ronova.coremod.util.WeakIdentitySet;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * 7.14C server-side half of the quarantine view.
 *
 * <p>This half owns the per-server identity registry and reads presence from the
 * projection bundle published for the current epoch. Both the quarantine flag and
 * the owner immunity that can override it come from that one bundle, so a decision
 * can never mix an admission match from one epoch with an owner set from another.</p>
 *
 * <p>It stays read-only with respect to Authority: it queries the composed view and
 * never binds, tags or clears anything.</p>
 */
final class ServerQuarantineView {
    private static final Map<MinecraftServer, WeakIdentitySet<Entity>> OBJECTS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private ServerQuarantineView() {}

    static void register(MinecraftServer server, Entity entity) {
        if (server == null || entity == null) {
            return;
        }
        synchronized (OBJECTS) {
            OBJECTS.computeIfAbsent(server, ignored -> new WeakIdentitySet<>()).add(entity);
        }
    }

    static boolean registered(MinecraftServer server, Entity entity) {
        synchronized (OBJECTS) {
            WeakIdentitySet<Entity> objects = OBJECTS.get(server);
            return objects != null && objects.contains(entity);
        }
    }

    static int objectCount(MinecraftServer server) {
        synchronized (OBJECTS) {
            WeakIdentitySet<Entity> objects = OBJECTS.get(server);
            return objects == null ? 0 : objects.size();
        }
    }

    static void clearServer(MinecraftServer server) {
        OBJECTS.remove(server);
    }

    static void clearIdentity(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return;
        }
        synchronized (OBJECTS) {
            WeakIdentitySet<Entity> objects = OBJECTS.get(server);
            if (objects != null) {
                objects.removeIf(entity -> uuid.equals(entity.getUUID()));
            }
        }
    }

    /** Resolves the raw quarantine flag for an entity in the published projection. */
    static boolean quarantined(
        MinecraftServer server, Entity entity, SubjectIdentity subject,
        ProjectionBundle projections
    ) {
        return registered(server, entity) || projections.admission().matches(subject);
    }

    /**
     * Adjudicates server-side presence. Immunity is not short-circuited here: the
     * projection's presence policy owns it, so an owner or Dominion-self subject
     * stays visible even if a future caller reaches this method directly.
     */
    static boolean matches(Entity entity, ServerLevel level) {
        MinecraftServer server = level.getServer();
        ProjectionBundle projections = ServerRuntimeRegistry.runtime(server).projection();
        SubjectIdentity subject = MinecraftSubjectAdapter.from(entity);
        return !projections.presence().decide(
            subject,
            projections.snapshot().epoch(),
            quarantined(server, entity, subject, projections)
        ).present();
    }
}
