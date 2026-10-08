package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.*;
import java.util.*;

/** Independently scoped counters in the existing Root subject record. */
public record SubjectRevisions(long bindingRevision, PhysicalIncarnationId activeIncarnation,
    long judgmentRevision, long admissionTokenRevision, long revokedThroughEpoch,
    Map<StateDomainId, OwnershipBinding> ownership) {
    public SubjectRevisions {
        if (bindingRevision < 0 || judgmentRevision < 0 || admissionTokenRevision < 0 || revokedThroughEpoch < 0
            || (bindingRevision == 0) != (activeIncarnation == null)) {
            throw new IllegalArgumentException("invalid subject revision vector");
        }
        ownership = Collections.unmodifiableMap(new TreeMap<>(Objects.requireNonNull(ownership, "ownership")));
        if (ownership.size() > 128 || ownership.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("invalid ownership domain count or value");
        }
    }
    public static SubjectRevisions initial() { return new SubjectRevisions(0, null, 0, 0, 0, Map.of()); }
    public SubjectRevisions bind(SubjectKey key, UUID nonce) {
        if (activeIncarnation != null && activeIncarnation.nonce().equals(nonce)) return this;
        return new SubjectRevisions(Math.addExact(bindingRevision, 1), new PhysicalIncarnationId(key,
            activeIncarnation == null ? 1 : Math.addExact(activeIncarnation.sequence(), 1), nonce),
            judgmentRevision, admissionTokenRevision, revokedThroughEpoch, ownership);
    }
    public SubjectRevisions qualify(long previousEpoch, boolean judgmentChanged) {
        return new SubjectRevisions(bindingRevision, activeIncarnation,
            judgmentChanged ? Math.addExact(judgmentRevision, 1) : judgmentRevision,
            Math.addExact(admissionTokenRevision, 1), previousEpoch, ownership);
    }
    public SubjectRevisions own(StateDomainId domain, SubjectKey owner) {
        OwnershipBinding old = ownership.get(domain);
        if (Objects.equals(old == null ? null : old.owner(), owner)) return this;
        var next = new TreeMap<>(ownership);
        next.put(domain, new OwnershipBinding(old == null ? 1 : Math.addExact(old.revision(), 1), owner));
        return new SubjectRevisions(bindingRevision, activeIncarnation, judgmentRevision,
            admissionTokenRevision, revokedThroughEpoch, next);
    }
    public String canonical() {
        StringBuilder value = new StringBuilder().append(bindingRevision).append('|')
            .append(activeIncarnation == null ? "-" : activeIncarnation.canonical()).append('|')
            .append(judgmentRevision).append('|').append(admissionTokenRevision).append('|')
            .append(revokedThroughEpoch);
        ownership.forEach((domain, binding) -> value.append('|').append(domain.value()).append('=')
            .append(binding.revision()).append(':').append(binding.owner() == null ? "-" : binding.owner().canonical()));
        return value.toString();
    }
}
