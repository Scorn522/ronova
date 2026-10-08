package dev.ronova.coremod.policy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Versioned per-option preset policy introduced by 7.17.
 *
 * <p>The matrix selects requested strength only. Runtime support, evidence and
 * activation are still resolved independently by {@link StrengthPolicySnapshot}.
 * Keeping all 54 keys in one audited table prevents individual planes from
 * silently falling back to the old uniform r52 mapping.</p>
 */
public final class ProfilePresetMatrix {
    public static final int CURRENT_REVISION = 1;

    private static final Map<StrengthProfile, Map<StrengthOptionKey, MechanismLevel>>
        REVISION_1 = revisionOne();
    private static final Map<StrengthOptionKey, MechanismLevel> SAFETY_FLOORS =
        safetyFloors();

    private ProfilePresetMatrix() {}

    public static boolean supports(int revision) {
        return revision == CURRENT_REVISION;
    }

    public static MechanismLevel presetLevel(
        int revision,
        StrengthProfile profile,
        StrengthOptionKey option
    ) {
        requireSupported(revision);
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(option, "option");
        MechanismLevel level = REVISION_1.get(profile).get(option);
        if (level == null || level == MechanismLevel.FOLLOW_PROFILE) {
            throw new IllegalStateException(
                "incomplete profile preset matrix for " + profile + "/" + option
            );
        }
        return level;
    }

    public static MechanismLevel minimumLevel(StrengthOptionKey option) {
        Objects.requireNonNull(option, "option");
        return SAFETY_FLOORS.getOrDefault(option, MechanismLevel.OFF);
    }

    public static Resolution resolve(
        int revision,
        StrengthProfile profile,
        StrengthOptionKey option,
        MechanismLevel configuredLevel
    ) {
        MechanismLevel configured = configuredLevel == null
            ? MechanismLevel.FOLLOW_PROFILE : configuredLevel;
        MechanismLevel preset = presetLevel(revision, profile, option);
        MechanismLevel selected = configured == MechanismLevel.FOLLOW_PROFILE
            ? preset : MechanismLevel.runnable(configured);
        MechanismLevel floor = minimumLevel(option);
        boolean floorApplied = selected.rank() < floor.rank();
        MechanismLevel requested = floorApplied ? floor : selected;
        return new Resolution(
            revision, profile, option, configured, preset, requested, floorApplied
        );
    }

    /**
     * Freezes one profile image into explicit runnable values. This is used
     * when entering CUSTOM so later matrix revisions cannot silently change
     * the options the player did not edit.
     */
    public static Map<StrengthOptionKey, MechanismLevel> materialize(
        int revision,
        StrengthProfile profile,
        Map<StrengthOptionKey, MechanismLevel> configuredLevels
    ) {
        requireSupported(revision);
        Objects.requireNonNull(profile, "profile");
        Map<StrengthOptionKey, MechanismLevel> source = configuredLevels == null
            ? Map.of() : configuredLevels;
        EnumMap<StrengthOptionKey, MechanismLevel> result =
            new EnumMap<>(StrengthOptionKey.class);
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            result.put(option, resolve(
                revision, profile, option,
                source.getOrDefault(option, MechanismLevel.FOLLOW_PROFILE)
            ).requestedLevel());
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<StrengthProfile, Map<StrengthOptionKey, MechanismLevel>>
    revisionOne() {
        EnumMap<StrengthProfile, Map<StrengthOptionKey, MechanismLevel>> profiles =
            new EnumMap<>(StrengthProfile.class);

        EnumMap<StrengthOptionKey, MechanismLevel> compatible =
            filled(MechanismLevel.MONITOR);

        // Compatible attack: stable target/Authority paths are active; broad
        // source, task, callback and dynamic-code paths remain observational.
        compatible.put(StrengthOptionKey.ATTACK_STATE_SOURCE,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.ATTACK_REGISTRATION_SOURCE,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.ATTACK_NETWORK_RECONSTRUCTION,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.ATTACK_DISPATCH,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.ATTACK_RUNTIME_PATCH_DISABLEMENT,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.ATTACK_TARGET_CLASS_LINEAGE,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER,
            MechanismLevel.PROTECT_RONOVA);

        // Interception and protection in Compatible are scoped to Ronova.
        putPlane(compatible, PlaneId.INTERCEPTION, MechanismLevel.PROTECT_RONOVA);
        putPlane(compatible, PlaneId.PROTECTION, MechanismLevel.PROTECT_RONOVA);

        // Compatible runtime distinguishes active Ronova continuity from broad
        // inventory/diagnostic observation.
        compatible.put(StrengthOptionKey.RUNTIME_JAVA_AGENT,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.RUNTIME_NATIVE,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.RUNTIME_NATIVE_LOADER,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.RUNTIME_RENDER_GUARD,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.RUNTIME_THREAD_CONTINUITY,
            MechanismLevel.PROTECT_RONOVA);
        compatible.put(StrengthOptionKey.RUNTIME_BACKGROUND_INTEGRITY,
            MechanismLevel.PROTECT_RONOVA);

        profiles.put(StrengthProfile.COMPATIBLE, immutable(compatible));
        profiles.put(StrengthProfile.ENHANCED,
            immutable(filled(MechanismLevel.RELATED_PATHS)));
        profiles.put(StrengthProfile.EXTREME,
            immutable(filled(MechanismLevel.STRICT_ALL)));
        profiles.put(StrengthProfile.CUSTOM,
            immutable(filled(MechanismLevel.MONITOR)));

        validateComplete(profiles);
        return Collections.unmodifiableMap(profiles);
    }

    private static Map<StrengthOptionKey, MechanismLevel> safetyFloors() {
        EnumMap<StrengthOptionKey, MechanismLevel> floors =
            new EnumMap<>(StrengthOptionKey.class);
        floors.put(StrengthOptionKey.PROTECT_CORE_AUTHORITY,
            MechanismLevel.PROTECT_RONOVA);
        floors.put(StrengthOptionKey.PROTECT_RESTART_RECOVERY,
            MechanismLevel.PROTECT_RONOVA);
        floors.put(StrengthOptionKey.PROTECT_ROLLBACK,
            MechanismLevel.PROTECT_RONOVA);
        floors.put(StrengthOptionKey.PROTECT_LOOP_GUARD,
            MechanismLevel.PROTECT_RONOVA);
        return Collections.unmodifiableMap(floors);
    }

    private static EnumMap<StrengthOptionKey, MechanismLevel> filled(
        MechanismLevel level
    ) {
        MechanismLevel runnable = MechanismLevel.runnable(level);
        EnumMap<StrengthOptionKey, MechanismLevel> values =
            new EnumMap<>(StrengthOptionKey.class);
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            values.put(option, runnable);
        }
        return values;
    }

