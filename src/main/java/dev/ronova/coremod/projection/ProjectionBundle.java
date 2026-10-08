package dev.ronova.coremod.projection;

import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.presence.PresencePolicy;
import java.util.Objects;

/** All runtime projections produced from exactly one immutable snapshot epoch. */
public record ProjectionBundle(
    DominionSnapshot snapshot,
    AuthorityTopologyProjection authorityTopology,
    CapabilityProjection capabilities,
    AdmissionProjection admission,
    PresencePolicy presence
) {
    public ProjectionBundle {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        authorityTopology = Objects.requireNonNull(authorityTopology, "authorityTopology");
        capabilities = Objects.requireNonNull(capabilities, "capabilities");
        admission = Objects.requireNonNull(admission, "admission");
        presence = Objects.requireNonNull(presence, "presence");
        if (!snapshot.epoch().equals(authorityTopology.epoch())
            || !snapshot.epoch().equals(admission.epoch())) {
            throw new IllegalArgumentException("projection epoch mismatch");
        }
    }
}
