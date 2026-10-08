package dev.ronova.coremod.event;

import java.util.*;

/** Counts this runtime's registered callback entries and cancellation requests; never claims final bus denial. */
public final class RuntimeEventLedger {
    public enum Kind { ATTACK, HURT, DAMAGE, DEATH, JOIN }
    private final long[] entries=new long[Kind.values().length],cancellations=new long[Kind.values().length];
    public synchronized void entered(Kind kind){int i=kind.ordinal();if(entries[i]!=Long.MAX_VALUE)entries[i]++;}
    public synchronized void cancellationRequested(Kind kind){int i=kind.ordinal();if(cancellations[i]!=Long.MAX_VALUE)cancellations[i]++;}
    public synchronized Map<String,Long> counters(){var result=new LinkedHashMap<String,Long>();for(var kind:Kind.values()){
        result.put(kind.name().toLowerCase(Locale.ROOT)+"_entries",entries[kind.ordinal()]);
        result.put(kind.name().toLowerCase(Locale.ROOT)+"_cancel_requests",cancellations[kind.ordinal()]);}return Map.copyOf(result);}
}
