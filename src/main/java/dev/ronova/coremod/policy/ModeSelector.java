package dev.ronova.coremod.policy;

import dev.ronova.coremod.contract.RealmId;
import java.util.Objects;
import java.util.UUID;

/** Immutable selector for deterministic 7.16 policy resolution. */
public record ModeSelector(
    RealmId realmId,
    String dimensionId,
    String subjectClass,
    UUID subjectId,
    String stateDomainId
) {
    public static final String WILDCARD_DOMAIN = "*";

    public ModeSelector {
        dimensionId = normalizeOptional(dimensionId);
        subjectClass = normalizeOptional(subjectClass);
        stateDomainId = normalizeDomain(stateDomainId);
    }

    public ModeSelector(
        RealmId realmId,
        String dimensionId,
        String subjectClass,
        UUID subjectId
    ) {
        this(realmId, dimensionId, subjectClass, subjectId, WILDCARD_DOMAIN);
    }

    public static ModeSelector global() {
        return new ModeSelector(null, null, null, null, WILDCARD_DOMAIN);
    }

    public static ModeSelector realm(RealmId realmId) {
        return new ModeSelector(Objects.requireNonNull(realmId, "realmId"), null,
            null, null, WILDCARD_DOMAIN);
    }

    public static ModeSelector dimension(RealmId realmId, String dimensionId) {
        return new ModeSelector(Objects.requireNonNull(realmId, "realmId"), dimensionId,
            null, null, WILDCARD_DOMAIN);
    }

    public static ModeSelector subjectClass(
        RealmId realmId, String dimensionId, String subjectClass
    ) {
        return new ModeSelector(Objects.requireNonNull(realmId, "realmId"), dimensionId,
            subjectClass, null, WILDCARD_DOMAIN);
    }

    public static ModeSelector subject(
        RealmId realmId, String dimensionId, String subjectClass, UUID subjectId
    ) {
        return new ModeSelector(Objects.requireNonNull(realmId, "realmId"), dimensionId,
            subjectClass, Objects.requireNonNull(subjectId, "subjectId"), WILDCARD_DOMAIN);
    }

    public ModeSelector inDomain(String domain) {
        return new ModeSelector(realmId, dimensionId, subjectClass, subjectId, domain);
    }

    public boolean matches(ModeSelector query) {
        if (query == null) {
            return false;
        }
        return matchesValue(realmId, query.realmId())
            && matchesValue(dimensionId, query.dimensionId())
            && matchesValue(subjectClass, query.subjectClass())
            && matchesValue(subjectId, query.subjectId())
            && (WILDCARD_DOMAIN.equals(stateDomainId)
                || stateDomainId.equals(query.stateDomainId()));
    }

    /** Specificity excludes the orthogonal state-domain selector. */
    public int specificity() {
        if (subjectId != null) {
            return 5;
        }
        if (subjectClass != null) {
            return 4;
        }
        if (dimensionId != null) {
            return 3;
        }
        if (realmId != null) {
            return 2;
        }
        return 1;
    }

    public boolean domainSpecific() {
        return !WILDCARD_DOMAIN.equals(stateDomainId);
    }

    public String canonicalKey() {
        return (realmId == null ? "*" : realmId.canonical()) + '|'
            + valueOrWildcard(dimensionId) + '|'
            + valueOrWildcard(subjectClass) + '|'
            + (subjectId == null ? "*" : subjectId) + '|'
            + stateDomainId;
    }

    private static boolean matchesValue(Object selector, Object query) {
        return selector == null || selector.equals(query);
    }

    private static String normalizeOptional(String value) {
        String normalized = Objects.toString(value, "").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeDomain(String value) {
        String normalized = Objects.toString(value, WILDCARD_DOMAIN).trim();
        return normalized.isEmpty() ? WILDCARD_DOMAIN : normalized;
    }

    private static String valueOrWildcard(String value) {
        return value == null ? "*" : value;
    }
}
