package dev.ronova.coremod.policy;

import java.util.Map;

public final class TerminalActionPolicySelfTest {
    public static void main(String[] args) {
        dev.ronova.coremod.root.api.WriteSetLeaseSelfTest.main(new String[0]);
        var extreme=StrengthPolicySnapshot.fromConfiguration(new StrengthPolicyConfiguration(
            PolicyRevision.INITIAL,StrengthProfile.EXTREME,Map.of()));
        for(var step:TerminalActionPolicy.Step.values()) {
            var allowed=TerminalActionPolicy.resolve(extreme,step,true);
            require(allowed.permitted()&&allowed.effectiveDepth()==MechanismLevel.PROTECT_RONOVA,
                "registered executor overstated its implemented depth");
            require(!TerminalActionPolicy.resolve(extreme,step,false).permitted(),"unverified boundary granted a write");
            for(var action:step.actions()) {
                var disabled=extreme.configure(StrengthProfile.EXTREME,Map.of(),Map.of(action,AttackActionLevel.DISABLED)).activateConfigured();
                require(!TerminalActionPolicy.resolve(disabled,step,true).permitted(),"required ordinary switch ignored: "+action);
            }
        }
        var noLife=extreme.configure(StrengthProfile.EXTREME,Map.of(),Map.of(AttackAction.REWRITE_LIFE,AttackActionLevel.DISABLED)).activateConfigured();
        require(!TerminalActionPolicy.resolve(noLife,TerminalActionPolicy.Step.LIFE,true).permitted()
            &&TerminalActionPolicy.resolve(noLife,TerminalActionPolicy.Step.REMOVAL,true).permitted(),"life switch leaked into removal");
        var noClear=extreme.configure(StrengthProfile.EXTREME,Map.of(),Map.of(AttackAction.DIRECT_CLEAR,AttackActionLevel.DISABLED)).activateConfigured();
        require(TerminalActionPolicy.resolve(noClear,TerminalActionPolicy.Step.REMOVED_RECORDS,true).permitted()
            &&!TerminalActionPolicy.resolve(noClear,TerminalActionPolicy.Step.BYPASS_RECORDS,true).permitted(),
            "index cleanup either bypassed directClear or suppressed normal removal consistency");
        var pending=extreme.configure(StrengthProfile.COMPATIBLE,Map.of());
        require(!TerminalActionPolicy.resolve(pending,TerminalActionPolicy.Step.LIFE,true).permitted(),"pending policy used scoped capability");
        var monitor=extreme.configure(StrengthProfile.EXTREME,Map.of(StrengthOptionKey.ATTACK_STATE_SOURCE,MechanismLevel.MONITOR)).activateConfigured();
        require(!TerminalActionPolicy.resolve(monitor,TerminalActionPolicy.Step.LIFE,true).permitted(),"monitor-only granted mutation");
        require(extreme.resolve(StrengthOptionKey.ATTACK_STATE_SOURCE).effectiveLevel()==MechanismLevel.OFF,
            "local executor evidence mutated global capability");
        System.out.println("TerminalActionPolicySelfTest: PASS (separate actions, observed boundary, scoped depth, pending and monitor)");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
