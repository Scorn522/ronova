package dev.ronova.coremod.bootstrap;

import dev.ronova.coremod.authority.AuthorityIdentity;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.persistence.LocalDominionStore;
import dev.ronova.coremod.projection.ProjectionCoordinator;
import dev.ronova.coremod.runtime.DominionServerRuntime;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.UUID;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Process registry for explicitly composed per-server runtimes. */
public final class ServerRuntimeRegistry {
    private static final StartupRegistry<MinecraftServer, DominionServerRuntime> SERVERS = new StartupRegistry<>();

    private ServerRuntimeRegistry() {}

    public static DominionServerRuntime runtime(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        synchronized (SERVERS) {
            return SERVERS.getOrCreate(server, DominionCompositionRoot::create,
                value -> releaseResources(value, null));
        }
    }

    /** Lookup only: cleanup and diagnostics must never initialize a runtime. */
    public static DominionServerRuntime getIfPresent(MinecraftServer server) {
        return server == null ? null : SERVERS.get(server);
    }

    public static int anchorCount() {
        synchronized (SERVERS) {
            return SERVERS.values().stream()
                .mapToInt(value -> value.authorityState().anchorCount()).sum();
        }
    }
    /** Route only by each tracker's observed storage identity; never initialize a runtime or call under the registry lock. */
    public static void savedDataReplacing(net.minecraft.world.level.storage.DimensionDataStorage storage, String name) {
        if (storage == null || name == null) return;
        java.util.List<DominionServerRuntime> runtimes;
        synchronized (SERVERS) { runtimes = java.util.List.copyOf(SERVERS.values()); }
        Throwable failure = null;
        for (var runtime : runtimes) try { runtime.recoverySources().storageReplacing(storage, name); }
        catch (RuntimeException | LinkageError error) {
            if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
        }
        if (failure != null) org.slf4j.LoggerFactory.getLogger(ServerRuntimeRegistry.class)
            .warn("SavedData source invalidation encountered a bookkeeping failure", failure);
    }

    /** Read-only observation of the actual writer input; failure cannot change vanilla save behavior. */
    public static void savedDataEncoding(net.minecraft.world.level.saveddata.SavedData owner,net.minecraft.nbt.CompoundTag image) {
        java.util.List<DominionServerRuntime> runtimes;
        synchronized(SERVERS){runtimes=java.util.List.copyOf(SERVERS.values());}
        for(var runtime:runtimes)try {runtime.recoverySources().observeStorageEncoding(owner,image);}
        catch(RuntimeException|LinkageError unavailable) {
            org.slf4j.LoggerFactory.getLogger(ServerRuntimeRegistry.class).warn("SavedData encoding observation unavailable",unavailable);
        }
    }

    public static int connectionCount() {
        synchronized (SERVERS) {
            return SERVERS.values().stream()
                .mapToInt(value -> value.authorityState().connectionCount()).sum();
        }
    }
    public static void savedDataEncoded(net.minecraft.world.level.saveddata.SavedData owner,
        dev.ronova.coremod.observation.SavedDataSaveTrace.EncodedWrite encoded) {
        java.util.List<DominionServerRuntime> runtimes;
        synchronized(SERVERS){runtimes=java.util.List.copyOf(SERVERS.values());}
        for(var runtime:runtimes)runtime.recoverySources().confirmStorageEncoding(owner,encoded);
    }

    public static ServerPlayer protectedPlayer(Connection connection) {
        if (connection == null) {
            return null;
        }
        synchronized (SERVERS) {
            for (DominionServerRuntime runtime : SERVERS.values()) {
                var state = runtime.authorityState();
                var player = state.protectedPlayer(connection);
                if (player != null) return player;
            }
        }
        return null;
    }

    public static int passedAdmissionObjects() {
        synchronized (SERVERS) {
            return SERVERS.values().stream()
                .mapToInt(value -> value.spawnAdmission().passedObjects()).sum();
        }
    }

    public static int rejectedAdmissionObjects() {
        synchronized (SERVERS) {
            return SERVERS.values().stream()
                .mapToInt(value -> value.spawnAdmission().rejectedObjects()).sum();
        }
    }

    public static int terminalWatchEntries() {
        synchronized (SERVERS) {
            long total = 0L;
            for (DominionServerRuntime runtime : SERVERS.values()) {
                total += runtime.terminalWatch().size();
                if (total >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            }
            return (int) total;
        }
    }

    public static void invalidateAdmissionPasses() {
        synchronized (SERVERS) {
            SERVERS.values().forEach(value -> value.spawnAdmission().invalidatePasses());
        }
    }

    public static void clearAdmissionIdentity(UUID uuid) {
        if (uuid == null) {
            return;
        }
        synchronized (SERVERS) {
            SERVERS.values().forEach(value -> value.spawnAdmission().clearIdentity(uuid));
        }
    }

    public static void release(MinecraftServer server) {
        if (server == null) {
            return;
        }
        DominionServerRuntime runtime = SERVERS.remove(server);
        releaseResources(server, runtime);
    }

    private static void releaseResources(MinecraftServer server, DominionServerRuntime runtime) {
        java.util.List<Runnable> cleanup = new java.util.ArrayList<>();
        cleanup.add(() -> dev.ronova.coremod.runtime.RuntimeCoordinator.release(server));
        if (runtime != null) {
            cleanup.add(() -> runtime.authorityState().beginShutdown());
            cleanup.add(() -> runtime.ingressWork().close());
            cleanup.add(() -> runtime.recoverySources().flushShutdownResults());
            cleanup.add(() -> runtime.obligations().flushCheckpoint());
            cleanup.add(() -> runtime.terminalWatch().clear());
            cleanup.add(() -> runtime.observation().clear());
            cleanup.add(() -> runtime.spawnAdmission().close());
            cleanup.add(runtime::clearIncarnations);
            cleanup.add(() -> runtime.terminalPlans().clear());
            cleanup.add(() -> runtime.recoverySources().close());
            cleanup.add(() -> runtime.obligations().clear());
            cleanup.add(() -> runtime.authorityState().release());
        }
        cleanup.add(() -> AuthorityIdentity.clearServer(server));
        cleanup.add(() -> AuthorityView.release(server));
        cleanup.add(() -> ProjectionCoordinator.release(server));
        cleanup.add(() -> LocalDominionStore.release(server));
        Throwable failed = null;
        for (Runnable step : cleanup) {
            try { step.run(); }
            catch (RuntimeException | Error error) {
                if (failed == null) failed = error; else if (failed != error) failed.addSuppressed(error);
            }
        }
        if (failed instanceof RuntimeException error) throw error;
        if (failed instanceof Error error) throw error;
    }
}
