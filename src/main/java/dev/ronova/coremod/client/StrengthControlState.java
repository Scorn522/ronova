package dev.ronova.coremod.client;

import dev.ronova.coremod.network.StrengthControl;
import java.util.*;

/** One outstanding GUI request; late replies cannot acknowledge another edit or connection. */
public final class StrengthControlState {
    private StrengthControl.Context context;
    private StrengthControl.Snapshot snapshot;
    private StrengthControl.Request pending;
    private long sentAt,acceptedAt;
    public static final long REFRESH_INTERVAL_MILLIS=1000, SNAPSHOT_FRESH_MILLIS=5000;
    public synchronized void bind(StrengthControl.Context next) { if(!next.equals(context)){reset();context=next;} }
    public synchronized StrengthControl.Request begin(StrengthControl.Operation operation,int key,int value,long now) {
        expire(now);
        if(context==null||pending!=null||operation!=StrengthControl.Operation.READ&&(snapshot==null||!snapshot.editable()||!fresh(now)))return null;
        pending=new StrengthControl.Request(context,UUID.randomUUID(),snapshot==null?0:snapshot.revision(),operation,key,value);sentAt=now;return pending;
    }
    public synchronized boolean accept(StrengthControl.Snapshot next) { return accept(next,sentAt); }
    public synchronized boolean accept(StrengthControl.Snapshot next,long now) {
        expire(now);
        if(pending==null||!next.context().equals(context)||!next.requestId().equals(pending.requestId())
            ||snapshot!=null&&next.revision()<snapshot.revision())return false;
        snapshot=next;acceptedAt=now;pending=null;return true;
    }
    public synchronized void expire(long now){if(pending!=null&&(now<sentAt||now-sentAt>=5000))pending=null;}
    /** Only the visible screen requests refreshes; this state does not create a background network loop. */
    public synchronized boolean refreshDue(long now) {
        return context!=null&&pending==null&&(snapshot==null||now<sentAt||now-sentAt>=REFRESH_INTERVAL_MILLIS);
    }
    public synchronized boolean fresh(long now) {return snapshot!=null&&now>=acceptedAt&&now-acceptedAt<SNAPSHOT_FRESH_MILLIS;}
    public synchronized long snapshotAgeMillis(long now) {return snapshot==null||now<acceptedAt?-1:now-acceptedAt;}
    public synchronized boolean pending(){return pending!=null;}
    public synchronized StrengthControl.Snapshot snapshot(){return snapshot;}
    public synchronized void reset(){context=null;snapshot=null;pending=null;sentAt=0;acceptedAt=0;}
}
