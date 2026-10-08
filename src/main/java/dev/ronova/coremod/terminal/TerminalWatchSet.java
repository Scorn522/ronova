package dev.ronova.coremod.terminal;

import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.WorkBudget;
import dev.ronova.coremod.engine.JudgmentMark;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Compatibility diagnostics plus the 7.16 durable tombstone-owned watch seam.
 * Logical membership is never bounded by a retry counter.
 */
public interface TerminalWatchSet {
    enum Mode {
        PASSIVE_DISABLED,
        PASSIVE_LEGACY_MIRROR,
        DURABLE_TOMBSTONE
    }

    enum Completion {
        LEGACY_ATTEMPTS_EXHAUSTED,
        ENTITY_UNRESOLVED,
        SERVER_RELEASED,
        TOMBSTONE_CLEARED
    }

    record LegacyRetryKey(UUID uuid, ResourceKey<Level> dimension) {
        public LegacyRetryKey {
            Objects.requireNonNull(uuid, "uuid");
            Objects.requireNonNull(dimension, "dimension");
        }
    }

    record Scheduled(int attempt, long nextTick) {}

    record Attempt(int attempt, boolean entityResolved, boolean reasserted) {}

    record EntrySnapshot(
        LegacyRetryKey key,
        List<Scheduled> schedules,
        List<Attempt> attempts,
        List<Completion> completions
    ) {
        public EntrySnapshot {
            Objects.requireNonNull(key, "key");
            schedules = List.copyOf(schedules);
            attempts = List.copyOf(attempts);
            completions = List.copyOf(completions);
        }
    }

    /** Immutable, UUID-scoped diagnostic view. */
    record WatchSnapshot(Mode mode, Map<LegacyRetryKey, EntrySnapshot> entries) {
        private static final WatchSnapshot DISABLED = new WatchSnapshot(
            Mode.PASSIVE_DISABLED, Map.of()
        );

        public WatchSnapshot {
            Objects.requireNonNull(mode, "mode");
            entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
        }

        public int size() {
            return entries.size();
        }

        public static WatchSnapshot disabled() {
            return DISABLED;
        }

        public static WatchSnapshot passiveEmpty() {
            return new WatchSnapshot(Mode.PASSIVE_LEGACY_MIRROR, Map.of());
        }
    }

    void observeScheduled(LegacyRetryKey key, int attempt, long nextTick);

    void observeAttempt(
        LegacyRetryKey key,
        int attempt,
        boolean entityResolved,
        boolean reasserted
    );

    void observeCompleted(LegacyRetryKey key, Completion completion);

    /** Registers one Root-committed durable tombstone and its current incarnation. */
    default void track(JudgmentMark mark, Entity incarnation) {}

    /** Promotes a tombstone-matching reinsert detected by an Admission boundary. */
    default void observeIncarnation(Entity incarnation) {}

    /** Runs bounded repeatable state assertion; logical membership is tombstone-owned. */
    default BudgetReceipt maintain(WorkBudget budget) {
        if (budget == null) {
            throw new IllegalArgumentException("budget is required");
        }
        return budget.finish(0L, 0L, size(), size());
    }

    /** Copies only diagnostic entries for one UUID; it never scans unrelated targets. */
    WatchSnapshot snapshot(UUID uuid);

    /** O(1) aggregate telemetry for logging and status surfaces. */
    TerminalWatchMetrics metrics();

    /** O(1) active-entry count; Judgment and other decision paths must not read it. */
    int size();

    /** Immediate exact-key release after a committed ClearTombstone command. */
    default void clearIdentity(UUID uuid) {}

    void clear();
}
