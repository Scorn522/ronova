package dev.ronova.coremod.client;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Time-based client recovery window; independent of game ticks and frame rate. */
public final class ClientAuthorityState {
    private static final long DEFAULT_RECOVERY_NANOS = 5_000_000_000L;

    private final LongSupplier clock;
    private boolean armed;
    private long recoveryDeadline;

    public ClientAuthorityState() {
        this(System::nanoTime);
    }

    ClientAuthorityState(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized void arm() {
        long now = clock.getAsLong();
        long candidate = now > Long.MAX_VALUE - DEFAULT_RECOVERY_NANOS
            ? Long.MAX_VALUE : now + DEFAULT_RECOVERY_NANOS;
        recoveryDeadline = armed ? Math.max(recoveryDeadline, candidate) : candidate;
        armed = true;
    }

    public synchronized boolean isActive() {
        if (!armed) {
            return false;
        }
        if (recoveryDeadline > clock.getAsLong()) {
            return true;
        }
        armed = false;
        recoveryDeadline = 0L;
        return false;
    }

    public synchronized void reset() {
        armed = false;
        recoveryDeadline = 0L;
    }
}
