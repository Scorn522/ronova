package dev.ronova.coremod.runtime;

import dev.ronova.coremod.root.api.TerminalOperationPlan;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Short-lived, thread-confined permission for one exact Root plan. Never grants Root authority. */
public final class PlanExecutionLease implements AutoCloseable {
    public static final long MAX_DURATION_NANOS = 5_000_000_000L;
    private final TerminalOperationPlan plan;
    private final Thread owner = Thread.currentThread();
    private final LongSupplier clock;
    private final long issued, duration;
    private boolean closed;

    public PlanExecutionLease(TerminalOperationPlan plan, LongSupplier clock, long duration) {
        this.plan = Objects.requireNonNull(plan);
        this.clock = Objects.requireNonNull(clock);
        if (duration <= 0 || duration > MAX_DURATION_NANOS) throw new IllegalArgumentException("invalid lease duration");
        this.duration = duration;
        issued = clock.getAsLong();
    }

    public boolean valid(TerminalOperationPlan expected) {
        if (Thread.currentThread() != owner || closed || !plan.equals(expected)) return false;
        long elapsed = clock.getAsLong() - issued;
        return elapsed >= 0 && elapsed < duration;
    }

    @Override public void close() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("lease belongs to another execution thread");
        closed = true;
    }
}
