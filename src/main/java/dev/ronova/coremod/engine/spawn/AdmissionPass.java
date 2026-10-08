package dev.ronova.coremod.engine.spawn;

import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.root.api.AuthorityStamp;
import java.util.Objects;
import java.util.UUID;

/** Cached decision for exactly one physical object, Root expectation and published admission view. */
public record AdmissionPass(AuthorityStamp stamp, UUID physicalNonce, DominionEpoch projectionEpoch) {
    public AdmissionPass {
        Objects.requireNonNull(stamp); Objects.requireNonNull(physicalNonce); Objects.requireNonNull(projectionEpoch);
    }
    public boolean current(AuthorityStamp now, UUID nonce, DominionEpoch view) {
        return stamp.sameAuthority(now) && now.settled() && physicalNonce.equals(nonce)
            && projectionEpoch.equals(view) && view.compareTo(now.authorityEpoch()) >= 0;
    }
}
