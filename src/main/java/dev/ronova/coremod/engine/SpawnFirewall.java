package dev.ronova.coremod.engine;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.engine.spawn.SpawnAdmissionMetrics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import java.util.UUID;

/** Compatibility facade for the composed per-server spawn-admission runtime. */
public final class SpawnFirewall {
    private SpawnFirewall() {}

    public enum Boundary {
        CONSTRUCTION(false),
        NBT(false),
        FACTORY(false),
        JOIN(true),
        LEVEL(true),
        ENTITY_MANAGER(true),
        FIRST_TICK(true);

        private final boolean finalDecision;

        Boundary(boolean finalDecision) {
            this.finalDecision = finalDecision;
        }
    }

    public static void observeConstruction(Entity entity) {
        if (entity == null) {
            return;
        }
        try {
            if (entity.level() instanceof ServerLevel level) {
                var runtime = ServerRuntimeRegistry.getIfPresent(level.getServer());
                if (runtime != null && !runtime.authorityState().shuttingDown()) runtime.spawnAdmission().observeConstruction(entity);
            }
        } catch (Throwable ignored) {
            // Final admission boundaries remain independent.
        }
    }
    public static void observeFactoryResult(Entity entity) {
        if (entity == null) return;
        try {
            if (entity.level() instanceof ServerLevel level) {
                var runtime = ServerRuntimeRegistry.getIfPresent(level.getServer());
                if (runtime != null && !runtime.authorityState().shuttingDown()) runtime.spawnAdmission().observeFactoryResult(entity);
            }
        } catch (Throwable ignored) {
            // Factory observation does not replace the independent final admission gates.
        }
    }

    public static boolean reject(ServerLevel level, Entity entity, Boundary boundary) {
        if (level == null || boundary == null) return false;
        var admission = ServerRuntimeRegistry.runtime(level.getServer()).spawnAdmission();
        return boundary == Boundary.FIRST_TICK ? admission.rejectExecution(level, entity)
            : admission.reject(level, entity, boundary.finalDecision);
    }

    public static boolean rejectNbt(Level level, CompoundTag tag) {
        return level instanceof ServerLevel serverLevel
            && ServerRuntimeRegistry.runtime(serverLevel.getServer()).spawnAdmission()
                .rejectNbt(level, tag);
    }

    public static boolean matchesTombstone(Entity entity) {
        try {
            return entity != null && entity.level() instanceof ServerLevel level
                && ServerRuntimeRegistry.runtime(level.getServer()).spawnAdmission()
                    .matchesTombstone(entity);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static ExecutionResult execute(
        Entity entity,
        ServerPlayer owner,
        JudgmentScope requestedScope,
        double requestedRadius
    ) {
        return JudgmentExecutor.execute(entity, owner, requestedScope, requestedRadius);
    }

    static void beginExecution(Entity entity) {
        runtime(entity).beginExecution(entity);
    }

    static void commitExecution(Entity entity) {
        runtime(entity).commitExecution(entity);
    }

    static void invalidateAdmissionPasses() {
        ServerRuntimeRegistry.invalidateAdmissionPasses();
    }

    static void invalidateAdmissionPasses(MinecraftServer server) {
        if (server != null) {
            ServerRuntimeRegistry.runtime(server).spawnAdmission().invalidatePasses();
        }
    }

    public static void reassert(Entity entity) {
        if (entity != null && entity.level() instanceof ServerLevel level) {
            ServerRuntimeRegistry.runtime(level.getServer()).spawnAdmission().reassert(entity);
        }
    }

    public static void drainConstructed(MinecraftServer server) {
        if (server != null) {
            ServerRuntimeRegistry.runtime(server).spawnAdmission().drainConstructed();
        }
    }

    public static BudgetReceipt drainConstructed(
        MinecraftServer server, WorkBudget budget
    ) {
        if (server == null || budget == null) {
            return budget == null ? null
                : budget.finish(0L, 0L, 0L, 0L);
        }
        return ServerRuntimeRegistry.runtime(server).spawnAdmission()
            .drainConstructed(budget);
    }

    public static long rejectedAdmissions() {
        return SpawnAdmissionMetrics.rejectedAdmissions();
    }

    public static long nbtRejections() {
        return SpawnAdmissionMetrics.nbtRejections();
    }

    public static long rejectedAdmissions(MinecraftServer server) {
        return server == null ? 0L
            : ServerRuntimeRegistry.runtime(server).spawnAdmission().rejectedAdmissions();
    }

    public static long nbtRejections(MinecraftServer server) {
        return server == null ? 0L
            : ServerRuntimeRegistry.runtime(server).spawnAdmission().nbtRejections();
    }

    public static int passedObjects() {
        return ServerRuntimeRegistry.passedAdmissionObjects();
    }

    public static int rejectedObjects() {
        return ServerRuntimeRegistry.rejectedAdmissionObjects();
    }

    public static void clearIdentity(MinecraftServer server, UUID uuid) {
        if (server == null || uuid == null) {
            return;
        }
        ServerRuntimeRegistry.runtime(server).spawnAdmission().clearIdentity(uuid);
        JudgmentState.clearIdentity(server, uuid);
        QuarantineView.clearIdentity(server, uuid);
    }

    static void clearCachedIdentity(UUID uuid) {
        ServerRuntimeRegistry.clearAdmissionIdentity(uuid);
    }

    public static void onTombstonesCleared(MinecraftServer server) {
        if (server == null) {
            return;
        }
        var runtime = ServerRuntimeRegistry.runtime(server);
        runtime.spawnAdmission().clearCaches();
        runtime.terminalWatch().clear();
        JudgmentState.clearServer(server);
        QuarantineView.clearServer(server);
    }

    static void clearCachedServer(MinecraftServer server) {
        var runtime = ServerRuntimeRegistry.getIfPresent(server);
        if (runtime != null) runtime.spawnAdmission().clearCaches();
    }

    public static void clearServer(MinecraftServer server) {
        if (server == null) {
            return;
        }
        var runtime = ServerRuntimeRegistry.getIfPresent(server);
        if (runtime != null) runtime.spawnAdmission().close();
        JudgmentState.clearServer(server);
    }

    private static dev.ronova.coremod.engine.spawn.SpawnAdmissionRuntime runtime(Entity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) {
            throw new IllegalArgumentException("server entity is required");
        }
        return ServerRuntimeRegistry.runtime(level.getServer()).spawnAdmission();
    }

    public record ExecutionResult(boolean firstExecution, JudgmentMark mark) {
        private static final ExecutionResult NONE = new ExecutionResult(false, null);
    }
}
