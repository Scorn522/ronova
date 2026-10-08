package dev.ronova.coremod.policy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable 7.17 strength policy image. Configuration, preset matrix, runtime ceiling,
 * evidence and activation remain separate so the UI cannot overclaim EXTREME.
 */
public final class StrengthPolicySnapshot {
    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final int SCHEMA_VERSION = 3;

    private final PolicyRevision revision;
    private final int configuredMatrixRevision;
    private final int activeMatrixRevision;
    private final StrengthProfile configuredProfile;
    private final StrengthProfile activeProfile;
    private final Map<StrengthOptionKey, MechanismLevel> configuredLevels;
    private final Map<StrengthOptionKey, MechanismLevel> activeLevels;
    private final Map<StrengthOptionKey, MechanismLevel> supportedCeilings;
    private final Map<StrengthOptionKey, MechanismLevel> evidenceCeilings;
    private final Map<StrengthOptionKey, EvidenceState> evidenceStates;
    private final Map<StrengthOptionKey, ActivationState> runtimeStates;
    private final AttackActionPolicy configuredActions;
    private final AttackActionPolicy activeActions;

    public StrengthPolicySnapshot(
        PolicyRevision revision,
        StrengthProfile configuredProfile,
        StrengthProfile activeProfile,
        Map<StrengthOptionKey, MechanismLevel> configuredLevels,
        Map<StrengthOptionKey, MechanismLevel> activeLevels,
        Map<StrengthOptionKey, MechanismLevel> supportedCeilings,
        Map<StrengthOptionKey, EvidenceState> evidenceStates,
        Map<StrengthOptionKey, ActivationState> runtimeStates
    ) {
        this(
            revision,
            ProfilePresetMatrix.CURRENT_REVISION,
            ProfilePresetMatrix.CURRENT_REVISION,
            configuredProfile,
            activeProfile,
            configuredLevels,
            activeLevels,
            supportedCeilings,
            null,
            evidenceStates,
            runtimeStates
        );
    }

    public StrengthPolicySnapshot(
        PolicyRevision revision,
        int configuredMatrixRevision,
        int activeMatrixRevision,
        StrengthProfile configuredProfile,
        StrengthProfile activeProfile,
        Map<StrengthOptionKey, MechanismLevel> configuredLevels,
        Map<StrengthOptionKey, MechanismLevel> activeLevels,
        Map<StrengthOptionKey, MechanismLevel> supportedCeilings,
        Map<StrengthOptionKey, MechanismLevel> evidenceCeilings,
        Map<StrengthOptionKey, EvidenceState> evidenceStates,
        Map<StrengthOptionKey, ActivationState> runtimeStates
    ) {
        this(revision, configuredMatrixRevision, activeMatrixRevision,
            configuredProfile, activeProfile, configuredLevels, activeLevels,
            supportedCeilings, evidenceCeilings, evidenceStates, runtimeStates,
            AttackActionPolicy.legacyDefaults(configuredProfile),
            AttackActionPolicy.legacyDefaults(activeProfile));
    }

    private StrengthPolicySnapshot(
        PolicyRevision revision, int configuredMatrixRevision, int activeMatrixRevision,
        StrengthProfile configuredProfile, StrengthProfile activeProfile,
        Map<StrengthOptionKey, MechanismLevel> configuredLevels,
        Map<StrengthOptionKey, MechanismLevel> activeLevels,
        Map<StrengthOptionKey, MechanismLevel> supportedCeilings,
        Map<StrengthOptionKey, MechanismLevel> evidenceCeilings,
        Map<StrengthOptionKey, EvidenceState> evidenceStates,
        Map<StrengthOptionKey, ActivationState> runtimeStates,
        AttackActionPolicy configuredActions, AttackActionPolicy activeActions
    ) {
        this.revision = Objects.requireNonNull(revision, "revision");
        if (!ProfilePresetMatrix.supports(configuredMatrixRevision)
            || !ProfilePresetMatrix.supports(activeMatrixRevision)) {
            throw new IllegalArgumentException(
                "unsupported profile preset matrix revision"
            );
        }
        this.configuredMatrixRevision = configuredMatrixRevision;
        this.activeMatrixRevision = activeMatrixRevision;
        this.configuredProfile = Objects.requireNonNull(configuredProfile, "configuredProfile");
        this.activeProfile = Objects.requireNonNull(activeProfile, "activeProfile");
        this.configuredLevels = levels(configuredLevels, MechanismLevel.FOLLOW_PROFILE);
        this.activeLevels = levels(activeLevels, MechanismLevel.FOLLOW_PROFILE);
        this.supportedCeilings = levels(supportedCeilings, MechanismLevel.OFF);
        this.evidenceStates = evidence(evidenceStates);
        this.evidenceCeilings = evidenceCeilings == null
            ? evidenceCeilings(this.supportedCeilings, this.evidenceStates)
            : levels(evidenceCeilings, MechanismLevel.OFF);
        this.runtimeStates = activation(runtimeStates);
        this.configuredActions = Objects.requireNonNull(configuredActions, "configuredActions");
        this.activeActions = Objects.requireNonNull(activeActions, "activeActions");
    }

