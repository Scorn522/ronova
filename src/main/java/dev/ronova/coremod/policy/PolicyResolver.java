package dev.ronova.coremod.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Deterministic selector resolver; it never resolves conflicts by load order. */
public final class PolicyResolver {
    private static final Comparator<PolicyOverlay> STABLE_ORDER =
        Comparator.comparing((PolicyOverlay value) -> value.selector().canonicalKey())
            .thenComparing(value -> value.revision().value())
            .thenComparing(PolicyOverlay::effectiveCanonical);

    private PolicyResolver() {}

    public static PolicyResolution resolve(
        Collection<PolicyOverlay> overlays,
        ModeSelector query
    ) {
        Objects.requireNonNull(query, "query");
        if (overlays == null || overlays.isEmpty()) {
            return new PolicyResolution(PolicyResolution.Status.NO_MATCH, query, null, "");
        }

        List<PolicyOverlay> domainMatches = new ArrayList<>();
        List<PolicyOverlay> wildcardMatches = new ArrayList<>();
        for (PolicyOverlay overlay : overlays) {
            if (overlay == null || !overlay.selector().matches(query)) {
                continue;
            }
            if (overlay.selector().domainSpecific()) {
                domainMatches.add(overlay);
            } else {
                wildcardMatches.add(overlay);
            }
        }
        List<PolicyOverlay> candidates = domainMatches.isEmpty()
            ? wildcardMatches : domainMatches;
        if (candidates.isEmpty()) {
            return new PolicyResolution(PolicyResolution.Status.NO_MATCH, query, null, "");
        }

        int specificity = candidates.stream()
            .mapToInt(value -> value.selector().specificity()).max().orElse(0);
        List<PolicyOverlay> mostSpecific = candidates.stream()
            .filter(value -> value.selector().specificity() == specificity)
            .sorted(STABLE_ORDER)
            .toList();
        PolicyOverlay first = mostSpecific.get(0);
        for (int index = 1; index < mostSpecific.size(); index++) {
            if (!first.sameEffectiveValue(mostSpecific.get(index))) {
                return new PolicyResolution(
                    PolicyResolution.Status.POLICY_CONFLICT, query, null,
                    conflictRuleId(mostSpecific)
                );
            }
        }
        return new PolicyResolution(
            PolicyResolution.Status.RESOLVED, query, first,
            ruleId(first)
        );
    }

    public static PolicyResolution resolve(PolicyState state, ModeSelector query) {
        Objects.requireNonNull(state, "state");
        return resolve(state.overlays(), query);
    }

    /** Validates every selector bucket before a state is published. */
    public static void validate(Collection<PolicyOverlay> overlays) {
        if (overlays == null) {
            return;
        }
        List<PolicyOverlay> values = overlays.stream().filter(Objects::nonNull).toList();
        for (int left = 0; left < values.size(); left++) {
            PolicyOverlay first = values.get(left);
            for (int right = left + 1; right < values.size(); right++) {
                PolicyOverlay second = values.get(right);
                if (first.selector().canonicalKey().equals(second.selector().canonicalKey())
                    && !first.sameEffectiveValue(second)) {
                    throw new PolicyConflictException(
                        "conflicting policy overlays for " + first.selector().canonicalKey()
                    );
                }
            }
        }
    }

    private static String ruleId(PolicyOverlay overlay) {
        return overlay.selector().canonicalKey();
    }

    private static String conflictRuleId(List<PolicyOverlay> overlays) {
        return overlays.stream().map(PolicyResolver::ruleId).sorted().reduce(
            (left, right) -> left + "," + right
        ).orElse("");
    }
}
