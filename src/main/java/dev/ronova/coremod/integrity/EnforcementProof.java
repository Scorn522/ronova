package dev.ronova.coremod.integrity;

/** Immutable, loader-neutral view of the process-wide Native enforcement lease. */
public record EnforcementProof(
    boolean supported,
    boolean current,
    boolean watchdogArmed,
    int requiredMask,
    int verifiedMask,
    long generation,
    long successfulLeases,
    long failedLeases,
    long transformations,
    long semanticFailures,
    long reassertions,
    long ageMillis,
    long consecutiveFailures
) {
    public static final EnforcementProof UNAVAILABLE = new EnforcementProof(
        false, false, false, 0, 0, 0L, 0L, 0L, 0L, 0L, 0L, -1L, 0L
    );

    public EnforcementProof {
        if ((requiredMask | verifiedMask) < 0) {
            throw new IllegalArgumentException("enforcement masks must be non-negative");
        }
        if (generation < 0L || successfulLeases < 0L || failedLeases < 0L
            || transformations < 0L || semanticFailures < 0L || reassertions < 0L
            || ageMillis < -1L || consecutiveFailures < 0L) {
            throw new IllegalArgumentException("enforcement counters must be non-negative");
        }
        current = supported && current && requiredMask != 0
            && (verifiedMask & requiredMask) == requiredMask;
    }

    public State state() {
        if (!supported) return State.UNAVAILABLE;
        return current ? State.ACTIVE : State.DEGRADED;
    }

    public enum State {
        ACTIVE,
        DEGRADED,
        UNAVAILABLE
    }
}
