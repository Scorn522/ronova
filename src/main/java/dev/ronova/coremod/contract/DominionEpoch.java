package dev.ronova.coremod.contract;

/** Monotonic version shared by decisions and immutable snapshots. */
public record DominionEpoch(long generation, long sequence)
    implements Comparable<DominionEpoch> {
    public static final DominionEpoch ZERO = new DominionEpoch(0L, 0L);
    public static final DominionEpoch LOCAL_INITIAL = new DominionEpoch(1L, 0L);

    public DominionEpoch {
        if (generation < 0L || sequence < 0L) {
            throw new IllegalArgumentException("epoch values must be non-negative");
        }
    }

    public DominionEpoch nextSequence() {
        if (sequence == Long.MAX_VALUE) {
            if (generation == Long.MAX_VALUE) {
                throw new IllegalStateException("dominion epoch exhausted");
            }
            return new DominionEpoch(generation + 1L, 0L);
        }
        return new DominionEpoch(generation, sequence + 1L);
    }

    public boolean isAfter(DominionEpoch other) {
        return other == null || compareTo(other) > 0;
    }

    @Override
    public int compareTo(DominionEpoch other) {
        if (other == null) {
            return 1;
        }
        int byGeneration = Long.compare(generation, other.generation);
        return byGeneration != 0 ? byGeneration : Long.compare(sequence, other.sequence);
    }
}
