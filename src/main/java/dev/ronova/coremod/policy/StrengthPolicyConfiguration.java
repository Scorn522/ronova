package dev.ronova.coremod.policy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Durable requested policy. Runtime support/evidence are deliberately not persisted here. */
public final class StrengthPolicyConfiguration {
    private final PolicyRevision revision;
    private final int profileMatrixRevision;
    private final StrengthProfile profile;
    private final Map<StrengthOptionKey, MechanismLevel> levels;
    private final AttackActionPolicy attacks;
    private final int sourceSchema;

    public StrengthPolicyConfiguration(
        PolicyRevision revision,
        StrengthProfile profile,
        Map<StrengthOptionKey, MechanismLevel> levels
    ) {
        this(
            revision, ProfilePresetMatrix.CURRENT_REVISION, profile, levels
        );
    }

    public StrengthPolicyConfiguration(
        PolicyRevision revision,
        int profileMatrixRevision,
        StrengthProfile profile,
        Map<StrengthOptionKey, MechanismLevel> levels
    ) {
        this(revision, profileMatrixRevision, profile, levels,
            AttackActionPolicy.legacyDefaults(profile), StrengthPolicySnapshot.SCHEMA_VERSION);
    }

    public StrengthPolicyConfiguration(
        PolicyRevision revision, int profileMatrixRevision, StrengthProfile profile,
        Map<StrengthOptionKey, MechanismLevel> levels, AttackActionPolicy attacks,
        int sourceSchema
    ) {
        this.revision = Objects.requireNonNull(revision, "revision");
        if (!ProfilePresetMatrix.supports(profileMatrixRevision)) {
            throw new IllegalArgumentException(
                "unsupported profile preset matrix revision: " + profileMatrixRevision
            );
        }
        this.profileMatrixRevision = profileMatrixRevision;
        this.profile = Objects.requireNonNull(profile, "profile");
        EnumMap<StrengthOptionKey, MechanismLevel> values =
            new EnumMap<>(StrengthOptionKey.class);
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            values.put(option, MechanismLevel.FOLLOW_PROFILE);
        }
        if (levels != null) {
            levels.forEach((option, level) -> values.put(
                Objects.requireNonNull(option, "option"),
                Objects.requireNonNull(level, "level")
            ));
        }
        this.levels = Collections.unmodifiableMap(values);
        this.attacks = Objects.requireNonNull(attacks, "attacks");
        if (sourceSchema < 1 || sourceSchema > StrengthPolicySnapshot.SCHEMA_VERSION
            || sourceSchema < 3 && !attacks.equals(AttackActionPolicy.legacyDefaults(profile))) {
            throw new IllegalArgumentException("invalid source schema/action policy combination");
        }
        this.sourceSchema = sourceSchema;
    }

    public static StrengthPolicyConfiguration defaults() {
        return new StrengthPolicyConfiguration(
            PolicyRevision.INITIAL, ProfilePresetMatrix.CURRENT_REVISION,
            StrengthProfile.COMPATIBLE, Map.of()
        );
    }

    public PolicyRevision revision() { return revision; }
    public int profileMatrixRevision() { return profileMatrixRevision; }
    public StrengthProfile profile() { return profile; }
    public Map<StrengthOptionKey, MechanismLevel> levels() { return levels; }
    public AttackActionPolicy attacks() { return attacks; }
    public int sourceSchema() { return sourceSchema; }

    /** Additional permission gate for existing baseline lanes, not a capability claim. */
    public boolean permitsBaselineAction(AttackAction action) {
        Objects.requireNonNull(action, "action");
        // Never use this legacy gate as an evidence-free authorization for a
        // future code/return/render executor. Those require resolveAttack.
        if (action != AttackAction.TERMINALIZE) {
            return false;
        }
        if (attacks.resolve(profile, action).depth() == MechanismLevel.OFF) return false;
        for (StrengthOptionKey key : action.requiredKeys()) {
            if (ProfilePresetMatrix.resolve(profileMatrixRevision, profile, key, levels.get(key))
                .requestedLevel().rank() < MechanismLevel.PROTECT_RONOVA.rank()) return false;
        }
        return true;
    }

    public StrengthPolicyConfiguration next(
        StrengthProfile nextProfile,
        Map<StrengthOptionKey, MechanismLevel> nextLevels
    ) {
        return next(nextProfile, nextLevels, Map.of());
    }

    public StrengthPolicyConfiguration next(
        StrengthProfile nextProfile, Map<StrengthOptionKey, MechanismLevel> nextLevels,
        Map<AttackAction, AttackActionLevel> nextActions
    ) {
        StrengthProfile selectedProfile = Objects.requireNonNull(
            nextProfile, "nextProfile"
        );
        Map<StrengthOptionKey, MechanismLevel> selectedLevels = nextLevels;
        if (selectedProfile == StrengthProfile.CUSTOM) {
            EnumMap<StrengthOptionKey, MechanismLevel> explicit =
                new EnumMap<>(ProfilePresetMatrix.materialize(
                    profileMatrixRevision, profile, levels
                ));
            if (nextLevels != null) {
                nextLevels.forEach((option, level) -> explicit.put(
                    Objects.requireNonNull(option, "option"),
                    Objects.requireNonNull(level, "level")
                ));
            }
            selectedLevels = explicit;
        }
        return new StrengthPolicyConfiguration(
            revision.next(), ProfilePresetMatrix.CURRENT_REVISION,
            selectedProfile, selectedLevels,
            (selectedProfile == StrengthProfile.CUSTOM
                ? attacks.materialize(profile) : AttackActionPolicy.followingProfile()).with(nextActions),
            StrengthPolicySnapshot.SCHEMA_VERSION
        );
    }

    public String canonicalKey() {
        return canonicalKeyForSchema(sourceSchema);
    }

    /** Canonical compatibility encoding used by durable schema migration. */
    public String canonicalKeyForSchema(int schemaVersion) {
        if (schemaVersion < 1 || schemaVersion > StrengthPolicySnapshot.SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                "unsupported strength policy schema: " + schemaVersion
            );
        }
        StringBuilder value = new StringBuilder()
            .append(schemaVersion).append('|').append(revision.value()).append('|');
        if (schemaVersion >= 2) {
            value.append(profileMatrixRevision).append('|');
        }
        value.append(profile);
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            value.append('|').append(option.canonical()).append('=')
                .append(levels.get(option));
        }
        if (schemaVersion >= 3) value.append("|actions=").append(attacks.canonicalKey());
        return value.toString();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof StrengthPolicyConfiguration value
            && revision.equals(value.revision)
            && profileMatrixRevision == value.profileMatrixRevision
            && profile == value.profile
            && levels.equals(value.levels)
            && attacks.equals(value.attacks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, profileMatrixRevision, profile, levels, attacks);
    }

    @Override
    public String toString() {
        return canonicalKey();
    }
}
