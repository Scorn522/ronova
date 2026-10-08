package dev.ronova.coremod.policy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable, Root-authorized policy value for one selector. */
public record PolicyOverlay(
    PolicyRevision revision,
    ModeSelector selector,
    AuthorityMode mode,
    Map<PolicyParameterId, PolicyValue> parameters
) {
    public PolicyOverlay {
        revision = Objects.requireNonNull(revision, "revision");
        selector = Objects.requireNonNull(selector, "selector");
        mode = Objects.requireNonNull(mode, "mode");
        Map<PolicyParameterId, PolicyValue> copy = new LinkedHashMap<>();
        if (parameters != null) {
            for (Map.Entry<PolicyParameterId, PolicyValue> entry : parameters.entrySet()) {
                copy.put(
                    Objects.requireNonNull(entry.getKey(), "policy parameter id"),
                    Objects.requireNonNull(entry.getValue(), "policy parameter value")
                );
            }
        }
        parameters = Collections.unmodifiableMap(copy);
    }

    public PolicyOverlay(
        PolicyRevision revision,
        ModeSelector selector,
        AuthorityMode mode
    ) {
        this(revision, selector, mode, Map.of());
    }

    /** The effective value used to detect same-specificity conflicts. */
    public String effectiveCanonical() {
        StringBuilder value = new StringBuilder(mode.name());
        parameters.keySet().stream().sorted().forEach(key -> value.append('|')
            .append(key.canonical()).append('=').append(parameters.get(key).canonicalValue()));
        return value.toString();
    }

    public String canonicalKey() {
        return selector.canonicalKey() + '@' + revision.value() + ':' + effectiveCanonical();
    }

    public boolean sameEffectiveValue(PolicyOverlay other) {
        return other != null && effectiveCanonical().equals(other.effectiveCanonical());
    }
}
