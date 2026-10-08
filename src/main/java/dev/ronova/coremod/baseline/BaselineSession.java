package dev.ronova.coremod.baseline;

import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.engine.JudgmentEngine;
import dev.ronova.coremod.engine.QuarantineView;
import dev.ronova.coremod.network.RonovaAction;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;

/** Fixed-capacity primitive storage for one server baseline run. */
final class BaselineSession {
    enum Phase {
        WARMUP,
        MEASUREMENT,
        COMPLETE
    }

    private final long createdEpochMillis = System.currentTimeMillis();
    private final long createdNanos = System.nanoTime();
    private final long warmupSeconds;
    private final long measurementSeconds;
    private final long warmupDeadlineNanos;
    private final long measurementDeadlineNanos;
    private final int sampleCapacity;
    private final boolean rawObservationEnabled;
    private final String rawObservationFlagSource;
    private final TickSamples ticks;
    private final EnumMap<RonovaAction, ActionSamples> actions =
        new EnumMap<>(RonovaAction.class);
    private final AdmissionSamples admission;
    private final ProjectionSamples projections;
    private final RetrySamples retries;
    private final RootCommitSamples rootCommits;
    private final MutableRange tombstones = new MutableRange();
    private final MutableRange candidates = new MutableRange();
    private final MutableRange topology = new MutableRange();
    private final MutableRange quarantineObjects = new MutableRange();
    private final MutableRange passiveWatchEntries = new MutableRange();
    private final AtomicBoolean archiveScheduled = new AtomicBoolean();

    private volatile Phase phase = Phase.WARMUP;
    private long measurementStartedNanos;
    private long completedNanos;
    private long tickStartedNanos;
    private long terminalInvocations;
    private long passiveWatchNotificationFailures;
    private boolean tickHookObserved;
    private String completionReason = "LIVE_BASELINE_PENDING";

    BaselineSession(long warmupSeconds, long measurementSeconds, int sampleCapacity) {
        this(warmupSeconds, measurementSeconds, sampleCapacity, false, "DEFAULT");
    }

    BaselineSession(
        long warmupSeconds,
        long measurementSeconds,
        int sampleCapacity,
        boolean rawObservationEnabled,
        String rawObservationFlagSource
    ) {
        this.warmupSeconds = warmupSeconds;
        this.measurementSeconds = measurementSeconds;
        this.sampleCapacity = sampleCapacity;
        this.rawObservationEnabled = rawObservationEnabled;
        this.rawObservationFlagSource = rawObservationFlagSource == null
            ? "DEFAULT" : rawObservationFlagSource;
        warmupDeadlineNanos = saturatingAdd(
            createdNanos, secondsToNanos(warmupSeconds)
        );
        measurementDeadlineNanos = saturatingAdd(
            warmupDeadlineNanos, secondsToNanos(measurementSeconds)
        );
        ticks = new TickSamples(sampleCapacity);
        for (RonovaAction action : RonovaAction.values()) {
            actions.put(action, new ActionSamples(sampleCapacity));
        }
        admission = new AdmissionSamples(sampleCapacity);
        projections = new ProjectionSamples(sampleCapacity);
        retries = new RetrySamples(sampleCapacity);
        rootCommits = new RootCommitSamples(sampleCapacity);
    }

    boolean measuring() {
        return phase == Phase.MEASUREMENT;
    }

