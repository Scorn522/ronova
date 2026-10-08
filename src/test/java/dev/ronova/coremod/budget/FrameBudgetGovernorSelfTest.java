package dev.ronova.coremod.budget;

import java.util.EnumMap;
import java.util.Map;

/** 7.14D governor isolation, reset and receipt accounting gate. */
public final class FrameBudgetGovernorSelfTest {
    private FrameBudgetGovernorSelfTest() {}

    public static void main(String[] args) {
        verifiesCategoryIsolationAndReceipts();
        verifiesTickResetAndMonotonicIdentity();
        verifiesTimeGateAndValidation();
        verifiesImmutableSnapshotAndQueryTelemetry();
        System.out.println("FrameBudgetGovernorSelfTest: PASS");
    }

    private static void verifiesCategoryIsolationAndReceipts() {
        MutableClock clock = new MutableClock();
        FrameBudgetGovernor governor = governor(clock, 10_000L, 2);
        governor.beginTick(4L);

        WorkBudget terminal = governor.budget(BudgetCategory.TERMINAL);
        WorkBudget admission = governor.budget(BudgetCategory.ADMISSION);
        require(terminal.tryConsumeCount(2), "terminal count capacity was not available");
        require(!terminal.tryConsumeCount(1), "terminal count limit was not enforced");
        terminal.deferCount(1);
        BudgetReceipt terminalReceipt = terminal.finish(2L, 1L, 3L, 2L);
        require(terminalReceipt.attempted() == 3L
                && terminalReceipt.processed() == 2L
                && terminalReceipt.deferred() == 1L,
            "terminal receipt accounting is incorrect");
        require(terminalReceipt.carryIn() == 3L
                && terminalReceipt.carryOut() == 2L
                && terminalReceipt.maxObservedBacklog() == 3L,
            "terminal backlog accounting is incorrect");

        require(admission.tryConsumeCount(1),
            "terminal exhaustion borrowed or consumed admission capacity");
        BudgetReceipt admissionReceipt = admission.finish(1L, 0L);
        require(admissionReceipt.category() == BudgetCategory.ADMISSION
                && admissionReceipt.attempted() == 1L,
            "admission receipt was not isolated");

        BudgetTelemetrySnapshot snapshot = governor.snapshot();
        require(snapshot.categoryReceipts().get(BudgetCategory.TERMINAL).overBudget(),
            "terminal exhaustion did not set the receipt over-budget flag");
        require(snapshot.overBudgetCounts().get(BudgetCategory.TERMINAL) == 1L,
            "terminal over-budget count is not exact");
        require(snapshot.overBudgetCounts().get(BudgetCategory.ADMISSION) == 0L,
            "admission inherited terminal over-budget state");
    }

    private static void verifiesTickResetAndMonotonicIdentity() {
        MutableClock clock = new MutableClock();
        FrameBudgetGovernor governor = governor(clock, 10_000L, 1);
        governor.beginTick(10L);
        WorkBudget budget = governor.budget(BudgetCategory.TERMINAL);
        require(budget.tryConsumeCount(1), "reset fixture could not reserve work");
        require(!budget.tryConsumeCount(1), "reset fixture did not exhaust count capacity");
        budget.deferCount(1);
        budget.finish(1L, 1L);
        governor.recordSynchronousQuery(
            BudgetTelemetrySnapshot.QueryKind.AUTHORITY_QUERY, 7L
        );
        require(governor.snapshot().tickId() == 10L, "first tick identity was lost");

        governor.beginTick(11L);
        BudgetTelemetrySnapshot reset = governor.snapshot();
        require(reset.tickId() == 11L
                && reset.categoryReceipts().get(BudgetCategory.TERMINAL).attempted() == 0L
                && reset.categoryReceipts().get(BudgetCategory.TERMINAL).deferred() == 0L
                && !reset.categoryReceipts().get(BudgetCategory.TERMINAL).overBudget(),
            "budget receipts did not reset at the next tick");
        require(reset.synchronousQueries()
                .get(BudgetTelemetrySnapshot.QueryKind.AUTHORITY_QUERY).callCount() == 0L,
            "synchronous query counters did not reset at the next tick");
        require(reset.overBudgetCounts().get(BudgetCategory.TERMINAL) == 0L,
            "over-budget counters did not reset at the next tick");

        boolean rejected = false;
        try {
            governor.beginTick(10L);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "a regressing tick identity was accepted");
    }

