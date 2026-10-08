package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.SubjectIdentity;
import dev.ronova.coremod.engine.JudgmentScope;
import dev.ronova.coremod.engine.LineageIndex;
import dev.ronova.coremod.engine.LineageRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Shadow-side tombstone component. It deliberately has no FIFO limit or
 * lineage-folding operation; storage optimization must preserve exact matches.
 */
public final class TombstoneStore {
    private final LineageIndex<Record> records = new LineageIndex<>();

    public synchronized Record put(Record record) {
        return records.put(Objects.requireNonNull(record, "record"));
    }

    public synchronized Record remove(UUID uuid) {
        return records.remove(uuid);
    }

    public synchronized Record get(UUID uuid) {
        return records.get(uuid);
    }

    public synchronized boolean contains(UUID uuid) {
        return uuid != null && records.contains(uuid);
    }

    public synchronized boolean matches(
        UUID uuid,
        String type,
        String dimension,
        double x,
        double y,
        double z
    ) {
        return records.matches(uuid, type, dimension, x, y, z);
    }

    public synchronized boolean matchesDetachedLineage(String type, String dimension) {
        return records.matchesDetachedLineage(type, dimension);
    }

    public synchronized int size() {
        return records.size();
    }

    public synchronized List<Record> snapshot() {
        return records.values();
    }

    public synchronized void replaceAll(Iterable<Record> values) {
        records.replaceAll(values);
    }

    public synchronized void clear() {
        records.clear();
    }

    /** Exact durable fact copied from the legacy ledger without a capacity cap. */
    public record Record(
        UUID uuid,
        String type,
        String className,
        String dimension,
        double x,
        double y,
        double z,
        double radius,
        JudgmentScope scope,
        long sequence,
        Boolean playerOverride
    ) implements LineageRecord {
        public Record {
            if (playerOverride != null && playerOverride == (scope == JudgmentScope.IDENTITY)) playerOverride = null;
            Objects.requireNonNull(uuid, "uuid");
            type = normalize(type, "unknown:unknown");
            className = normalize(className, "unknown");
            dimension = normalize(dimension, "unknown:unknown");
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("tombstone coordinates must be finite");
            }
            if (!Double.isFinite(radius) || radius < 0.0D) {
                throw new IllegalArgumentException("tombstone radius must be finite and non-negative");
            }
            scope = Objects.requireNonNull(scope, "scope");
            if (sequence < 0L) {
                throw new IllegalArgumentException("tombstone sequence must be non-negative");
            }
        }

        public Record(UUID uuid, String type, String className, String dimension, double x, double y, double z,
            double radius, JudgmentScope scope, long sequence) {
            this(uuid, type, className, dimension, x, y, z, radius, scope, sequence, null);
        }

        public DominionDecision.Judgment toDecision(DominionEpoch currentEpoch) {
            Objects.requireNonNull(currentEpoch, "currentEpoch");
            SubjectIdentity subject = new SubjectIdentity(
                uuid, type, className, dimension, x, y, z,
                playerOverride == null ? scope == JudgmentScope.IDENTITY : playerOverride
            );
            DominionEpoch decisionEpoch = new DominionEpoch(
                currentEpoch.generation(), Math.min(sequence, currentEpoch.sequence())
            );
            return new DominionDecision.Judgment(
                subject, decisionEpoch, true, toContractScope(scope),
                scope == JudgmentScope.IDENTITY ? radius : 0.0D,
                "shadow-persisted-judgment"
            );
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("uuid", uuid);
            tag.putString("type", type);
            tag.putString("class", className);
            tag.putString("dimension", dimension);
            tag.putDouble("x", x);
            tag.putDouble("y", y);
            tag.putDouble("z", z);
            tag.putDouble("radius", radius);
            tag.putString("scope", scope.name());
            tag.putLong("sequence", sequence);
            if (playerOverride != null) tag.putBoolean("subject_player", playerOverride);
            return tag;
        }

        public static Record fromTag(CompoundTag tag) {
            if (tag == null || !tag.hasUUID("uuid")
                || !tag.contains("type", Tag.TAG_STRING)
                || !tag.contains("scope", Tag.TAG_STRING)) {
                return null;
            }
            try {
                if (tag.contains("subject_player") && (!tag.contains("subject_player", Tag.TAG_BYTE)
                    || tag.getByte("subject_player") != 0 && tag.getByte("subject_player") != 1)) return null;
                return new Record(
                    tag.getUUID("uuid"), tag.getString("type"), tag.getString("class"),
                    tag.getString("dimension"), tag.getDouble("x"), tag.getDouble("y"),
                    tag.getDouble("z"), tag.getDouble("radius"),
                    JudgmentScope.valueOf(tag.getString("scope")), tag.getLong("sequence"),
                    tag.contains("subject_player") ? tag.getBoolean("subject_player") : null
                );
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        private static DominionDecision.LineageScope toContractScope(JudgmentScope value) {
            return switch (value) {
                case IDENTITY -> DominionDecision.LineageScope.IDENTITY;
                case LOCAL_LINEAGE -> DominionDecision.LineageScope.LOCAL_LINEAGE;
                case GLOBAL_LINEAGE -> DominionDecision.LineageScope.GLOBAL_LINEAGE;
            };
        }

        private static String normalize(String value, String fallback) {
            String normalized = Objects.toString(value, "").trim();
            return normalized.isEmpty() ? fallback : normalized;
        }
    }
}
