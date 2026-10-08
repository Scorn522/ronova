package dev.ronova.coremod.core;

import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionRequest;

/** Local policy proposals; a requested identity scope must never widen to a lineage. */
public final class LocalDominionDecisionSource implements DominionDecisionSource {
    public static final LocalDominionDecisionSource INSTANCE =
        new LocalDominionDecisionSource();

    private LocalDominionDecisionSource() {}

    @Override
    public DominionDecision decide(DominionRequest request) {
        if (request instanceof DominionRequest.Authority authority) {
            return new DominionDecision.Authority(
                authority.subject(), authority.epoch(), authority.recordedOwner(),
                authority.recordedOwner() ? CapabilitySet.OWNER_DEFAULT : CapabilitySet.NONE,
                authority.recordedOwner() ? "ledger-owner" : "not-ledger-owner"
            );
        }
        if (request instanceof DominionRequest.Judgment judgment) {
            DominionDecision.LineageScope scope = judgment.subject().player()
                ? DominionDecision.LineageScope.IDENTITY
                : judgment.requestedScope();
            boolean committed = judgment.actorAuthorized() && !judgment.alreadyTerminal();
            double radius = scope == DominionDecision.LineageScope.IDENTITY
                ? judgment.requestedRadius() : 0.0D;
            return new DominionDecision.Judgment(
                judgment.subject(), judgment.epoch(), committed, scope, radius,
                !judgment.actorAuthorized() ? "actor-not-authorized"
                    : judgment.alreadyTerminal() ? "already-terminal" : "commit"
            );
        }
        if (request instanceof DominionRequest.Admission admission) {
            boolean admitted = !admission.combatObject() || admission.protectedSubject()
                || (!admission.alreadyTerminal() && !admission.tombstoneMatch());
            String reason;
            if (!admission.combatObject()) {
                reason = "world-result";
            } else if (admission.protectedSubject()) {
                reason = "authority-subject";
            } else if (admission.alreadyTerminal()) {
                reason = "terminal-object";
            } else if (admission.tombstoneMatch()) {
                reason = "tombstone-match";
            } else if (admission.finalBoundary() && admission.passedFinalBoundary()) {
                reason = "cached-final-pass";
            } else {
                reason = admission.finalBoundary() ? "final-pass" : "provisional-pass";
            }
            return new DominionDecision.Admission(
                admission.subject(), admission.epoch(), admitted, reason
            );
        }
        if (request instanceof DominionRequest.Presence presence) {
            return new DominionDecision.Presence(
                presence.subject(), presence.epoch(), !presence.quarantined(),
                presence.quarantined() ? "quarantined" : "present"
            );
        }
        throw new IllegalArgumentException("unsupported dominion request: " + request);
    }
}
