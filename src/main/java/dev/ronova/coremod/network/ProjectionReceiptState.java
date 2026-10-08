package dev.ronova.coremod.network;

import dev.ronova.coremod.contract.DominionEpoch;
import java.util.*;

/** One connection's expected image and acknowledgement; a new publication invalidates its old receipt. */
public final class ProjectionReceiptState {
    private final UUID realm,stream;
    private final long context;
    private long revision,acknowledged,receivedAt;
    private DominionEpoch epoch;
    private String digest;
    private ProjectionAcknowledgement lastAck;
    public ProjectionReceiptState(UUID realm,UUID stream,long context){this.realm=Objects.requireNonNull(realm);this.stream=Objects.requireNonNull(stream);if(context<1)throw new IllegalArgumentException("invalid context");this.context=context;}
    public synchronized void expect(long next,DominionEpoch epoch,String digest) {
        if(next<=revision||!digest.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("invalid projection expectation");
        revision=next;this.epoch=Objects.requireNonNull(epoch);this.digest=digest;
    }
    public synchronized boolean accept(ProjectionAcknowledgement ack,long now) {
        if(!matchesPublication(ack)||!Objects.equals(digest,ack.digest()))return false;
        if(acknowledged!=revision){acknowledged=revision;receivedAt=now;lastAck=ack;}return true;
    }
    public synchronized boolean matchesPublication(ProjectionAcknowledgement ack) {
        return realm.equals(ack.realm())&&stream.equals(ack.stream())&&context==ack.context()&&revision==ack.revision()&&Objects.equals(epoch,ack.epoch());
    }
    public synchronized boolean current(){return revision>0&&acknowledged==revision;}
    public synchronized long acknowledgedRevision(){return acknowledged;}
    public synchronized long receivedAt(){return receivedAt;}
    public synchronized boolean expects(long minimum,DominionEpoch epoch,String digest) {
        return revision>=minimum&&Objects.equals(this.epoch,epoch)&&Objects.equals(this.digest,digest);
    }
    public synchronized boolean confirms(long minimum,DominionEpoch epoch,String digest) {
        return current()&&Objects.equals(this.epoch,epoch)&&Objects.equals(this.digest,digest)
            &&lastAck!=null&&lastAck.revision()>=minimum&&lastAck.epoch().equals(epoch)&&lastAck.digest().equals(digest);
    }
}
