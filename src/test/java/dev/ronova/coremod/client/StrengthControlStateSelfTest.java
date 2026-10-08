package dev.ronova.coremod.client;

import dev.ronova.coremod.network.StrengthControl;
import dev.ronova.coremod.policy.*;
import java.util.*;

public final class StrengthControlStateSelfTest {
    public static void main(String[] args) {
        var context=new StrengthControl.Context(new UUID(717,1),new UUID(717,2),1);
        var state=new StrengthControlState();state.bind(context);
        require(state.begin(StrengthControl.Operation.PROFILE,0,1,0)==null,"edit before server authorization");
        var read=state.begin(StrengthControl.Operation.READ,0,0,0);
        var policy=StrengthPolicySnapshot.defaults();
        var extreme=StrengthPolicySnapshot.fromConfiguration(new StrengthPolicyConfiguration(PolicyRevision.INITIAL,StrengthProfile.EXTREME,Map.of()));
        var scoped=StrengthControl.snapshot(read,extreme,true,StrengthControl.Outcome.SNAPSHOT,
            Map.of(TerminalActionPolicy.Step.LIFE,true,TerminalActionPolicy.Step.REMOVAL,false,TerminalActionPolicy.Step.REMOVED_RECORDS,true));
        require(scoped.actions().get(AttackAction.REWRITE_LIFE.ordinal()).effective()==MechanismLevel.PROTECT_RONOVA
            &&scoped.actions().get(AttackAction.DIRECT_CLEAR.ordinal()).effective()==MechanismLevel.OFF,
            "GUI erased a registered action or enabled an unavailable sibling");
        require(scoped.options().get(StrengthOptionKey.ATTACK_STATE_SOURCE.ordinal()).effective()==MechanismLevel.OFF
            &&scoped.actions().get(AttackAction.ATTACK_RENDER.ordinal()).effective()==MechanismLevel.OFF,
            "GUI broadened entity-local evidence into global or renderer coverage");
        require(state.accept(StrengthControl.snapshot(read,policy,false,StrengthControl.Outcome.SNAPSHOT)),"initial read rejected");
        require(state.begin(StrengthControl.Operation.PROFILE,0,1,1)==null,"read-only client enabled edit");
        var authorized=state.begin(StrengthControl.Operation.READ,0,0,2);
        require(state.accept(StrengthControl.snapshot(authorized,policy,true,StrengthControl.Outcome.SNAPSHOT)),"authorized snapshot failed");
        var edit=state.begin(StrengthControl.Operation.ACTION,AttackAction.REWRITE_LIFE.ordinal(),AttackActionLevel.DISABLED.ordinal(),3);
        require(state.begin(StrengthControl.Operation.PROFILE,0,2,4)==null,"overlapping edits queued");
        var next=StrengthControl.apply(edit,policy.configuration());
        require(next.revision().value()==2&&next.attacks().levels().get(AttackAction.REWRITE_LIFE)==AttackActionLevel.DISABLED,"edit did not use canonical action policy");
        require(!state.accept(StrengthControl.snapshot(authorized,policy,true,StrengthControl.Outcome.SNAPSHOT)),"old reply acknowledged a new edit");
        require(state.accept(StrengthControl.snapshot(edit,StrengthPolicySnapshot.fromConfigurations(next,policy.configuration()),true,StrengthControl.Outcome.SAVED)),"save receipt rejected");
        require(state.snapshot().pendingRestart(),"saved request became active without restart");
        try{StrengthControl.apply(edit,next);throw new AssertionError("stale edit silently renewed");}catch(IllegalArgumentException expected){}
        var late=state.begin(StrengthControl.Operation.READ,0,0,5);state.expire(5005);
        require(!state.pending()&&!state.accept(StrengthControl.snapshot(late,policy,true,StrengthControl.Outcome.SNAPSHOT)),"timeout accepted late receipt");
        state.bind(new StrengthControl.Context(context.realm(),new UUID(717,3),2));
        require(state.snapshot()==null,"connection change retained writable policy");
        var live=new StrengthControlState();live.bind(context);
        require(live.refreshDue(0),"empty connected screen never requested initial state");
        var initial=live.begin(StrengthControl.Operation.READ,0,0,0);
        require(!live.refreshDue(1000),"automatic refresh overlapped an outstanding request");
        live.accept(StrengthControl.snapshot(initial,policy,true,StrengthControl.Outcome.SNAPSHOT),10);
        require(!live.refreshDue(999)&&live.refreshDue(1000),"visible refresh cadence is unbounded or stalled");
        require(live.fresh(5009)&&!live.fresh(5010),"old completion snapshot remained current indefinitely");
        require(live.begin(StrengthControl.Operation.PROFILE,0,1,5010)==null,"stale policy snapshot authorized a new edit");
        var timed=live.begin(StrengthControl.Operation.READ,0,0,5010);
        require(!live.accept(StrengthControl.snapshot(timed,policy,true,StrengthControl.Outcome.SNAPSHOT),10010),"late packet revived a timed-out request without screen tick");
        var newer=live.begin(StrengthControl.Operation.READ,0,0,10011);
        live.bind(new StrengthControl.Context(context.realm(),new UUID(717,99),2));
        require(!live.accept(StrengthControl.snapshot(newer,policy,true,StrengthControl.Outcome.SNAPSHOT),10012)&&!live.fresh(10012),"old connection reply revived prior completion");
        System.out.println("StrengthControlStateSelfTest: PASS (server authorization, single outstanding edit, exact reply, stale revision, pending restart, timeout and reconnect)");
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
