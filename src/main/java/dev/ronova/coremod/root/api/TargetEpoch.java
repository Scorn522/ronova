package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.SubjectKey;
import java.util.Objects;

/** Per-subject qualification generation, independent of the Realm transaction clock. */
public record TargetEpoch(SubjectKey subjectKey, long generation)
    implements Comparable<TargetEpoch> {
    public TargetEpoch {
        Objects.requireNonNull(subjectKey, "subjectKey");
        if (generation < 0L) throw new IllegalArgumentException("negative target generation");
    }

    public boolean isAfter(TargetEpoch other) {
        requireSameTarget(other);
        return generation > other.generation;
    }

    public boolean accepts(TargetEpoch submitted) {
        return equals(submitted);
    }

    @Override
    public int compareTo(TargetEpoch other) {
        requireSameTarget(other);
        return Long.compare(generation, other.generation);
    }

    private void requireSameTarget(TargetEpoch other) {
        if (other == null || !subjectKey.equals(other.subjectKey)) {
            throw new IllegalArgumentException("TargetEpoch identities differ");
        }
    }

    /** Used by Root staging only; constructing a value is not execution permission. */
    public TargetEpoch next() {
        if (generation == Long.MAX_VALUE) throw new IllegalStateException("target generation exhausted");
        return new TargetEpoch(subjectKey, generation + 1L);
    }
}
