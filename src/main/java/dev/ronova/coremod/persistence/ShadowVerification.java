package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.DominionEpoch;
import java.util.Objects;

/** Result of one authoritative-SavedData to shadow comparison. */
public record ShadowVerification(
    Status status,
    DominionEpoch expectedEpoch,
    DominionEpoch observedEpoch,
    String expectedHash,
    String observedHash,
    String detail
) {
    public ShadowVerification {
        status = Objects.requireNonNull(status, "status");
        expectedEpoch = Objects.requireNonNull(expectedEpoch, "expectedEpoch");
        observedEpoch = Objects.requireNonNull(observedEpoch, "observedEpoch");
        expectedHash = Objects.toString(expectedHash, "");
        observedHash = Objects.toString(observedHash, "");
        detail = Objects.toString(detail, "");
    }

    public boolean accepted() {
        return status == Status.VERIFIED
            || status == Status.MIRRORED
            || status == Status.RECEIPT_MIRRORED;
    }

    public boolean matches() {
        return status == Status.VERIFIED;
    }

    public boolean rejected() {
        return !accepted();
    }

    public enum Status {
        VERIFIED,
        MIRRORED,
        RECEIPT_MIRRORED,
        REALM_MISMATCH,
        ROLLBACK_REJECTED,
        STATE_CONFLICT,
        RECEIPT_DRIFT,
        READ_ONLY,
        FAILPOINT,
        INVALID_INPUT
    }
}
