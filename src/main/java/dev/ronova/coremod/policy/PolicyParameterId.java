package dev.ronova.coremod.policy;

import java.util.Objects;

/** Stable identifier for a policy parameter without an executable payload. */
public record PolicyParameterId(String namespace, String name) implements Comparable<PolicyParameterId> {
    public PolicyParameterId {
        namespace = normalize(namespace, "core");
        name = normalize(name, "unnamed");
    }

    public PolicyParameterId(String name) {
        this("core", name);
    }

    public String canonical() {
        return namespace + ':' + name;
    }

    @Override
    public int compareTo(PolicyParameterId other) {
        if (other == null) {
            return 1;
        }
        int byNamespace = namespace.compareTo(other.namespace);
        return byNamespace != 0 ? byNamespace : name.compareTo(other.name);
    }

    private static String normalize(String value, String fallback) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
