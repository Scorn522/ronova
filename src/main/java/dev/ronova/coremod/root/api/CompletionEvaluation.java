package dev.ronova.coremod.root.api;

import java.util.Objects;
import java.util.UUID;
import java.util.Locale;

/** Current, revisioned evaluation; it is evidence and never a second Truth. */
public record CompletionEvaluation(
    UUID operationId,
    CompletionContractId contract,
    long revision,
    boolean completionSatisfied,
    String evidenceHash,
    String detail
) {
    public CompletionEvaluation {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(contract, "contract");
        if (revision <= 0L) {
            throw new IllegalArgumentException("completion revision must be positive");
        }
        evidenceHash = Objects.toString(evidenceHash, "").trim()
            .toLowerCase(Locale.ROOT);
        detail = Objects.toString(detail, "").trim();
        if (completionSatisfied && evidenceHash.isEmpty()) {
            throw new IllegalArgumentException(
                "satisfied completion requires evidence hash"
            );
        }
    }
}
