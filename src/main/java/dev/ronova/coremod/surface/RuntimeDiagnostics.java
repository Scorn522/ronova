package dev.ronova.coremod.surface;

import dev.ronova.coremod.budget.*;
import dev.ronova.coremod.runtime.DominionServerRuntime;
import java.util.*;
import java.util.function.Supplier;

/** Per-runtime bounded sampler. Host-wide network counters are explicitly labeled and never attributed to this Realm. */
public final class RuntimeDiagnostics {
    private final DominionServerRuntime runtime;
    private final SurfaceRegistry surfaces = new SurfaceRegistry();
    private long sampleRevision;
    private long lastSample;
    public RuntimeDiagnostics(DominionServerRuntime runtime) {
        this.runtime = Objects.requireNonNull(runtime);
        add(RuntimeSurface.EVENT,"ronova:registered-event-callbacks","this Realm's handler entries/cancel requests; final bus outcome unproven",()->runtime.eventLedger().counters());
        add(RuntimeSurface.RESOURCE,"ronova:owned-permits","registered owned permits only; unobserved domains remain unavailable",()->{
            var counters=new LinkedHashMap<String,Long>();long observed=0;
            for(var row:runtime.resources().snapshot())if(row.ownedUsage()!=null){observed++;
                counters.put(row.domain().name().toLowerCase(Locale.ROOT)+"_permits",row.ownedUsage());
                counters.put(row.domain().name().toLowerCase(Locale.ROOT)+"_denials",row.denied());}
            counters.put("observed_domains",observed);return counters;
        });
        add(RuntimeSurface.DATA, "ronova:durable-root-and-recovery",
            "Realm Root and recovery observations/depth; requested policy does not prove disposal or durable save", () -> {
            var counters = new LinkedHashMap<String, Long>(Map.of(
            "commit_index", runtime.durableStore().commitIndex(), "effect_records", (long) runtime.durableStore().effectRecordCount(),
            "effect_unresolved", runtime.durableStore().effectUnresolvedCount(), "storage_rejections", runtime.durableStore().storageRejections(),
            "plan_reports_accepted", runtime.terminalPlans().acceptedReports(), "plan_fenced", runtime.terminalPlans().fencedPlans()));
            counters.putAll(runtime.recoverySources().counters());
            counters.putAll(runtime.recoverySources().policyCounters());
            counters.put("static_records_removed", runtime.recoverySources().removedStaticRecords());
            counters.put("loaded_records_removed", runtime.recoverySources().removedLoadedRecords());
            return counters;
        });
        add(RuntimeSurface.TEMPORAL, "ronova:registered-queues", "Realm construction and Ronova-owned ingress only; foreign tasks unproven", () -> {
            var counters=new LinkedHashMap<String,Long>(Map.of(
            "pending", (long) runtime.spawnAdmission().pendingConstructionRetries(),
            "stale", runtime.spawnAdmission().staleConstructionWork(), "dead_letters", (long) runtime.spawnAdmission().quarantinedConstructionFailures(),
            "backpressure", runtime.spawnAdmission().constructionBackpressure(),
            "obligation_details",(long)runtime.obligations().registry().size(),"obligation_reviews",runtime.obligations().reviews(),
            "obligation_overflow",runtime.obligations().registry().coverage().overflowCount(),
            "checkpoint_revision",runtime.obligations().checkpointRevision(),"checkpoint_failures",runtime.obligations().checkpointFailures()));
            counters.putAll(runtime.ingressWork().counters());
            counters.putAll(runtime.recoverySources().recordTaskCounters());return counters;
        });
        add(RuntimeSurface.REGISTRY, "ronova:admission", "Realm admission and observed static source registration; source disposal unproven", () -> Map.of(
            "cached_passes", (long) runtime.spawnAdmission().passedObjects(), "held_attempts", runtime.spawnAdmission().heldAdmissionAttempts(),
            "observed_objects", (long) runtime.observation().size(),
            "static_source_discoveries", runtime.recoverySources().staticSourceDiscoveries(),
            "static_snapshots_cleared", runtime.recoverySources().clearedStaticSnapshots(),
            "loaded_snapshots_cleared", runtime.recoverySources().clearedLoadedSnapshots(),
            "snapshot_writes_unverified", runtime.recoverySources().unverifiedSnapshotWrites(),
            "static_records_removed", runtime.recoverySources().removedStaticRecords(),
            "record_writes_unverified", runtime.recoverySources().unverifiedRecordWrites()));
        add(RuntimeSurface.NETWORK, "ronova:channel-counters", "process-wide shared counters; not per-Realm or opponent attribution", () -> Map.of(
            "stale_ingress", dev.ronova.coremod.network.ServerActionService.rejectedIngressWork(),
            "duplicate_receipts", dev.ronova.coremod.network.ServerActionService.duplicateIngressReceipts(),
            "wrong_realm", dev.ronova.coremod.network.ServerActionService.rejectedRealmRequests(),
            "backpressure", dev.ronova.coremod.network.ServerActionService.resourceBackpressure()));
    }
    private void add(RuntimeSurface surface, String source, String scope, Supplier<Map<String, Long>> counters) {
        surfaces.register(new SurfaceAdapter() {
            @Override public RuntimeSurface surface() { return surface; }
            @Override public Observation observe(Context context) { return new Observation(context, source, scope, counters.get()); }
        });
    }
    public synchronized void sampleIfDue(long now) {
        if (runtime.durableStore() == null || sampleRevision != 0 && now - lastSample < 1_000_000_000L) return;
        lastSample = now; sampleRevision = Math.addExact(sampleRevision, 1);
        JvmResourceSampler.sample(runtime.resources(), now);
        surfaces.sample(new SurfaceAdapter.Context(runtime.durableStore().realmId(), runtime.commands().epoch(), sampleRevision, now));
    }
    public List<SurfaceRegistry.Report> surfaces() { return surfaces.snapshot(); }
    public List<HostResourceGovernor.Row> resources() { return runtime.resources().snapshot(); }
}
