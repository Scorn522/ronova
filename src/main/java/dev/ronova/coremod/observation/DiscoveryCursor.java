package dev.ronova.coremod.observation;

import java.util.Iterator;
import java.util.Objects;

/** Short idle lease for a registered discovery iterator, released on context exit or inactivity. */
final class DiscoveryCursor<T> {
    static final long MAX_IDLE_NANOS=2_000_000_000L;
    private Iterator<? extends T> iterator;
    private long touched;
    void set(Iterator<? extends T> value,long now) { iterator=Objects.requireNonNull(value);touched=now; }
    Iterator<? extends T> get(long now) { expire(now);return iterator; }
    void touch(long now) { if(!expire(now)&&iterator!=null)touched=now; }
    boolean expire(long now) {
        long age=now-touched;
        if(iterator!=null&&(age<0||age>=MAX_IDLE_NANOS)){clear();return true;}
        return false;
    }
    void clear() { iterator=null;touched=0; }
}
