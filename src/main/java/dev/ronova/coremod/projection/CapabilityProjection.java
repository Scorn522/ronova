package dev.ronova.coremod.projection;

import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Immutable capability lookup; gameplay code never infers grants from inventory state. */
public final class CapabilityProjection {
    private final Map<UUID, CapabilitySet> capabilities;

    public CapabilityProjection(DominionSnapshot snapshot) {
        Map<UUID, CapabilitySet> projected = new LinkedHashMap<>();
        for (DominionDecision.Authority authority : snapshot.authorities()) {
            UUID uuid = authority.subject().uuid();
            if (authority.authorized() && uuid != null) {
                projected.put(uuid, authority.capabilities());
            }
        }
        this.capabilities = Map.copyOf(projected);
    }

    public CapabilitySet capabilities(UUID uuid) {
        return uuid == null ? CapabilitySet.NONE
            : capabilities.getOrDefault(uuid, CapabilitySet.NONE);
    }

    public boolean allows(UUID uuid, Capability capability) {
        return capability != null && capabilities(uuid).contains(capability);
    }
}
