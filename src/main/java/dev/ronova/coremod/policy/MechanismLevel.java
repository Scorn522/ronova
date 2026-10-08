package dev.ronova.coremod.policy;

/** Ordered enforcement levels for mechanism interception options. */
public enum MechanismLevel {
    FOLLOW_PROFILE,
    OFF,
    MONITOR,
    PROTECT_RONOVA,
    RELATED_PATHS,
    STRICT_ALL;

    public boolean isStricterThan(MechanismLevel other) {
        return other != null && rank() > other.rank();
    }

    /** FOLLOW_PROFILE is configuration syntax, not a runnable strength. */
    public int rank() {
        return switch (this) {
            case FOLLOW_PROFILE -> -1;
            case OFF -> 0;
            case MONITOR -> 1;
            case PROTECT_RONOVA -> 2;
            case RELATED_PATHS -> 3;
            case STRICT_ALL -> 4;
        };
    }

    public static MechanismLevel lowerOf(MechanismLevel requested, MechanismLevel ceiling) {
        MechanismLevel left = runnable(requested);
        MechanismLevel right = runnable(ceiling);
        return left.rank() <= right.rank() ? left : right;
    }

    public static MechanismLevel runnable(MechanismLevel value) {
        return value == null || value == FOLLOW_PROFILE ? OFF : value;
    }
}
