package dev.ronova.coremod.network;

import dev.ronova.coremod.policy.*;
import java.util.*;

/** Bounded GUI contract. Values describe configured policy and evidence, never client-issued authority. */
public final class StrengthControl {
    private StrengthControl() {}
    public enum Operation { READ, PROFILE, OPTION, ACTION }
    public enum Outcome { SNAPSHOT, SAVED, STALE, FORBIDDEN, UNAVAILABLE }
    public enum AttackOutcome { NO_OPERATION, TERMINAL_COMMITTED, CHANGED }
    public enum Execution { IDLE, PARTIAL, FENCED }
    public record Context(UUID realm, UUID stream, long generation) {
        public Context { Objects.requireNonNull(realm); Objects.requireNonNull(stream); if(generation<1)throw new IllegalArgumentException("invalid context"); }
    }
    public record Request(Context context, UUID requestId, long expectedRevision, Operation operation, int key, int value) {
        public Request {
            Objects.requireNonNull(context);Objects.requireNonNull(requestId);Objects.requireNonNull(operation);
            if(expectedRevision<0)throw new IllegalArgumentException("invalid expected policy revision");
            switch(operation) {
                case READ -> { if(key!=0||value!=0)throw new IllegalArgumentException("invalid read"); }
                case PROFILE -> { if(key!=0||value<0||value>=StrengthProfile.values().length)throw new IllegalArgumentException("invalid profile"); }
                case OPTION -> { if(key<0||key>=StrengthOptionKey.values().length||value<0||value>=MechanismLevel.values().length)throw new IllegalArgumentException("invalid option"); }
                case ACTION -> { if(key<0||key>=AttackAction.values().length||value<0||value>=AttackActionLevel.values().length)throw new IllegalArgumentException("invalid action"); }
            }
        }
    }
    public record Option(StrengthOptionKey key, MechanismLevel configured, MechanismLevel effective, ActivationState activation) {
        public Option { Objects.requireNonNull(key);Objects.requireNonNull(configured);Objects.requireNonNull(effective);Objects.requireNonNull(activation); }
    }
    public record Action(AttackAction key, AttackActionLevel configured, MechanismLevel effective) {
        public Action { Objects.requireNonNull(key);Objects.requireNonNull(configured);Objects.requireNonNull(effective); }
    }
    public record Snapshot(Context context, UUID requestId, long revision, StrengthProfile configured, StrengthProfile active,
                           boolean editable, boolean pendingRestart, Outcome outcome, List<Option> options, List<Action> actions,
                           AttackOutcome attackOutcome,Execution execution,boolean completionSatisfied,
                           long completionRevision, String completionDetail) {
        public Snapshot(Context context,UUID requestId,long revision,StrengthProfile configured,StrengthProfile active,boolean editable,
            boolean pendingRestart,Outcome outcome,List<Option> options,List<Action> actions,AttackOutcome attackOutcome,Execution execution,boolean completionSatisfied) {
            this(context,requestId,revision,configured,active,editable,pendingRestart,outcome,options,actions,attackOutcome,execution,completionSatisfied,0,"");
        }
        public Snapshot(Context context,UUID requestId,long revision,StrengthProfile configured,StrengthProfile active,boolean editable,
            boolean pendingRestart,Outcome outcome,List<Option> options,List<Action> actions) {
            this(context,requestId,revision,configured,active,editable,pendingRestart,outcome,options,actions,AttackOutcome.NO_OPERATION,Execution.IDLE,false);
        }
        public Snapshot {
            Objects.requireNonNull(context);Objects.requireNonNull(requestId);Objects.requireNonNull(configured);Objects.requireNonNull(active);Objects.requireNonNull(outcome);
            Objects.requireNonNull(attackOutcome);Objects.requireNonNull(execution);
            Objects.requireNonNull(completionDetail);
            if(completionRevision<0||completionDetail.length()>256)throw new IllegalArgumentException("invalid completion detail");
            if(completionSatisfied&&(attackOutcome!=AttackOutcome.TERMINAL_COMMITTED||execution==Execution.FENCED))throw new IllegalArgumentException("inconsistent completion report");
            options=List.copyOf(options);actions=List.copyOf(actions);
            if(revision<1||options.size()!=StrengthOptionKey.values().length||actions.size()!=AttackAction.values().length)
                throw new IllegalArgumentException("incomplete policy snapshot");
            for(int i=0;i<options.size();i++)if(options.get(i).key().ordinal()!=i)throw new IllegalArgumentException("duplicate/out-of-order option");
            for(int i=0;i<actions.size();i++)if(actions.get(i).key().ordinal()!=i)throw new IllegalArgumentException("duplicate/out-of-order action");
        }
        public Snapshot operation(AttackOutcome result,Execution state,boolean complete) {
            return new Snapshot(context,requestId,revision,configured,active,editable,pendingRestart,outcome,options,actions,result,state,complete);
        }
        public Snapshot completion(long revision,String detail) {
            return new Snapshot(context,requestId,this.revision,configured,active,editable,pendingRestart,outcome,options,actions,
                attackOutcome,execution,completionSatisfied,revision,detail);
        }
    }
    public static StrengthPolicyConfiguration apply(Request request, StrengthPolicyConfiguration current) {
        if(request.expectedRevision()!=current.revision().value())throw new IllegalArgumentException("stale policy edit");
        return switch(request.operation()) {
            case READ -> current;
            case PROFILE -> current.next(StrengthProfile.values()[request.value()],Map.of());
            case OPTION -> current.next(StrengthProfile.CUSTOM,Map.of(StrengthOptionKey.values()[request.key()],MechanismLevel.values()[request.value()]));
            case ACTION -> current.next(StrengthProfile.CUSTOM,Map.of(),Map.of(AttackAction.values()[request.key()],AttackActionLevel.values()[request.value()]));
        };
    }
    public static Snapshot snapshot(Request request, StrengthPolicySnapshot policy, boolean editable, Outcome outcome) {
        return snapshot(request,policy,editable,outcome,Map.of());
    }
    /** Display-only, measured registered paths. This snapshot grants no execution authority. */
    public static Snapshot snapshot(Request request, StrengthPolicySnapshot policy, boolean editable, Outcome outcome,
        Map<TerminalActionPolicy.Step,Boolean> registeredBoundaries) {
        List<Option> options=new ArrayList<>();List<Action> actions=new ArrayList<>();
        for(var key:StrengthOptionKey.values()) {
            var resolved=policy.resolve(key); options.add(new Option(key,policy.configuredLevels().get(key),resolved.effectiveLevel(),resolved.activationState()));
        }
        for(var key:AttackAction.values()) {
            var step=switch(key) {
                case REWRITE_LIFE->TerminalActionPolicy.Step.LIFE;
                case DIRECT_CLEAR->TerminalActionPolicy.Step.REMOVAL;
                case CLEAR_ENTITY_RECORDS->TerminalActionPolicy.Step.REMOVED_RECORDS;
                default->null;
            };
            var effective=step!=null&&registeredBoundaries.containsKey(step)
                ?TerminalActionPolicy.resolve(policy,step,Boolean.TRUE.equals(registeredBoundaries.get(step))).effectiveDepth()
                :policy.resolveAttack(Set.of(key),Set.of()).effectiveDepth();
            actions.add(new Action(key,policy.configuredActions().levels().get(key),effective));
        }
        return new Snapshot(request.context(),request.requestId(),policy.revision().value(),policy.configuredProfile(),policy.activeProfile(),
            editable,policy.pendingRestart(),outcome,options,actions);
    }
}
