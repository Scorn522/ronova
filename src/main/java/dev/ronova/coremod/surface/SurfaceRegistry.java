package dev.ronova.coremod.surface;

import java.util.*;

/** Reports all eight categories; an observation-only adapter cannot claim enforcement or FULL. */
public final class SurfaceRegistry {
    public enum Maturity { S0_ABSENT, S1_OBSERVE, S2_ATTRIBUTE, S3_ENFORCE, S4_REPAIR, S5_RECOVER, S6_COMPLETE }
    public enum State { UNAVAILABLE, MONITOR, FAILED }
    public record Report(RuntimeSurface surface, Maturity supported, Maturity evidence, State state,
        long registryRevision, long sampledRevision, SurfaceAdapter.Observation lastTrustedObservation, String diagnostic) {}
    private final EnumMap<RuntimeSurface, SurfaceAdapter> adapters = new EnumMap<>(RuntimeSurface.class);
    private final EnumMap<RuntimeSurface, SurfaceAdapter.Observation> trusted = new EnumMap<>(RuntimeSurface.class);
    private final EnumMap<RuntimeSurface, String> failures = new EnumMap<>(RuntimeSurface.class);
    private final EnumMap<RuntimeSurface, Long> registrations = new EnumMap<>(RuntimeSurface.class);
    private final EnumMap<RuntimeSurface, Long> trustedRegistrations = new EnumMap<>(RuntimeSurface.class);
    private long revision;
    private long sampledRevision;
    public synchronized void register(SurfaceAdapter adapter) {
        Objects.requireNonNull(adapter);
        RuntimeSurface surface = Objects.requireNonNull(adapter.surface());
        if (adapters.containsKey(surface)) throw new IllegalArgumentException("surface adapter already registered");
        revision = Math.addExact(revision, 1); adapters.put(surface, adapter);
        registrations.put(surface, revision); failures.remove(surface);
    }
    public synchronized void remove(RuntimeSurface surface) {
        if (adapters.containsKey(surface)) { revision = Math.addExact(revision, 1); adapters.remove(surface); }
    }
    public synchronized void sample(SurfaceAdapter.Context context) {
        if (context.sampleRevision() <= sampledRevision) throw new IllegalArgumentException("non-monotonic surface observation");
        sampledRevision = context.sampleRevision();
        adapters.forEach((surface, adapter) -> {
            try {
                var value = Objects.requireNonNull(adapter.observe(context), "missing observation");
                if (!context.equals(value.context())) throw new IllegalArgumentException("observation context mismatch");
                trusted.put(surface, value); trustedRegistrations.put(surface, registrations.get(surface)); failures.remove(surface);
            } catch (RuntimeException failed) {
                String text = failed.getClass().getSimpleName() + ": " + Objects.toString(failed.getMessage(), "");
                failures.put(surface, text.substring(0, Math.min(256, text.length())));
            }
        });
    }
    public synchronized List<Report> snapshot() {
        List<Report> rows = new ArrayList<>();
        for (RuntimeSurface surface : RuntimeSurface.values()) {
            boolean registered = adapters.containsKey(surface), observed = registered && trusted.containsKey(surface)
                && Objects.equals(registrations.get(surface), trustedRegistrations.get(surface)) && !failures.containsKey(surface);
            rows.add(new Report(surface, registered ? Maturity.S1_OBSERVE : Maturity.S0_ABSENT,
                observed ? Maturity.S1_OBSERVE : Maturity.S0_ABSENT,
                !registered ? State.UNAVAILABLE : failures.containsKey(surface) ? State.FAILED : observed ? State.MONITOR : State.UNAVAILABLE,
                revision, sampledRevision, trusted.get(surface), failures.getOrDefault(surface,
                    registered ? "Observation only; attribution, plan, execution and completion ports are not registered" : "No registered observer")));
        }
        return List.copyOf(rows);
    }
}
