package dev.ronova.coremod.contract;

import dev.ronova.coremod.contract.DominionDecision.LineageScope;
import java.util.Objects;

/** Loader-neutral observed facts submitted to a Dominion decision source. */
public sealed interface DominionRequest permits
    DominionRequest.Authority,
    DominionRequest.Judgment,
    DominionRequest.Admission,
    DominionRequest.Presence {

    SubjectIdentity subject();

    DominionEpoch epoch();

    record Authority(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean recordedOwner
    ) implements DominionRequest {
        public Authority {
            subject = requiredSubject(subject);
            epoch = requiredEpoch(epoch);
        }
    }

    record Judgment(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean actorAuthorized,
        boolean alreadyTerminal,
        LineageScope requestedScope,
        double requestedRadius
    ) implements DominionRequest {
        public Judgment {
            subject = requiredSubject(subject);
            epoch = requiredEpoch(epoch);
            requestedScope = Objects.requireNonNull(requestedScope, "requestedScope");
            if (!Double.isFinite(requestedRadius) || requestedRadius < 0.0D) {
                throw new IllegalArgumentException("requested radius must be finite and non-negative");
            }
        }
    }

    record Admission(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean combatObject,
        boolean protectedSubject,
        boolean alreadyTerminal,
        boolean tombstoneMatch,
        boolean passedFinalBoundary,
        boolean finalBoundary
    ) implements DominionRequest {
        public Admission {
            subject = requiredSubject(subject);
            epoch = requiredEpoch(epoch);
        }
    }

    record Presence(
        SubjectIdentity subject,
        DominionEpoch epoch,
        boolean quarantined
    ) implements DominionRequest {
        public Presence {
            subject = requiredSubject(subject);
            epoch = requiredEpoch(epoch);
        }
    }

    private static SubjectIdentity requiredSubject(SubjectIdentity value) {
        return Objects.requireNonNull(value, "subject");
    }

    private static DominionEpoch requiredEpoch(DominionEpoch value) {
        return Objects.requireNonNull(value, "epoch");
    }
}
