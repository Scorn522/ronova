package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.engine.ExecutionTargetPolicy;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Per-server weak raw graph. Terminal state is deliberately outside this type. */
public final class TopologyGraph {
    private final MinecraftServer owner;
    private final WeakTopologyCore<Entity, ResourceKey<Level>, MinecraftServer> core;

    public TopologyGraph(MinecraftServer owner) {
        this.owner = Objects.requireNonNull(owner, "owner");
        core = new WeakTopologyCore<>(
            owner,
            TopologyGraph::serverOf,
            TopologyGraph::levelKeyOf,
            Entity::getUUID,
            Entity::getId,
            TopologyGraph::shouldIndexRaw
        );
    }

    public void observe(Entity entity) {
        core.observe(entity);
    }

    public List<Entity> snapshot(ServerLevel level) {
        if (level == null || level.getServer() != owner) {
            return List.of();
        }
        return core.snapshot(level.dimension());
    }

    public List<Entity> snapshot(UUID uuid) {
        return core.snapshot(uuid);
    }

    public List<Entity> related(Entity source) {
        return core.related(source);
    }

    public BudgetReceipt maintainStale(WorkBudget budget) {
        if (budget == null) {
            throw new IllegalArgumentException("budget is required");
        }
        WeakTopologyCore.StaleDrain drain = core.maintainStale(budget);
        return budget.finish(
            drain.processed(), drain.deferred(), drain.carryIn(), drain.carryOut()
        );
    }

    public int size() {
        return core.size();
    }

    public void clear() {
        core.clear();
    }

    private static MinecraftServer serverOf(Entity entity) {
        return entity != null && entity.level() instanceof ServerLevel level
            ? level.getServer() : null;
    }

    private static ResourceKey<Level> levelKeyOf(Entity entity) {
        return entity != null && entity.level() instanceof ServerLevel level
            ? level.dimension() : null;
    }

    private static boolean shouldIndexRaw(Entity entity) {
        return ExecutionTargetPolicy.shouldIndex(entity);
    }
}

/** Generic package-private seam so weak graph behavior is testable without a Forge server. */
final class WeakTopologyCore<T, L, S> {
    private static final int STALE_TRANSFER_LIMIT =
        BudgetCategory.OBSERVATION.defaultCount();

    private final S owner;
    private final Function<? super T, ? extends S> ownerOf;
    private final Function<? super T, ? extends L> levelOf;
    private final Function<? super T, UUID> uuidOf;
    private final ToIntFunction<? super T> idOf;
    private final Predicate<? super T> shouldIndex;
    private ReferenceQueue<T> stale = new ReferenceQueue<>();
    private final ArrayDeque<RawEntityRef<T, L>> pendingStale = new ArrayDeque<>();
    private final Map<RawEntityRef<T, L>, RawEntityRef<T, L>> observed = new HashMap<>();
    private final Map<L, Set<RawEntityRef<T, L>>> byLevel = new HashMap<>();
    private final Map<UUID, Set<RawEntityRef<T, L>>> byUuid = new HashMap<>();
    private final Map<Integer, Set<RawEntityRef<T, L>>> byId = new HashMap<>();
    private final RawEntityLookup<T> lookup = new RawEntityLookup<>();

