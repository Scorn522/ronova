package dev.ronova.coremod.projection;

import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Local presence policy shared by query, render and synchronization adapters.
 * The containing projection bundle has already been accepted by the Root, so a
 * read must never cross JNI or IPC again.
 */
public final class PresenceProjection {
    private final AtomicLong evaluations = new AtomicLong();

    public DominionDecision.Presence decide(
        SubjectIdentity subject, DominionEpoch epoch, boolean quarantined
    ) {
        evaluations.incrementAndGet();
        return new DominionDecision.Presence(
            subject, epoch, !quarantined,
            quarantined ? "projection-quarantined" : "projection-present"
        );
    }

    public long evaluations() {
        return evaluations.get();
    }
}
