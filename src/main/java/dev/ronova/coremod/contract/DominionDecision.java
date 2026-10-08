package dev.ronova.coremod.contract;

import java.util.Objects;

/** Unified immutable result returned by every Dominion decision domain. */
public sealed interface DominionDecision permits
    DominionDecision.Authority,
    DominionDecision.Judgment,
    DominionDecision.Admission,
    DominionDecision.Presence {

    Domain domain();

    SubjectIdentity subject();

    DominionEpoch epoch();

    boolean accepted();

    String reason();

    enum Domain {
        AUTHORITY,
        JUDGMENT,
        ADMISSION,
        PRESENCE
    }

    enum LineageScope {
        IDENTITY,
        LOCAL_LINEAGE,
        GLOBAL_LINEAGE
    }

    record Authority(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean authorized,
        CapabilitySet capabilities,
        String reason
    ) implements DominionDecision {
        public Authority {
            subject = Objects.requireNonNull(subject, "subject");
            epoch = Objects.requireNonNull(epoch, "epoch");
            capabilities = authorized
                ? Objects.requireNonNull(capabilities, "capabilities") : CapabilitySet.NONE;
            reason = normalizeReason(reason);
        }

        @Override public Domain domain() { return Domain.AUTHORITY; }
        @Override public boolean accepted() { return authorized; }
    }

    record Judgment(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean committed,
        LineageScope scope,
        double radius,
        String reason
    ) implements DominionDecision {
        public Judgment {
            subject = Objects.requireNonNull(subject, "subject");
            epoch = Objects.requireNonNull(epoch, "epoch");
            scope = Objects.requireNonNull(scope, "scope");
            if (!Double.isFinite(radius) || radius < 0.0D) {
                throw new IllegalArgumentException("judgment radius must be finite and non-negative");
            }
            reason = normalizeReason(reason);
        }

        @Override public Domain domain() { return Domain.JUDGMENT; }
        @Override public boolean accepted() { return committed; }
    }

    record Admission(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean admitted,
        String reason
    ) implements DominionDecision {
        public Admission {
            subject = Objects.requireNonNull(subject, "subject");
            epoch = Objects.requireNonNull(epoch, "epoch");
            reason = normalizeReason(reason);
        }

        @Override public Domain domain() { return Domain.ADMISSION; }
        @Override public boolean accepted() { return admitted; }
    }

    record Presence(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean present,
        String reason
    ) implements DominionDecision {
        public Presence {
            subject = Objects.requireNonNull(subject, "subject");
            epoch = Objects.requireNonNull(epoch, "epoch");
            reason = normalizeReason(reason);
        }

        @Override public Domain domain() { return Domain.PRESENCE; }
        @Override public boolean accepted() { return present; }
    }

    private static String normalizeReason(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? "unspecified" : normalized;
    }
}
