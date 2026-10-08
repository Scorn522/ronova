package dev.ronova.coremod.policy;

/** Configuration syntax, independent of the advanced mechanism vocabulary. */
public enum AttackActionLevel {
    FOLLOW_PROFILE, DISABLED, COMPATIBLE, ENHANCED, EXTREME;

    public MechanismLevel depth() {
        return switch (this) {
            case FOLLOW_PROFILE, DISABLED -> MechanismLevel.OFF;
            case COMPATIBLE -> MechanismLevel.PROTECT_RONOVA;
            case ENHANCED -> MechanismLevel.RELATED_PATHS;
            case EXTREME -> MechanismLevel.STRICT_ALL;
        };
    }
}
