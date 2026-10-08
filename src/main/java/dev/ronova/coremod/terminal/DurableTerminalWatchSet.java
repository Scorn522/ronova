package dev.ronova.coremod.terminal;

import dev.ronova.coremod.adapter.MinecraftSubjectAdapter;
import dev.ronova.coremod.authority.AuthorityView;
import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.engine.JudgmentMark;
import dev.ronova.coremod.engine.JudgmentScope;
import dev.ronova.coremod.engine.SpawnFirewall;
import dev.ronova.coremod.observation.TopologyObservation;
import dev.ronova.coremod.projection.AdmissionProjection;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Per-server 7.16 terminal worker. Logical membership is reconstructed from the
 * immutable Tombstone projection and therefore has no retry-count expiry;
 * physical references remain weak and all work is count/time budgeted.
 */
public final class DurableTerminalWatchSet implements TerminalWatchSet {
    private static final int[] WARM_DELAYS = {2, 5, 10, 20};
    private static final int COLD_DELAY = 100;
    private static final int TRANSIENT_ABSENT_LIMIT = 4;
    private static final Comparator<Scheduled> SCHEDULE_ORDER =
        Comparator.comparingLong(Scheduled::nextTick).thenComparing(Scheduled::uuid);

    private final MinecraftServer server;
    private final Supplier<AdmissionProjection> projection;
    private final AuthorityView authorityView;
    private final TopologyObservation observation;
    private final Map<UUID, Entry> entries = new LinkedHashMap<>();
    private final EnumMap<Phase, TreeSet<Scheduled>> lanes = new EnumMap<>(Phase.class);
    private DominionEpoch observedProjectionEpoch;
    private List<JudgmentMark> pendingSeeds = List.of();
    private int seedCursor;
    private long scheduledNotifications;
    private long attemptNotifications;
    private long completionNotifications;
    private final dev.ronova.coremod.budget.FairLaneCursor fairLanes=new dev.ronova.coremod.budget.FairLaneCursor(4);

    public DurableTerminalWatchSet(
        MinecraftServer server,
        Supplier<AdmissionProjection> projection,
        AuthorityView authorityView,
        TopologyObservation observation
    ) {
        this.server = Objects.requireNonNull(server, "server");
        this.projection = Objects.requireNonNull(projection, "projection");
        this.authorityView = Objects.requireNonNull(authorityView, "authorityView");
        this.observation = Objects.requireNonNull(observation, "observation");
        for (Phase phase : Phase.values()) {
            lanes.put(phase, new TreeSet<>(SCHEDULE_ORDER));
        }
    }

    @Override
    public synchronized void track(JudgmentMark mark, Entity incarnation) {
        if (mark == null || mark.uuid() == null) {
            return;
        }
        Entry entry = entries.get(mark.uuid());
        if (entry == null) {
            entry = Entry.durable(mark);
            entries.put(mark.uuid(), entry);
        } else {
            entry.mark = mark;
            entry.subject = subject(mark);
            entry.durableBacked = true;
            entry.tombstoneId = mark.uuid();
        }
        if (belongsToServer(incarnation)) {
            entry.reference = new WeakReference<>(incarnation);
            entry.subject = MinecraftSubjectAdapter.from(incarnation);
            entry.absentCycles = 0;
            observeSchedule(entry, server.getTickCount() + 1L);
        } else if (entry.scheduled == null) {
            schedule(entry, Phase.COLD, server.getTickCount());
        }
    }

    @Override
    public synchronized void observeIncarnation(Entity incarnation) {
        if (!belongsToServer(incarnation) || authorityView.isProtected(incarnation)) {
            return;
        }
        SubjectIdentity subject = MinecraftSubjectAdapter.from(incarnation);
        if (!projection.get().matches(subject)) {
            return;
        }
        UUID uuid = incarnation.getUUID();
        Entry entry = entries.get(uuid);
        if (entry == null) {
            JudgmentMark mark = JudgmentMark.from(incarnation, JudgmentScope.IDENTITY, 0.0D);
            entry = Entry.transientIncarnation(mark, subject, incarnation);
            entries.put(uuid, entry);
        } else {
            entry.reference = new WeakReference<>(incarnation);
            entry.subject = subject;
        }
        entry.absentCycles = 0;
        observeSchedule(entry, server.getTickCount() + 1L);
    }

