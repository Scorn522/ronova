package dev.ronova.coremod.engine;

import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.WorkBudget;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Weak construction-time topology with secondary level, UUID and entity-ID
 * indexes. Related-object lookup never scans the global observed population.
 */
public final class TopologyIndex {
    private static final int STALE_TRANSFER_LIMIT =
        BudgetCategory.OBSERVATION.defaultCount();
    private static final Object LOCK = new Object();
    private static ReferenceQueue<Entity> STALE = new ReferenceQueue<>();
    private static final ArrayDeque<EntityReference> PENDING_STALE = new ArrayDeque<>();
    private static boolean staleQueueSaturated;
    private static final Map<EntityReference, Observation> OBSERVED = new HashMap<>();
    private static final Map<Level, Set<EntityReference>> BY_LEVEL = new WeakHashMap<>();
    private static final Map<UUID, Set<EntityReference>> BY_UUID = new HashMap<>();
    private static final Map<Integer, Set<EntityReference>> BY_ID = new HashMap<>();

    private TopologyIndex() {}

    public static void observe(Entity entity) {
        if (!shouldLegacyTrack(entity)) {
            return;
        }
        synchronized (LOCK) {
            EntityReference lookup = new EntityReference(entity);
            if (OBSERVED.containsKey(lookup)) {
                return;
            }
            EntityReference reference = new EntityReference(entity, STALE);
            Observation observation = new Observation(
                reference,
                entity.getUUID(),
                entity.getId(),
                new WeakReference<>(entity.level())
            );
            OBSERVED.put(reference, observation);
            BY_LEVEL.computeIfAbsent(entity.level(), ignored -> new HashSet<>()).add(reference);
            BY_UUID.computeIfAbsent(observation.uuid(), ignored -> new HashSet<>()).add(reference);
            if (observation.id() != 0) {
                BY_ID.computeIfAbsent(observation.id(), ignored -> new HashSet<>()).add(reference);
            }
        }
    }

    public static List<Entity> snapshot(ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        synchronized (LOCK) {
            Set<EntityReference> references = BY_LEVEL.get(level);
            if (references == null || references.isEmpty()) {
                return List.of();
            }
            List<Entity> result = new ArrayList<>(references.size());
            for (EntityReference reference : List.copyOf(references)) {
                Entity entity = reference.get();
                if (entity == null || entity.level() != level
                    || !shouldLegacyTrack(entity)) {
                    forgetReference(reference);
                } else {
                    result.add(entity);
                }
            }
            return result;
        }
    }

    public static List<Entity> related(Entity source) {
        if (source == null) {
            return List.of();
        }
        synchronized (LOCK) {
            Set<EntityReference> references = new HashSet<>();
            addBucket(references, BY_UUID.get(source.getUUID()));
            if (source.getId() != 0) {
                addBucket(references, BY_ID.get(source.getId()));
            }

            Observation original = OBSERVED.get(new EntityReference(source));
            if (original != null) {
                addBucket(references, BY_UUID.get(original.uuid()));
                if (original.id() != 0) {
                    addBucket(references, BY_ID.get(original.id()));
                }
            }

            List<Entity> result = new ArrayList<>(references.size());
            for (EntityReference reference : references) {
                Entity candidate = reference.get();
                if (candidate == null || !shouldLegacyTrack(candidate)) {
                    forgetReference(reference);
                } else if (candidate != source && candidate.level() == source.level()) {
                    result.add(candidate);
                }
            }
            return result;
        }
    }

    /** Returns every still-live object observed under one UUID without a global scan. */
    public static List<Entity> snapshot(UUID uuid) {
        if (uuid == null) {
            return List.of();
        }
        synchronized (LOCK) {
            Set<EntityReference> references = BY_UUID.get(uuid);
            if (references == null || references.isEmpty()) {
                return List.of();
            }
            List<Entity> result = new ArrayList<>(references.size());
            for (EntityReference reference : List.copyOf(references)) {
                Entity entity = reference.get();
                if (entity == null || !shouldLegacyTrack(entity)) {
                    forgetReference(reference);
                } else {
                    result.add(entity);
                }
            }
            return result;
        }
    }

    public static void forget(Entity entity) {
        if (entity == null) {
            return;
        }
        synchronized (LOCK) {
            Observation observation = OBSERVED.get(new EntityReference(entity));
            if (observation != null) {
                forgetReference(observation.reference());
            }
        }
    }

    public static int size() {
        synchronized (LOCK) {
            return OBSERVED.size();
        }
    }

