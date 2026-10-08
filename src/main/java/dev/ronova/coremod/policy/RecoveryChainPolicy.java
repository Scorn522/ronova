package dev.ronova.coremod.policy;

import java.util.*;

/** Fixed production requirements for recovery-chain work; a caller cannot omit a dependency. */
public final class RecoveryChainPolicy {
    private static final Set<StrengthOptionKey> NBT_GATE_KEYS = Set.of(
        StrengthOptionKey.RUNTIME_NATIVE, StrengthOptionKey.RUNTIME_JVMTI,
        StrengthOptionKey.RUNTIME_CRITICAL_CLASS_WATCH, StrengthOptionKey.PROTECT_CODE_INTEGRITY);
    public enum Step {
        DIRECT_SOURCE(AttackAction.SUPPRESS_RESPAWN, MechanismLevel.RELATED_PATHS,
            StrengthOptionKey.ATTACK_RECOVERY_SOURCE),
        RECORD_DISPOSAL(AttackAction.SUPPRESS_RESPAWN, MechanismLevel.RELATED_PATHS,
            StrengthOptionKey.ATTACK_RECOVERY_SOURCE, StrengthOptionKey.ATTACK_STATE_SOURCE),
        SAVED_RECORD_DISPOSAL(AttackAction.SUPPRESS_RESPAWN, MechanismLevel.RELATED_PATHS,
            StrengthOptionKey.ATTACK_RECOVERY_SOURCE, StrengthOptionKey.ATTACK_STATE_SOURCE,
            StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION),
        DIRECT_TASK(AttackAction.ATTACK_CALL_CHAIN, MechanismLevel.RELATED_PATHS),
        REBUILD_OUTLET(AttackAction.SUPPRESS_REBUILD, MechanismLevel.RELATED_PATHS),
        DERIVED_SOURCE(AttackAction.SUPPRESS_REBUILD, MechanismLevel.STRICT_ALL,
            StrengthOptionKey.ATTACK_LOGIC_SOURCE, StrengthOptionKey.ATTACK_STATE_SOURCE),
        DESCENDANT_TASK(AttackAction.ATTACK_CALL_CHAIN, MechanismLevel.STRICT_ALL),
        RELOAD_RECONSTRUCTION(AttackAction.SUPPRESS_REBUILD, MechanismLevel.STRICT_ALL,
            StrengthOptionKey.ATTACK_STATE_SOURCE, StrengthOptionKey.ATTACK_SAVE_RECONSTRUCTION);

        private final Set<AttackAction> actions;
        private final Set<StrengthOptionKey> keys;
        private final MechanismLevel minimum;
        Step(AttackAction action, MechanismLevel minimum, StrengthOptionKey... extras) {
            this.minimum = minimum;
            // Every chain action also requires the ordinary suppression switch.
            var requiredActions = EnumSet.of(AttackAction.SUPPRESS_RESPAWN, action);
            if (action == AttackAction.ATTACK_CALL_CHAIN && minimum == MechanismLevel.STRICT_ALL)
                requiredActions.add(AttackAction.SUPPRESS_REBUILD);
            actions = Set.copyOf(requiredActions);
            var requiredKeys = EnumSet.noneOf(StrengthOptionKey.class);
            actions.forEach(value -> requiredKeys.addAll(value.requiredKeys()));
            requiredKeys.addAll(Arrays.asList(extras)); keys = Set.copyOf(requiredKeys);
        }
        public Set<AttackAction> actions() { return actions; }
        public Set<StrengthOptionKey> keys() { return keys; }
        public MechanismLevel minimum() { return minimum; }
    }
    public record Decision(Step step, MechanismLevel requested, MechanismLevel effective,
                           boolean observationRequested, boolean executionPermitted, String reason) {}
    private RecoveryChainPolicy() {}

