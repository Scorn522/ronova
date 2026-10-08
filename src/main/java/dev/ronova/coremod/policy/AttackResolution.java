package dev.ronova.coremod.policy;

import java.util.Objects;

/** Policy permission is not proof of an implemented executor or an attack effect. */
public record AttackResolution(
    MechanismLevel requestedDepth, MechanismLevel effectiveDepth,
    boolean permitted, String reason
) {
    public AttackResolution {
        Objects.requireNonNull(requestedDepth, "requestedDepth");
        Objects.requireNonNull(effectiveDepth, "effectiveDepth");
        Objects.requireNonNull(reason, "reason");
        if (effectiveDepth.rank() > requestedDepth.rank()
            || permitted && effectiveDepth.rank() < MechanismLevel.PROTECT_RONOVA.rank()) {
            throw new IllegalArgumentException("invalid attack resolution");
        }
    }
}
