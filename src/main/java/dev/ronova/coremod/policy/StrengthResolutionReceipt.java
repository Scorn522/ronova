package dev.ronova.coremod.policy;

import java.util.Objects;

/** Machine-checkable configured/supported/effective result for one option. */
public record StrengthResolutionReceipt(
    PolicyRevision policyRevision,
    int configuredMatrixRevision,
    int activeMatrixRevision,
    StrengthOptionKey option,
    MechanismLevel configuredLevel,
    MechanismLevel presetLevel,
    MechanismLevel requestedLevel,
    MechanismLevel supportedCeiling,
    MechanismLevel evidenceCeiling,
    MechanismLevel effectiveLevel,
    EvidenceState evidenceState,
    ActivationState activationState,
    boolean minimumFloorApplied,
    String reason
) {
    public StrengthResolutionReceipt {
        Objects.requireNonNull(policyRevision, "policyRevision");
        if (!ProfilePresetMatrix.supports(configuredMatrixRevision)
            || !ProfilePresetMatrix.supports(activeMatrixRevision)) {
            throw new IllegalArgumentException(
                "unsupported profile preset matrix revision"
            );
        }
        Objects.requireNonNull(option, "option");
        Objects.requireNonNull(configuredLevel, "configuredLevel");
        presetLevel = MechanismLevel.runnable(presetLevel);
        requestedLevel = MechanismLevel.runnable(requestedLevel);
        supportedCeiling = MechanismLevel.runnable(supportedCeiling);
        evidenceCeiling = MechanismLevel.runnable(evidenceCeiling);
        effectiveLevel = MechanismLevel.runnable(effectiveLevel);
        Objects.requireNonNull(evidenceState, "evidenceState");
        Objects.requireNonNull(activationState, "activationState");
        reason = Objects.toString(reason, "").trim();
        if (effectiveLevel.rank() > supportedCeiling.rank()) {
            throw new IllegalArgumentException("effective level exceeds supported ceiling");
        }
        if (effectiveLevel.rank() > evidenceCeiling.rank()) {
            throw new IllegalArgumentException("effective level exceeds evidence ceiling");
        }
        if (effectiveLevel.rank() > requestedLevel.rank()) {
            throw new IllegalArgumentException("effective level exceeds requested level");
        }
        if (requestedLevel.rank()
            < ProfilePresetMatrix.minimumLevel(option).rank()) {
            throw new IllegalArgumentException(
                "requested level is below the mandatory safety floor"
            );
        }
        if (activationState == ActivationState.ACTIVE
            && effectiveLevel == MechanismLevel.OFF) {
            throw new IllegalArgumentException("OFF option cannot be ACTIVE");
        }
    }

    public boolean pendingRestart() {
        return activationState == ActivationState.PENDING_RESTART;
    }

    public boolean available() {
        return activationState == ActivationState.ACTIVE
            || activationState == ActivationState.INACTIVE;
    }
}
