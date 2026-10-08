package dev.ronova.coremod.projection;

import dev.ronova.coremod.baseline.BaselineTelemetry;
import dev.ronova.coremod.budget.BudgetTelemetrySnapshot;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.nativebridge.NativeDominionRoot;
import dev.ronova.coremod.nativebridge.NativeSnapshotCodec;
import dev.ronova.coremod.presence.OwnerImmunity;
import dev.ronova.coremod.presence.PresencePolicy;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.MinecraftServer;

/** Atomically publishes one coherent projection bundle and rejects epoch rollback. */
public final class ProjectionCoordinator {
    private static final Map<MinecraftServer, ProjectionCoordinator> SERVERS =
        Collections.synchronizedMap(new WeakHashMap<>());

    private final AtomicReference<ProjectionBundle> current = new AtomicReference<>();
    private final AdmissionProjection.AdmissionMetrics admissionMetrics =
        new AdmissionProjection.AdmissionMetrics();
    private final AtomicLong publications = new AtomicLong();
    private final AtomicLong staleRejections = new AtomicLong();
    private final AtomicLong nativeRecoveries = new AtomicLong();
    private final AtomicLong publishedFingerprint = new AtomicLong();
    private final AtomicLong lastReconciliationClock = new AtomicLong(-1L);
    private volatile FrameBudgetGovernor frameBudgetGovernor;

    public ProjectionCoordinator() {}

