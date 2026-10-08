package dev.ronova.coremod.persistence;

/** Explicit rejection for reusing one durable command id with another payload. */
public final class CommandIdConflictException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public CommandIdConflictException(String detail) {
        super("command id conflict: " + detail);
    }
}
