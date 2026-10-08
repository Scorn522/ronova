package dev.ronova.coremod.contract;

import java.util.EnumSet;
import java.util.Set;

/** Compact loader-neutral capability projection. */
public record CapabilitySet(long mask) {
    public static final CapabilitySet NONE = new CapabilitySet(0L);
    public static final CapabilitySet OWNER_DEFAULT = of(Capability.values());

    public boolean contains(Capability capability) {
        return capability != null && (mask & bit(capability)) != 0L;
    }

    public CapabilitySet with(Capability capability) {
        return capability == null ? this : new CapabilitySet(mask | bit(capability));
    }

    public CapabilitySet without(Capability capability) {
        return capability == null ? this : new CapabilitySet(mask & ~bit(capability));
    }

    public Set<Capability> asSet() {
        EnumSet<Capability> result = EnumSet.noneOf(Capability.class);
        for (Capability capability : Capability.values()) {
            if (contains(capability)) {
                result.add(capability);
            }
        }
        return Set.copyOf(result);
    }

    public static CapabilitySet of(Capability... capabilities) {
        long value = 0L;
        if (capabilities != null) {
            for (Capability capability : capabilities) {
                if (capability != null) {
                    value |= bit(capability);
                }
            }
        }
        return new CapabilitySet(value);
    }

    private static long bit(Capability capability) {
        return 1L << capability.stableId();
    }
}
