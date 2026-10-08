package dev.ronova.coremod.engine;

import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.observation.TopologyObservation;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Discovers eligible roots only; policy commitment and execution live elsewhere. */
public final class TargetDiscovery {
    private TargetDiscovery() {}

    /** Compatibility entry point; the server runtime supplies the read view. */
    public static Discovery ray(ServerPlayer owner, double range) {
        return ray(owner, range, authorityView(owner));
    }

    public static Discovery ray(
        ServerPlayer owner,
        double range,
        AuthorityView authorityView
    ) {
        if (owner == null || owner.server == null || authorityView == null) {
            return new Discovery(List.of(), 0);
        }
        FrameBudgetGovernor budget = discoveryBudget(owner);
        long startedNanos = System.nanoTime();
        int candidateCount = 0;
        try {
            ServerLevel level = owner.serverLevel();
            double boundedRange = Math.min(JudgmentEngine.PRIMARY_RANGE, Math.max(0.0D, range));
            Vec3 eye = owner.getEyePosition();
            Vec3 end = eye.add(owner.getLookAngle().scale(boundedRange));
            AABB broadPhase = new AABB(eye, end).inflate(2.0D);
            Set<Entity> candidates = identitySet();
            candidates.addAll(level.getEntities(
                owner, broadPhase, entity -> canJudge(entity, authorityView)
            ));
            TopologyObservation observation = ServerRuntimeRegistry.runtime(owner.server)
                .observation();
            candidates.addAll(observation.snapshot(level));
            candidates.removeIf(candidate -> !canJudge(candidate, authorityView)
                || candidate == owner || candidate.level() != level);
            candidateCount = candidates.size();

            BlockHitResult blockHit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner
            ));
            double obstructionDistance = blockHit.getType() == HitResult.Type.MISS
                ? boundedRange * boundedRange : eye.distanceToSqr(blockHit.getLocation());
            Entity best = null;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (Entity candidate : candidates) {
                if (!candidate.getBoundingBox().inflate(2.0D).intersects(broadPhase)) {
                    continue;
                }
                AABB bounds = candidate.getBoundingBox().inflate(
                    Math.max(0.3F, candidate.getPickRadius())
                );
                Optional<Vec3> intersection = bounds.clip(eye, end);
                if (intersection.isPresent()) {
                    double distance = eye.distanceToSqr(intersection.get());
                    if (distance <= obstructionDistance && distance < bestDistance) {
                        best = candidate;
                        bestDistance = distance;
                    }
                }
            }
            return new Discovery(best == null ? List.of() : List.of(best), candidateCount);
        } finally {
            recordDiscoveryMeasurement(budget, candidateCount, startedNanos);
        }
    }

    /** Compatibility entry point; the server runtime supplies the read view. */
    public static Discovery area(ServerPlayer owner, double radius) {
        return area(owner, radius, authorityView(owner));
    }

    public static Discovery area(
        ServerPlayer owner,
        double radius,
        AuthorityView authorityView
    ) {
        if (owner == null || owner.server == null || authorityView == null) {
            return new Discovery(List.of(), 0);
        }
        FrameBudgetGovernor budget = discoveryBudget(owner);
        long startedNanos = System.nanoTime();
        int candidateCount = 0;
        try {
            double boundedRadius = Math.min(
                JudgmentEngine.AREA_RADIUS, Math.max(0.0D, radius)
            );
            double ownerX = owner.getX();
            double ownerY = owner.getY();
            double ownerZ = owner.getZ();
            Set<Entity> targets = identitySet();
            ServerLevel level = owner.serverLevel();
            targets.addAll(level.getEntities(
                owner,
                owner.getBoundingBox().inflate(boundedRadius),
                entity -> canJudge(entity, authorityView)
                    && SphericalRange.contains(
                        ownerX, ownerY, ownerZ,
                        entity.getX(), entity.getY(), entity.getZ(), boundedRadius
                    )
            ));
            TopologyObservation observation = ServerRuntimeRegistry.runtime(owner.server)
                .observation();
            for (Entity candidate : observation.snapshot(level)) {
                if (candidate != owner && candidate.level() == level
                    && canJudge(candidate, authorityView)
                    && SphericalRange.contains(
                        ownerX, ownerY, ownerZ,
                        candidate.getX(), candidate.getY(), candidate.getZ(), boundedRadius
                    )) {
                    targets.add(candidate);
                }
            }
            candidateCount = targets.size();
            return new Discovery(List.copyOf(targets), candidateCount);
        } finally {
            recordDiscoveryMeasurement(budget, candidateCount, startedNanos);
        }
    }

    /** Compatibility entry point; the server runtime supplies the read view. */
    public static Discovery loaded(ServerPlayer owner) {
        return loaded(owner, authorityView(owner));
    }

    public static Discovery loaded(
        ServerPlayer owner,
        AuthorityView authorityView
    ) {
        if (owner == null || owner.server == null || authorityView == null) {
            return new Discovery(List.of(), 0);
        }
        FrameBudgetGovernor budget = discoveryBudget(owner);
        long startedNanos = System.nanoTime();
        int candidateCount = 0;
        try {
            ServerLevel level = owner.serverLevel();
            Set<Entity> targets = identitySet();
            for (Object value : level.getAllEntities()) {
                if (value instanceof Entity entity) {
                    targets.add(entity);
                }
            }
            targets.addAll(ServerRuntimeRegistry.runtime(owner.server).observation().snapshot(level));
            targets.removeIf(target -> !canJudge(target, authorityView)
                || target == owner || target.level() != level);
            candidateCount = targets.size();
            return new Discovery(List.copyOf(targets), candidateCount);
        } finally {
            recordDiscoveryMeasurement(budget, candidateCount, startedNanos);
        }
    }

    private static boolean canJudge(Entity entity, AuthorityView authorityView) {
        return ExecutionTargetPolicy.shouldJudge(entity)
            && !authorityView.isProtected(entity);
    }

    private static FrameBudgetGovernor discoveryBudget(ServerPlayer owner) {
        return ServerRuntimeRegistry.runtime(owner.server).frameBudgetGovernor();
    }

    /** Records discovery latency and candidate volume without gating the query. */
    private static void recordDiscoveryMeasurement(
        FrameBudgetGovernor budget,
        int candidates,
        long startedNanos
    ) {
        if (budget == null || startedNanos == 0L) {
            return;
        }
        long elapsedUs = Math.max(0L, System.nanoTime() - startedNanos) / 1_000L;
        try {
            long candidateCount = Math.max(0L, candidates);
            budget.recordMeasurement(
                BudgetCategory.DISCOVERY,
                candidateCount,
                candidateCount,
                0L,
                0L,
                0L,
                elapsedUs
            );
        } catch (RuntimeException ignored) {
            // Discovery result must remain synchronous when telemetry fails.
        }
    }

    private static AuthorityView authorityView(ServerPlayer owner) {
        if (owner == null || owner.server == null) {
            return AuthorityView.client();
        }
        return ServerRuntimeRegistry.runtime(owner.server).authorityView();
    }

    private static Set<Entity> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    public record Discovery(List<Entity> roots, int candidates) {
        public Discovery {
            roots = roots == null ? List.of() : List.copyOf(roots);
            if (candidates < roots.size()) {
                throw new IllegalArgumentException("candidate count cannot be smaller than roots");
            }
        }
    }
}
