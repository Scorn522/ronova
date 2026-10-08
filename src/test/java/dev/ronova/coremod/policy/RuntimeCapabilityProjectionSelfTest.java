package dev.ronova.coremod.policy;

import dev.ronova.coremod.integrity.*;
import java.util.Map;

public final class RuntimeCapabilityProjectionSelfTest {
    public static void main(String[] args) {
        var configuration=new StrengthPolicyConfiguration(PolicyRevision.INITIAL,StrengthProfile.EXTREME,Map.of());
        var source=StrengthPolicySnapshot.fromConfiguration(configuration);
        var challenge=new CapabilityChallengeProof(true,true,3,3,3,0);
        var current=RuntimeCapabilityProjection.apply(source,EnforcementProof.UNAVAILABLE,new CodeIntegrityProof(true,4,3,3,1),challenge);
        var code=current.resolve(StrengthOptionKey.PROTECT_CODE_INTEGRITY);
        require(code.effectiveLevel()==MechanismLevel.PROTECT_RONOVA&&code.activationState()==ActivationState.ACTIVE,
            "registered code plus live behavior challenge did not publish the selected protection scope");
        require(current.configuredProfile()==source.configuredProfile()&&current.configuredLevels().equals(source.configuredLevels())
            &&current.configuredActions().equals(source.configuredActions()),"runtime evidence rewrote requested policy");

        var unchallenged=RuntimeCapabilityProjection.apply(source,EnforcementProof.UNAVAILABLE,
            new CodeIntegrityProof(true,4,3,3,1),CapabilityChallengeProof.UNAVAILABLE);
        require(unchallenged.resolve(StrengthOptionKey.PROTECT_CODE_INTEGRITY).activationState()==ActivationState.FENCED
            &&unchallenged.resolve(StrengthOptionKey.PROTECT_CODE_INTEGRITY).effectiveLevel()==MechanismLevel.OFF
            &&unchallenged.resolve(StrengthOptionKey.PROTECT_CODE_INTEGRITY).evidenceState()==EvidenceState.OBSERVED,
            "static image evidence was promoted to behavior-verified protection");
        require(unchallenged.resolve(StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH).effectiveLevel()==MechanismLevel.PROTECT_RONOVA,
            "the first behavior canary was incorrectly advertised as proof for unrelated code-watch semantics");

        var degradedChallenge=new CapabilityChallengeProof(true,false,2,3,2,1);
        var degraded=RuntimeCapabilityProjection.apply(source,EnforcementProof.UNAVAILABLE,
            new CodeIntegrityProof(true,4,3,3,1),degradedChallenge);
        require(degraded.resolve(StrengthOptionKey.PROTECT_CODE_INTEGRITY).activationState()==ActivationState.FENCED
            &&degraded.resolve(StrengthOptionKey.PROTECT_CODE_INTEGRITY).evidenceState()==EvidenceState.DEGRADED,
            "failed behavior challenge did not fence only the selected scope");

        var failed=RuntimeCapabilityProjection.apply(source,EnforcementProof.UNAVAILABLE,
            new CodeIntegrityProof(true,4,3,2,1),challenge);
        require(failed.resolve(StrengthOptionKey.PROTECT_CODE_INTEGRITY).activationState()==ActivationState.FENCED
            &&failed.resolve(StrengthOptionKey.PROTECT_CODE_INTEGRITY).effectiveLevel()==MechanismLevel.OFF
            &&failed.resolve(StrengthOptionKey.PROTECT_CODE_INTEGRITY).evidenceState()==EvidenceState.FAILED,
            "failed code seal retained effective permission");
        require(current.resolve(StrengthOptionKey.ATTACK_DISPATCH).effectiveLevel()==MechanismLevel.OFF,
            "self-protection evidence enabled an attack executor");

        var nativeCurrent=new EnforcementProof(true,true,true,0xffff,0xffff,1,1,0,0,0,0,0,0);
        var measured=RuntimeCapabilityProjection.apply(source,nativeCurrent,new CodeIntegrityProof(true,4,3,3,1),challenge);
        for(var key:java.util.Set.of(StrengthOptionKey.ATTACK_STATE_SOURCE,StrengthOptionKey.ATTACK_REGISTRATION_SOURCE,
            StrengthOptionKey.ATTACK_FINAL_RESULT_TAKEOVER))
            require(measured.resolve(key).effectiveLevel()==MechanismLevel.OFF,"entity-local evidence advertised a global attack capability");
        System.out.println("RuntimeCapabilityProjectionSelfTest: PASS (behavior challenge gate, scoped code evidence, no implied attack permission)");
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
