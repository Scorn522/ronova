package dev.ronova.coremod.network;

import dev.ronova.coremod.contract.*;
import dev.ronova.coremod.root.api.*;
import java.util.*;

public final class ClientParticipationSelfTest {
    public static void main(String[] args) {
        var realm=new RealmId(new UUID(717,100));var key=new SubjectKey(realm,new UUID(717,101));
        var q=QualificationPreconditions.baseline(key).judgmentTransition(new UUID(717,102));
        var plan=TerminalOperationPlan.terminal(new UUID(717,103),EffectRecord.hash("root"),1,1,q,1,1);
        var required=new ClientParticipationContract.Member(new ClientParticipationContract.Endpoint(new UUID(717,104),new UUID(717,105),1,4),true,1);
        var optional=new ClientParticipationContract.Member(new ClientParticipationContract.Endpoint(new UUID(717,106),new UUID(717,107),1,4),false,0);
        var contract=new ClientParticipationContract(plan,new DominionEpoch(1,1),ProjectionDigest.of(List.of()),List.of(required,optional));
        var tracker=new ClientParticipationTracker(contract);
        require(!tracker.sample(member->!member.required(),0).requiredViewsStable(),"optional success satisfied missing required client");
        require(!tracker.sample(member->member.required(),1).requiredViewsStable(),"single observation bypassed stable window");
        tracker.sample(member->member.required(),11);
        require(tracker.sample(member->member.required(),21).requiredViewsStable(),"optional disconnect blocked required model convergence");
        var lost=tracker.sample(member->false,22);
        require(!lost.requiredViewsStable()&&lost.missingRequired().equals(Set.of(required.endpoint().client())),"disconnect silently shrank required membership");
        require(!tracker.sample(member->true,23).requiredViewsStable(),"reconnect reused the old stability window");
        tracker.sample(member->true,33);
        require(tracker.sample(member->true,43).requiredViewsStable(),"new stable window never completed");
        require(!tracker.sample(member->true,20).requiredViewsStable(),"clock rollback preserved completion");
        var changed=new ClientParticipationContract(plan,contract.epoch(),contract.imageDigest(),List.of(optional));
        require(!changed.id().equals(contract.id())&&contract.members().size()==2,"scope change rewrote the old contract");
        var interrupted=new ClientParticipationTracker(contract);
        interrupted.sample(member->true,0);
        require(!interrupted.sample(member->true,20).requiredViewsStable(),"long missing observation interval completed the contract");
        var deadline=new ClientParticipationTracker(contract);
        for(int tick=0;tick<=200;tick+=10)deadline.sample(member->false,tick);
        require(deadline.sample(member->true,201).deadlineExceeded(),"missing required client never reached deadline");
        deadline.sample(member->true,211);
        require(!deadline.sample(member->true,221).requiredViewsStable(),"late ACK erased a declared deadline failure");
        var refreshed=new ClientParticipationTracker(contract);
        refreshed.sample(member->true,0);refreshed.sample(member->true,10);
        var newImage=new ClientParticipationContract(plan,new DominionEpoch(1,2),contract.imageDigest(),contract.members());
        refreshed.refresh(newImage,15);
        require(!refreshed.sample(member->true,20).requiredViewsStable(),"image refresh reused old stability evidence");
        refreshed.sample(member->true,30);
        require(refreshed.sample(member->true,40).requiredViewsStable(),"refreshed image could not obtain new stable evidence");
        System.out.println("ClientParticipationSelfTest: PASS (frozen required/optional sets, stable window, disconnect, reconnect, clock rollback, distinct scope identity)");
    }
    private static void require(boolean value,String reason){if(!value)throw new AssertionError(reason);}
}
