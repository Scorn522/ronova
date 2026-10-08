package dev.ronova.coremod.budget;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Immutable, bounded per-tick budget and synchronous-query telemetry. */
public record BudgetTelemetrySnapshot(
    long tickId,
    long snapshotEpoch,
    Map<BudgetCategory, BudgetReceipt> categoryReceipts,
    Map<QueryKind, SynchronousQueryTelemetry> synchronousQueries,
    Map<BudgetCategory, Long> overBudgetCounts
) {
    public BudgetTelemetrySnapshot {
        if (tickId < 0L || snapshotEpoch < 0L) {
            throw new IllegalArgumentException("telemetry identity must be non-negative");
        }
        categoryReceipts = immutableCategoryReceipts(categoryReceipts);
        synchronousQueries = immutableQueryTelemetry(synchronousQueries);
        overBudgetCounts = immutableOverBudgetCounts(overBudgetCounts);
    }

    public enum QueryKind {
        AUTHORITY_QUERY,
        PRESENCE_QUERY
    }

    public record SynchronousQueryTelemetry(
        QueryKind queryKind,
        long callCount,
        long totalElapsedUs,
        long maxElapsedUs
    ) {
        public SynchronousQueryTelemetry {
            Objects.requireNonNull(queryKind, "queryKind");
            if (callCount < 0L || totalElapsedUs < 0L || maxElapsedUs < 0L) {
                throw new IllegalArgumentException(
                    "synchronous query telemetry must be non-negative"
                );
            }
            if (callCount == 0L && (totalElapsedUs != 0L || maxElapsedUs != 0L)) {
                throw new IllegalArgumentException(
                    "empty synchronous query telemetry cannot have elapsed time"
                );
            }
            if (callCount > 0L && maxElapsedUs > totalElapsedUs) {
                throw new IllegalArgumentException(
                    "maximum query latency cannot exceed total latency"
                );
            }
        }

        public static SynchronousQueryTelemetry empty(QueryKind kind) {
            return new SynchronousQueryTelemetry(kind, 0L, 0L, 0L);
        }
    }

    private static Map<BudgetCategory, BudgetReceipt> immutableCategoryReceipts(
        Map<BudgetCategory, BudgetReceipt> source
    ) {
        Objects.requireNonNull(source, "categoryReceipts");
        EnumMap<BudgetCategory, BudgetReceipt> copy = new EnumMap<>(BudgetCategory.class);
        copy.putAll(source);
        for (BudgetCategory category : BudgetCategory.values()) {
            BudgetReceipt receipt = copy.get(category);
            if (receipt == null) {
                throw new IllegalArgumentException(
                    "missing category receipt: " + category
                );
            }
            if (receipt.category() != category) {
                throw new IllegalArgumentException(
                    "receipt category mismatch: " + category
                );
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Map<QueryKind, SynchronousQueryTelemetry> immutableQueryTelemetry(
        Map<QueryKind, SynchronousQueryTelemetry> source
    ) {
        Objects.requireNonNull(source, "synchronousQueries");
        EnumMap<QueryKind, SynchronousQueryTelemetry> copy = new EnumMap<>(QueryKind.class);
        copy.putAll(source);
        for (QueryKind kind : QueryKind.values()) {
            SynchronousQueryTelemetry telemetry = copy.get(kind);
            if (telemetry == null) {
                throw new IllegalArgumentException("missing query telemetry: " + kind);
            }
            if (telemetry.queryKind() != kind) {
                throw new IllegalArgumentException("query telemetry kind mismatch: " + kind);
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static Map<BudgetCategory, Long> immutableOverBudgetCounts(
        Map<BudgetCategory, Long> source
    ) {
        Objects.requireNonNull(source, "overBudgetCounts");
        EnumMap<BudgetCategory, Long> copy = new EnumMap<>(BudgetCategory.class);
        for (BudgetCategory category : BudgetCategory.values()) {
            Long value = source.get(category);
            if (value == null || value < 0L) {
                throw new IllegalArgumentException(
                    "missing or negative over-budget count: " + category
                );
            }
            copy.put(category, value);
        }
        return Collections.unmodifiableMap(copy);
    }
}
