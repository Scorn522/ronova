package dev.ronova.coremod.capability;

import dev.ronova.coremod.contract.Capability;
import java.util.concurrent.atomic.AtomicLongArray;

/** Counted compatibility-boundary failures; capability recovery never fails silently. */
public final class CapabilityDiagnostics {
    private final AtomicLongArray failures = new AtomicLongArray(Capability.slotCount());

    void record(Capability capability) {
        failures.incrementAndGet(capability.stableId());
    }

    public long failures(Capability capability) {
        return capability == null ? 0L : failures.get(capability.stableId());
    }

    public long totalFailures() {
        long total = 0L;
        for (Capability capability : Capability.values()) {
            total += failures(capability);
        }
        return total;
    }
}
