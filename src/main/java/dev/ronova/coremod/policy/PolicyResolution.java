package dev.ronova.coremod.policy;

import java.util.Objects;

/** Explicit outcome of deterministic policy resolution. */
public record PolicyResolution(
    Status status,
    ModeSelector query,
    PolicyOverlay overlay,
    String ruleId
) {
    public PolicyResolution {
        status = Objects.requireNonNull(status, "status");
        query = Objects.requireNonNull(query, "query");
        ruleId = Objects.toString(ruleId, "");
        if (status == Status.RESOLVED && overlay == null) {
            throw new IllegalArgumentException("resolved policy requires an overlay");
        }
        if (status != Status.RESOLVED && overlay != null) {
            throw new IllegalArgumentException("unresolved policy cannot expose an overlay");
        }
    }

    public boolean resolved() {
        return status == Status.RESOLVED;
    }

    public AuthorityMode mode() {
        return overlay == null ? AuthorityMode.OBSERVATIONAL : overlay.mode();
    }

    public enum Status {
        RESOLVED,
        NO_MATCH,
        POLICY_CONFLICT,
        INVALID
    }
}
