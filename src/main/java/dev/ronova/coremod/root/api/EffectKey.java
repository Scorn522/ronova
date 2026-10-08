package dev.ronova.coremod.root.api;

import java.util.Objects;
import java.util.UUID;

/** Stable semantic effect identity. Plan revisions and execution attempts are deliberately absent. */
public record EffectKey(UUID canonicalCommandId, TargetEpoch targetEpoch,
    String semanticOperationId, String semanticSlotId) {
    public EffectKey {
        Objects.requireNonNull(canonicalCommandId, "canonicalCommandId");
        Objects.requireNonNull(targetEpoch, "targetEpoch");
        if (targetEpoch.generation() == 0L) throw new IllegalArgumentException("legacy baseline cannot qualify a new effect");
        semanticOperationId = canonicalName(semanticOperationId);
        semanticSlotId = canonicalName(semanticSlotId);
    }

    public String canonical() {
        return targetEpoch.subjectKey().canonical() + "/" + canonicalCommandId + "/"
            + targetEpoch.generation() + "/" + semanticOperationId.length() + ":" + semanticOperationId
            + "/" + semanticSlotId.length() + ":" + semanticSlotId;
    }

    /** Structural receipt membership; current policy/lease and durable intent ACK are additional gates. */
    public boolean belongsTo(CommitReceipt receipt) {
        return receipt != null && receipt.committed() && canonicalCommandId.equals(receipt.commandId())
            && targetEpoch.equals(receipt.targetEpoch(targetEpoch.subjectKey()));
    }

    private static String canonicalName(String value) {
        Objects.requireNonNull(value, "semantic name");
        if (value.isEmpty() || value.length() > 128) throw new IllegalArgumentException("semantic name length is invalid");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= '0' && c <= '9')
                && c != ':' && c != '.' && c != '_' && c != '-' && c != '/') {
                throw new IllegalArgumentException("semantic name is not canonical");
            }
        }
        return value;
    }
}
