package dev.ronova.coremod.surface;

import dev.ronova.coremod.contract.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SurfaceRegistrySelfTest {
    public static void main(String[] args) {
        var registry = new SurfaceRegistry();
        require(registry.snapshot().size() == 8 && registry.snapshot().stream().allMatch(r -> r.supported() == SurfaceRegistry.Maturity.S0_ABSENT),
            "missing adapter was reported as supported");
        AtomicBoolean fail = new AtomicBoolean();
        registry.register(new SurfaceAdapter() {
            @Override public RuntimeSurface surface() { return RuntimeSurface.DATA; }
            @Override public Observation observe(Context context) {
                if (fail.get()) throw new IllegalStateException("observer unavailable");
                return new Observation(context, "test:observer", "fixture only", Map.of("count", 3L));
            }
        });
        RealmId realm = new RealmId(new UUID(62, 1));
        registry.sample(new SurfaceAdapter.Context(realm, DominionEpoch.LOCAL_INITIAL, 1, 1));
        var good = registry.snapshot().get(0);
        require(good.evidence() == SurfaceRegistry.Maturity.S1_OBSERVE && good.state() == SurfaceRegistry.State.MONITOR, "working observer missing");
        fail.set(true); registry.sample(new SurfaceAdapter.Context(realm, DominionEpoch.LOCAL_INITIAL, 2, 2));
        var failed = registry.snapshot().get(0);
        require(failed.state() == SurfaceRegistry.State.FAILED && failed.evidence() == SurfaceRegistry.Maturity.S0_ABSENT
            && failed.lastTrustedObservation().equals(good.lastTrustedObservation()), "observer failure discarded trusted evidence or claimed current support");
        fail.set(false); registry.sample(new SurfaceAdapter.Context(realm, DominionEpoch.LOCAL_INITIAL, 3, 3));
        require(registry.snapshot().stream().allMatch(r -> r.supported().ordinal() <= 1 && r.evidence().ordinal() <= 1),
            "observation-only port claimed enforcement/completion");
        registry.remove(RuntimeSurface.DATA);
        require(registry.snapshot().get(0).supported() == SurfaceRegistry.Maturity.S0_ABSENT
            && registry.snapshot().get(0).lastTrustedObservation() != null, "unregistered adapter stayed enabled or lost diagnostic history");
        registry.register(new SurfaceAdapter() {
            @Override public RuntimeSurface surface() { return RuntimeSurface.DATA; }
            @Override public Observation observe(Context context) { return new Observation(context, "test:replacement", "new fixture", Map.of()); }
        });
        require(registry.snapshot().get(0).evidence() == SurfaceRegistry.Maturity.S0_ABSENT,
            "replacement observer inherited old provider evidence");
        registry.sample(new SurfaceAdapter.Context(realm, DominionEpoch.LOCAL_INITIAL, 4, 4));
        require(registry.snapshot().get(0).evidence() == SurfaceRegistry.Maturity.S1_OBSERVE, "replacement observer could not establish new evidence");
        System.out.println("SurfaceRegistrySelfTest: PASS (eight surfaces, explicit absence, bounded typed observation, failure/recovery/removal, no false FULL)");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
