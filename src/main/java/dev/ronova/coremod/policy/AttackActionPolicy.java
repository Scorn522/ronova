package dev.ronova.coremod.policy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable Root-owned action request. No runtime evidence is persisted here. */
public record AttackActionPolicy(int matrixRevision, Map<AttackAction, AttackActionLevel> levels) {
    public static final int CURRENT_MATRIX_REVISION = 1;
    private static final Set<AttackAction> COMPATIBLE_DISABLED = Set.of(
        AttackAction.DIRECT_CLEAR, AttackAction.SUPPRESS_REBUILD,
        AttackAction.STRIP_CODE_DEFENSE, AttackAction.ATTACK_CALL_CHAIN, AttackAction.ALL_RETURN
    );

    public AttackActionPolicy {
        if (matrixRevision != CURRENT_MATRIX_REVISION) {
            throw new IllegalArgumentException("unsupported action matrix revision: " + matrixRevision);
        }
        Objects.requireNonNull(levels, "levels");
        EnumMap<AttackAction, AttackActionLevel> copy = new EnumMap<>(AttackAction.class);
        for (AttackAction action : AttackAction.values()) {
            copy.put(action, Objects.requireNonNull(levels.get(action), "missing action " + action));
        }
        if (copy.size() != levels.size()) throw new IllegalArgumentException("unexpected action fields");
        levels = Collections.unmodifiableMap(copy);
    }

    public static AttackActionPolicy followingProfile() {
        return filled(AttackActionLevel.FOLLOW_PROFILE);
    }

    /** Legacy Custom never implies consent to newly introduced attack actions. */
    public static AttackActionPolicy legacyDefaults(StrengthProfile profile) {
        return profile == StrengthProfile.CUSTOM
            ? filled(AttackActionLevel.DISABLED) : followingProfile();
    }

    private static AttackActionPolicy filled(AttackActionLevel level) {
        EnumMap<AttackAction, AttackActionLevel> values = new EnumMap<>(AttackAction.class);
        for (AttackAction action : AttackAction.values()) values.put(action, level);
        return new AttackActionPolicy(CURRENT_MATRIX_REVISION, values);
    }

    public AttackActionLevel resolve(StrengthProfile profile, AttackAction action) {
        Objects.requireNonNull(profile, "profile");
        AttackActionLevel configured = levels.get(Objects.requireNonNull(action, "action"));
        if (configured != AttackActionLevel.FOLLOW_PROFILE) return configured;
        return switch (profile) {
            case CUSTOM -> AttackActionLevel.DISABLED;
            case COMPATIBLE -> COMPATIBLE_DISABLED.contains(action)
                ? AttackActionLevel.DISABLED : AttackActionLevel.COMPATIBLE;
            case ENHANCED -> AttackActionLevel.ENHANCED;
            case EXTREME -> AttackActionLevel.EXTREME;
        };
    }

    public AttackActionPolicy materialize(StrengthProfile profile) {
        EnumMap<AttackAction, AttackActionLevel> values = new EnumMap<>(AttackAction.class);
        for (AttackAction action : AttackAction.values()) values.put(action, resolve(profile, action));
        return new AttackActionPolicy(matrixRevision, values);
    }

    public AttackActionPolicy with(Map<AttackAction, AttackActionLevel> changes) {
        EnumMap<AttackAction, AttackActionLevel> values = new EnumMap<>(levels);
        Objects.requireNonNull(changes, "changes").forEach((key, value) -> values.put(
            Objects.requireNonNull(key, "action"), Objects.requireNonNull(value, "level")
        ));
        return new AttackActionPolicy(matrixRevision, values);
    }

    public String canonicalKey() {
        StringBuilder value = new StringBuilder().append(matrixRevision);
        for (AttackAction action : AttackAction.values()) {
            value.append('|').append(action.canonical()).append('=').append(levels.get(action));
        }
        return value.toString();
    }
}
