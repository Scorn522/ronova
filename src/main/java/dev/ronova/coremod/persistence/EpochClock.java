package dev.ronova.coremod.persistence;

import dev.ronova.coremod.contract.DominionEpoch;
import java.util.Objects;

/** Monotonic shadow clock; it never accepts an older authoritative epoch. */
public final class EpochClock {
    private DominionEpoch current;

    public EpochClock() {
        this(DominionEpoch.LOCAL_INITIAL);
    }

    public EpochClock(DominionEpoch initial) {
        current = Objects.requireNonNull(initial, "initial");
    }

    public synchronized DominionEpoch current() {
        return current;
    }

    public synchronized DominionEpoch next() {
        return current.nextSequence();
    }

    /** Observes an authoritative value without permitting rollback. */
    public synchronized boolean observe(DominionEpoch candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (candidate.compareTo(current) < 0) {
            return false;
        }
        current = candidate;
        return true;
    }

    /** Advances the local clock for focused model tests and future cutover work. */
    public synchronized DominionEpoch advance() {
        current = current.nextSequence();
        return current;
    }
}
