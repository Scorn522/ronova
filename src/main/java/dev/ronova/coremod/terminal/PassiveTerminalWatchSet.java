package dev.ronova.coremod.terminal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Runtime-only diagnostic mirror. It never schedules, resolves, or reasserts entities. */
public final class PassiveTerminalWatchSet implements TerminalWatchSet {
    static final int MAX_DIAGNOSTIC_HISTORY = 1024;

    private final Map<UUID, LinkedHashMap<LegacyRetryKey, MutableEntry>> activeByUuid =
        new LinkedHashMap<>();
    private final LinkedHashMap<LegacyRetryKey, EntrySnapshot> completedHistory =
        new LinkedHashMap<>();
    private final Map<UUID, LinkedHashMap<LegacyRetryKey, EntrySnapshot>> completedByUuid =
        new LinkedHashMap<>();
    private int activeEntries;
    private long scheduledNotifications;
    private long attemptNotifications;
    private long completionNotifications;

    @Override
    public synchronized void observeScheduled(LegacyRetryKey key, int attempt, long nextTick) {
        requireAttempt(attempt);
        entry(key).schedules.add(new Scheduled(attempt, nextTick));
        scheduledNotifications++;
    }

    @Override
    public synchronized void observeAttempt(
        LegacyRetryKey key,
        int attempt,
        boolean entityResolved,
        boolean reasserted
    ) {
        requireAttempt(attempt);
        entry(key).attempts.add(new Attempt(attempt, entityResolved, reasserted));
        attemptNotifications++;
    }

    @Override
    public synchronized void observeCompleted(LegacyRetryKey key, Completion completion) {
        MutableEntry value = entry(key);
        value.completions.add(Objects.requireNonNull(completion, "completion"));
        completionNotifications++;
        EntrySnapshot snapshot = new EntrySnapshot(
            key, value.schedules, value.attempts, value.completions
        );
        removeActive(key);
        retainCompleted(snapshot);
    }

    @Override
    public synchronized WatchSnapshot snapshot(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        Map<LegacyRetryKey, EntrySnapshot> copy = new LinkedHashMap<>();
        Map<LegacyRetryKey, EntrySnapshot> completed = completedByUuid.get(uuid);
        if (completed != null) {
            copy.putAll(completed);
        }
        Map<LegacyRetryKey, MutableEntry> active = activeByUuid.get(uuid);
        if (active != null) {
            for (Map.Entry<LegacyRetryKey, MutableEntry> entry : active.entrySet()) {
                MutableEntry value = entry.getValue();
                copy.put(entry.getKey(), new EntrySnapshot(
                    entry.getKey(), value.schedules, value.attempts, value.completions
                ));
            }
        }
        return new WatchSnapshot(Mode.PASSIVE_LEGACY_MIRROR, copy);
    }

    @Override
    public synchronized TerminalWatchMetrics metrics() {
        return new TerminalWatchMetrics(
            Mode.PASSIVE_LEGACY_MIRROR,
            activeEntries,
            completedHistory.size(),
            scheduledNotifications,
            attemptNotifications,
            completionNotifications
        );
    }

    @Override
    public synchronized int size() {
        return activeEntries;
    }

    @Override
    public synchronized void clear() {
        activeByUuid.clear();
        completedHistory.clear();
        completedByUuid.clear();
        activeEntries = 0;
        scheduledNotifications = 0L;
        attemptNotifications = 0L;
        completionNotifications = 0L;
    }

    private MutableEntry entry(LegacyRetryKey key) {
        Objects.requireNonNull(key, "key");
        discardCompleted(key);
        LinkedHashMap<LegacyRetryKey, MutableEntry> entries = activeByUuid.computeIfAbsent(
            key.uuid(), ignored -> new LinkedHashMap<>()
        );
        MutableEntry value = entries.get(key);
        if (value == null) {
            value = new MutableEntry();
            entries.put(key, value);
            activeEntries++;
        }
        return value;
    }

    private void removeActive(LegacyRetryKey key) {
        LinkedHashMap<LegacyRetryKey, MutableEntry> entries = activeByUuid.get(key.uuid());
        if (entries == null || entries.remove(key) == null) {
            return;
        }
        activeEntries--;
        if (entries.isEmpty()) {
            activeByUuid.remove(key.uuid());
        }
    }

    private void retainCompleted(EntrySnapshot snapshot) {
        LegacyRetryKey key = snapshot.key();
        discardCompleted(key);
        completedHistory.put(key, snapshot);
        completedByUuid.computeIfAbsent(
            key.uuid(), ignored -> new LinkedHashMap<>()
        ).put(key, snapshot);
        while (completedHistory.size() > MAX_DIAGNOSTIC_HISTORY) {
            Iterator<Map.Entry<LegacyRetryKey, EntrySnapshot>> iterator =
                completedHistory.entrySet().iterator();
            LegacyRetryKey oldest = iterator.next().getKey();
            iterator.remove();
            removeCompletedIndex(oldest);
        }
    }

    private void discardCompleted(LegacyRetryKey key) {
        if (completedHistory.remove(key) != null) {
            removeCompletedIndex(key);
        }
    }

    private void removeCompletedIndex(LegacyRetryKey key) {
        LinkedHashMap<LegacyRetryKey, EntrySnapshot> entries = completedByUuid.get(key.uuid());
        if (entries == null) {
            return;
        }
        entries.remove(key);
        if (entries.isEmpty()) {
            completedByUuid.remove(key.uuid());
        }
    }

    private static void requireAttempt(int attempt) {
        if (attempt < 0) {
            throw new IllegalArgumentException("attempt must be non-negative");
        }
    }

    private static final class MutableEntry {
        private final List<Scheduled> schedules = new ArrayList<>();
        private final List<Attempt> attempts = new ArrayList<>();
        private final List<Completion> completions = new ArrayList<>();
    }
}
