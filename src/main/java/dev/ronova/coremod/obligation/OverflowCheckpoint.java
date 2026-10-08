package dev.ronova.coremod.obligation;

import dev.ronova.coremod.root.api.TargetEpoch;
import java.util.*;

/** Retained retry evidence, then bounded re-observation scopes, then explicitly unscoped loss. */
public record OverflowCheckpoint(List<ObligationRegistry.View> retryEntries, List<Scope> scopes, long unscopedCount) {
    public static final int MAX_RETRY_ENTRIES=128, MAX_SCOPES=128;
    public static final OverflowCheckpoint EMPTY=new OverflowCheckpoint(List.of(),List.of(),0);
    public record Scope(TargetEpoch target, Set<ObligationKey.Source> sources, long count, long firstSeen, long lastSeen) {
        public Scope {
            Objects.requireNonNull(target);Objects.requireNonNull(sources);
            if(sources.isEmpty()||count<1||firstSeen<0||lastSeen<firstSeen)
                throw new IllegalArgumentException("invalid overflow scope");
            sources=Collections.unmodifiableSet(EnumSet.copyOf(sources));
        }
    }
    public OverflowCheckpoint {
        Objects.requireNonNull(retryEntries);Objects.requireNonNull(scopes);
        if(retryEntries.size()>MAX_RETRY_ENTRIES||scopes.size()>MAX_SCOPES||unscopedCount<0)
            throw new IllegalArgumentException("invalid overflow bounds");
        var keys=new HashSet<ObligationKey>();
        for(var entry:retryEntries)if(entry==null||!keys.add(entry.key()))
            throw new IllegalArgumentException("duplicate overflow retry entry");
        var targets=new HashSet<TargetEpoch>();
        for(var scope:scopes)if(scope==null||!targets.add(scope.target()))
            throw new IllegalArgumentException("duplicate overflow scope");
        retryEntries=retryEntries.stream().sorted(Comparator.comparing(entry->entry.key().toString())).toList();
        scopes=scopes.stream().sorted(Comparator.comparing(scope->scope.target().toString())).toList();
    }
    /** Old counters did not identify which observations were lost. */
    public static OverflowCheckpoint legacy(long overflowCount) {
        return overflowCount==0?EMPTY:new OverflowCheckpoint(List.of(),List.of(),overflowCount);
    }
}
