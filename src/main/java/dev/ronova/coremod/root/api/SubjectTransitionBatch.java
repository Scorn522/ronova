package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.*;
import java.util.*;

/** Guarded administrative transitions through the existing canonical Root command port. */
public record SubjectTransitionBatch(UUID commandId, UUID actor, DominionEpoch expectedEpoch,
    QualificationPreconditions preconditions, List<Transition> transitions) {
    public enum Kind { REBIND, REACTIVATE, SET_OWNERSHIP }
    public record Transition(SubjectKey subjectKey, Kind kind, UUID incarnationNonce,
        StateDomainId domain, SubjectKey owner) {
        public Transition {
            Objects.requireNonNull(subjectKey, "subjectKey"); Objects.requireNonNull(kind, "kind");
            if (kind == Kind.REBIND && (incarnationNonce == null || domain != null || owner != null)
                || kind == Kind.REACTIVATE && (incarnationNonce != null || domain != null || owner != null)
                || kind == Kind.SET_OWNERSHIP && (incarnationNonce != null || domain == null)
                || owner != null && !owner.realmId().equals(subjectKey.realmId())) {
                throw new IllegalArgumentException("invalid subject transition payload");
            }
        }
        public SubjectQualification apply(SubjectQualification before) {
            if (!subjectKey.equals(before.subjectKey())) throw new IllegalArgumentException("transition subject mismatch");
            return switch (kind) {
                case REBIND -> before.rebind(incarnationNonce);
                case REACTIVATE -> before.reactivate();
                case SET_OWNERSHIP -> before.own(domain, owner);
            };
        }
    }
    public SubjectTransitionBatch {
        Objects.requireNonNull(commandId, "commandId"); Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(expectedEpoch, "expectedEpoch"); Objects.requireNonNull(preconditions, "preconditions");
        transitions = Objects.requireNonNull(transitions, "transitions").stream()
            .sorted(Comparator.comparing(Transition::subjectKey)).toList();
        var keys = new HashSet<SubjectKey>();
        for (Transition transition : transitions) {
            if (!keys.add(transition.subjectKey()) || !transition.subjectKey().realmId().equals(preconditions.realmId())) {
                throw new IllegalArgumentException("duplicate or foreign transition subject");
            }
        }
        if (!keys.equals(new HashSet<>(preconditions.subjects().stream().map(SubjectQualification::subjectKey).toList()))) {
            throw new IllegalArgumentException("transition preconditions must cover the exact batch");
        }
    }
    public List<SubjectQualification> changedQualifications() {
        List<SubjectQualification> changed = new ArrayList<>();
        for (int i = 0; i < transitions.size(); i++) {
            SubjectQualification before = preconditions.subjects().get(i);
            SubjectQualification after = transitions.get(i).apply(before);
            if (!after.equals(before)) changed.add(after);
        }
        return List.copyOf(changed);
    }
    public String canonicalKey() {
        StringBuilder value = new StringBuilder("subject-transitions-v1:").append(actor).append(':')
            .append(expectedEpoch.generation()).append(':').append(expectedEpoch.sequence()).append(';')
            .append(preconditions.canonicalKey());
        for (Transition t : transitions) value.append('|').append(t.subjectKey().canonical()).append('|')
            .append(t.kind()).append('|').append(t.incarnationNonce()).append('|')
            .append(t.domain() == null ? "-" : t.domain().value()).append('|')
            .append(t.owner() == null ? "-" : t.owner().canonical());
        return value.toString();
    }
}
