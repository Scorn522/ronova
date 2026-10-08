package dev.ronova.coremod.policy;

/** What the current artifact can prove about one configured mechanism. */
public enum EvidenceState {
    UNOBSERVED,
    OBSERVED,
    VERIFIED,
    DEGRADED,
    FAILED
}
