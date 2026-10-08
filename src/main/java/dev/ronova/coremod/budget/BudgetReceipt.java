package dev.ronova.coremod.budget;

import java.util.Objects;

/** Immutable accounting result for one category in one server tick. */
public record BudgetReceipt(
    long tickId,
    BudgetCategory category,
    long attempted,
    long processed,
    long deferred,
    long carryIn,
    long carryOut,
    long elapsedUs,
    boolean overBudget,
    long maxObservedBacklog
) {
    public BudgetReceipt {
        Objects.requireNonNull(category, "category");
        if (tickId < 0L || attempted < 0L || processed < 0L || deferred < 0L
            || carryIn < 0L || carryOut < 0L || elapsedUs < 0L
            || maxObservedBacklog < 0L) {
            throw new IllegalArgumentException("budget receipt values must be non-negative");
        }
        if (Long.MAX_VALUE - processed < deferred || processed + deferred != attempted) {
            throw new IllegalArgumentException(
                "budget receipt requires processed + deferred == attempted"
            );
        }
        if (maxObservedBacklog < carryIn || maxObservedBacklog < carryOut) {
            throw new IllegalArgumentException(
                "maxObservedBacklog must cover carryIn and carryOut"
            );
        }
    }

    public static BudgetReceipt empty(long tickId, BudgetCategory category) {
        return new BudgetReceipt(tickId, category, 0L, 0L, 0L, 0L, 0L, 0L, false, 0L);
    }
}
