package dev.ronova.coremod.nativebridge;

import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionRequest;
import dev.ronova.coremod.contract.DominionSnapshot;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** JNI protocol adapter. DLL loading and gameplay coordination live elsewhere. */
final class NativeRootBridge {
    private static final AtomicLong DEGRADED_DECISIONS = new AtomicLong();

    private NativeRootBridge() {}

    static PublishResult publish(DominionSnapshot snapshot) {
        NativeSnapshotCodec.Encoded encoded = NativeSnapshotCodec.encode(snapshot);
        if (!NativeBridge.isRootCapable()) {
            return new PublishResult(PublishCode.UNAVAILABLE, encoded.fingerprint(), encoded.sha256());
        }
        try {
            PublishCode code = switch (installSnapshot(encoded.payload())) {
                case 0 -> PublishCode.INSTALLED;
                case 1 -> PublishCode.STALE;
                case 2 -> PublishCode.CURRENT;
                case 3 -> PublishCode.CONFLICT;
                default -> PublishCode.FAILED;
            };
            return new PublishResult(code, encoded.fingerprint(), encoded.sha256());
        } catch (Throwable ignored) {
            return new PublishResult(PublishCode.FAILED, encoded.fingerprint(), encoded.sha256());
        }
    }

    static DominionDecision decide(DominionRequest request) {
        Objects.requireNonNull(request, "request");
        if (!NativeBridge.isRootCapable()) {
            return null;
        }
        var subject = request.subject();
        var uuid = subject.uuid();
        try {
            long packed = decide0(
                domain(request), request.epoch().generation(), request.epoch().sequence(),
                uuid != null, uuid == null ? 0L : uuid.getMostSignificantBits(),
                uuid == null ? 0L : uuid.getLeastSignificantBits(),
                subject.registryType(), subject.runtimeClass(), subject.dimension(),
                subject.x(), subject.y(), subject.z(), subject.player(), facts(request),
                request instanceof DominionRequest.Judgment judgment
                    ? judgment.requestedScope().ordinal() : 0,
                request instanceof DominionRequest.Judgment judgment
                    ? judgment.requestedRadius() : 0.0D
            );
            return packed < 0L ? null : unpack(request, packed);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static RootStatus status() {
        if (!NativeBridge.isRootCapable()) {
            return RootStatus.UNAVAILABLE;
        }
        try {
            return RootStatus.from(state());
        } catch (Throwable ignored) {
            return RootStatus.UNAVAILABLE;
        }
    }

    static Optional<DominionSnapshot> snapshot() {
        if (!NativeBridge.isRootCapable()) {
            return Optional.empty();
        }
        try {
            byte[] payload = exportSnapshot();
            return payload == null || payload.length == 0
                ? Optional.empty() : Optional.of(NativeSnapshotCodec.decode(payload));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    static void acknowledge(long generation, long sequence, long fingerprint) {
        if (NativeBridge.isRootCapable()) {
            try {
                acknowledge0(generation, sequence, fingerprint);
            } catch (Throwable ignored) {
                // Reconciliation will retry on the next Native clock edge.
            }
        }
    }

    static int bootstrapLibrary() {
        return bootstrap();
    }

    static void startWatchdog(int intervalMillis) {
        startWatchdog0(Math.max(15_000, Math.min(60_000, intervalMillis)));
    }

    static void shutdownLibrary() {
        shutdown();
    }

    static void reset() {
        DEGRADED_DECISIONS.set(0L);
        if (NativeBridge.isRootCapable()) {
            try {
                reset0();
            } catch (Throwable ignored) {
                // Safe during server teardown and process shutdown.
            }
        }
    }

    static void recordDegradedDecision() {
        DEGRADED_DECISIONS.incrementAndGet();
    }

    static long degradedDecisions() {
        return DEGRADED_DECISIONS.get();
    }

    private static DominionDecision unpack(DominionRequest request, long packed) {
        boolean accepted = (packed & 1L) != 0L;
        int scopeOrdinal = (int) ((packed >>> 1) & 0x3L);
        int reasonCode = (int) ((packed >>> 8) & 0xffL);
        String reason = reason(reasonCode);
        var subject = request.subject();
        var uuid = subject.uuid();
        if (request instanceof DominionRequest.Authority) {
            long mask = accepted && reasonCode == 15
                ? CapabilitySet.OWNER_DEFAULT.mask()
                : accepted && uuid != null
                    ? capabilities(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()) : 0L;
            return new DominionDecision.Authority(
                subject, request.epoch(), accepted, new CapabilitySet(mask), reason
            );
        }
        if (request instanceof DominionRequest.Judgment judgment) {
            DominionDecision.LineageScope[] scopes = DominionDecision.LineageScope.values();
            DominionDecision.LineageScope scope = scopeOrdinal < scopes.length
                ? scopes[scopeOrdinal] : DominionDecision.LineageScope.GLOBAL_LINEAGE;
            return new DominionDecision.Judgment(
                subject, request.epoch(), accepted, scope,
                scope == DominionDecision.LineageScope.IDENTITY
                    ? judgment.requestedRadius() : 0.0D,
                reason
            );
        }
        if (request instanceof DominionRequest.Admission) {
            return new DominionDecision.Admission(subject, request.epoch(), accepted, reason);
        }
        return new DominionDecision.Presence(subject, request.epoch(), accepted, reason);
    }

    private static int domain(DominionRequest request) {
        if (request instanceof DominionRequest.Authority) return 0;
        if (request instanceof DominionRequest.Judgment) return 1;
        if (request instanceof DominionRequest.Admission) return 2;
        return 3;
    }

    private static long facts(DominionRequest request) {
        if (request instanceof DominionRequest.Authority authority) {
            return authority.recordedOwner() ? 1L : 0L;
        }
        if (request instanceof DominionRequest.Judgment judgment) {
            return (judgment.actorAuthorized() ? 1L : 0L)
                | (judgment.alreadyTerminal() ? 2L : 0L);
        }
        if (request instanceof DominionRequest.Admission admission) {
            return (admission.combatObject() ? 1L : 0L)
                | (admission.protectedSubject() ? 2L : 0L)
                | (admission.alreadyTerminal() ? 4L : 0L)
                | (admission.tombstoneMatch() ? 8L : 0L)
                | (admission.passedFinalBoundary() ? 16L : 0L)
                | (admission.finalBoundary() ? 32L : 0L);
        }
        return request instanceof DominionRequest.Presence presence && presence.quarantined()
            ? 1L : 0L;
    }

    private static String reason(int code) {
        return switch (code) {
            case 1 -> "native-owner";
            case 2 -> "native-not-owner";
            case 3 -> "native-actor-not-authorized";
            case 4 -> "native-already-terminal";
            case 5 -> "native-commit";
            case 6 -> "native-world-result";
            case 7 -> "native-authority-subject";
            case 8 -> "native-terminal-object";
            case 9 -> "native-tombstone-match";
            case 10 -> "native-cached-final-pass";
            case 11 -> "native-final-pass";
            case 12 -> "native-provisional-pass";
            case 13 -> "native-quarantined";
            case 14 -> "native-present";
            case 15 -> "native-proposal-owner";
            case 16 -> "native-proposal-not-owner";
            default -> "native-unspecified";
        };
    }

    private static native int installSnapshot(byte[] payload);
    private static native byte[] exportSnapshot();
    private static native long decide0(
        int domain, long generation, long sequence,
        boolean identified, long uuidMost, long uuidLeast,
        String registryType, String runtimeClass, String dimension,
        double x, double y, double z, boolean player, long facts,
        int requestedScope, double requestedRadius
    );
    private static native long capabilities(long uuidMost, long uuidLeast);
    private static native long[] state();
    private static native void acknowledge0(long generation, long sequence, long fingerprint);
    private static native void reset0();
    private static native int bootstrap();
    private static native void startWatchdog0(int intervalMillis);
    private static native void shutdown();

    enum PublishCode {
        INSTALLED, CURRENT, STALE, CONFLICT, UNAVAILABLE, FAILED;

        boolean accepted() {
            return this == INSTALLED || this == CURRENT;
        }
    }

    record PublishResult(PublishCode code, long fingerprint, String sha256) {}

    record RootStatus(
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
        long judgmentCount
    ) {
        static final RootStatus UNAVAILABLE = new RootStatus(
            false, false, 0L, 0L, 0L, 0L, 0L, 0L,
            0L, 0L, 0L, 0L, 0L, 0L, 0L
        );

        private static RootStatus from(long[] values) {
            if (values == null || values.length < 14) return UNAVAILABLE;
            long flags = values[0];
            return new RootStatus(
                (flags & 1L) != 0L, (flags & 2L) != 0L,
                values[1], values[2], values[3], values[4], values[5], values[6],
                values[7], values[8], values[9], values[10], values[11], values[12],
                values[13]
            );
        }
    }
}