    @Override
    public synchronized BudgetReceipt maintain(WorkBudget budget) {
        Objects.requireNonNull(budget, "budget");
        AdmissionProjection current = projection.get();
        refreshSeed(current);
        long carryIn = backlog();
        long processed = 0L;
        long deferred = 0L;
        long now = server.getTickCount();

        while (true) {
            int ready=seedCursor<pendingSeeds.size()?8:0;
            for(Phase phase:Phase.values()){var lane=lanes.get(phase);if(!lane.isEmpty()&&lane.first().nextTick()<=now)ready|=1<<phase.ordinal();}
            int selected=fairLanes.peek(ready);if(selected<0)break;
            if (!budget.tryConsumeCount(1)) {
                budget.deferCount(1);
                deferred = 1L;
                break;
            }
            fairLanes.consumed(selected);
            if(selected==3) {
                // Finish an immutable seed pass across projection churn, but never reinsert a cleared/stale mark.
                var mark=current.mark(pendingSeeds.get(seedCursor++).uuid());if(mark!=null)trackSeed(mark,now);
            } else {
                Entry entry=pollDue(Phase.values()[selected],now);if(entry!=null)maintainEntry(entry,current,now);
            }
            processed++;
        }
        if (seedCursor >= pendingSeeds.size()) {
            pendingSeeds = List.of();
            seedCursor = 0;
        }
        return budget.finish(processed, deferred, carryIn, backlog());
    }

    @Override public void observeScheduled(LegacyRetryKey key, int attempt, long nextTick) {}

    @Override public void observeAttempt(
        LegacyRetryKey key, int attempt, boolean entityResolved, boolean reasserted
    ) {}

    @Override public void observeCompleted(LegacyRetryKey key, Completion completion) {}

    @Override
    public synchronized WatchSnapshot snapshot(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        Entry entry = entries.get(uuid);
        if (entry == null) {
            return new WatchSnapshot(Mode.DURABLE_TOMBSTONE, Map.of());
        }
        LegacyRetryKey key = new LegacyRetryKey(uuid, dimension(entry.mark.dimension()));
        List<TerminalWatchSet.Scheduled> schedules = entry.scheduled == null
            ? List.of() : List.of(new TerminalWatchSet.Scheduled(
                boundedAttempt(entry.attempts), entry.nextTick
            ));
        List<Attempt> attempts = entry.attempts == 0L
            ? List.of() : List.of(new Attempt(
                boundedAttempt(entry.attempts - 1L), entry.lastResolved, entry.lastReasserted
            ));
        return new WatchSnapshot(Mode.DURABLE_TOMBSTONE, Map.of(
            key, new EntrySnapshot(key, schedules, attempts, List.of())
        ));
    }

    @Override
    public synchronized TerminalWatchMetrics metrics() {
        return new TerminalWatchMetrics(
            Mode.DURABLE_TOMBSTONE, entries.size(), 0,
            scheduledNotifications, attemptNotifications, completionNotifications
        );
    }

    @Override
    public synchronized int size() {
        return entries.size();
    }

    @Override
    public synchronized void clearIdentity(UUID uuid) {
        remove(uuid, Completion.TOMBSTONE_CLEARED);
    }

    @Override
    public synchronized void clear() {
        entries.clear();
        for (TreeSet<Scheduled> lane : lanes.values()) {
            lane.clear();
        }
        pendingSeeds = List.of();
        seedCursor = 0;
        observedProjectionEpoch = null;
        scheduledNotifications = 0L;
        attemptNotifications = 0L;
        completionNotifications = 0L;
        fairLanes.reset();
    }

    private void refreshSeed(AdmissionProjection current) {
        if (observedProjectionEpoch != null
            && observedProjectionEpoch.equals(current.epoch())) {
            return;
        }
        if(seedCursor<pendingSeeds.size())return;
        observedProjectionEpoch = current.epoch();
        pendingSeeds = current.marks();
        seedCursor = 0;
    }

