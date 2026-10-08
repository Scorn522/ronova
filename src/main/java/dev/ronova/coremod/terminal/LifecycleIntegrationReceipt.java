package dev.ronova.coremod.terminal;

import dev.ronova.coremod.contract.DominionEpoch;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Durable reservation of the once-only lifecycle entry, not effect completion.
 * Historical field names and canonical encoding remain unchanged for old hashes.
 * A restored claim has EFFECT_UNKNOWN semantics: never replay an opaque callback
 * merely because its completion receipt is absent.
 */
public record LifecycleIntegrationReceipt(
    UUID judgmentId,
    String firstPhysicalIdentity,
    DominionEpoch integratedEpoch
) {
    public LifecycleIntegrationReceipt {
        Objects.requireNonNull(judgmentId, "judgmentId");
        firstPhysicalIdentity = normalize(firstPhysicalIdentity);
        integratedEpoch = Objects.requireNonNull(integratedEpoch, "integratedEpoch");
    }

    public String canonicalKey() {
        return judgmentId + "|" + firstPhysicalIdentity + "|"
            + integratedEpoch.generation() + ":" + integratedEpoch.sequence();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("judgment_id", judgmentId);
        tag.putString("first_physical_identity", firstPhysicalIdentity);
        tag.putLong("integrated_generation", integratedEpoch.generation());
        tag.putLong("integrated_sequence", integratedEpoch.sequence());
        return tag;
    }

    public static LifecycleIntegrationReceipt fromTag(CompoundTag tag) {
        if (tag == null || !tag.hasUUID("judgment_id")
            || !tag.contains("first_physical_identity", Tag.TAG_STRING)) {
            return null;
        }
        try {
            return new LifecycleIntegrationReceipt(
                tag.getUUID("judgment_id"),
                tag.getString("first_physical_identity"),
                new DominionEpoch(
                    tag.getLong("integrated_generation"),
                    tag.getLong("integrated_sequence")
                )
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String normalize(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? "unknown" : normalized;
    }
}
