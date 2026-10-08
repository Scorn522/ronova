package dev.ronova.coremod.contract;

import java.util.Objects;
import java.util.UUID;

/** Root-issued sequence for one observed physical object, scoped to its logical subject. */
public record PhysicalIncarnationId(SubjectKey subjectKey, long sequence, UUID nonce) {
    public PhysicalIncarnationId {
        Objects.requireNonNull(subjectKey, "subjectKey");
        Objects.requireNonNull(nonce, "nonce");
        if (sequence <= 0L) throw new IllegalArgumentException("incarnation sequence must be positive");
    }
    public String canonical() { return subjectKey.canonical() + ":" + sequence + ":" + nonce; }
}
