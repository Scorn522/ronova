package dev.ronova.coremod.budget;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;

/** 7.14D exhaustion, carry and no-drop semantics gate. */
public final class BudgetSemanticsSelfTest {
    private BudgetSemanticsSelfTest() {}

    public static void main(String[] args) {
        verifiesCountExhaustionCarriesWorkForward();
        verifiesTimeExhaustionCarriesWorkForward();
        verifiesCarryIsNotInventedAttemptedWork();
        verifiesReservationRefundsAreBounded();
        System.out.println("BudgetSemanticsSelfTest: PASS");
    }

    private static void verifiesCountExhaustionCarriesWorkForward() {
        MutableClock clock = new MutableClock();
        FrameBudgetGovernor governor = governor(clock, 100_000L, 1);
        ArrayDeque<Integer> work = new ArrayDeque<>();
        work.add(1);
        work.add(2);
        work.add(3);

        BudgetReceipt first = drain(work, governor, 1L);
        require(first.attempted() == 2L && first.processed() == 1L
                && first.deferred() == 1L && first.carryIn() == 3L
                && first.carryOut() == 2L && work.size() == 2,
            "count exhaustion dropped or mis-accounted queued work");

        BudgetReceipt second = drain(work, governor, 2L);
        BudgetReceipt third = drain(work, governor, 3L);
        require(second.processed() == 1L && second.deferred() == 1L
                && third.processed() == 1L && third.deferred() == 0L
                && work.isEmpty(),
            "deferred work did not resume on later ticks");
        require(first.processed() + second.processed() + third.processed() == 3L,
            "the complete queue was not eventually processed exactly once");
    }

    private static void verifiesTimeExhaustionCarriesWorkForward() {
        MutableClock clock = new MutableClock();
        FrameBudgetGovernor governor = governor(clock, 50L, 10);
        governor.beginTick(4L);
        WorkBudget budget = governor.budget(BudgetCategory.TERMINAL);
        require(budget.tryConsumeCount(1), "time fixture could not claim its first unit");
        clock.advanceNanos(100_000L);
        require(!budget.tryConsumeCount(1), "time exhaustion did not reject the next unit");
        budget.deferCount(1);
        BudgetReceipt receipt = budget.finish(1L, 1L, 2L, 1L);
        require(receipt.processed() == 1L && receipt.deferred() == 1L
                && receipt.attempted() == 2L && receipt.carryOut() == 1L
                && receipt.overBudget(),
            "time exhaustion did not preserve deferred work and over-budget state");
    }

    private static void verifiesCarryIsNotInventedAttemptedWork() {
        MutableClock clock = new MutableClock();
        FrameBudgetGovernor governor = governor(clock, 100_000L, 10);
        governor.beginTick(5L);
        WorkBudget budget = governor.budget(BudgetCategory.TERMINAL);
        require(budget.tryConsumeCount(1), "carry fixture could not claim work");
        BudgetReceipt receipt = budget.finish(1L, 0L, 3L, 2L);
        require(receipt.attempted() == 1L && receipt.processed() == 1L
                && receipt.deferred() == 0L && receipt.carryIn() == 3L
                && receipt.carryOut() == 2L,
            "carry backlog was folded into attempted accounting");
    }

    private static void verifiesReservationRefundsAreBounded() {
        MutableClock clock = new MutableClock();
        FrameBudgetGovernor governor = governor(clock, 100_000L, 2);
        governor.beginTick(6L);
        WorkBudget budget = governor.budget(BudgetCategory.TERMINAL);
        require(budget.tryConsumeCount(1), "refund fixture could not reserve work");
        budget.refundCount(1);
        require(budget.remainingCount() == 2,
            "refunded reservation did not restore count capacity");

        boolean rejected = false;
        try {
            budget.refundCount(1);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "refund exceeded the unaccounted reservation");
        BudgetReceipt receipt = budget.finish(0L, 0L);
        require(receipt.attempted() == 0L, "refunded work remained in the receipt");
    }

    private static BudgetReceipt drain(
        ArrayDeque<Integer> work, FrameBudgetGovernor governor, long tick
    ) {
        governor.beginTick(tick);
        WorkBudget budget = governor.budget(BudgetCategory.TERMINAL);
        long carryIn = work.size();
        long processed = 0L;
        long deferred = 0L;
        while (!work.isEmpty()) {
            if (!budget.tryConsumeCount(1)) {
                budget.deferCount(1);
                deferred++;
                break;
            }
            work.removeFirst();
            processed++;
        }
        return budget.finish(processed, deferred, carryIn, work.size());
    }

    private static FrameBudgetGovernor governor(
        MutableClock clock, long terminalBudgetUs, int terminalCount
    ) {
        EnumMap<BudgetCategory, Long> times = new EnumMap<>(BudgetCategory.class);
        times.put(BudgetCategory.TERMINAL, terminalBudgetUs);
        EnumMap<BudgetCategory, Integer> counts = new EnumMap<>(BudgetCategory.class);
        counts.put(BudgetCategory.TERMINAL, terminalCount);
        return new FrameBudgetGovernor(
            new BudgetSplitFlags.Selection(true, BudgetSplitFlags.Source.DEFAULT, null),
            clock::now,
            times,
            counts
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class MutableClock {
        private long nanos;

        private long now() {
            return nanos;
        }

        private void advanceNanos(long amount) {
            nanos += amount;
        }
    }
}
