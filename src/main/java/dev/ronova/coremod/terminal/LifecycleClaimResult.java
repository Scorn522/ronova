package dev.ronova.coremod.terminal;

import java.util.Objects;

/**
 * Result of reserving the inherited once-only death entry. A claim is an intent,
 * never evidence that death, loot or an opaque callback actually completed.
 */
public record LifecycleClaimResult(Status status, LifecycleIntegrationReceipt receipt) {
    public LifecycleClaimResult {
        Objects.requireNonNull(status, "status");
        if ((status == Status.GRANTED || status == Status.ALREADY_CLAIMED_EFFECT_UNKNOWN)
            && receipt == null) {
            throw new IllegalArgumentException("resolved claim requires its receipt");
        }
        if (status == Status.REJECTED && receipt != null) {
            throw new IllegalArgumentException("rejected claim cannot grant a receipt");
        }
    }

    /** Only the original successful prepare may enter the public death callback. */
    public boolean mayExecute() {
        return status == Status.GRANTED;
    }

    public enum Status {
        GRANTED,
        ALREADY_CLAIMED_EFFECT_UNKNOWN,
        CLAIM_PERSISTENCE_UNKNOWN,
        REJECTED,
        STORAGE_BLOCKED
    }
}
