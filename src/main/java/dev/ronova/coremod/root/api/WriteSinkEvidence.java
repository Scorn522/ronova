package dev.ronova.coremod.root.api;

import dev.ronova.coremod.policy.PolicyRevision;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Bounded evidence emitted by a registered final write gate. */
public record WriteSinkEvidence(
    UUID operationId,
    WriteSinkId sink,
    TargetEpoch targetEpoch,
    PolicyRevision policyRevision,
    Outcome outcome,
    String evidenceHash
) {
    public WriteSinkEvidence {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(sink, "sink");
        Objects.requireNonNull(targetEpoch, "targetEpoch");
        Objects.requireNonNull(policyRevision, "policyRevision");
        Objects.requireNonNull(outcome, "outcome");
        evidenceHash = Objects.toString(evidenceHash, "").trim()
            .toLowerCase(Locale.ROOT);
        if (evidenceHash.isEmpty()) {
            throw new IllegalArgumentException("write sink evidence hash is required");
        }
    }

    public enum Outcome {
        ACCEPTED,
        REJECTED_STALE_EPOCH,
        REJECTED_POLICY,
        FENCED_UNVERIFIED
    }
}
