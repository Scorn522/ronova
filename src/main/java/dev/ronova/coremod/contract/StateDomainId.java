package dev.ronova.coremod.contract;

import java.util.Objects;

/** Stable domain vocabulary; domain ownership does not imply ownership of other state. */
public record StateDomainId(String value) implements Comparable<StateDomainId> {
    public StateDomainId {
        Objects.requireNonNull(value, "value");
        if (!value.matches("[a-z][a-z0-9_.:/-]{0,95}")) throw new IllegalArgumentException("invalid state domain");
    }
    @Override public int compareTo(StateDomainId other) { return value.compareTo(other.value); }
}
