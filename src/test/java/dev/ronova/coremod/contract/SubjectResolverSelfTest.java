package dev.ronova.coremod.contract;

import dev.ronova.coremod.root.api.QualificationPreconditions;
import java.util.UUID;

public final class SubjectResolverSelfTest {
    public static void main(String[] args) {
        RealmId realm = new RealmId(new UUID(71, 1));
        SubjectKey key = new SubjectKey(realm, new UUID(71, 2));
        UUID nonce = new UUID(71, 3);
        var legacy = QualificationPreconditions.baseline(key);
        var root = legacy.judgmentTransition(nonce);
        var resolved = SubjectResolver.incarnation(realm, key, nonce, root, 10);
        require(resolved.bound() && resolved.qualification().equals(root) && resolved.resolverRevision() == 10,
            "acknowledged binding failed");
        require(SubjectResolver.incarnation(realm, key, UUID.randomUUID(), root, 10).status() == SubjectResolver.Status.CONFLICT,
            "another physical object inherited UUID authority");
        SubjectKey foreign = new SubjectKey(new RealmId(new UUID(71, 4)), key.subjectId());
        require(SubjectResolver.incarnation(realm, foreign, nonce, root, 10).status() == SubjectResolver.Status.FOREIGN_REALM,
            "cross-world identity accepted");
        require(SubjectResolver.incarnation(realm, key, nonce, legacy, 10).status() == SubjectResolver.Status.LEGACY_UNBOUND,
            "legacy metadata fabricated physical binding");
        require(SubjectResolver.incarnation(realm, key, nonce, null, 10).status() == SubjectResolver.Status.UNRESOLVED,
            "unseen observation created qualification");
        SubjectKey other = new SubjectKey(realm, new UUID(71, 5));
        require(SubjectResolver.incarnation(realm, other, nonce, root, 10).status() == SubjectResolver.Status.CONFLICT,
            "mapping conflict silently merged identities");
        require(SubjectResolver.incarnation(realm, key, null, root, 10).status() == SubjectResolver.Status.UNRESOLVED,
            "missing physical evidence inherited qualification");
        require(SubjectResolver.bindingIndex(java.util.List.of(root)).get(nonce).equals(java.util.Set.of(key)),"reverse binding lost canonical subject");
        require(SubjectResolver.bindingIndex(java.util.List.of(root,QualificationPreconditions.baseline(other).rebind(nonce)))
            .get(nonce).equals(java.util.Set.of(key,other)),"subject-qualified nonce alias was guessed away");
        System.out.println("SubjectResolverSelfTest: PASS");
    }
    private static void require(boolean condition, String reason) { if (!condition) throw new AssertionError(reason); }
}
