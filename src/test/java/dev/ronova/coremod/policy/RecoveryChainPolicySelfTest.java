package dev.ronova.coremod.policy;

import java.util.Map;

public final class RecoveryChainPolicySelfTest {
    public static void main(String[] args) {
        var compatible = policy(StrengthProfile.COMPATIBLE);
        var enhanced = policy(StrengthProfile.ENHANCED);
        var extreme = policy(StrengthProfile.EXTREME);
        var direct = RecoveryChainPolicy.Step.SAVED_RECORD_DISPOSAL;
        var derived = RecoveryChainPolicy.Step.DERIVED_SOURCE;
        require(!RecoveryChainPolicy.resolve(compatible, direct).observationRequested(), "compatible traverses recovery containers");
        require(RecoveryChainPolicy.resolve(enhanced, direct).executionPermitted(), "enhanced cannot handle proved direct records");
        require(!RecoveryChainPolicy.resolve(enhanced, derived).observationRequested(), "enhanced widened to derived-source policy");
        require(RecoveryChainPolicy.resolve(extreme, derived).executionPermitted(), "extreme cannot follow derived sources");
        var missingSave = extreme.withCapability(StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION,
            MechanismLevel.OFF, MechanismLevel.OFF, EvidenceState.UNOBSERVED, ActivationState.UNAVAILABLE);
        require(!RecoveryChainPolicy.resolve(missingSave, direct).executionPermitted()
            && RecoveryChainPolicy.resolve(missingSave, RecoveryChainPolicy.Step.DIRECT_SOURCE).executionPermitted(),
            "missing save support was bypassed or disabled an unrelated observer");
        var changed = extreme.configure(StrengthProfile.EXTREME, Map.of(), Map.of(AttackAction.ATTACK_CALL_CHAIN, AttackActionLevel.DISABLED));
        require(!RecoveryChainPolicy.resolve(changed, RecoveryChainPolicy.Step.DESCENDANT_TASK).executionPermitted(),
            "pending policy change reused an executable task policy");
        var custom = changed.activateConfigured();
        require(!RecoveryChainPolicy.resolve(custom, RecoveryChainPolicy.Step.DIRECT_TASK).observationRequested(),
            "disabled ordinary task action was restored by extreme");
        require(RecoveryChainPolicy.Step.DESCENDANT_TASK.actions().contains(AttackAction.SUPPRESS_REBUILD),
            "descendant task omitted rebuild permission");
        var monitor = extreme.withCapability(StrengthOptionKey.ATTACK_RECOVERY_SOURCE,
            MechanismLevel.STRICT_ALL, MechanismLevel.MONITOR, EvidenceState.VERIFIED, ActivationState.ACTIVE);
        require(!RecoveryChainPolicy.resolve(monitor, direct).executionPermitted(), "monitor became destructive permission");
        require(RecoveryChainPolicy.registeredNbtOutlet(enhanced, 1, true).executionPermitted()
            && !RecoveryChainPolicy.registeredNbtOutlet(enhanced, 2, true).executionPermitted()
            && !RecoveryChainPolicy.registeredNbtOutlet(extreme, 2, false).executionPermitted(),
            "scoped NBT permission bypassed depth or actual boundary evidence");
        var nativeOff = extreme.configure(StrengthProfile.CUSTOM,
            Map.of(StrengthOptionKey.RUNTIME_NATIVE, MechanismLevel.OFF)).activateConfigured();
        require(!RecoveryChainPolicy.registeredNbtOutlet(nativeOff, 1, true).executionPermitted(),
            "already loaded Native code bypassed an explicit Native OFF policy");
        require(RecoveryChainPolicy.registeredAtomicSnapshot(enhanced, 1, true).executionPermitted()
            && !RecoveryChainPolicy.registeredAtomicSnapshot(enhanced, 2, true).executionPermitted()
            && RecoveryChainPolicy.registeredAtomicSnapshot(extreme, 2, true).executionPermitted()
            && !RecoveryChainPolicy.registeredAtomicSnapshot(extreme, 1, false).executionPermitted(),
            "atomic snapshot action bypassed depth or current binding evidence");
        var recordOnly = enhanced.configure(StrengthProfile.ENHANCED, Map.of(),
            Map.of(AttackAction.SUPPRESS_REBUILD, AttackActionLevel.DISABLED)).activateConfigured();
        require(RecoveryChainPolicy.registeredAtomicSnapshot(recordOnly, 1, true).executionPermitted()
            && !RecoveryChainPolicy.registeredNbtOutlet(recordOnly, 1, true).executionPermitted(),
            "direct record action incorrectly required the independent rebuild outlet");
        var recordOff = extreme.configure(StrengthProfile.CUSTOM,
            Map.of(StrengthOptionKey.ATTACK_STATE_SOURCE, MechanismLevel.OFF)).activateConfigured();
        require(!RecoveryChainPolicy.registeredAtomicSnapshot(recordOff, 1, true).executionPermitted()
            && !RecoveryChainPolicy.registeredAtomicSnapshot(nativeOff, 1, true).executionPermitted()
            && !RecoveryChainPolicy.registeredAtomicSnapshot(changed, 1, true).executionPermitted(),
            "snapshot writer bypassed state/Native OFF or pending activation");
        System.out.println("RecoveryChainPolicySelfTest PASS");
    }
    private static StrengthPolicySnapshot policy(StrengthProfile profile) {
        var policy = StrengthPolicySnapshot.defaults().configure(profile, Map.of()).activateConfigured();
        for (var key : StrengthOptionKey.values()) policy = policy.withCapability(key, MechanismLevel.STRICT_ALL,
            MechanismLevel.STRICT_ALL, EvidenceState.VERIFIED, ActivationState.ACTIVE);
        return policy;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
