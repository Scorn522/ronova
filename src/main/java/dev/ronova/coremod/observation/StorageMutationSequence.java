package dev.ronova.coremod.observation;

import java.util.concurrent.atomic.AtomicLong;

/** Observation stamp only; it never locks a foreign cache or grants permission to write it. */
public final class StorageMutationSequence {
    private static final long ACTIVE_MASK = 0xffffL;
    private final AtomicLong state = new AtomicLong();
    public void begin() {
        for (;;) {
            long prior = state.get();
            if (prior < 0) return;
            long active = prior & ACTIVE_MASK;
            long next = active == ACTIVE_MASK || prior > Long.MAX_VALUE - 0x10001L
                ? Long.MIN_VALUE : prior + 0x10001L;
            if (state.compareAndSet(prior, next)) return;
        }
    }
    public void end() {
        for (;;) {
            long prior = state.get();
            if (prior < 0) return;
            long next = (prior & ACTIVE_MASK) == 0 ? Long.MIN_VALUE : prior - 1;
            if (state.compareAndSet(prior, next)) return;
        }
    }
    public long observationVersion() {
        long current = state.get();
        return current < 0 || (current & ACTIVE_MASK) != 0 ? -1 : current;
    }
}
