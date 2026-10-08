package dev.ronova.coremod.persistence;

/** Explicit failure of the 7.16 authoritative Root durability boundary. */
public final class DurableRootPersistenceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DurableRootPersistenceException(String message) {
        super(message);
    }

    public DurableRootPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