    public ProjectionBundle publish(DominionSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot is required");
        }
        long baselineStarted = BaselineTelemetry.projectionStarted();
        while (true) {
            ProjectionBundle previous = current.get();
            if (previous != null) {
                int ordering = snapshot.epoch().compareTo(previous.snapshot().epoch());
                if (ordering < 0) {
                    staleRejections.incrementAndGet();
                    return previous;
                }
                if (ordering == 0) {
                    if (!snapshot.equals(previous.snapshot())) {
                        staleRejections.incrementAndGet();
                    }
                    return previous;
                }
            }
            NativeDominionRoot.Publication nativePublication =
                NativeDominionRoot.publish(snapshot);
            if (nativePublication.enforced() && !nativePublication.accepted()) {
                staleRejections.incrementAndGet();
                if (previous != null) {
                    return previous;
                }
                DominionSnapshot authoritative = NativeDominionRoot.snapshot()
                    .orElseThrow(() -> new IllegalStateException(
                        "Native root rejected initial projection without an exportable snapshot"
                    ));
                ProjectionBundle recovered = build(authoritative);
                if (current.compareAndSet(null, recovered)) {
                    long fingerprint = NativeSnapshotCodec.fingerprint(authoritative);
                    publishedFingerprint.set(fingerprint);
                    nativeRecoveries.incrementAndGet();
                    publications.incrementAndGet();
                    NativeDominionRoot.acknowledge(authoritative, fingerprint);
                    BaselineTelemetry.recordProjection(
                        baselineStarted, authoritative,
                        recovered.admission().tombstoneCount()
                    );
                    return recovered;
                }
                continue;
            }
            ProjectionBundle next = build(snapshot);
            if (current.compareAndSet(previous, next)) {
                publishedFingerprint.set(nativePublication.fingerprint());
                publications.incrementAndGet();
                NativeDominionRoot.acknowledge(snapshot, nativePublication.fingerprint());
                BaselineTelemetry.recordProjection(
                    baselineStarted, snapshot, next.admission().tombstoneCount()
                );
                return next;
            }
        }
    }

    public ProjectionBundle published() {
        ProjectionBundle value = current.get();
        return value == null ? build(DominionSnapshot.EMPTY) : value;
    }

    public long publications() {
        return publications.get();
    }

    public long staleRejections() {
        return staleRejections.get();
    }

    public long nativeRecoveries() {
        return nativeRecoveries.get();
    }

    /** Returns the optional per-server 7.14D telemetry sink. */
    public FrameBudgetGovernor frameBudgetGovernor() {
        return frameBudgetGovernor;
    }

    public static ProjectionBundle current(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        return coordinator(server).published();
    }

    public static ProjectionBundle synchronize(
        MinecraftServer server, DominionSnapshot snapshot
    ) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        return coordinator(server).publish(snapshot);
    }

    public static long publications(MinecraftServer server) {
        return coordinator(server).publications();
    }

    public static long staleRejections(MinecraftServer server) {
        return coordinator(server).staleRejections();
    }

    public static long nativeRecoveries(MinecraftServer server) {
        return coordinator(server).nativeRecoveries();
    }

    /** Rebuilds a missing or drifted Java projection from the Native-owned snapshot. */
    public static boolean reconcileNative(MinecraftServer server) {
        if (server == null) {
            return false;
        }
        return coordinator(server).reconcileNative();
    }

    /** Instance reconciliation used by the composed runtime. */
    public boolean reconcileNative() {
        ProjectionCoordinator coordinator = this;
        long baselineStarted = BaselineTelemetry.projectionStarted();
        NativeDominionRoot.Status status = NativeDominionRoot.status();
        if (!status.ready()) {
            return false;
        }
        ProjectionBundle javaProjection = coordinator.current.get();
        long lastClock = coordinator.lastReconciliationClock.get();
        boolean clockEdge = lastClock != status.reconciliationClock();
        if (javaProjection != null
            && javaProjection.snapshot().epoch().generation() == status.generation()
            && javaProjection.snapshot().epoch().sequence() == status.sequence()
            && coordinator.publishedFingerprint.get() == status.fingerprint()) {
            if (!clockEdge) {
                return false;
            }
            long verified = NativeSnapshotCodec.fingerprint(javaProjection.snapshot());
            if (verified == status.fingerprint()) {
                coordinator.lastReconciliationClock.set(status.reconciliationClock());
                NativeDominionRoot.acknowledge(javaProjection.snapshot(), verified);
                return false;
            }
        }

        DominionSnapshot authoritative = NativeDominionRoot.snapshot().orElse(null);
        if (authoritative == null
            || authoritative.epoch().generation() != status.generation()
            || authoritative.epoch().sequence() != status.sequence()) {
            return false;
        }
        long fingerprint = NativeSnapshotCodec.fingerprint(authoritative);
        if (fingerprint != status.fingerprint()) {
            return false;
        }
        ProjectionBundle recovered = coordinator.build(authoritative);
        coordinator.current.set(recovered);
        coordinator.publishedFingerprint.set(fingerprint);
        coordinator.lastReconciliationClock.set(status.reconciliationClock());
        coordinator.nativeRecoveries.incrementAndGet();
        coordinator.publications.incrementAndGet();
        NativeDominionRoot.acknowledge(authoritative, fingerprint);
        BaselineTelemetry.recordProjection(
            baselineStarted, authoritative, recovered.admission().tombstoneCount()
        );
        return true;
    }

    public static void release(MinecraftServer server) {
        if (server != null) {
            synchronized (SERVERS) {
                SERVERS.remove(server);
                if (SERVERS.isEmpty()) {
                    NativeDominionRoot.reset();
                }
            }
        }
    }

    private static ProjectionCoordinator coordinator(MinecraftServer server) {
        synchronized (SERVERS) {
            return SERVERS.computeIfAbsent(server, ignored -> new ProjectionCoordinator());
        }
    }

    /** Explicit composition seam; does not load persistence or publish a snapshot. */
    public static ProjectionCoordinator attach(MinecraftServer server) {
        return attach(server, null);
    }

    /** Attaches the per-server 7.14D telemetry sink before projection publication. */
    public static ProjectionCoordinator attach(
        MinecraftServer server,
        FrameBudgetGovernor frameBudgetGovernor
    ) {
        if (server == null) {
            throw new IllegalArgumentException("server is required");
        }
        ProjectionCoordinator value = coordinator(server);
        FrameBudgetGovernor previous = value.frameBudgetGovernor;
        value.frameBudgetGovernor = frameBudgetGovernor;
        if (previous != frameBudgetGovernor && value.current.get() != null) {
            value.current.updateAndGet(bundle ->
                bundle == null ? null : value.build(bundle.snapshot())
            );
        }
        return value;
    }

    /**
     * Builds one epoch-coherent bundle. The 7.14C presence policy is created here
     * and bound to this same bundle's owner topology, so owner immunity can never
     * be evaluated against a different epoch than the quarantine flag it guards.
     */
    private ProjectionBundle build(DominionSnapshot snapshot) {
        AuthorityTopologyProjection topology = new AuthorityTopologyProjection(snapshot);
        return new ProjectionBundle(
            snapshot,
            topology,
            new CapabilityProjection(snapshot),
            new AdmissionProjection(snapshot, admissionMetrics),
            new PresencePolicy(OwnerImmunity.of(topology::owns), frameBudgetGovernor)
        );
    }
}
