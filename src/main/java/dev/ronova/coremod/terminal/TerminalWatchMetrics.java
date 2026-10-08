package dev.ronova.coremod.terminal;

import java.util.Objects;

/** Constant-time passive-watch telemetry; it never exposes retry control state. */
public record TerminalWatchMetrics(
    TerminalWatchSet.Mode mode,
    int activeEntries,
    int retainedDiagnostics,
    long scheduledNotifications,
    long attemptNotifications,
    long completionNotifications
) {
    private static final TerminalWatchMetrics DISABLED = new TerminalWatchMetrics(
        TerminalWatchSet.Mode.PASSIVE_DISABLED, 0, 0, 0L, 0L, 0L
    );

    public TerminalWatchMetrics {
        Objects.requireNonNull(mode, "mode");
        if (activeEntries < 0 || retainedDiagnostics < 0
            || scheduledNotifications < 0L || attemptNotifications < 0L
            || completionNotifications < 0L) {
            throw new IllegalArgumentException("watch metrics must be non-negative");
        }
    }

    public static TerminalWatchMetrics disabled() {
        return DISABLED;
    }
}
