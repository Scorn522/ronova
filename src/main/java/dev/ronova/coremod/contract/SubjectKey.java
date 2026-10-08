package dev.ronova.coremod.contract;

import java.util.Objects;
import java.util.UUID;

/** Logical identity. Metadata and physical incarnations never participate in equality. */
public record SubjectKey(RealmId realmId, UUID subjectId) implements Comparable<SubjectKey> {
    public SubjectKey {
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(subjectId, "subjectId");
    }

    public String canonical() { return realmId.canonical() + "/" + subjectId; }

    @Override public int compareTo(SubjectKey other) {
        int realm = realmId.value().compareTo(other.realmId.value());
        return realm != 0 ? realm : subjectId.compareTo(other.subjectId);
    }
}
