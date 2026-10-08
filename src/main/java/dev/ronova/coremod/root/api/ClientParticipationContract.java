package dev.ronova.coremod.root.api;

import dev.ronova.coremod.contract.DominionEpoch;
import java.util.*;

/** Frozen projection-view sub-contract. It never asserts whole-operation or renderer completion. */
public record ClientParticipationContract(TerminalOperationPlan plan,DominionEpoch epoch,String imageDigest,List<Member> members) {
    public record Endpoint(UUID client,UUID stream,long context,long minimumRevision) {
        public Endpoint {Objects.requireNonNull(client);Objects.requireNonNull(stream);if(context<1||minimumRevision<1)throw new IllegalArgumentException("invalid client endpoint");}
    }
    public record Member(Endpoint endpoint,boolean required,long ownerRevision) {
        public Member {Objects.requireNonNull(endpoint);if(ownerRevision<0)throw new IllegalArgumentException("invalid owner revision");}
    }
    public ClientParticipationContract {
        Objects.requireNonNull(plan);Objects.requireNonNull(epoch);Objects.requireNonNull(imageDigest);
        members=members.stream().sorted(Comparator.comparing(m->m.endpoint().client())).toList();
        if(!imageDigest.matches("[0-9a-f]{64}")||members.size()>64||members.stream().map(m->m.endpoint().client()).distinct().count()!=members.size())
            throw new IllegalArgumentException("invalid client participation contract");
    }
    public String id(){return EffectRecord.hash("projection-view-contract/v2/"+plan.causalityKey()+"/"+epoch+"/"+imageDigest+"/"+members);}
}
