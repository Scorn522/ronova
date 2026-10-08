package dev.ronova.coremod.policy;

import java.util.*;

/** Fixed permissions for the currently implemented entity-local terminal executors. */
public final class TerminalActionPolicy {
    public enum Step {
        LIFE(0x8063,Set.of(AttackAction.REWRITE_LIFE)),
        REMOVAL(0x3,Set.of(AttackAction.DIRECT_CLEAR)),
        REMOVED_RECORDS(0x1f89,Set.of(AttackAction.CLEAR_ENTITY_RECORDS)),
        BYPASS_RECORDS(0x1f89,Set.of(AttackAction.CLEAR_ENTITY_RECORDS,AttackAction.DIRECT_CLEAR));
        private final int mask;
        private final Set<AttackAction> actions;
        Step(int mask,Set<AttackAction> actions){this.mask=mask;this.actions=Set.copyOf(actions);}
        public Set<AttackAction> actions(){return actions;}
        public int mask(){return mask;}
    }
    private static final Set<StrengthOptionKey> KEYS=Set.of(StrengthOptionKey.ATTACK_STATE_SOURCE,
        StrengthOptionKey.RUNTIME_NATIVE,StrengthOptionKey.RUNTIME_JVMTI,
        StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH,StrengthOptionKey.PROTECT_CODE_INTEGRITY);
    private TerminalActionPolicy(){}
    public static MechanismLevel requestedDepth(StrengthPolicySnapshot policy,Step step) {
        var keys=EnumSet.copyOf(KEYS);var depth=MechanismLevel.STRICT_ALL;
        for(var action:step.actions()) {
            depth=MechanismLevel.lowerOf(depth,policy.activeActions().resolve(policy.activeProfile(),action).depth());
            keys.addAll(action.requiredKeys());
        }
        for(var key:keys)depth=MechanismLevel.lowerOf(depth,ProfilePresetMatrix.resolve(policy.activeMatrixRevision(),
            policy.activeProfile(),key,policy.activeLevels().get(key)).requestedLevel());
        return depth;
    }
    public static AttackResolution resolve(StrengthPolicySnapshot policy,Step step,boolean boundaryVerified) {
        Objects.requireNonNull(policy);Objects.requireNonNull(step);
        if(policy.pendingRestart())return new AttackResolution(MechanismLevel.OFF,MechanismLevel.OFF,false,"PENDING_RESTART");
        // Only this registered executor receives the measured boundary capability.
        // Do not advertise these broad option keys as supported for other surfaces.
        var keys=EnumSet.copyOf(KEYS);
        step.actions().forEach(action->keys.addAll(action.requiredKeys()));
        var scoped=policy;
        for(var key:keys)scoped=scoped.withCapability(key,MechanismLevel.PROTECT_RONOVA,
            boundaryVerified?MechanismLevel.PROTECT_RONOVA:MechanismLevel.OFF,
            boundaryVerified?EvidenceState.VERIFIED:EvidenceState.UNOBSERVED,
            boundaryVerified?ActivationState.ACTIVE:ActivationState.UNAVAILABLE);
        var result=scoped.resolveAttack(step.actions(),KEYS);
        if(!result.permitted())return result;
        // These executors act on one registered entity and its exact index references.
        // Selecting Extreme does not claim producer, native or arbitrary resource coverage.
        return new AttackResolution(result.requestedDepth(),MechanismLevel.PROTECT_RONOVA,true,"REGISTERED_ENTITY_ONLY");
    }
}
