package dev.ronova.coremod.contract;

/** Stable capability bits. IDs are protocol data and never depend on declaration order. */
public enum Capability {
    LIFECYCLE_CONTINUITY(0),
    TOPOLOGY_CONTINUITY(1),
    VITALS_RECOVERY(2),
    NEGATIVE_EFFECT_CLEANSE(3),
    FLIGHT(4),
    BLADE_CONTINUITY(5),
    CONNECTION_CONTINUITY(6),
    PRIMARY_ACTION(7),
    AREA_ACTION(8),
    LOADED_ACTION(9),
    INSTANT_MINING(10);

    private final int stableId;

    Capability(int stableId) {
        if (stableId < 0 || stableId >= Long.SIZE) {
            throw new IllegalArgumentException("capability id outside long mask: " + stableId);
        }
        this.stableId = stableId;
    }

    public int stableId() { return stableId; }

    public static int slotCount() {
        int highest = 0;
        for (Capability capability : values()) {
            highest = Math.max(highest, capability.stableId);
        }
        return highest + 1;
    }
}
