package dev.ronova.coremod.presence;

import dev.ronova.coremod.budget.BudgetTelemetrySnapshot;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 7.14C presence policy: the single adjudication point for whether an entity is
 * visible to external query, render and tracking sinks.
 *
 * <p>Unlike the 7.13/7.14B {@code PresenceProjection}, which only echoed the
 * caller-supplied {@code quarantined} flag and relied on a caller short-circuit
 * for immunity, this policy owns immunity as its own invariant: a quarantined
 * owner or Dominion-self subject is still {@code present=true} even when
 * {@code decide} is invoked directly, and even when the owner lookup fails.</p>
 *
 * <p>This type never influences raw observation. It is only consulted on the way
 * out, by the server and client quarantine views.</p>
 */
public final class PresencePolicy {
    private final OwnerImmunity immunity;
    private final FrameBudgetGovernor frameBudgetGovernor;
    private final AtomicLong evaluations = new AtomicLong();
    private final AtomicLong immuneDecisions = new AtomicLong();
    private final AtomicLong unresolvedImmunities = new AtomicLong();

    public PresencePolicy(OwnerImmunity immunity) {
        this(immunity, null);
    }

    /** Creates the same synchronous policy with optional measurement-only telemetry. */
    public PresencePolicy(
        OwnerImmunity immunity,
        FrameBudgetGovernor frameBudgetGovernor
    ) {
        this.immunity = Objects.requireNonNull(immunity, "immunity");
        this.frameBudgetGovernor = frameBudgetGovernor;
    }

    /**
     * Adjudicates presence. Immunity is applied here, so no caller can bypass
     * it, and a failed owner lookup fails static to present.
     */
    public DominionDecision.Presence decide(
        SubjectIdentity subject, DominionEpoch epoch, boolean quarantined
    ) {
        long startedNanos = frameBudgetGovernor == null ? 0L : System.nanoTime();
        try {
            evaluations.incrementAndGet();
            OwnerImmunity.Kind kind = immunity.classify(subject);
            if (kind == OwnerImmunity.Kind.UNRESOLVED) {
                unresolvedImmunities.incrementAndGet();
            }
            if (kind.immune()) {
                immuneDecisions.incrementAndGet();
                return new DominionDecision.Presence(subject, epoch, true, kind.reason());
            }
            return new DominionDecision.Presence(
                subject, epoch, !quarantined,
                quarantined ? "presence-quarantined" : "presence-visible"
            );
        } finally {
            recordTelemetry(startedNanos);
        }
    }

    private void recordTelemetry(long startedNanos) {
        if (frameBudgetGovernor == null) {
            return;
        }
        long elapsedNanos = Math.max(0L, System.nanoTime() - startedNanos);
        try {
            frameBudgetGovernor.recordSynchronousQuery(
                BudgetTelemetrySnapshot.QueryKind.PRESENCE_QUERY,
                elapsedNanos / 1_000L
            );
        } catch (RuntimeException ignored) {
            // Telemetry accounting must never change the synchronous Presence answer.
        }
    }

    /** Immunity predicate owned by this policy, exposed for diagnostics only. */
    public OwnerImmunity immunity() {
        return immunity;
    }

    public long evaluations() {
        return evaluations.get();
    }

    public long immuneDecisions() {
        return immuneDecisions.get();
    }

    public long unresolvedImmunities() {
        return unresolvedImmunities.get();
    }
}