    private static void putPlane(
        EnumMap<StrengthOptionKey, MechanismLevel> values,
        PlaneId plane,
        MechanismLevel level
    ) {
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            if (option.plane() == plane) {
                values.put(option, level);
            }
        }
    }

    private static Map<StrengthOptionKey, MechanismLevel> immutable(
        EnumMap<StrengthOptionKey, MechanismLevel> values
    ) {
        return Collections.unmodifiableMap(new EnumMap<>(values));
    }

    private static void validateComplete(
        Map<StrengthProfile, Map<StrengthOptionKey, MechanismLevel>> profiles
    ) {
        for (StrengthProfile profile : StrengthProfile.values()) {
            Map<StrengthOptionKey, MechanismLevel> values = profiles.get(profile);
            if (values == null || values.size() != StrengthOptionKey.values().length) {
                throw new ExceptionInInitializerError(
                    "profile preset matrix is incomplete for " + profile
                );
            }
            for (StrengthOptionKey option : StrengthOptionKey.values()) {
                MechanismLevel level = values.get(option);
                if (level == null || level == MechanismLevel.FOLLOW_PROFILE) {
                    throw new ExceptionInInitializerError(
                        "profile preset matrix has no runnable level for "
                            + profile + "/" + option
                    );
                }
            }
        }
    }

    private static void requireSupported(int revision) {
        if (!supports(revision)) {
            throw new IllegalArgumentException(
                "unsupported profile preset matrix revision: " + revision
            );
        }
    }

    /** Complete resolution before runtime ceilings and evidence are applied. */
    public record Resolution(
        int matrixRevision,
        StrengthProfile profile,
        StrengthOptionKey option,
        MechanismLevel configuredLevel,
        MechanismLevel presetLevel,
        MechanismLevel requestedLevel,
        boolean minimumFloorApplied
    ) {
        public Resolution {
            if (!ProfilePresetMatrix.supports(matrixRevision)) {
                throw new IllegalArgumentException(
                    "unsupported profile preset matrix revision: " + matrixRevision
                );
            }
            Objects.requireNonNull(profile, "profile");
            Objects.requireNonNull(option, "option");
            Objects.requireNonNull(configuredLevel, "configuredLevel");
            presetLevel = MechanismLevel.runnable(presetLevel);
            requestedLevel = MechanismLevel.runnable(requestedLevel);
            if (requestedLevel.rank()
                < ProfilePresetMatrix.minimumLevel(option).rank()) {
                throw new IllegalArgumentException(
                    "requested level is below the mandatory safety floor"
                );
            }
        }
    }
}