    public static Decision resolve(StrengthPolicySnapshot policy, Step step) {
        Objects.requireNonNull(policy); Objects.requireNonNull(step);
        MechanismLevel requested = requested(policy, step);
        boolean observes = requested.rank() >= step.minimum().rank();
        if (!observes) return new Decision(step, requested, MechanismLevel.OFF, false, false,
            "OUTSIDE_ACTIVE_RECOVERY_DEPTH");
        var resolution = policy.resolveAttack(step.actions(), step.keys());
        boolean executes = resolution.permitted() && resolution.effectiveDepth().rank() >= step.minimum().rank();
        return new Decision(step, requested, resolution.effectiveDepth(), true, executes,
            executes ? "POLICY_PERMITTED_REQUIRES_CURRENT_SOURCE_AND_WRITE_LEASE"
                : resolution.permitted() ? "RECOVERY_DEPTH_LIMITED_BY_DEPENDENCY" : resolution.reason());
    }
    private static MechanismLevel requested(StrengthPolicySnapshot policy, Step step) {
        MechanismLevel requested = MechanismLevel.STRICT_ALL;
        for (AttackAction action : step.actions()) requested = MechanismLevel.lowerOf(requested,
            policy.activeActions().resolve(policy.activeProfile(), action).depth());
        for (StrengthOptionKey key : step.keys()) requested = MechanismLevel.lowerOf(requested,
            ProfilePresetMatrix.resolve(policy.activeMatrixRevision(), policy.activeProfile(), key,
                policy.activeLevels().get(key)).requestedLevel());
        return requested;
    }
    public static Map<Step, Decision> snapshot(StrengthPolicySnapshot policy) {
        var decisions = new EnumMap<Step, Decision>(Step.class);
        for (Step step : Step.values()) decisions.put(step, resolve(policy, step));
        return Collections.unmodifiableMap(decisions);
    }
    /** Exact static AtomicReference invalidation; this does not raise the general record or save ceiling. */
    public static Decision registeredAtomicSnapshot(StrengthPolicySnapshot policy, int copyDepth, boolean bindingVerified) {
        if (copyDepth < 0) throw new IllegalArgumentException("invalid copy depth");
        Step step = Step.RECORD_DISPOSAL;
        var requested = requested(policy, step);
        var minimum = step.minimum();
        if (copyDepth > 1) {
            requested = MechanismLevel.lowerOf(requested, requested(policy, Step.DERIVED_SOURCE));
            minimum = Step.DERIVED_SOURCE.minimum();
        }
        for (var key : NBT_GATE_KEYS) requested = MechanismLevel.lowerOf(requested,
            ProfilePresetMatrix.resolve(policy.activeMatrixRevision(), policy.activeProfile(), key,
                policy.activeLevels().get(key)).requestedLevel());
        boolean observes = requested.rank() >= minimum.rank();
        boolean permitted = observes && !policy.pendingRestart() && bindingVerified;
        return new Decision(step, requested, permitted ? requested : MechanismLevel.OFF, observes, permitted,
            !observes ? "OUTSIDE_ACTIVE_RECOVERY_DEPTH" : policy.pendingRestart() ? "PENDING_RESTART"
                : !bindingVerified ? "STATIC_ATOMIC_BINDING_UNVERIFIED" : "PERMITTED_FOR_THIS_STATIC_ATOMIC_SNAPSHOT_ONLY");
    }
    /** Scopes capability evidence to the registered NBT outlet; it is never published as a global key ceiling. */
    public static Decision registeredNbtOutlet(StrengthPolicySnapshot policy, int copyDepth, boolean boundaryVerified) {
        Step step = copyDepth > 1 ? Step.DERIVED_SOURCE : Step.REBUILD_OUTLET;
        if (copyDepth < 0) throw new IllegalArgumentException("invalid copy depth");
        var requested = requested(policy, step);
        for (var key : NBT_GATE_KEYS) requested = MechanismLevel.lowerOf(requested,
            ProfilePresetMatrix.resolve(policy.activeMatrixRevision(), policy.activeProfile(), key,
                policy.activeLevels().get(key)).requestedLevel());
        boolean observes = requested.rank() >= step.minimum().rank();
        boolean permitted = observes && !policy.pendingRestart() && boundaryVerified;
        return new Decision(step, requested, permitted ? requested : MechanismLevel.OFF, observes, permitted,
            !observes ? "OUTSIDE_ACTIVE_RECOVERY_DEPTH" : policy.pendingRestart() ? "PENDING_RESTART"
                : !boundaryVerified ? "REGISTERED_NBT_BOUNDARY_UNVERIFIED" : "PERMITTED_FOR_THIS_OBSERVED_NBT_SOURCE_ONLY");
    }
    public static Decision registeredTaskDisposition(StrengthPolicySnapshot policy,int copyDepth,boolean bindingVerified) {
        return registeredWork(policy,copyDepth>1?Step.DESCENDANT_TASK:Step.DIRECT_TASK,copyDepth,bindingVerified);
    }
    public static Decision registeredStorageCommit(StrengthPolicySnapshot policy,int copyDepth,boolean bindingVerified) {
        return registeredWork(policy,copyDepth>1?Step.RELOAD_RECONSTRUCTION:Step.SAVED_RECORD_DISPOSAL,copyDepth,bindingVerified);
    }
    private static Decision registeredWork(StrengthPolicySnapshot policy,Step step,int copyDepth,boolean bindingVerified) {
        if(copyDepth<0)throw new IllegalArgumentException("invalid copy depth");
        var requested=requested(policy,step);
        for(var key:NBT_GATE_KEYS)requested=MechanismLevel.lowerOf(requested,
            ProfilePresetMatrix.resolve(policy.activeMatrixRevision(),policy.activeProfile(),key,policy.activeLevels().get(key)).requestedLevel());
        boolean observes=requested.rank()>=step.minimum().rank(),permitted=observes&&!policy.pendingRestart()&&bindingVerified;
        return new Decision(step,requested,permitted?requested:MechanismLevel.OFF,observes,permitted,
            !observes?"OUTSIDE_ACTIVE_RECOVERY_DEPTH":policy.pendingRestart()?"PENDING_RESTART":!bindingVerified?"RECOVERY_WORK_BINDING_UNVERIFIED":"PERMITTED_FOR_THIS_REGISTERED_RECOVERY_WORK_ONLY");
    }
}