    public static StrengthPolicySnapshot defaults() {
        return new StrengthPolicySnapshot(
            PolicyRevision.INITIAL,
            StrengthProfile.COMPATIBLE,
            StrengthProfile.COMPATIBLE,
            Map.of(), Map.of(), Map.of(), Map.of(), Map.of()
        );
    }

    /** The persisted request becomes active only during a clean bootstrap. */
    public static StrengthPolicySnapshot fromConfiguration(
        StrengthPolicyConfiguration configuration
    ) {
        return fromConfigurations(configuration, configuration);
    }

    /** Separates the saved request from the policy image loaded by this JVM. */
    public static StrengthPolicySnapshot fromConfigurations(
        StrengthPolicyConfiguration configured,
        StrengthPolicyConfiguration active
    ) {
        Objects.requireNonNull(configured, "configured");
        Objects.requireNonNull(active, "active");
        return new StrengthPolicySnapshot(
            configured.revision(),
            configured.profileMatrixRevision(), active.profileMatrixRevision(),
            configured.profile(), active.profile(),
            configured.levels(), active.levels(),
            Map.of(), Map.of(), Map.of(), Map.of(), configured.attacks(), active.attacks()
        );
    }

    public PolicyRevision revision() { return revision; }
    public int configuredMatrixRevision() { return configuredMatrixRevision; }
    public int activeMatrixRevision() { return activeMatrixRevision; }
    public StrengthProfile configuredProfile() { return configuredProfile; }
    public StrengthProfile activeProfile() { return activeProfile; }
    public AttackActionPolicy configuredActions() { return configuredActions; }
    public AttackActionPolicy activeActions() { return activeActions; }
    public Map<StrengthOptionKey, MechanismLevel> configuredLevels() {
        return configuredLevels;
    }
    public Map<StrengthOptionKey, MechanismLevel> activeLevels() {
        return activeLevels;
    }
    public Map<StrengthOptionKey, MechanismLevel> supportedCeilings() {
        return supportedCeilings;
    }
    public Map<StrengthOptionKey, MechanismLevel> evidenceCeilings() {
        return evidenceCeilings;
    }
    public Map<StrengthOptionKey, EvidenceState> evidenceStates() {
        return evidenceStates;
    }
    public Map<StrengthOptionKey, ActivationState> runtimeStates() {
        return runtimeStates;
    }

    public StrengthPolicyConfiguration configuration() {
        return new StrengthPolicyConfiguration(
            revision, configuredMatrixRevision, configuredProfile, configuredLevels,
            configuredActions, SCHEMA_VERSION
        );
    }

