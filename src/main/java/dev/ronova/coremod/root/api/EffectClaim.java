package dev.ronova.coremod.root.api;

import java.util.Objects;

/** Preparation result. The issuing Root must also admit this exact one-use instance before execution. */
public record EffectClaim(Status status, EffectRecord record) {
    public EffectClaim {
        Objects.requireNonNull(status, "status");
        if (status == Status.GRANTED && (record == null || record.state() != EffectRecord.State.PREPARED)) {
            throw new IllegalArgumentException("effect grant requires a prepared record");
        }
    }
    public boolean granted() { return status == Status.GRANTED; }
    public enum Status {
        GRANTED, ALREADY_RECORDED, PERSISTENCE_UNKNOWN, STORAGE_BLOCKED,
        LEGACY_UNQUALIFIED, LEGACY_CLAIM_UNKNOWN, REJECTED
    }
}
