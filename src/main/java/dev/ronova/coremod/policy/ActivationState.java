package dev.ronova.coremod.policy;

/** Runtime activation is independent from the configured and supported levels. */
public enum ActivationState {
    INACTIVE,
    ACTIVE,
    PENDING_RESTART,
    UNAVAILABLE,
    REJECTED,
    FENCED
}