    /**
     * Intersects every actual action/effect with its advanced dependencies.
     * Additional keys describe the concrete executor branch (e.g. bytecode or
     * non-entity HUD). This permission receipt does not prove physical success.
     */
    public AttackResolution resolveAttack(
        java.util.Set<AttackAction> actions,
        java.util.Set<StrengthOptionKey> additionalKeys
    ) {
        Objects.requireNonNull(actions, "actions");
        Objects.requireNonNull(additionalKeys, "additionalKeys");
        if (actions.isEmpty() || actions.equals(java.util.Set.of(AttackAction.STRONG_ATTACK))) {
            return new AttackResolution(MechanismLevel.OFF, MechanismLevel.OFF, false,
                "NO_EXECUTABLE_ACTION");
        }
        java.util.EnumSet<StrengthOptionKey> required =
            java.util.EnumSet.noneOf(StrengthOptionKey.class);
        MechanismLevel requested = MechanismLevel.STRICT_ALL;
        for (AttackAction action : actions) {
            MechanismLevel depth = activeActions.resolve(activeProfile, action).depth();
            if (depth == MechanismLevel.OFF) {
                return new AttackResolution(MechanismLevel.OFF, MechanismLevel.OFF, false,
                    "ACTION_DISABLED:" + action.canonical());
            }
            requested = MechanismLevel.lowerOf(requested, depth);
            required.addAll(action.requiredKeys());
        }
        required.addAll(additionalKeys);
        MechanismLevel effective = requested;
        for (StrengthOptionKey key : required) {
            StrengthResolutionReceipt receipt = resolve(key);
            if (receipt.activationState() != ActivationState.ACTIVE) {
                return new AttackResolution(requested, MechanismLevel.OFF, false,
                    receipt.activationState() + ":" + key.canonical());
            }
            effective = MechanismLevel.lowerOf(effective, receipt.effectiveLevel());
        }
        boolean permitted = effective.rank() >= MechanismLevel.PROTECT_RONOVA.rank();
        return new AttackResolution(requested, effective, permitted,
            permitted ? effective == requested ? "PERMITTED" : "LIMITED_BY_DEPENDENCY"
                : "DEPENDENCY_MONITOR_ONLY");
    }

    public StrengthResolutionReceipt resolve(StrengthOptionKey option) {
        Objects.requireNonNull(option, "option");
        MechanismLevel configured = configuredLevels.get(option);
        ProfilePresetMatrix.Resolution activeResolution = ProfilePresetMatrix.resolve(
            activeMatrixRevision, activeProfile, option, activeLevels.get(option)
        );
        MechanismLevel activeRequested = activeResolution.requestedLevel();
        MechanismLevel supportedCeiling = MechanismLevel.runnable(
            supportedCeilings.get(option)
        );
        MechanismLevel evidenceCeiling = MechanismLevel.runnable(
            evidenceCeilings.get(option)
        );
        MechanismLevel effective = MechanismLevel.lowerOf(
            MechanismLevel.lowerOf(activeRequested, supportedCeiling),
            evidenceCeiling
        );
        EvidenceState evidence = evidenceStates.get(option);
        ActivationState activation = activationState(
            option, activeRequested, supportedCeiling,
            evidenceCeiling, evidence
        );
        return new StrengthResolutionReceipt(
            revision, configuredMatrixRevision, activeMatrixRevision,
            option, configured, activeResolution.presetLevel(), activeRequested,
            supportedCeiling, evidenceCeiling, effective, evidence, activation,
            activeResolution.minimumFloorApplied(),
            reason(
                activeRequested, effective, supportedCeiling, evidenceCeiling,
                evidence, activation, activeResolution.minimumFloorApplied()
            )
        );
    }

    /** Persists a new requested policy while leaving the current JVM image unchanged. */
    public StrengthPolicySnapshot configure(
        StrengthProfile profile,
        Map<StrengthOptionKey, MechanismLevel> levels
    ) {
        return configure(profile, levels, Map.of());
    }

    public StrengthPolicySnapshot configure(
        StrengthProfile profile, Map<StrengthOptionKey, MechanismLevel> levels,
        Map<AttackAction, AttackActionLevel> actions
    ) {
        StrengthPolicyConfiguration next = configuration().next(profile, levels, actions);
        return new StrengthPolicySnapshot(
            next.revision(), next.profileMatrixRevision(), activeMatrixRevision,
            next.profile(), activeProfile,
            next.levels(), activeLevels, supportedCeilings, evidenceCeilings,
            evidenceStates, runtimeStates, next.attacks(), activeActions
        );
    }

