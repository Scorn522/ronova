package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.SubjectKey;
import java.util.Objects;

/** One acknowledged Root read. It records expectations, not permission to create new Truth. */
public record AuthorityStamp(SubjectKey subjectKey, SubjectQualification qualification,
    long policyRevision, boolean owner, long ownerAuthorizationRevision, boolean settled,
    dev.ronova.coremod.contract.DominionEpoch authorityEpoch) {
    public AuthorityStamp {
        Objects.requireNonNull(subjectKey, "subjectKey");
        Objects.requireNonNull(authorityEpoch, "authorityEpoch");
        if (policyRevision <= 0 || ownerAuthorizationRevision < 0
            || qualification != null && !subjectKey.equals(qualification.subjectKey())) {
            throw new IllegalArgumentException("invalid authority stamp");
        }
    }
    /** Unrelated Root transactions do not change this subject's or actor's qualification. */
    public boolean sameAuthority(AuthorityStamp other) {
        return other != null && subjectKey.equals(other.subjectKey) && Objects.equals(qualification, other.qualification)
            && policyRevision == other.policyRevision && owner == other.owner
            && ownerAuthorizationRevision == other.ownerAuthorizationRevision && settled == other.settled;
    }
}
