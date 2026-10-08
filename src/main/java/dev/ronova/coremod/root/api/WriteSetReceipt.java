package dev.ronova.coremod.root.api;

import dev.ronova.coremod.policy.PolicyRevision;
import java.util.*;

/** Typed proof that one or more declared final-write sinks were reached under a current upstream lease. */
public record WriteSetReceipt(
    UUID operationId,
    TargetEpoch targetEpoch,
    PolicyRevision policyRevision,
    Set<WriteSinkId> declared,
    Set<WriteSinkId> consumed,
    Set<WriteSinkId> rejected,
    boolean upstreamCurrent,
    String evidenceHash
) {
    public WriteSetReceipt {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(targetEpoch, "targetEpoch");
        Objects.requireNonNull(policyRevision, "policyRevision");
        declared = Set.copyOf(Objects.requireNonNull(declared, "declared"));
        consumed = Set.copyOf(Objects.requireNonNull(consumed, "consumed"));
        rejected = Set.copyOf(Objects.requireNonNull(rejected, "rejected"));
        evidenceHash = EffectRecord.checkedHash(evidenceHash);
        if (declared.isEmpty() || !declared.containsAll(consumed))
            throw new IllegalArgumentException("write-set receipt escaped declaration");
        var overlap = new LinkedHashSet<>(consumed); overlap.retainAll(rejected);
        if (!overlap.isEmpty()) throw new IllegalArgumentException("write-set receipt accepted and rejected same sink");
    }

    public boolean exact() {
        return upstreamCurrent && rejected.isEmpty() && declared.equals(consumed);
    }

    public boolean matches(TerminalOperationPlan plan) {
        return plan != null && operationId.equals(plan.planId())
            && targetEpoch.equals(plan.qualification().targetEpoch())
            && policyRevision.value() == plan.activePolicyRevision();
    }

    public static WriteSetReceipt combine(Collection<WriteSetReceipt> receipts) {
        Objects.requireNonNull(receipts, "receipts");
        var values = receipts.stream().filter(Objects::nonNull).toList();
        if (values.isEmpty()) return null;
        WriteSetReceipt first = values.get(0);
        var declared = new LinkedHashSet<WriteSinkId>();
        var consumed = new LinkedHashSet<WriteSinkId>();
        var rejected = new LinkedHashSet<WriteSinkId>();
        var hashes = new ArrayList<String>();
        boolean current = true;
        for (WriteSetReceipt value : values) {
            if (!first.operationId.equals(value.operationId) || !first.targetEpoch.equals(value.targetEpoch)
                || !first.policyRevision.equals(value.policyRevision))
                throw new IllegalArgumentException("cannot combine different write-set scopes");
            declared.addAll(value.declared); consumed.addAll(value.consumed); rejected.addAll(value.rejected);
            current &= value.upstreamCurrent; hashes.add(value.evidenceHash);
        }
        hashes.sort(String::compareTo);
        String hash = EffectRecord.hash("ronova:write-set-receipt/v1\n" + first.operationId + "\n"
            + first.targetEpoch.subjectKey().canonical() + "\n" + first.targetEpoch.generation() + "\n"
            + first.policyRevision.value() + "\n" + String.join("\n", hashes));
        return new WriteSetReceipt(first.operationId, first.targetEpoch, first.policyRevision,
            declared, consumed, rejected, current, hash);
    }
}