    /** Applies the persisted policy at the next clean runtime bootstrap. */
    public StrengthPolicySnapshot activateConfigured() {
        return new StrengthPolicySnapshot(
            revision, configuredMatrixRevision, configuredMatrixRevision,
            configuredProfile, configuredProfile,
            configuredLevels, configuredLevels, supportedCeilings,
            evidenceCeilings, evidenceStates, runtimeStates, configuredActions, configuredActions
        );
    }

    public StrengthPolicySnapshot withCapability(
        StrengthOptionKey option,
        MechanismLevel supportedCeiling,
        EvidenceState evidenceState,
        ActivationState runtimeState
    ) {
        MechanismLevel inferredEvidenceCeiling = switch (
            Objects.requireNonNull(evidenceState, "evidenceState")
        ) {
            case UNOBSERVED, FAILED -> MechanismLevel.OFF;
            case OBSERVED, VERIFIED, DEGRADED ->
                MechanismLevel.runnable(supportedCeiling);
        };
        return withCapability(
            option, supportedCeiling, inferredEvidenceCeiling,
            evidenceState, runtimeState
        );
    }

    public StrengthPolicySnapshot withCapability(
        StrengthOptionKey option,
        MechanismLevel supportedCeiling,
        MechanismLevel evidenceCeiling,
        EvidenceState evidenceState,
        ActivationState runtimeState
    ) {
        Objects.requireNonNull(option, "option");
        EnumMap<StrengthOptionKey, MechanismLevel> ceilings =
            new EnumMap<>(supportedCeilings);
        EnumMap<StrengthOptionKey, MechanismLevel> evidenceLimits =
            new EnumMap<>(evidenceCeilings);
        EnumMap<StrengthOptionKey, EvidenceState> evidence =
            new EnumMap<>(evidenceStates);
        EnumMap<StrengthOptionKey, ActivationState> states =
            new EnumMap<>(runtimeStates);
        ceilings.put(option, MechanismLevel.runnable(supportedCeiling));
        evidenceLimits.put(option, MechanismLevel.runnable(evidenceCeiling));
        evidence.put(option, Objects.requireNonNull(evidenceState, "evidenceState"));
        states.put(option, Objects.requireNonNull(runtimeState, "runtimeState"));
        return new StrengthPolicySnapshot(
            revision, configuredMatrixRevision, activeMatrixRevision,
            configuredProfile, activeProfile,
            configuredLevels, activeLevels, ceilings, evidenceLimits,
            evidence, states, configuredActions, activeActions
        );
    }