    private void trackSeed(JudgmentMark mark, long now) {
        if (mark == null || mark.uuid() == null) {
            return;
        }
        Entry entry = entries.get(mark.uuid());
        if (entry == null) {
            entry = Entry.durable(mark);
            entries.put(mark.uuid(), entry);
            schedule(entry, Phase.COLD, now);
            return;
        }
        entry.mark = mark;
        entry.durableBacked = true;
        entry.tombstoneId = mark.uuid();
        if (entry.scheduled == null) {
            schedule(entry, Phase.COLD, now);
        }
    }

    private void maintainEntry(Entry entry, AdmissionProjection current, long now) {
        if (!logicallyRetained(entry, current)) {
            remove(entry.mark.uuid(), Completion.TOMBSTONE_CLEARED);
            return;
        }
        Set<Entity> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
        Entity referenced = entry.reference.get();
        if (belongsToServer(referenced)) {
            candidates.add(referenced);
        }
        for (Entity candidate : observation.snapshot(entry.mark.uuid())) {
            if (belongsToServer(candidate)) {
                candidates.add(candidate);
            }
        }
        ServerLevel level = server.getLevel(dimension(entry.mark.dimension()));
        Entity direct = level == null ? null : level.getEntity(entry.mark.uuid());
        if (belongsToServer(direct)) {
            candidates.add(direct);
        }

        boolean resolved = false;
        boolean reasserted = false;
        for (Entity candidate : candidates) {
            boolean needsRepair = physicallyActive(candidate);
            boolean periodicIndexReview = entry.durableBacked && entry.phase == Phase.COLD;
            if ((!needsRepair && !periodicIndexReview) || authorityView.isProtected(candidate)) {
                continue;
            }
            SubjectIdentity candidateSubject = MinecraftSubjectAdapter.from(candidate);
            if (!entry.durableBacked && !current.matches(candidateSubject)) {
                continue;
            }
            resolved = true;
            entry.reference = new WeakReference<>(candidate);
            entry.subject = candidateSubject;
            // Presence is written by the validated plan; a weak observation cannot authorize it.
            SpawnFirewall.reassert(candidate);
            reasserted |= needsRepair || physicallyActive(candidate);
        }
        entry.attempts = increment(entry.attempts);
        attemptNotifications = increment(attemptNotifications);
        entry.lastResolved = resolved;
        entry.lastReasserted = reasserted;

        if (reasserted) {
            entry.absentCycles = 0;
            schedule(entry, Phase.HOT, now + 1L);
            return;
        }
        // Convergence is not disappearance. Retain the weak reference for later drift;
        // it still cannot keep the physical entity alive after its real owners release it.
        entry.absentCycles = Math.min(Integer.MAX_VALUE, entry.absentCycles + 1);
        if (!entry.durableBacked && entry.absentCycles >= TRANSIENT_ABSENT_LIMIT) {
            remove(entry.mark.uuid(), Completion.ENTITY_UNRESOLVED);
            return;
        }
        if (entry.absentCycles <= WARM_DELAYS.length) {
            schedule(
                entry, Phase.WARM,
                now + WARM_DELAYS[Math.max(0, entry.absentCycles - 1)]
            );
        } else {
            schedule(entry, Phase.COLD, now + COLD_DELAY);
        }
    }

    private boolean logicallyRetained(Entry entry, AdmissionProjection current) {
        return entry.durableBacked
            ? current.hasTombstone(entry.tombstoneId)
            : current.matches(entry.subject);
    }

    private Entry pollDue(Phase phase,long now) {
            TreeSet<Scheduled> lane = lanes.get(phase);
            Scheduled scheduled = lane.isEmpty() ? null : lane.first();
            if (scheduled == null || scheduled.nextTick() > now) {
                return null;
            }
            lane.remove(scheduled);
            Entry entry = entries.get(scheduled.uuid());
            if (entry != null) {
                entry.scheduled = null;
                return entry;
            }
        return null;
    }

