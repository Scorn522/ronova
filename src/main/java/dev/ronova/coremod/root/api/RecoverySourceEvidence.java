package dev.ronova.coremod.root.api;

import java.util.Objects;

/** Short-lived evidence supplied by the registered observer; never serialized as a live reference. */
public record RecoverySourceEvidence(String sourceLocator, AuthorityStamp authority, long sourceRevision,
                                     int copyDepth, boolean observerVerified, long observedAtNanos) {
    public static final long MAX_AGE_NANOS = 5_000_000_000L;
    public RecoverySourceEvidence {
        sourceLocator = EffectRecord.checkedHash(sourceLocator); Objects.requireNonNull(authority);
        if (sourceRevision < 1 || copyDepth < 0) throw new IllegalArgumentException("invalid source evidence revision");
    }
    public boolean fresh(long now) {
        long age = now - observedAtNanos;
        return observerVerified && age >= 0 && age <= MAX_AGE_NANOS;
    }
}