    public boolean pendingRestart() {
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            if (resolve(option).pendingRestart()) {
                return true;
            }
        }
        return false;
    }

    public String canonicalKey() {
        StringBuilder value = new StringBuilder()
            .append(SCHEMA_VERSION).append('|').append(revision.value())
            .append('|').append(configuredMatrixRevision)
            .append('|').append(activeMatrixRevision)
            .append('|').append(configuredProfile).append('|').append(activeProfile);
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            StrengthResolutionReceipt receipt = resolve(option);
            value.append('|').append(option.canonical())
                .append('=').append(configuredLevels.get(option))
                .append('/').append(activeLevels.get(option))
                .append('/').append(receipt.supportedCeiling())
                .append('/').append(receipt.evidenceCeiling())
                .append('/').append(receipt.evidenceState())
                .append('/').append(receipt.activationState());
        }
        value.append("|configuredActions=").append(configuredActions.canonicalKey())
            .append("|activeActions=").append(activeActions.canonicalKey());
        return value.toString();
    }

    private ActivationState activationState(
        StrengthOptionKey option,
        MechanismLevel activeRequested,
        MechanismLevel supportedCeiling,
        MechanismLevel evidenceCeiling,
        EvidenceState evidence
    ) {
        // One saved profile becomes one coherent runtime image. Hot activation
        // of only part of a profile would split attack/protection/runtime truth.
        // Compare configuration syntax as well as resolved strength: changing
        // FOLLOW_PROFILE to an equal explicit level is still a new durable image.
        if (configuredMatrixRevision != activeMatrixRevision
            || configuredProfile != activeProfile
            || !configuredLevels.equals(activeLevels)
            || !configuredActions.equals(activeActions)) {
            return ActivationState.PENDING_RESTART;
        }
        ActivationState explicit = runtimeStates.get(option);
        if (explicit == ActivationState.FENCED || explicit == ActivationState.REJECTED) {
            return explicit;
        }
        if (activeRequested == MechanismLevel.OFF) {
            return ActivationState.INACTIVE;
        }
        if (supportedCeiling == MechanismLevel.OFF
            || evidenceCeiling == MechanismLevel.OFF
            || evidence == EvidenceState.UNOBSERVED) {
            return ActivationState.UNAVAILABLE;
        }
        if (evidence == EvidenceState.FAILED) {
            return ActivationState.FENCED;
        }
        return explicit == ActivationState.UNAVAILABLE
            ? ActivationState.UNAVAILABLE : ActivationState.ACTIVE;
    }

    private static String reason(
        MechanismLevel activeRequested,
        MechanismLevel effective,
        MechanismLevel supportedCeiling,
        MechanismLevel evidenceCeiling,
        EvidenceState evidence,
        ActivationState activation,
        boolean minimumFloorApplied
    ) {
        return switch (activation) {
            case PENDING_RESTART -> "configured policy awaits restart";
            case UNAVAILABLE -> "runtime capability is unavailable or unobserved";
            case REJECTED -> "runtime rejected activation";
            case FENCED -> "runtime evidence is fenced or failed";
            case INACTIVE -> "option is disabled";
            case ACTIVE -> minimumFloorApplied
                ? "active at mandatory safety floor"
                : effective.rank() < activeRequested.rank()
                    ? effective.rank() < supportedCeiling.rank()
                        ? "active at evidence ceiling"
                        : "active at supported ceiling"
                    : evidence == EvidenceState.DEGRADED
                        ? "active with degraded evidence" : "active and supported";
        };
    }

    private static Map<StrengthOptionKey, MechanismLevel> evidenceCeilings(
        Map<StrengthOptionKey, MechanismLevel> supported,
        Map<StrengthOptionKey, EvidenceState> evidence
    ) {
        EnumMap<StrengthOptionKey, MechanismLevel> values =
            new EnumMap<>(StrengthOptionKey.class);
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            EvidenceState state = evidence.get(option);
            values.put(option, state == EvidenceState.UNOBSERVED
                    || state == EvidenceState.FAILED
                ? MechanismLevel.OFF : supported.get(option));
        }
        return Collections.unmodifiableMap(values);
    }

    private static Map<StrengthOptionKey, MechanismLevel> levels(
        Map<StrengthOptionKey, MechanismLevel> source,
        MechanismLevel fallback
    ) {
        EnumMap<StrengthOptionKey, MechanismLevel> values =
            new EnumMap<>(StrengthOptionKey.class);
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            values.put(option, fallback);
        }
        if (source != null) {
            source.forEach((key, value) -> values.put(
                Objects.requireNonNull(key, "option"),
                Objects.requireNonNull(value, "level")
            ));
        }
        return Collections.unmodifiableMap(values);
    }

    private static Map<StrengthOptionKey, EvidenceState> evidence(
        Map<StrengthOptionKey, EvidenceState> source
    ) {
        EnumMap<StrengthOptionKey, EvidenceState> values =
            new EnumMap<>(StrengthOptionKey.class);
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            values.put(option, EvidenceState.UNOBSERVED);
        }
        if (source != null) {
            source.forEach((key, value) -> values.put(
                Objects.requireNonNull(key, "option"),
                Objects.requireNonNull(value, "evidence")
            ));
        }
        return Collections.unmodifiableMap(values);
    }

    private static Map<StrengthOptionKey, ActivationState> activation(
        Map<StrengthOptionKey, ActivationState> source
    ) {
        EnumMap<StrengthOptionKey, ActivationState> values =
            new EnumMap<>(StrengthOptionKey.class);
        for (StrengthOptionKey option : StrengthOptionKey.values()) {
            values.put(option, ActivationState.UNAVAILABLE);
        }
        if (source != null) {
            source.forEach((key, value) -> values.put(
                Objects.requireNonNull(key, "option"),
                Objects.requireNonNull(value, "activation")
            ));
        }
        return Collections.unmodifiableMap(values);
    }
}
