package dev.ronova.coremod.obligation;

import java.util.Objects;

/** Current-context observations are independent of the stale qualification carried by a submitted object. */
public record CandidateEvidence(ObligationKey key, long policyRevision, long judgmentRevision, int ruleRevision,
    String eventRef, String basisRef, Family family, long observedAt, long validUntil,
    boolean observerVerified, boolean authorizedCompliant) {
    public enum Family {
        DIRECT_RESULT_CONFLICT(true), STALE_QUALIFICATION(true), DIRECT_RECONSTRUCTION(true),
        TIME_RELATION(false), STATE_RELATION(false), CALLBACK_RELATION(false), REPEAT_RELATION(false), SOURCE_RELATION(false);
        private final boolean strong;
        Family(boolean strong) { this.strong = strong; }
        public boolean strong() { return strong; }
    }
    public CandidateEvidence {
        Objects.requireNonNull(key); Objects.requireNonNull(family);
        if (policyRevision < 1 || judgmentRevision < 0 || ruleRevision != 1 || observedAt < 0 || validUntil < observedAt)
            throw new IllegalArgumentException("invalid candidate context/window");
        eventRef = bounded(eventRef); basisRef = bounded(basisRef);
    }
    private static String bounded(String text) {
        Objects.requireNonNull(text); if (text.isBlank() || text.length() > 128) throw new IllegalArgumentException("unbounded observation reference"); return text;
    }
}
