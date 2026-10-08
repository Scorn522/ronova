package dev.ronova.coremod.policy;

/** Explicit policy compilation failure; callers must not choose by load order. */
public final class PolicyConflictException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public PolicyConflictException(String message) {
        super(message);
    }
}
