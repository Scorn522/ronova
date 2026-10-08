package dev.ronova.coremod.budget;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.LongSupplier;

/** Per-server bounded-work governor with immutable snapshot output. */
public final class FrameBudgetGovernor {
    public static final long TICK_BUDGET_US = 2_000L;

    private final BudgetSplitFlags.Selection selection;
    private final LongSupplier clock;
    private final EnumMap<BudgetCategory, Long> budgetUs;
    private final EnumMap<BudgetCategory, Integer> countLimits;
    private final AtomicLongArray overBudgetCounts =
        new AtomicLongArray(BudgetCategory.values().length);
    private final EnumMap<BudgetTelemetrySnapshot.QueryKind, QueryCounters> queryCounters =
        new EnumMap<>(BudgetTelemetrySnapshot.QueryKind.class);
    private final Object lock = new Object();
    private EnumMap<BudgetCategory, WorkBudget> budgets;
    private long currentTickId;
    private long snapshotEpoch;
    private boolean started;

    public FrameBudgetGovernor() {
        this(BudgetSplitFlags.resolve());
    }

    public FrameBudgetGovernor(boolean enabled) {
        this(new BudgetSplitFlags.Selection(
            enabled,
            enabled ? BudgetSplitFlags.Source.DEFAULT : BudgetSplitFlags.Source.JVM_PROPERTY,
            null
        ));
    }

    public FrameBudgetGovernor(BudgetSplitFlags.Selection selection) {
        this(selection, System::nanoTime, null, null);
    }

    FrameBudgetGovernor(
        BudgetSplitFlags.Selection selection,
        LongSupplier clock,
        Map<BudgetCategory, Long> budgetUs,
        Map<BudgetCategory, Integer> countLimits
    ) {
        this.selection = Objects.requireNonNull(selection, "selection");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.budgetUs = copyBudgetUs(budgetUs);
        this.countLimits = copyCountLimits(countLimits);
        for (BudgetTelemetrySnapshot.QueryKind kind
            : BudgetTelemetrySnapshot.QueryKind.values()) {
            queryCounters.put(kind, new QueryCounters());
        }
    }

    public boolean enabled() {
        return selection.enabled();
    }

    public BudgetSplitFlags.Selection selection() {
        return selection;
    }

    public void beginTick(long tickId) {
        if (tickId < 0L) {
            throw new IllegalArgumentException("tickId must be non-negative");
        }
        synchronized (lock) {
            if (started && tickId < currentTickId) {
                throw new IllegalArgumentException("tickId must be monotonic");
            }
            if (started && tickId == currentTickId) {
                return;
            }
            startTickLocked(tickId);
        }
    }

    public void endTick(long tickId) {
        synchronized (lock) {
            ensureTick(tickId);
            for (WorkBudget budget : budgets.values()) {
                budget.close();
            }
        }
    }

    public WorkBudget budget(BudgetCategory category) {
        Objects.requireNonNull(category, "category");
        synchronized (lock) {
            ensureStarted();
            return budgets.get(category);
        }
    }

    public void setSnapshotEpoch(long epoch) {
        if (epoch < 0L) {
            throw new IllegalArgumentException("snapshot epoch must be non-negative");
        }
        synchronized (lock) {
            snapshotEpoch = epoch;
        }
    }

    public long currentTickId() {
        synchronized (lock) {
            ensureStarted();
            return currentTickId;
        }
    }

    public void recordSynchronousQuery(
        BudgetTelemetrySnapshot.QueryKind kind,
        long elapsedUs
    ) {
        Objects.requireNonNull(kind, "kind");
        if (elapsedUs < 0L) {
            throw new IllegalArgumentException("query elapsed time must be non-negative");
        }
        synchronized (lock) {
            ensureStarted();
            QueryCounters counters = queryCounters.get(kind);
            counters.callCount = addSaturated(counters.callCount, 1L);
            counters.totalElapsedUs = addSaturated(counters.totalElapsedUs, elapsedUs);
            counters.maxElapsedUs = Math.max(counters.maxElapsedUs, elapsedUs);
        }
    }

    /** Records a synchronous or measurement-only unit without applying a gate. */
    public BudgetReceipt recordMeasurement(
        BudgetCategory category,
        long attempted,
        long processed,
        long deferred,
        long carryIn,
        long carryOut,
        long elapsedUs
    ) {
        Objects.requireNonNull(category, "category");
        synchronized (lock) {
            ensureStarted();
            return budgets.get(category).recordMeasurement(
                attempted, processed, deferred, carryIn, carryOut, elapsedUs
            );
        }
    }