    WeakTopologyCore(
        S owner,
        Function<? super T, ? extends S> ownerOf,
        Function<? super T, ? extends L> levelOf,
        Function<? super T, UUID> uuidOf,
        ToIntFunction<? super T> idOf,
        Predicate<? super T> shouldIndex
    ) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.ownerOf = Objects.requireNonNull(ownerOf, "ownerOf");
        this.levelOf = Objects.requireNonNull(levelOf, "levelOf");
        this.uuidOf = Objects.requireNonNull(uuidOf, "uuidOf");
        this.idOf = Objects.requireNonNull(idOf, "idOf");
        this.shouldIndex = Objects.requireNonNull(shouldIndex, "shouldIndex");
    }

    synchronized boolean observe(T value) {
        if (value == null || ownerOf.apply(value) != owner || !shouldIndex.test(value)) {
            return false;
        }
        L level = levelOf.apply(value);
        if (level == null) {
            return false;
        }
        UUID uuid = Objects.requireNonNull(uuidOf.apply(value), "uuid");
        int id = idOf.applyAsInt(value);
        RawEntityRef<T, L> existing = findReference(value);
        if (existing != null) {
            if (existing.keysMatch(uuid, id, level)) {
                return false;
            }
            forgetReference(existing);
        }

        RawEntityRef<T, L> reference = new RawEntityRef<>(value, stale, uuid, id, level);
        observed.put(reference, reference);
        byLevel.computeIfAbsent(level, ignored -> new HashSet<>()).add(reference);
        byUuid.computeIfAbsent(uuid, ignored -> new HashSet<>()).add(reference);
        if (id != 0) {
            byId.computeIfAbsent(id, ignored -> new HashSet<>()).add(reference);
        }
        return true;
    }

    synchronized List<T> snapshot(L level) {
        if (level == null) {
            return List.of();
        }
        Set<RawEntityRef<T, L>> bucket = byLevel.get(level);
        if (bucket == null || bucket.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>(bucket.size());
        for (RawEntityRef<T, L> reference : List.copyOf(bucket)) {
            T value = resolve(reference);
            if (value != null && level.equals(levelOf.apply(value))) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    synchronized List<T> snapshot(UUID uuid) {
        if (uuid == null) {
            return List.of();
        }
        Set<RawEntityRef<T, L>> bucket = byUuid.get(uuid);
        if (bucket == null || bucket.isEmpty()) {
            return List.of();
        }
        List<T> result = new ArrayList<>(bucket.size());
        for (RawEntityRef<T, L> reference : List.copyOf(bucket)) {
            T value = resolve(reference);
            if (value != null && uuid.equals(uuidOf.apply(value))) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    synchronized List<T> related(T source) {
        if (source == null || ownerOf.apply(source) != owner) {
            return List.of();
        }
        UUID sourceUuid = Objects.requireNonNull(uuidOf.apply(source), "uuid");
        int sourceId = idOf.applyAsInt(source);
        L sourceLevel = Objects.requireNonNull(levelOf.apply(source), "level");
        Set<RawEntityRef<T, L>> references = new HashSet<>();
        addBucket(references, byUuid.get(sourceUuid));
        if (sourceId != 0) {
            addBucket(references, byId.get(sourceId));
        }
        RawEntityRef<T, L> original = findReference(source);
        if (original != null) {
            addBucket(references, byUuid.get(original.uuid()));
            if (original.entityId() != 0) {
                addBucket(references, byId.get(original.entityId()));
            }
        }

        List<T> result = new ArrayList<>(references.size());
        for (RawEntityRef<T, L> reference : references) {
            T candidate = resolve(reference);
            if (candidate != null && candidate != source
                && sourceLevel.equals(levelOf.apply(candidate))) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }

    synchronized int size() {
        return observed.size();
    }

    synchronized StaleDrain maintainStale(WorkBudget budget) {
        Objects.requireNonNull(budget, "budget");
        // Start the time gate before touching the ReferenceQueue. Discovery is
        // part of this worker's cost rather than invisible pre-budget work.
        budget.hasTimeRemaining();
        // Discover at most one bounded processing window plus one look-ahead.
        // ReferenceQueue has no constant-time exact size, so carry telemetry is
        // the exact discovered backlog, never an O(observed) global estimate.
        transferStaleReferences(STALE_TRANSFER_LIMIT + 1);
        long carryIn = pendingStale.size();
        long processed = 0L;
        long deferred = 0L;
        int inspected = 0;
        while (inspected < STALE_TRANSFER_LIMIT) {
            RawEntityRef<T, L> reference = pendingStale.pollFirst();
            if (reference == null) {
                break;
            }
            inspected++;
            if (!isCurrentReference(reference)) {
                continue;
            }
            if (!budget.tryConsumeCount(1)) {
                pendingStale.addFirst(reference);
                budget.deferCount(1);
                deferred = 1L;
                break;
            }
            forgetReference(reference);
            processed++;
        }
        return new StaleDrain(processed, deferred, carryIn, pendingStale.size());
    }

    synchronized void clear() {
        observed.clear();
        byLevel.clear();
        byUuid.clear();
        byId.clear();
        pendingStale.clear();
        // Old references keep their old queue; dropping it makes generation reset O(1).
        stale = new ReferenceQueue<>();
    }

    synchronized boolean enqueueCollectionForTest(T value) {
        RawEntityRef<T, L> reference = findReference(value);
        if (reference == null) {
            return false;
        }
        reference.clear();
        return reference.enqueue();
    }

    synchronized boolean simulateCollectionForTest(T value) {
        if (!enqueueCollectionForTest(value)) {
            return false;
        }
        drainStaleReferences();
        return true;
    }

    private T resolve(RawEntityRef<T, L> reference) {
        T value = reference.get();
        if (value == null || ownerOf.apply(value) != owner || !shouldIndex.test(value)) {
            forgetReference(reference);
            return null;
        }
        L level = levelOf.apply(value);
        if (level == null) {
            forgetReference(reference);
            return null;
        }
        UUID uuid = Objects.requireNonNull(uuidOf.apply(value), "uuid");
        int id = idOf.applyAsInt(value);
        if (!reference.keysMatch(uuid, id, level)) {
            forgetReference(reference);
            observe(value);
        }
        return value;
    }

    private boolean isCurrentReference(RawEntityRef<T, L> reference) {
        return observed.get(reference) == reference;
    }

    /** Reuses one non-owning identity key; every caller already holds this core's monitor. */
    private RawEntityRef<T, L> findReference(T value) {
        lookup.set(value);
        try {
            return observed.get(lookup);
        } finally {
            lookup.clear();
        }
    }

    /** Moves at most {@code maxTransfers} queue entries into the owned deque. */
    @SuppressWarnings("unchecked")
    private void transferStaleReferences(int maxTransfers) {
        if (maxTransfers <= 0) {
            throw new IllegalArgumentException("maxTransfers must be positive");
        }
        for (int transferred = 0; transferred < maxTransfers; transferred++) {
            RawEntityRef<T, L> reference = (RawEntityRef<T, L>) stale.poll();
            if (reference == null) {
                return;
            }
            enqueueReaped(reference);
        }
    }

    private void enqueueReaped(RawEntityRef<T, L> reference) {
        // A queue entry is transferred at most once. If direct lookup cleanup
        // already removed it, the latched entry is intentionally ignored.
        if (reference.markReaped() && isCurrentReference(reference)) {
            pendingStale.addLast(reference);
        }
    }

    private void drainStaleReferences() {
        transferStaleReferences(STALE_TRANSFER_LIMIT);
        int inspected = 0;
        while (inspected++ <= STALE_TRANSFER_LIMIT) {
            RawEntityRef<T, L> reference = pendingStale.pollFirst();
            if (reference == null) {
                break;
            }
            if (isCurrentReference(reference)) {
                forgetReference(reference);
            }
        }
    }

    static record StaleDrain(
        long processed,
        long deferred,
        long carryIn,
        long carryOut
    ) {}

    private void forgetReference(RawEntityRef<T, L> reference) {
        if (observed.remove(reference) == null) {
            return;
        }
        removeFromBucket(byLevel, reference.levelKey(), reference);
        removeFromBucket(byUuid, reference.uuid(), reference);
        if (reference.entityId() != 0) {
            removeFromBucket(byId, reference.entityId(), reference);
        }
    }

    private static <T> void addBucket(Set<T> destination, Set<T> source) {
        if (source != null) {
            destination.addAll(source);
        }
    }

    private static <K, T> void removeFromBucket(Map<K, Set<T>> index, K key, T value) {
        Set<T> bucket = index.get(key);
        if (bucket == null) {
            return;
        }
        bucket.remove(value);
        if (bucket.isEmpty()) {
            index.remove(key);
        }
    }
}
