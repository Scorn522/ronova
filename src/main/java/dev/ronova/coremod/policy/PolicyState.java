package dev.ronova.coremod.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable policy snapshot used by status surfaces and Root-side resolution. */
public final class PolicyState {
    private final PolicyRevision revision;
    private final StrengthProfile profile;
    private final Map<PlaneId, MechanismLevel> planes;
    private final List<PolicyOverlay> overlays;

    public PolicyState(
        PolicyRevision revision,
        StrengthProfile profile,
        Map<PlaneId, MechanismLevel> planes,
        List<PolicyOverlay> overlays
    ) {
        this.revision = Objects.requireNonNull(revision, "revision");
        this.profile = Objects.requireNonNull(profile, "profile");
        EnumMap<PlaneId, MechanismLevel> configured = new EnumMap<>(PlaneId.class);
        for (PlaneId plane : PlaneId.values()) {
            configured.put(plane, MechanismLevel.FOLLOW_PROFILE);
        }
        if (planes != null) {
            for (Map.Entry<PlaneId, MechanismLevel> entry : planes.entrySet()) {
                configured.put(
                    Objects.requireNonNull(entry.getKey(), "plane"),
                    Objects.requireNonNull(entry.getValue(), "mechanism level")
                );
            }
        }
        this.planes = Collections.unmodifiableMap(new EnumMap<>(configured));
        List<PolicyOverlay> copy = new ArrayList<>();
        if (overlays != null) {
            for (PolicyOverlay overlay : overlays) {
                copy.add(Objects.requireNonNull(overlay, "policy overlay"));
            }
        }
        PolicyResolver.validate(copy);
        this.overlays = List.copyOf(copy);
    }

    public PolicyState(
        PolicyRevision revision,
        StrengthProfile profile,
        Map<PlaneId, MechanismLevel> planes
    ) {
        this(revision, profile, planes, List.of());
    }

    public PolicyState(PolicyRevision revision, List<PolicyOverlay> overlays) {
        this(revision, StrengthProfile.COMPATIBLE, Map.of(), overlays);
    }

    public static PolicyState defaults() {
        return new PolicyState(
            PolicyRevision.INITIAL, StrengthProfile.COMPATIBLE, Map.of(), List.of()
        );
    }

    public PolicyRevision revision() {
        return revision;
    }

    public StrengthProfile profile() {
        return profile;
    }

    public Map<PlaneId, MechanismLevel> planes() {
        return planes;
    }

    public List<PolicyOverlay> overlays() {
        return overlays;
    }

    public MechanismLevel configuredLevel(PlaneId plane) {
        return plane == null ? MechanismLevel.FOLLOW_PROFILE : planes.get(plane);
    }

    public MechanismLevel effectiveLevel(PlaneId plane) {
        MechanismLevel configured = configuredLevel(plane);
        return configured == MechanismLevel.FOLLOW_PROFILE
            ? profileLevel(profile) : configured;
    }

    public PolicyResolution resolve(ModeSelector selector) {
        return PolicyResolver.resolve(overlays, selector);
    }

    public PolicyState withOverlay(PolicyOverlay overlay) {
        Objects.requireNonNull(overlay, "overlay");
        List<PolicyOverlay> next = new ArrayList<>(overlays);
        next.removeIf(value -> value.selector().canonicalKey()
            .equals(overlay.selector().canonicalKey()));
        next.add(overlay);
        return new PolicyState(overlay.revision(), profile, planes, next);
    }

    public PolicyState withoutSelector(ModeSelector selector, PolicyRevision nextRevision) {
        Objects.requireNonNull(selector, "selector");
        Objects.requireNonNull(nextRevision, "nextRevision");
        return new PolicyState(
            nextRevision, profile, planes,
            overlays.stream().filter(value -> !value.selector().canonicalKey()
                .equals(selector.canonicalKey())).toList()
        );
    }

    public String canonicalKey() {
        StringBuilder value = new StringBuilder().append(revision.value()).append('|').append(profile);
        for (PlaneId plane : PlaneId.values()) {
            value.append('|').append(plane.name()).append('=').append(effectiveLevel(plane));
        }
        overlays.stream().map(PolicyOverlay::canonicalKey).sorted()
            .forEach(overlay -> value.append('|').append(overlay));
        return value.toString();
    }

    private static MechanismLevel profileLevel(StrengthProfile value) {
        return switch (value) {
            case COMPATIBLE -> MechanismLevel.PROTECT_RONOVA;
            case ENHANCED -> MechanismLevel.RELATED_PATHS;
            case EXTREME -> MechanismLevel.STRICT_ALL;
            case CUSTOM -> MechanismLevel.MONITOR;
        };
    }
}