    boolean tickHead(long nowNanos) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return false;
            }
            tickHookObserved = true;
            boolean completed = advance(nowNanos);
            if (completed) {
                return true;
            }
            tickStartedNanos = phase == Phase.MEASUREMENT ? nowNanos : 0L;
            return false;
        }
    }

    boolean pulse(long nowNanos) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return false;
            }
            return advance(nowNanos);
        }
    }

    boolean tickTail(MinecraftServer server, long nowNanos) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return false;
            }
            boolean completed = advance(nowNanos);
            if (completed) {
                return true;
            }
            long started = tickStartedNanos;
            tickStartedNanos = 0L;
            if (phase != Phase.MEASUREMENT || started == 0L || server == null) {
                return false;
            }
            var runtime = ServerRuntimeRegistry.runtime(server);
            int tombstoneCount = runtime.projection().admission().tombstoneCount();
            int topologySize = runtime.observation().size();
            int watchEntries = runtime.terminalWatch().size();
            int quarantineCount = QuarantineView.objectCount(server);
            int queuedRetries = JudgmentEngine.pendingRetries();
            ticks.add(
                elapsedUs(started, nowNanos), tombstoneCount, topologySize,
                quarantineCount, queuedRetries
            );
            tombstones.add(tombstoneCount);
            topology.add(topologySize);
            quarantineObjects.add(quarantineCount);
            passiveWatchEntries.add(watchEntries);
            passiveWatchNotificationFailures = Math.max(
                passiveWatchNotificationFailures,
                runtime.passiveWatchNotificationFailures()
            );
            return false;
        }
    }

    boolean recordAction(
        long nowNanos,
        RonovaAction action,
        long elapsedUs,
        int candidateCount,
        int judgedCount,
        int tombstoneCount,
        int topologySize,
        int quarantineCount
    ) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return false;
            }
            boolean completed = advance(nowNanos);
            if (phase != Phase.MEASUREMENT) {
                return completed;
            }
            actions.get(action).add(
                elapsedUs, candidateCount, judgedCount, tombstoneCount,
                topologySize, quarantineCount
            );
            candidates.add(candidateCount);
            tombstones.add(tombstoneCount);
            topology.add(topologySize);
            quarantineObjects.add(quarantineCount);
            return completed;
        }
    }

    boolean recordAdmission(
        long nowNanos, boolean nbt, boolean rejected, long elapsedUs
    ) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return false;
            }
            boolean completed = advance(nowNanos);
            if (phase == Phase.MEASUREMENT) {
                admission.add(nbt, rejected, elapsedUs);
            }
            return completed;
        }
    }

    boolean recordProjection(
        long nowNanos, long elapsedUs, int tombstoneCount, int judgmentCount
    ) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return false;
            }
            boolean completed = advance(nowNanos);
            if (phase == Phase.MEASUREMENT) {
                projections.add(elapsedUs, tombstoneCount, judgmentCount);
                tombstones.add(tombstoneCount);
            }
            return completed;
        }
    }

    boolean recordRootCommit(
        long nowNanos, long elapsedUs, boolean changed
    ) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return false;
            }
            boolean completed = advance(nowNanos);
            if (phase == Phase.MEASUREMENT) {
                rootCommits.add(elapsedUs, changed);
            }
            return completed;
        }
    }

    boolean recordRetryTick(
        long nowNanos,
        long elapsedUs,
        int queued,
        int processed,
        int attempts,
        int remaining
    ) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return false;
            }
            boolean completed = advance(nowNanos);
            if (phase == Phase.MEASUREMENT) {
                retries.add(elapsedUs, queued, processed, attempts, remaining);
            }
            return completed;
        }
    }

    boolean recordTerminalInvocation(long nowNanos) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return false;
            }
            boolean completed = advance(nowNanos);
            if (phase == Phase.MEASUREMENT) {
                terminalInvocations++;
            }
            return completed;
        }
    }

    void finish(long nowNanos) {
        if (phase == Phase.COMPLETE) {
            return;
        }
        synchronized (this) {
            if (phase == Phase.COMPLETE) {
                return;
            }
            completedNanos = nowNanos;
            completionReason = "SERVER_STOPPED_BEFORE_WINDOW_COMPLETE";
            tickStartedNanos = 0L;
            phase = Phase.COMPLETE;
        }
    }

    boolean scheduleArchive() {
        return phase == Phase.COMPLETE && archiveScheduled.compareAndSet(false, true);
    }

    void archiveFailed() {
        archiveScheduled.set(false);
    }

    /**
     * Creates a deep-copy archive snapshot after volatile COMPLETE publishes the
     * final sample writes. This deliberately does not acquire the session monitor:
     * complete sessions are immutable and hot-path calls return before locking.
     */
    Snapshot snapshot() {
        Phase completedPhase = phase;
        if (completedPhase != Phase.COMPLETE) {
            throw new IllegalStateException("baseline snapshot requested before completion");
        }
        long effectiveMeasurementStart = measurementStartedNanos == 0L
            ? warmupDeadlineNanos : measurementStartedNanos;
        long measuredNanos = completedNanos <= effectiveMeasurementStart
            ? 0L : completedNanos - effectiveMeasurementStart;
        EnumMap<RonovaAction, ActionSnapshot> actionSnapshots =
            new EnumMap<>(RonovaAction.class);
        for (Map.Entry<RonovaAction, ActionSamples> entry : actions.entrySet()) {
            actionSnapshots.put(entry.getKey(), entry.getValue().snapshot());
        }
        return new Snapshot(
            createdEpochMillis,
            warmupSeconds,
            measurementSeconds,
            measuredNanos / 1_000_000L,
            sampleCapacity,
            completedPhase,
            completionReason,
            ticks.snapshot(),
            actionSnapshots,
            admission.snapshot(),
            projections.snapshot(),
            retries.snapshot(),
            rootCommits.snapshot(),
            terminalInvocations,
            tickHookObserved,
            tombstones.snapshot(),
            candidates.snapshot(),
            topology.snapshot(),
            quarantineObjects.snapshot(),
            rawObservationEnabled,
            rawObservationFlagSource,
            passiveWatchEntries.snapshot(),
            passiveWatchNotificationFailures
        );
    }

    /*
     * The remaining storage records are intentionally primitive and fixed-capacity.
     */

    static int judgmentCount(DominionSnapshot snapshot) {
        int count = 0;
        for (DominionDecision decision : snapshot.decisions()) {
            if (decision instanceof DominionDecision.Judgment) {
                count++;
            }
        }
        return count;
    }

    private boolean advance(long nowNanos) {
        if (phase == Phase.COMPLETE) {
            return false;
        }
        if (nowNanos >= measurementDeadlineNanos) {
            if (measurementStartedNanos == 0L) {
                measurementStartedNanos = warmupDeadlineNanos;
            }
            completedNanos = nowNanos;
            completionReason = "WINDOW_COMPLETE";
            phase = Phase.COMPLETE;
            tickStartedNanos = 0L;
            return true;
        }
        if (phase == Phase.WARMUP && nowNanos >= warmupDeadlineNanos) {
            measurementStartedNanos = nowNanos;
            phase = Phase.MEASUREMENT;
        }
        return false;
    }

    private static long secondsToNanos(long seconds) {
        return seconds > Long.MAX_VALUE / 1_000_000_000L
            ? Long.MAX_VALUE : seconds * 1_000_000_000L;
    }

    private static long saturatingAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static long elapsedUs(long startedNanos, long endedNanos) {
        return Math.max(0L, endedNanos - startedNanos) / 1_000L;
    }

    record Snapshot(
        long createdEpochMillis,
        long configuredWarmupSeconds,
        long configuredMeasurementSeconds,
        long actualMeasurementMillis,
        int sampleCapacity,
        Phase phase,
        String completionReason,
        TickSnapshot ticks,
        EnumMap<RonovaAction, ActionSnapshot> actions,
        AdmissionSnapshot admission,
        ProjectionSnapshot projections,
        RetrySnapshot retries,
        RootCommitSnapshot rootCommits,
        long terminalInvocations,
        boolean tickHookObserved,
        RangeSnapshot tombstones,
        RangeSnapshot candidates,
        RangeSnapshot topology,
        RangeSnapshot quarantineObjects,
        boolean rawObservationEnabled,
        String rawObservationFlagSource,
        RangeSnapshot passiveWatchEntries,
        long passiveWatchNotificationFailures
    ) {}

    record TickSnapshot(
        long totalCount,
        long dropped,
        long[] elapsedUs,
        int[] tombstones,
        int[] topology,
        int[] quarantineObjects,
        int[] pendingRetries
    ) {}

    record ActionSnapshot(
        long totalCount,
        long dropped,
        long[] elapsedUs,
        int[] candidates,
        int[] judged,
        int[] tombstones,
        int[] topology,
        int[] quarantineObjects
    ) {}

    record AdmissionSnapshot(
        long totalCount,
        long accepted,
        long rejected,
        long objectCalls,
        long nbtCalls,
        long dropped,
        long[] elapsedUs,
        boolean[] nbt,
        boolean[] rejectedSamples
    ) {}

    record ProjectionSnapshot(
        long totalCount,
        long dropped,
        long[] elapsedUs,
        int[] tombstones,
        int[] judgments
    ) {}

    record RetrySnapshot(
        long totalCount,
        long processedTotal,
        long attemptsTotal,
        long dropped,
        long[] elapsedUs,
        int[] queued,
        int[] processed,
        int[] attempts,
        int[] remaining
    ) {}

    record RootCommitSnapshot(
        long totalCount,
        long changedCount,
        long dropped,
        long[] elapsedUs,
        boolean[] changed
    ) {}

    record RangeSnapshot(boolean available, long minimum, long maximum) {}

    private static final class TickSamples {
        private final long[] elapsedUs;
        private final int[] tombstones;
        private final int[] topology;
        private final int[] quarantineObjects;
        private final int[] pendingRetries;
        private int size;
        private long totalCount;
        private long dropped;

        private TickSamples(int capacity) {
            elapsedUs = new long[capacity];
            tombstones = new int[capacity];
            topology = new int[capacity];
            quarantineObjects = new int[capacity];
            pendingRetries = new int[capacity];
        }

        private void add(
            long elapsed, int tombstoneCount, int topologySize,
            int quarantineCount, int retryCount
        ) {
            totalCount++;
            if (size >= elapsedUs.length) {
                dropped++;
                return;
            }
            elapsedUs[size] = elapsed;
            tombstones[size] = tombstoneCount;
            topology[size] = topologySize;
            quarantineObjects[size] = quarantineCount;
            pendingRetries[size] = retryCount;
            size++;
        }

        private TickSnapshot snapshot() {
            return new TickSnapshot(
                totalCount, dropped,
                Arrays.copyOf(elapsedUs, size),
                Arrays.copyOf(tombstones, size),
                Arrays.copyOf(topology, size),
                Arrays.copyOf(quarantineObjects, size),
                Arrays.copyOf(pendingRetries, size)
            );
        }
    }

    private static final class ActionSamples {
        private final long[] elapsedUs;
        private final int[] candidates;
        private final int[] judged;
        private final int[] tombstones;
        private final int[] topology;
        private final int[] quarantineObjects;
        private int size;
        private long totalCount;
        private long dropped;

        private ActionSamples(int capacity) {
            elapsedUs = new long[capacity];
            candidates = new int[capacity];
            judged = new int[capacity];
            tombstones = new int[capacity];
            topology = new int[capacity];
            quarantineObjects = new int[capacity];
        }

        private void add(
            long elapsed, int candidateCount, int judgedCount,
            int tombstoneCount, int topologySize, int quarantineCount
        ) {
            totalCount++;
            if (size >= elapsedUs.length) {
                dropped++;
                return;
            }
            elapsedUs[size] = elapsed;
            candidates[size] = candidateCount;
            judged[size] = judgedCount;
            tombstones[size] = tombstoneCount;
            topology[size] = topologySize;
            quarantineObjects[size] = quarantineCount;
            size++;
        }

        private ActionSnapshot snapshot() {
            return new ActionSnapshot(
                totalCount, dropped,
                Arrays.copyOf(elapsedUs, size),
                Arrays.copyOf(candidates, size),
                Arrays.copyOf(judged, size),
                Arrays.copyOf(tombstones, size),
                Arrays.copyOf(topology, size),
                Arrays.copyOf(quarantineObjects, size)
            );
        }
    }

    private static final class AdmissionSamples {
        private final long[] elapsedUs;
        private final boolean[] nbt;
        private final boolean[] rejectedSamples;
        private int size;
        private long totalCount;
        private long accepted;
        private long rejected;
        private long objectCalls;
        private long nbtCalls;
        private long dropped;

        private AdmissionSamples(int capacity) {
            elapsedUs = new long[capacity];
            nbt = new boolean[capacity];
            rejectedSamples = new boolean[capacity];
        }

        private void add(boolean nbtCall, boolean rejectedResult, long elapsed) {
            totalCount++;
            if (nbtCall) {
                nbtCalls++;
            } else {
                objectCalls++;
            }
            if (rejectedResult) {
                rejected++;
            } else {
                accepted++;
            }
            if (size >= elapsedUs.length) {
                dropped++;
                return;
            }
            elapsedUs[size] = elapsed;
            nbt[size] = nbtCall;
            rejectedSamples[size] = rejectedResult;
            size++;
        }

        private AdmissionSnapshot snapshot() {
            return new AdmissionSnapshot(
                totalCount, accepted, rejected, objectCalls, nbtCalls, dropped,
                Arrays.copyOf(elapsedUs, size),
                Arrays.copyOf(nbt, size),
                Arrays.copyOf(rejectedSamples, size)
            );
        }
    }

    private static final class ProjectionSamples {
        private final long[] elapsedUs;
        private final int[] tombstones;
        private final int[] judgments;
        private int size;
        private long totalCount;
        private long dropped;

        private ProjectionSamples(int capacity) {
            elapsedUs = new long[capacity];
            tombstones = new int[capacity];
            judgments = new int[capacity];
        }

        private void add(long elapsed, int tombstoneCount, int judgmentCount) {
            totalCount++;
            if (size >= elapsedUs.length) {
                dropped++;
                return;
            }
            elapsedUs[size] = elapsed;
            tombstones[size] = tombstoneCount;
            judgments[size] = judgmentCount;
            size++;
        }

        private ProjectionSnapshot snapshot() {
            return new ProjectionSnapshot(
                totalCount, dropped,
                Arrays.copyOf(elapsedUs, size),
                Arrays.copyOf(tombstones, size),
                Arrays.copyOf(judgments, size)
            );
        }
    }

    private static final class RetrySamples {
        private final long[] elapsedUs;
        private final int[] queued;
        private final int[] processed;
        private final int[] attempts;
        private final int[] remaining;
        private int size;
        private long totalCount;
        private long processedTotal;
        private long attemptsTotal;
        private long dropped;

        private RetrySamples(int capacity) {
            elapsedUs = new long[capacity];
            queued = new int[capacity];
            processed = new int[capacity];
            attempts = new int[capacity];
            remaining = new int[capacity];
        }

        private void add(
            long elapsed, int queuedCount, int processedCount,
            int attemptCount, int remainingCount
        ) {
            totalCount++;
            processedTotal += processedCount;
            attemptsTotal += attemptCount;
            if (size >= elapsedUs.length) {
                dropped++;
                return;
            }
            elapsedUs[size] = elapsed;
            queued[size] = queuedCount;
            processed[size] = processedCount;
            attempts[size] = attemptCount;
            remaining[size] = remainingCount;
            size++;
        }

        private RetrySnapshot snapshot() {
            return new RetrySnapshot(
                totalCount, processedTotal, attemptsTotal, dropped,
                Arrays.copyOf(elapsedUs, size),
                Arrays.copyOf(queued, size),
                Arrays.copyOf(processed, size),
                Arrays.copyOf(attempts, size),
                Arrays.copyOf(remaining, size)
            );
        }
    }

    private static final class RootCommitSamples {
        private final long[] elapsedUs;
        private final boolean[] changed;
        private int size;
        private long totalCount;
        private long changedCount;
        private long dropped;

        private RootCommitSamples(int capacity) {
            elapsedUs = new long[capacity];
            changed = new boolean[capacity];
        }

        private void add(long elapsed, boolean changedReceipt) {
            totalCount++;
            if (changedReceipt) {
                changedCount++;
            }
            if (size >= elapsedUs.length) {
                dropped++;
                return;
            }
            elapsedUs[size] = elapsed;
            changed[size] = changedReceipt;
            size++;
        }

        private RootCommitSnapshot snapshot() {
            return new RootCommitSnapshot(
                totalCount, changedCount, dropped,
                Arrays.copyOf(elapsedUs, size), Arrays.copyOf(changed, size)
            );
        }
    }

    private static final class MutableRange {
        private long minimum = Long.MAX_VALUE;
        private long maximum = Long.MIN_VALUE;

        private void add(long value) {
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }

        private RangeSnapshot snapshot() {
            boolean available = minimum != Long.MAX_VALUE;
            return new RangeSnapshot(
                available, available ? minimum : 0L, available ? maximum : 0L
            );
        }
    }
}