    public BudgetTelemetrySnapshot snapshot() {
        synchronized (lock) {
            ensureStarted();
            EnumMap<BudgetCategory, BudgetReceipt> receipts =
                new EnumMap<>(BudgetCategory.class);
            EnumMap<BudgetCategory, Long> overBudget =
                new EnumMap<>(BudgetCategory.class);
            for (BudgetCategory category : BudgetCategory.values()) {
                receipts.put(category, budgets.get(category).receipt());
                overBudget.put(category, overBudgetCounts.get(category.ordinal()));
            }
            EnumMap<BudgetTelemetrySnapshot.QueryKind,
                BudgetTelemetrySnapshot.SynchronousQueryTelemetry> queries =
                new EnumMap<>(BudgetTelemetrySnapshot.QueryKind.class);
            for (Map.Entry<BudgetTelemetrySnapshot.QueryKind, QueryCounters> entry
                : queryCounters.entrySet()) {
                QueryCounters counters = entry.getValue();
                queries.put(entry.getKey(), new BudgetTelemetrySnapshot.SynchronousQueryTelemetry(
                    entry.getKey(), counters.callCount, counters.totalElapsedUs,
                    counters.maxElapsedUs
                ));
            }
            return new BudgetTelemetrySnapshot(
                currentTickId, snapshotEpoch, receipts, queries, overBudget
            );
        }
    }

    void recordOverBudget(BudgetCategory category) {
        overBudgetCounts.incrementAndGet(category.ordinal());
    }

    private void ensureStarted() {
        if (!started) {
            startTickLocked(0L);
        }
    }

    private void ensureTick(long tickId) {
        if (tickId < 0L) {
            throw new IllegalArgumentException("tickId must be non-negative");
        }
        if (!started || tickId > currentTickId) {
            startTickLocked(tickId);
            return;
        }
        if (tickId < currentTickId) {
            throw new IllegalArgumentException("tickId must be monotonic");
        }
    }

    private void startTickLocked(long tickId) {
        currentTickId = tickId;
        started = true;
        for (BudgetTelemetrySnapshot.QueryKind kind
            : BudgetTelemetrySnapshot.QueryKind.values()) {
            queryCounters.get(kind).reset();
        }
        for (BudgetCategory category : BudgetCategory.values()) {
            overBudgetCounts.set(category.ordinal(), 0L);
        }
        budgets = new EnumMap<>(BudgetCategory.class);
        for (BudgetCategory category : BudgetCategory.values()) {
            budgets.put(category, new WorkBudget(
                this,
                category,
                tickId,
                selection.enabled(),
                budgetUs.get(category),
                countLimits.get(category),
                clock
            ));
        }
    }

    private static EnumMap<BudgetCategory, Long> copyBudgetUs(
        Map<BudgetCategory, Long> overrides
    ) {
        EnumMap<BudgetCategory, Long> values = new EnumMap<>(BudgetCategory.class);
        for (BudgetCategory category : BudgetCategory.values()) {
            Long value = overrides == null ? null : overrides.get(category);
            long selected = value == null ? category.defaultBudgetUs() : value;
            if (selected <= 0L) {
                throw new IllegalArgumentException("budget time must be positive: " + category);
            }
            values.put(category, selected);
        }
        return values;
    }

    private static EnumMap<BudgetCategory, Integer> copyCountLimits(
        Map<BudgetCategory, Integer> overrides
    ) {
        EnumMap<BudgetCategory, Integer> values = new EnumMap<>(BudgetCategory.class);
        for (BudgetCategory category : BudgetCategory.values()) {
            Integer value = overrides == null ? null : overrides.get(category);
            int selected = value == null ? category.defaultCount() : value;
            if (selected <= 0) {
                throw new IllegalArgumentException("budget count must be positive: " + category);
            }
            values.put(category, selected);
        }
        return values;
    }

    private static long addSaturated(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static final class QueryCounters {
        private long callCount;
        private long totalElapsedUs;
        private long maxElapsedUs;

        private void reset() {
            callCount = 0L;
            totalElapsedUs = 0L;
            maxElapsedUs = 0L;
        }
    }
}
