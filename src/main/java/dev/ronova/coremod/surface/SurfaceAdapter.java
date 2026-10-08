package dev.ronova.coremod.surface;

import dev.ronova.coremod.contract.*;
import java.util.*;

/** Typed observation port. Later execution ports require actual Root plans and receipts, not a claimed level. */
public interface SurfaceAdapter {
    record Context(RealmId realmId, DominionEpoch authorityEpoch, long sampleRevision, long sampledAtNanos) {
        public Context { Objects.requireNonNull(realmId); Objects.requireNonNull(authorityEpoch); }
    }
    record Observation(Context context, String sourceId, String scope, Map<String, Long> counters) {
        public Observation {
            Objects.requireNonNull(context); Objects.requireNonNull(sourceId); Objects.requireNonNull(scope);
            if (sourceId.isBlank() || sourceId.length() > 128 || scope.isBlank() || scope.length() > 256 || counters.size() > 32)
                throw new IllegalArgumentException("unbounded or missing observation provenance");
            counters = Collections.unmodifiableMap(new TreeMap<>(counters));
            counters.forEach((key, value) -> { if (key == null || key.isBlank() || key.length() > 96 || value == null || value < 0)
                throw new IllegalArgumentException("invalid observation counter"); });
        }
    }
    RuntimeSurface surface();
    Observation observe(Context context);
}