    /** Bounded legacy weak-reference maintenance used only through the rollback adapter. */
    public static BudgetReceipt maintainStale(WorkBudget budget) {
        if (budget == null) {
            throw new IllegalArgumentException("budget is required");
        }
        synchronized (LOCK) {
            transferStaleReferences(STALE_TRANSFER_LIMIT);
            long carryIn = staleBacklog();
            long processed = 0L;
            long deferred = 0L;
            int inspected = 0;
            while (inspected < STALE_TRANSFER_LIMIT) {
                EntityReference reference = PENDING_STALE.pollFirst();
                if (reference == null) {
                    break;
                }
                inspected++;
                if (!isCurrentReference(reference)) {
                    continue;
                }
                if (!budget.tryConsumeCount(1)) {
                    PENDING_STALE.addFirst(reference);
                    budget.deferCount(1);
                    deferred = 1L;
                    break;
                }
                forgetReference(reference);
                processed++;
            }
            // One bounded look-ahead records arrivals without draining the queue.
            transferStaleReferences(1);
            return budget.finish(
                processed, deferred, carryIn, staleBacklog()
            );
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            OBSERVED.clear();
            BY_LEVEL.clear();
            BY_UUID.clear();
            BY_ID.clear();
            PENDING_STALE.clear();
            staleQueueSaturated = false;
            // Old references keep their old queue; dropping it makes generation reset O(1).
            STALE = new ReferenceQueue<>();
        }
    }

    private static void addBucket(Set<EntityReference> destination, Set<EntityReference> source) {
        if (source != null) {
            destination.addAll(source);
        }
    }

    /** Keeps the 7.13 terminal filter private to the rollback adapter. */
    private static boolean shouldLegacyTrack(Entity entity) {
        return ExecutionTargetPolicy.shouldIndex(entity) && !JudgmentState.isJudged(entity);
    }

    /** Moves a bounded queue prefix and keeps one probe for saturation telemetry. */
    @SuppressWarnings("unchecked")
    private static void transferStaleReferences(int maxTransfers) {
        if (maxTransfers <= 0) {
            throw new IllegalArgumentException("maxTransfers must be positive");
        }
        for (int transferred = 0; transferred < maxTransfers; transferred++) {
            EntityReference reference = (EntityReference) STALE.poll();
            if (reference == null) {
                staleQueueSaturated = false;
                return;
            }
            enqueueReaped(reference);
        }

        EntityReference probe = (EntityReference) STALE.poll();
        if (probe == null) {
            staleQueueSaturated = false;
            return;
        }
        enqueueReaped(probe);
        staleQueueSaturated = true;
    }

    private static void enqueueReaped(EntityReference reference) {
        if (reference.markReaped() && isCurrentReference(reference)) {
            PENDING_STALE.addLast(reference);
        }
    }

    private static boolean isCurrentReference(EntityReference reference) {
        Observation observation = OBSERVED.get(reference);
        return observation != null && observation.reference() == reference;
    }

    /** Exact below the transfer cap; otherwise a conservative active-index bound. */
    private static long staleBacklog() {
        return staleQueueSaturated
            ? Math.max((long) PENDING_STALE.size(), (long) OBSERVED.size())
            : PENDING_STALE.size();
    }

    private static void forgetReference(EntityReference reference) {
        Observation observation = OBSERVED.remove(reference);
        if (observation == null) {
            return;
        }
        Level level = observation.level().get();
        if (level != null) {
            removeFromBucket(BY_LEVEL, level, observation.reference());
        }
        removeFromBucket(BY_UUID, observation.uuid(), observation.reference());
        if (observation.id() != 0) {
            removeFromBucket(BY_ID, observation.id(), observation.reference());
        }
    }

    private static <K> void removeFromBucket(
        Map<K, Set<EntityReference>> index,
        K key,
        EntityReference reference
    ) {
        Set<EntityReference> bucket = index.get(key);
        if (bucket == null) {
            return;
        }
        bucket.remove(reference);
        if (bucket.isEmpty()) {
            index.remove(key);
        }
    }

    private record Observation(
        EntityReference reference,
        UUID uuid,
        int id,
        WeakReference<Level> level
    ) {}

    private static final class EntityReference extends WeakReference<Entity> {
        private final int identityHash;
        private boolean reaped;

        private EntityReference(Entity entity) {
            super(entity);
            identityHash = System.identityHashCode(entity);
        }

        private EntityReference(Entity entity, ReferenceQueue<Entity> queue) {
            super(entity, queue);
            identityHash = System.identityHashCode(entity);
        }

        synchronized boolean reaped() {
            return reaped;
        }

        /** Returns true once when this reference leaves its registered queue. */
        synchronized boolean markReaped() {
            if (reaped) {
                return false;
            }
            reaped = true;
            return true;
        }

        @Override
        public int hashCode() {
            return identityHash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof EntityReference reference)) {
                return false;
            }
            Entity entity = get();
            return entity != null && entity == reference.get();
        }
    }
}
