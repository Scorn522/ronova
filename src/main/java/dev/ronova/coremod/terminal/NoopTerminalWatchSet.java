package dev.ronova.coremod.terminal;

import java.util.Objects;
import java.util.UUID;

/** Stateless rollback implementation for the legacy 7.13 mode. */
public final class NoopTerminalWatchSet implements TerminalWatchSet {
    public static final NoopTerminalWatchSet INSTANCE = new NoopTerminalWatchSet();

    private NoopTerminalWatchSet() {}

    @Override
    public void observeScheduled(LegacyRetryKey key, int attempt, long nextTick) {}

    @Override
    public void observeAttempt(
        LegacyRetryKey key,
        int attempt,
        boolean entityResolved,
        boolean reasserted
    ) {}

    @Override
    public void observeCompleted(LegacyRetryKey key, Completion completion) {}

    @Override
    public WatchSnapshot snapshot(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return WatchSnapshot.disabled();
    }

    @Override
    public TerminalWatchMetrics metrics() {
        return TerminalWatchMetrics.disabled();
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void clear() {}
}
