package dev.ronova.coremod.policy;

import dev.ronova.coremod.integrity.*;
import java.util.Set;

/** Connects measured support to policy presentation without granting new attack permissions. */
public final class RuntimeCapabilityProjection {
    private RuntimeCapabilityProjection() {}
    public static StrengthPolicySnapshot apply(StrengthPolicySnapshot policy,EnforcementProof enforcement,
        CodeIntegrityProof code,CapabilityChallengeProof challenge) {
        var result=policy;

        // First S1 vertical slice: registered-code inventory is necessary but no longer sufficient
        // for PROTECT_CODE_INTEGRITY. A bounded behavior canary must also have been observed now.
        boolean codeSupported=code.supported();
        boolean codeCurrent=code.current();
        boolean challenged=codeCurrent&&challenge.current();
        EvidenceState codeEvidence=challenged?EvidenceState.VERIFIED
            :!codeSupported?EvidenceState.UNOBSERVED
            :!codeCurrent?EvidenceState.FAILED
            :challenge.supported()?EvidenceState.DEGRADED:EvidenceState.OBSERVED;
        result=result.withCapability(StrengthOptionKey.PROTECT_CODE_INTEGRITY,
            codeSupported?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
            challenged?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
            codeEvidence,challenged?ActivationState.ACTIVE:codeSupported?ActivationState.FENCED:ActivationState.UNAVAILABLE);

        // These keys still expose the existing code-image evidence only. The canary does not prove
        // their complete runtime semantics, so Stage 1I deliberately does not over-claim them.
        for(var key:Set.of(StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH,StrengthOptionKey.INTERCEPT_INSTRUMENTATION)) {
            result=result.withCapability(key,codeSupported?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
                codeCurrent?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
                codeCurrent?EvidenceState.VERIFIED:codeSupported?EvidenceState.FAILED:EvidenceState.UNOBSERVED,
                codeCurrent?ActivationState.ACTIVE:codeSupported?ActivationState.FENCED:ActivationState.UNAVAILABLE);
        }
        for(var key:Set.of(StrengthOptionKey.RUNTIME_JVMTI,StrengthOptionKey.RUNTIME_NATIVE,StrengthOptionKey.RUNTIME_BACKGROUND_INTEGRITY)) {
            boolean verified=enforcement.current();
            result=result.withCapability(key,enforcement.supported()?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
                verified?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,verified?EvidenceState.VERIFIED:EvidenceState.UNOBSERVED,
                verified?ActivationState.ACTIVE:ActivationState.UNAVAILABLE);
        }
        return result;
    }
}
