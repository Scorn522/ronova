package dev.ronova.coremod.persistence;

/** Runtime marker for an intentionally interrupted shadow operation. */
public final class ShadowCrashException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ShadowCrashException(CrashFailpoint failpoint) {
        super("shadow failpoint interrupted operation: " + failpoint);
    }
}
