package dev.ronova.coremod.contract;

import java.util.List;
import java.util.Objects;

/** Versioned immutable collection suitable for local, Native or Root projection. */
public record DominionSnapshot(DominionEpoch epoch, List<DominionDecision> decisions) {
    public static final DominionSnapshot EMPTY =
        new DominionSnapshot(DominionEpoch.ZERO, List.of());

    public DominionSnapshot {
        epoch = Objects.requireNonNull(epoch, "epoch");
        decisions = decisions == null ? List.of() : List.copyOf(decisions);
        for (DominionDecision decision : decisions) {
            Objects.requireNonNull(decision, "snapshot decision");
            if (decision.epoch().compareTo(epoch) > 0) {
                throw new IllegalArgumentException("snapshot contains a future decision");
            }
        }
    }

    public List<DominionDecision.Authority> authorities() {
        return decisions.stream()
            .filter(DominionDecision.Authority.class::isInstance)
            .map(DominionDecision.Authority.class::cast)
            .toList();
    }

    public List<DominionDecision.Judgment> judgments() {
        return decisions.stream()
            .filter(DominionDecision.Judgment.class::isInstance)
            .map(DominionDecision.Judgment.class::cast)
            .toList();
    }

    public boolean isNewerThan(DominionSnapshot other) {
        return other == null || epoch.isAfter(other.epoch);
    }
}
