package dev.ronova.coremod.persistence;

/** Fail-closed signal for a persisted realm_id that cannot be trusted. */
public final class RealmIdLoadException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public RealmIdLoadException(String detail) {
        super("realm_id load rejected: " + detail);
    }

    public RealmIdLoadException(String detail, Throwable cause) {
        super("realm_id load rejected: " + detail, cause);
    }
}