    private static void verifiesTimeGateAndValidation() {
        MutableClock clock = new MutableClock();
        FrameBudgetGovernor governor = governor(clock, 100L, 10);
        governor.beginTick(1L);
        WorkBudget budget = governor.budget(BudgetCategory.TERMINAL);
        require(budget.tryConsumeCount(1), "time-gate fixture could not reserve work");
        clock.advanceNanos(200_000L);
        require(!budget.hasTimeRemaining(), "time gate stayed open past its limit");
        require(!budget.tryConsumeCount(1),
            "time exhaustion did not reject the next deferrable unit");
        require(budget.remainingCount() == 9,
            "a rejected unit consumed count capacity");
        budget.deferCount(1);
        BudgetReceipt receipt = budget.finish(1L, 1L, 2L, 1L);
        require(receipt.attempted() == receipt.processed() + receipt.deferred()
                && receipt.overBudget(),
            "time-gated receipt violates accounting or over-budget semantics");

        boolean invalidCount = false;
        try {
            budget.tryConsumeCount(0);
        } catch (IllegalArgumentException expected) {
            invalidCount = true;
        }
        require(invalidCount, "non-positive reservation was accepted");

        boolean invalidBudget = false;
        EnumMap<BudgetCategory, Long> times = new EnumMap<>(BudgetCategory.class);
        times.put(BudgetCategory.TERMINAL, 0L);
        try {
            new FrameBudgetGovernor(
                new BudgetSplitFlags.Selection(true, BudgetSplitFlags.Source.DEFAULT, null),
                clock::now,
                times,
                null
            );
        } catch (IllegalArgumentException expected) {
            invalidBudget = true;
        }
        require(invalidBudget, "a non-positive time envelope was accepted");
    }

    private static void verifiesImmutableSnapshotAndQueryTelemetry() {
        MutableClock clock = new MutableClock();
        FrameBudgetGovernor governor = governor(clock, 10_000L, 10);
        governor.beginTick(8L);
        governor.setSnapshotEpoch(1234L);
        governor.recordSynchronousQuery(
            BudgetTelemetrySnapshot.QueryKind.AUTHORITY_QUERY, 4L
        );
        governor.recordSynchronousQuery(
            BudgetTelemetrySnapshot.QueryKind.AUTHORITY_QUERY, 9L
        );
        governor.recordSynchronousQuery(
            BudgetTelemetrySnapshot.QueryKind.PRESENCE_QUERY, 2L
        );
        BudgetReceipt discovery = governor.recordMeasurement(
            BudgetCategory.DISCOVERY, 5L, 5L, 0L, 0L, 0L, 17L
        );
        require(discovery.attempted() == 5L && discovery.processed() == 5L
                && discovery.deferred() == 0L && discovery.elapsedUs() == 17L,
            "measurement-only receipt is incorrect");

        BudgetTelemetrySnapshot snapshot = governor.snapshot();
        require(snapshot.tickId() == 8L && snapshot.snapshotEpoch() == 1234L,
            "snapshot diagnostic identity is incorrect");
        BudgetTelemetrySnapshot.SynchronousQueryTelemetry authority =
            snapshot.synchronousQueries().get(
                BudgetTelemetrySnapshot.QueryKind.AUTHORITY_QUERY
            );
        require(authority.callCount() == 2L
                && authority.totalElapsedUs() == 13L
                && authority.maxElapsedUs() == 9L,
            "authority query telemetry is incorrect");

        assertUnmodifiable(snapshot.categoryReceipts(), "category receipt map");
        assertUnmodifiable(snapshot.synchronousQueries(), "query telemetry map");
        assertUnmodifiable(snapshot.overBudgetCounts(), "over-budget map");
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

    private static void assertUnmodifiable(Map<?, ?> map, String label) {
        boolean rejected = false;
        try {
            map.clear();
        } catch (UnsupportedOperationException expected) {
            rejected = true;
        }
        require(rejected, label + " is mutable");
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
