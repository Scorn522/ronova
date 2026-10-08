package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.contract.SubjectKey;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact acknowledged inputs to a judgment batch; never renewed during recovery. */
public record QualificationPreconditions(
    RealmId realmId, long policyRevision, List<SubjectQualification> subjects
) {
    public QualificationPreconditions {
        Objects.requireNonNull(realmId, "realmId");
        if (policyRevision <= 0L) throw new IllegalArgumentException("policy revision must be positive");
        subjects = Objects.requireNonNull(subjects, "subjects").stream()
            .sorted(Comparator.comparing(SubjectQualification::subjectKey)).toList();
        var keys = new HashSet<SubjectKey>();
        for (SubjectQualification value : subjects) {
            if (!realmId.equals(value.subjectKey().realmId()) || !keys.add(value.subjectKey())) {
                throw new IllegalArgumentException("duplicate or foreign qualification precondition");
            }
        }
    }

    /** Unseen subjects have a zero baseline; this does not issue an admission token. */
    public static SubjectQualification baseline(SubjectKey key) {
        return new SubjectQualification(new TargetEpoch(key, 0L), false);
    }

    public boolean matches(RealmId realm, long policy, Map<SubjectKey, SubjectQualification> current) {
        if (!realmId.equals(realm) || policyRevision != policy) return false;
        for (SubjectQualification expected : subjects) {
            if (!expected.equals(current.getOrDefault(expected.subjectKey(), baseline(expected.subjectKey())))) {
                return false;
            }
        }
        return true;
    }

    public String canonicalKey() {
        StringBuilder result = new StringBuilder("qualification-preconditions-v" + schemaVersion() + ":")
            .append(realmId.canonical()).append(':').append(policyRevision).append(';');
        for (SubjectQualification value : subjects) {
            result.append(value.subjectKey().canonical()).append(':')
                .append(value.targetEpoch().generation()).append(':').append(value.terminal());
            if (value.revisions() != null) result.append(":revisions-v1:").append(value.revisions().canonical());
            result.append(';');
        }
        return result.toString();
    }
    public int schemaVersion() { return subjects.stream().anyMatch(value -> value.revisions() != null) ? 2 : 1; }
}
