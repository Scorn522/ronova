package dev.ronova.coremod.policy;

import java.util.Objects;

/** Canonical scalar policy value; it carries no executable behavior. */
public record PolicyValue(String canonicalValue) implements Comparable<PolicyValue> {
    public PolicyValue {
        canonicalValue = Objects.toString(canonicalValue, "").trim();
        if (canonicalValue.isEmpty()) {
            throw new IllegalArgumentException("policy value must not be empty");
        }
    }

    public static PolicyValue of(String value) {
        return new PolicyValue(value);
    }

    public static PolicyValue of(boolean value) {
        return new PolicyValue(Boolean.toString(value));
    }

    public static PolicyValue of(long value) {
        return new PolicyValue(Long.toString(value));
    }

    @Override
    public int compareTo(PolicyValue other) {
        return other == null ? 1 : canonicalValue.compareTo(other.canonicalValue);
    }
}
