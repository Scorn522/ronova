package dev.ronova.coremod.contract;

import dev.ronova.coremod.root.api.SubjectQualification;
import java.util.Objects;
import java.util.UUID;

/** Resolves observations against acknowledged Root bindings; never creates or merges a subject. */
public final class SubjectResolver {
    public static final int SCHEMA = 1;
    public enum Status { BOUND, UNRESOLVED, FOREIGN_REALM, CONFLICT, LEGACY_UNBOUND }
    public record Resolution(Status status, SubjectKey key, SubjectQualification qualification,
                             long resolverRevision, String provenance) {
        public Resolution {
            Objects.requireNonNull(status); Objects.requireNonNull(provenance);
            if (resolverRevision < 0 || provenance.length() > 128) throw new IllegalArgumentException("invalid resolver evidence");
            if (status == Status.BOUND && (key == null || qualification == null || !key.equals(qualification.subjectKey())))
                throw new IllegalArgumentException("bound result requires exact Root qualification");
        }
        public boolean bound() { return status == Status.BOUND; }
    }
    private SubjectResolver() {}
    /** Nonces are subject-qualified; retain every matching key instead of guessing through an ambiguous alias. */
    public static java.util.Map<UUID,java.util.Set<SubjectKey>> bindingIndex(java.util.Collection<SubjectQualification> qualifications) {
        var result=new java.util.HashMap<UUID,java.util.Set<SubjectKey>>();
        for(var q:qualifications)if(q.revisions()!=null&&q.revisions().activeIncarnation()!=null) {
            result.computeIfAbsent(q.revisions().activeIncarnation().nonce(),ignored->new java.util.HashSet<>()).add(q.subjectKey());
        }
        result.replaceAll((nonce,keys)->java.util.Set.copyOf(keys));
        return java.util.Map.copyOf(result);
    }

    public static Resolution incarnation(RealmId realm, SubjectKey claimed, UUID physicalNonce,
                                         SubjectQualification root, long acknowledgedCommitIndex) {
        Objects.requireNonNull(realm);
        if (claimed == null || physicalNonce == null)
            return result(Status.UNRESOLVED, claimed, null, acknowledgedCommitIndex);
        if (!realm.equals(claimed.realmId()))
            return result(Status.FOREIGN_REALM, claimed, null, acknowledgedCommitIndex);
        if (root == null) return result(Status.UNRESOLVED, claimed, null, acknowledgedCommitIndex);
        if (!claimed.equals(root.subjectKey())) return result(Status.CONFLICT, claimed, null, acknowledgedCommitIndex);
        if (root.revisions() == null || root.revisions().activeIncarnation() == null)
            return result(Status.LEGACY_UNBOUND, claimed, root, acknowledgedCommitIndex);
        var physical = root.revisions().activeIncarnation();
        return result(physical.subjectKey().equals(claimed) && physical.nonce().equals(physicalNonce)
            ? Status.BOUND : Status.CONFLICT, claimed, root, acknowledgedCommitIndex);
    }

    private static Resolution result(Status status, SubjectKey key, SubjectQualification root, long revision) {
        return new Resolution(status, key, root, revision, "acknowledged-root/qualification-binding/v1");
    }
}
