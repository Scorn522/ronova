package dev.ronova.coremod.policy;

/** Monotonic realm-scoped policy revision. */
public record PolicyRevision(long value) implements Comparable<PolicyRevision> {
    public static final PolicyRevision INITIAL = new PolicyRevision(1L);

    public PolicyRevision {
        if (value <= 0L) {
            throw new IllegalArgumentException("policy revision must be positive");
        }
    }

    public PolicyRevision next() {
        if (value == Long.MAX_VALUE) {
            throw new IllegalStateException("policy revision exhausted");
        }
        return new PolicyRevision(value + 1L);
    }

    public boolean isAfter(PolicyRevision other) {
        return other == null || compareTo(other) > 0;
    }

    @Override
    public int compareTo(PolicyRevision other) {
        return other == null ? 1 : Long.compare(value, other.value);
    }
}
