package dev.ronova.coremod.engine.spawn;

import java.util.concurrent.atomic.AtomicLong;

/** Process-lifetime admission counters retained across per-server runtime replacement. */
public final class SpawnAdmissionMetrics {
    private static final AtomicLong REJECTED_ADMISSIONS = new AtomicLong();
    private static final AtomicLong NBT_REJECTIONS = new AtomicLong();

    private SpawnAdmissionMetrics() {}

    static void recordObjectRejection() {
        REJECTED_ADMISSIONS.incrementAndGet();
    }

    static void recordNbtRejection() {
        NBT_REJECTIONS.incrementAndGet();
        REJECTED_ADMISSIONS.incrementAndGet();
    }

    public static long rejectedAdmissions() {
        return REJECTED_ADMISSIONS.get();
    }

    public static long nbtRejections() {
        return NBT_REJECTIONS.get();
    }
}
