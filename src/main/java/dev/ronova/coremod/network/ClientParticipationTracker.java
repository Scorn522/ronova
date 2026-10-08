package dev.ronova.coremod.network;

import dev.ronova.coremod.root.api.ClientParticipationContract;
import java.util.*;
import java.util.function.Predicate;

/** Stability is about required client model receipts. Optional disconnects do not shrink required membership. */
public final class ClientParticipationTracker {
    public static final int STABLE_TICKS=20, MAX_SAMPLE_GAP=10, WAIT_DEADLINE_TICKS=200;
    private ClientParticipationContract contract;
    private long since=-1,last=-1,waitingSince=-1,revision;
    private boolean expired;
    private Status previous;
    public record Status(boolean requiredViewsStable,Set<UUID> missingRequired,Set<UUID> missingOptional,long stableTicks,
                         boolean deadlineExceeded,long revision) {
        public Status {missingRequired=Set.copyOf(missingRequired);missingOptional=Set.copyOf(missingOptional);}
    }
    public ClientParticipationTracker(ClientParticipationContract contract){this.contract=Objects.requireNonNull(contract);}
    public synchronized ClientParticipationContract contract(){return contract;}
    /** Refresh the required image while retaining the original participants and wait deadline. */
    public synchronized void refresh(ClientParticipationContract next,long now) {
        Objects.requireNonNull(next);
        if(now<0||!contract.plan().effectKey().equals(next.plan().effectKey())||!contract.members().equals(next.members()))
            throw new IllegalArgumentException("refresh cannot change the frozen client membership");
        if(contract.equals(next))return;
        contract=next;since=-1;if(waitingSince<0)waitingSince=now;revision=Math.addExact(revision,1);
    }
    public synchronized Status sample(Predicate<ClientParticipationContract.Member> confirmed,long now) {
        if(now<0)throw new IllegalArgumentException("invalid clock");
        if(last>=0&&(now<last||now-last>MAX_SAMPLE_GAP)) {
            since=-1;if(waitingSince<0)waitingSince=Math.min(last,now);
        }
        last=now;
        var required=new HashSet<UUID>();var optional=new HashSet<UUID>();int requiredCount=0;
        for(var member:contract.members()) {
            if(member.required())requiredCount++;
            boolean accepted;
            try{accepted=confirmed.test(member);}catch(RuntimeException unavailable){accepted=false;}
            if(!accepted)(member.required()?required:optional).add(member.endpoint().client());
        }
        if(!required.isEmpty())since=-1;else if(since<0)since=now;
        long stable=since<0?0:now-since;
        boolean complete=required.isEmpty()&&(requiredCount==0||stable>=STABLE_TICKS);
        if(!complete) {
            if(waitingSince<0)waitingSince=now;
            if(now-waitingSince>=WAIT_DEADLINE_TICKS)expired=true;
        } else if(!expired)waitingSince=-1;
        complete&=!expired;
        if(previous==null||previous.requiredViewsStable()!=complete||previous.deadlineExceeded()!=expired
            ||!previous.missingRequired().equals(required)||!previous.missingOptional().equals(optional))revision=Math.addExact(revision,1);
        previous=new Status(complete,required,optional,stable,expired,revision);return previous;
    }
}
