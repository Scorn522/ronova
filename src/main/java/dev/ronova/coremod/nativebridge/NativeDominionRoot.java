package dev.ronova.coremod.nativebridge;

import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.core.LocalDominionCore;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Process-local Native authority root used by the R3 projection cutover. */
public final class NativeDominionRoot {
    private static final AtomicBoolean SOURCE_INSTALLED = new AtomicBoolean();

    private NativeDominionRoot() {}

    public static Publication publish(DominionSnapshot snapshot) {
        NativeRootBridge.PublishResult result = NativeRootBridge.publish(snapshot);
        boolean nativeTruthAvailable = result.code().accepted()
            || ((result.code() == NativeRootBridge.PublishCode.STALE
                || result.code() == NativeRootBridge.PublishCode.CONFLICT)
                && NativeRootBridge.status().ready());
        if (nativeTruthAvailable && SOURCE_INSTALLED.compareAndSet(false, true)) {
            LocalDominionCore.install(NativeDominionDecisionSource.INSTANCE);
        }
        return new Publication(
            PublishState.valueOf(result.code().name()), result.fingerprint(), result.sha256()
        );
    }

    public static void acknowledge(DominionSnapshot snapshot, long fingerprint) {
        if (snapshot != null && authoritative()) {
            NativeRootBridge.acknowledge(
                snapshot.epoch().generation(), snapshot.epoch().sequence(), fingerprint
            );
        }
    }

    public static Optional<DominionSnapshot> snapshot() {
        return NativeRootBridge.snapshot();
    }

    public static Status status() {
        NativeRootBridge.RootStatus status = NativeRootBridge.status();
        return new Status(
            status.ready(), status.projectionCurrent(), status.generation(), status.sequence(),
            status.fingerprint(), status.policyGeneration(), status.admissionGeneration(),
            status.reconciliationClock(), status.publications(), status.staleRejections(),
            status.conflicts(), status.driftDetections(), status.nativeDecisions(),
            status.authorityCount(), status.judgmentCount(), NativeRootBridge.degradedDecisions()
        );
    }

    public static boolean authoritative() {
        return SOURCE_INSTALLED.get() && status().ready();
    }

    public static long degradedDecisions() {
        return NativeRootBridge.degradedDecisions();
    }

    public static void reset() {
        SOURCE_INSTALLED.set(false);
        LocalDominionCore.resetLocal();
        NativeRootBridge.reset();
    }

    public record Publication(
        PublishState state,
        long fingerprint,
        String sha256
    ) {
        public boolean accepted() {
            return state == PublishState.INSTALLED || state == PublishState.CURRENT;
        }

        public boolean enforced() {
            return state != PublishState.UNAVAILABLE;
        }
    }

    public enum PublishState {
        INSTALLED, CURRENT, STALE, CONFLICT, UNAVAILABLE, FAILED
    }

    public record Status(
        boolean ready,
        boolean projectionCurrent,
        long generation,
        long sequence,
        long fingerprint,
        long policyGeneration,
        long admissionGeneration,
        long reconciliationClock,
        long publications,
        long staleRejections,
        long conflicts,
        long driftDetections,
        long nativeDecisions,
        long authorityCount,
        long judgmentCount,
        long degradedDecisions
    ) {}
}
