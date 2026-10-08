package dev.ronova.coremod.budget;

import java.util.Objects;
import java.util.function.LongSupplier;

/** One category's count/time gate and receipt accumulator for one tick. */
public final class WorkBudget {
    private final FrameBudgetGovernor governor;
    private final BudgetCategory category;
    private final long tickId;
    private final boolean enabled;
    private final long maxBudgetUs;
    private final long maxCount;
    private final LongSupplier clock;
    private long gateStartedNanos;
    private boolean gateRunning;
    private long gateElapsedUs;
    private long measuredElapsedUs;
    private long reservedCount;
    private long accountedReservedCount;
    private long deferredAllowance;
    private long accountedDeferredAllowance;
    private long processed;
    private long deferred;
    private long carryIn;
    private long carryOut;
    private long maxObservedBacklog;
    private boolean overBudget;
    private boolean overBudgetReported;
    private boolean activity;

    WorkBudget(
        FrameBudgetGovernor governor,
        BudgetCategory category,
        long tickId,
        boolean enabled,
        long maxBudgetUs,
        long maxCount,
        LongSupplier clock
    ) {
        this.governor = Objects.requireNonNull(governor, "governor");
        this.category = Objects.requireNonNull(category, "category");
        this.tickId = tickId;
        this.enabled = enabled;
        this.maxBudgetUs = maxBudgetUs;
        this.maxCount = maxCount;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public BudgetCategory category() {
        return category;
    }

    public long tickId() {
        return tickId;
    }

    /** Reserves a unit for this receipt; false leaves the caller's work untouched. */
    public boolean tryConsumeCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be positive");
        }
        synchronized (this) {
            startGateLocked();
            long elapsed = gateElapsedUsLocked();
            if (enabled
                && (count > maxCount - reservedCount || elapsed >= maxBudgetUs)) {
                markOverBudget();
                return false;
            }
            reservedCount = addSaturated(reservedCount, count);
            activity = true;
            return true;
        }
    }

    /** Returns count capacity that can be reserved without changing the receipt. */
    public int remainingCount() {
        synchronized (this) {
            if (!enabled) {
                return Integer.MAX_VALUE;
            }
            long remaining = Math.max(0L, maxCount - reservedCount);
            return remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
        }
    }

    public boolean hasTimeRemaining() {
        synchronized (this) {
            startGateLocked();
            return !enabled || gateElapsedUsLocked() < maxBudgetUs;
        }
    }

    /** Explicitly records work deferred after it became part of this receipt. */
    public void deferCount(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("deferred count must be positive");
        }
        synchronized (this) {
            deferredAllowance = addExact(deferredAllowance, count);
            activity = true;
        }
    }

    /** Releases a reservation for work that was never claimed and is carried out. */
    public void refundCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("refund count must be non-negative");
        }
        synchronized (this) {
            long unaccounted = reservedCount - accountedReservedCount;
            if (count > unaccounted) {
                throw new IllegalArgumentException("refund exceeds unaccounted reservation");
            }
            reservedCount -= count;
        }
    }

    public BudgetReceipt finish(long processed, long deferred) {
        return finish(processed, deferred, 0L, 0L);
    }

    public BudgetReceipt finish(
        long processed,
        long deferred,
        long carryIn,
        long carryOut
    ) {
        validateCounts(processed, deferred, carryIn, carryOut);
        synchronized (this) {
            long attempted = addExact(processed, deferred);
            long availableReserved = reservedCount - accountedReservedCount;
            long availableDeferred = deferredAllowance - accountedDeferredAllowance;
            if (attempted > availableReserved
                && attempted - availableReserved > availableDeferred) {
                throw new IllegalArgumentException(
                    "receipt accounts more work than this budget allowed"
                );
            }
            long fromReserved = Math.min(attempted, availableReserved);
            accountedReservedCount = addExact(accountedReservedCount, fromReserved);
            accountedDeferredAllowance = addExact(
                accountedDeferredAllowance, attempted - fromReserved
            );
            this.processed = addSaturated(this.processed, processed);
            this.deferred = addSaturated(this.deferred, deferred);
            this.carryIn = carryIn;
            this.carryOut = carryOut;
            maxObservedBacklog = Math.max(maxObservedBacklog, Math.max(carryIn, carryOut));
            activity |= attempted > 0L;
            stopGateLocked();
            long elapsed = totalElapsedUsLocked();
            if (enabled && elapsed >= maxBudgetUs && activity) {
                markOverBudget();
            }
            return receiptLocked(elapsed);
        }
    }

    /** Adds measurement-only work without reserving or gating the caller. */
    public BudgetReceipt recordMeasurement(
        long attempted,
        long processed,
        long deferred,
        long carryIn,
        long carryOut,
        long measurementElapsedUs
    ) {
        validateCounts(processed, deferred, carryIn, carryOut);
        if (attempted < 0L || attempted != addExact(processed, deferred)
            || measurementElapsedUs < 0L) {
            throw new IllegalArgumentException("invalid measurement accounting");
        }
        synchronized (this) {
            this.processed = addSaturated(this.processed, processed);
            this.deferred = addSaturated(this.deferred, deferred);
            this.carryIn = carryIn;
            this.carryOut = carryOut;
            maxObservedBacklog = Math.max(maxObservedBacklog, Math.max(carryIn, carryOut));
            measuredElapsedUs = addSaturated(measuredElapsedUs, measurementElapsedUs);
            activity |= attempted > 0L || measurementElapsedUs > 0L;
            long elapsed = totalElapsedUsLocked();
            if (enabled && elapsed >= maxBudgetUs && activity) {
                markOverBudget();
            }
            return receiptLocked(elapsed);
        }
    }

    public BudgetReceipt receipt() {
        synchronized (this) {
            long elapsed = totalElapsedUsLocked();
            if (enabled && elapsed >= maxBudgetUs && activity) {
                markOverBudget();
            }
            return receiptLocked(elapsed);
        }
    }

    /** Closes the time gate at the paired server-tick tail. */
    void close() {
        synchronized (this) {
            stopGateLocked();
            long elapsed = totalElapsedUsLocked();
            if (enabled && elapsed >= maxBudgetUs && activity) {
                markOverBudget();
            }
        }
    }

    private void markOverBudget() {
        overBudget = true;
        if (!overBudgetReported) {
            overBudgetReported = true;
            governor.recordOverBudget(category);
        }
    }

    private BudgetReceipt receiptLocked(long elapsed) {
        long attempted = addExact(processed, deferred);
        return new BudgetReceipt(
            tickId,
            category,
            attempted,
            processed,
            deferred,
            carryIn,
            carryOut,
            elapsed,
            overBudget,
            maxObservedBacklog
        );
    }

    private void startGateLocked() {
        if (!gateRunning) {
            gateStartedNanos = clock.getAsLong();
            gateRunning = true;
        }
    }

    private void stopGateLocked() {
        if (!gateRunning) {
            return;
        }
        gateElapsedUs = addSaturated(gateElapsedUs, currentGateElapsedUsLocked());
        gateRunning = false;
    }

    private long totalElapsedUsLocked() {
        return addSaturated(measuredElapsedUs, gateElapsedUsLocked());
    }

    private long gateElapsedUsLocked() {
        long currentGate = gateRunning ? currentGateElapsedUsLocked() : 0L;
        return addSaturated(gateElapsedUs, currentGate);
    }

    private long currentGateElapsedUsLocked() {
        long now = clock.getAsLong();
        long elapsedNanos = now >= gateStartedNanos ? now - gateStartedNanos : 0L;
        return elapsedNanos / 1_000L;
    }

    private static void validateCounts(
        long processed,
        long deferred,
        long carryIn,
        long carryOut
    ) {
        if (processed < 0L || deferred < 0L || carryIn < 0L || carryOut < 0L) {
            throw new IllegalArgumentException("budget counts must be non-negative");
        }
        addExact(processed, deferred);
    }

    private static long addExact(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            throw new IllegalArgumentException("budget attempted count overflow");
        }
        return left + right;
    }

    private static long addSaturated(long left, long right) {
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }
}