    private void schedule(Entry entry, Phase phase, long nextTick) {
        if (entry.scheduled != null) {
            lanes.get(entry.phase).remove(entry.scheduled);
        }
        entry.phase = Objects.requireNonNull(phase, "phase");
        entry.nextTick = Math.max(0L, nextTick);
        entry.scheduled = new Scheduled(entry.mark.uuid(), entry.nextTick);
        lanes.get(phase).add(entry.scheduled);
        scheduledNotifications = increment(scheduledNotifications);
    }
    private void observeSchedule(Entry entry,long nextTick) {
        schedule(entry,Phase.HOT,entry.scheduled==null?nextTick:Math.min(entry.nextTick,nextTick));
    }

    private void remove(UUID uuid, Completion completion) {
        Entry removed = uuid == null ? null : entries.remove(uuid);
        if (removed == null) {
            return;
        }
        if (removed.scheduled != null) {
            lanes.get(removed.phase).remove(removed.scheduled);
        }
        completionNotifications = increment(completionNotifications);
    }

    private long backlog() {
        return (long) entries.size() + Math.max(0, pendingSeeds.size() - seedCursor);
    }

    private boolean belongsToServer(Entity entity) {
        try {
            return entity != null && entity.level() instanceof ServerLevel level
                && level.getServer() == server;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean physicallyActive(Entity entity) {
        try {
            if (entity == null) return false;
            var runtime = dev.ronova.coremod.bootstrap.ServerRuntimeRegistry.getIfPresent(server);
            if(runtime==null)return true;
            int mask=0x8063|dev.ronova.coremod.policy.TerminalActionPolicy.Step.REMOVED_RECORDS.mask();
            try(var observation=runtime.enforcement().beginObservation(mask)) {
                if(observation==null||!dev.ronova.coremod.integrity.RawAccessorDispatch.matches(entity,Entity.class,
                    dev.ronova.coremod.mixin.EntityStateAccessor.class))return true;
                var state=(dev.ronova.coremod.mixin.EntityStateAccessor)entity;
                var rawLevel=state.ronova$getLevel();
                if(!(rawLevel instanceof ServerLevel level)||level.getServer()!=server
                    ||server.getLevel(level.dimension())!=level)return true;
                boolean active=!dev.ronova.coremod.engine.TerminalLifecycle.controlledStatePresent(entity)
                    ||runtime.terminalPlans().indices().pending(entity)
                    ||dev.ronova.coremod.engine.TerminalIndexRepair.published(entity);
                return active||state.ronova$getLevel()!=rawLevel||!observation.finish();
            }
        } catch (Throwable ignored) {
            return entity != null;
        }
    }

    private static SubjectIdentity subject(JudgmentMark mark) {
        return new SubjectIdentity(
            mark.uuid(), mark.type(), "unknown", mark.dimension(),
            mark.x(), mark.y(), mark.z(), false
        );
    }

    private static ResourceKey<Level> dimension(String value) {
        ResourceLocation location = ResourceLocation.tryParse(Objects.toString(value, ""));
        return location == null ? Level.OVERWORLD
            : ResourceKey.create(Registries.DIMENSION, location);
    }

    private static int boundedAttempt(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private static long increment(long value) {
        return value == Long.MAX_VALUE ? value : value + 1L;
    }

    private enum Phase {
        HOT,
        WARM,
        COLD
    }

    private record Scheduled(UUID uuid, long nextTick) {}

    private static final class Entry {
        private JudgmentMark mark;
        private SubjectIdentity subject;
        private boolean durableBacked;
        private UUID tombstoneId;
        private WeakReference<Entity> reference;
        private Phase phase = Phase.COLD;
        private Scheduled scheduled;
        private long nextTick;
        private long attempts;
        private int absentCycles;
        private boolean lastResolved;
        private boolean lastReasserted;

        private static Entry durable(JudgmentMark mark) {
            Entry entry = new Entry();
            entry.mark = mark;
            entry.subject = subject(mark);
            entry.durableBacked = true;
            entry.tombstoneId = mark.uuid();
            entry.reference = new WeakReference<>(null);
            return entry;
        }

        private static Entry transientIncarnation(
            JudgmentMark mark, SubjectIdentity subject, Entity incarnation
        ) {
            Entry entry = new Entry();
            entry.mark = mark;
            entry.subject = subject;
            entry.reference = new WeakReference<>(incarnation);
            return entry;
        }
    }
}
