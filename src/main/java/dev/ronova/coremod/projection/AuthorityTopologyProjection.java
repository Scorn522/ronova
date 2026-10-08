package dev.ronova.coremod.projection;

import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Immutable owner/topology truth derived from one Dominion snapshot. */
public final class AuthorityTopologyProjection {
    private final DominionEpoch epoch;
    private final Map<UUID, DominionDecision.Authority> authorities;

    public AuthorityTopologyProjection(DominionSnapshot snapshot) {
        this.epoch = snapshot.epoch();
        Map<UUID, DominionDecision.Authority> projected = new LinkedHashMap<>();
        for (DominionDecision.Authority authority : snapshot.authorities()) {
            UUID uuid = authority.subject().uuid();
            if (authority.authorized() && uuid != null) {
                projected.put(uuid, authority);
            }
        }
        this.authorities = Map.copyOf(projected);
    }

    public DominionEpoch epoch() {
        return epoch;
    }

    public boolean owns(UUID uuid) {
        return uuid != null && authorities.containsKey(uuid);
    }

    public DominionDecision.Authority authority(UUID uuid) {
        return uuid == null ? null : authorities.get(uuid);
    }

    /** Resolves Authority entirely from the immutable Root-approved projection. */
    public DominionDecision.Authority resolve(SubjectIdentity subject) {
        DominionDecision.Authority projected = subject == null || subject.uuid() == null
            ? null : authorities.get(subject.uuid());
        if (projected != null) {
            return projected;
        }
        return new DominionDecision.Authority(
            subject, epoch, false, CapabilitySet.NONE, "projection-not-owner"
        );
    }

    public int ownerCount() {
        return authorities.size();
    }
}
