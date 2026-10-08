package dev.ronova.coremod.baseline;

import com.mojang.logging.LogUtils;
import dev.ronova.coremod.bootstrap.ServerRuntimeRegistry;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.network.RonovaAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;

/**
 * Opt-in, bounded 7.13 baseline telemetry. Disabled mode is one static branch
 * at each observation point and never calls {@link System#nanoTime()}.
 */
public final class BaselineTelemetry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean ENABLED = booleanSetting(
        "ronova.baseline.enabled", "RONOVA_BASELINE_ENABLED", false
    );
    private static final long WARMUP_SECONDS = longSetting(
        "ronova.baseline.warmupSeconds", "RONOVA_BASELINE_WARMUP_SECONDS",
        60L, 0L, 86_400L
    );
    private static final long MEASUREMENT_SECONDS = longSetting(
        "ronova.baseline.measurementSeconds",
        "RONOVA_BASELINE_MEASUREMENT_SECONDS", 300L, 1L, 86_400L
    );
    private static final int SAMPLE_CAPACITY = (int) longSetting(
        "ronova.baseline.sampleCapacity", "RONOVA_BASELINE_SAMPLE_CAPACITY",
        20_000L, 1_000L, 100_000L
    );
    private static final Map<MinecraftServer, BaselineSession> SESSIONS =
        new ConcurrentHashMap<>();
    private static volatile BaselineSession unscopedSession;

    private BaselineTelemetry() {}

    public static boolean enabled() {
        return ENABLED;
    }

    /** Called only by the real MinecraftServer.tickServer HEAD injection. */
    public static void serverTickHead(MinecraftServer server) {
        if (!ENABLED || server == null) {
            return;
        }
        try {
            BaselineSession session = session(server);
            if (session.tickHead(System.nanoTime())) {
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // Measurement must never affect the server tick.
        }
    }

    /** Advances WARMUP/MEASUREMENT when only the Forge END fallback is active. */
    public static void serverPulse(MinecraftServer server) {
        if (!ENABLED || server == null) {
            return;
        }
        try {
            BaselineSession session = session(server);
            if (session.pulse(System.nanoTime())) {
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // The fallback advances the window but never fabricates tick duration.
        }
    }

    /** Called only by the real MinecraftServer.tickServer TAIL injection. */
    public static void serverTickTail(MinecraftServer server) {
        if (!ENABLED || server == null) {
            return;
        }
        try {
            BaselineSession session = SESSIONS.get(server);
            if (session != null && session.tickTail(server, System.nanoTime())) {
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // Measurement must never affect the server tick.
        }
    }

    public static void recordAction(
        MinecraftServer server,
        RonovaAction action,
        long elapsedUs,
        int candidates,
        int judged,
        int tombstones,
        int topologySize,
        int quarantineObjects
    ) {
        if (!ENABLED || server == null || action == null) {
            return;
        }
        try {
            BaselineSession session = SESSIONS.get(server);
            if (session != null && session.recordAction(
                System.nanoTime(), action, elapsedUs, candidates, judged, tombstones,
                topologySize, quarantineObjects
            )) {
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // RONOVA_ACTION behavior and logging remain authoritative.
        }
    }

    public static long admissionStarted(MinecraftServer server) {
        if (!ENABLED || server == null) {
            return 0L;
        }
        try {
            BaselineSession session = SESSIONS.get(server);
            return session != null && session.measuring() ? System.nanoTime() : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public static void recordAdmission(
        MinecraftServer server, boolean nbt, boolean rejected, long startedNanos
    ) {
        if (!ENABLED || server == null || startedNanos == 0L) {
            return;
        }
        try {
            long ended = System.nanoTime();
            BaselineSession session = SESSIONS.get(server);
            if (session != null && session.recordAdmission(
                ended, nbt, rejected, elapsedUs(startedNanos, ended)
            )) {
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // Admission results and exception behavior must remain unchanged.
        }
    }

    public static long projectionStarted() {
        return unscopedStarted();
    }

    public static void recordProjection(
        long startedNanos, DominionSnapshot snapshot, int tombstones
    ) {
        if (!ENABLED || startedNanos == 0L || snapshot == null) {
            return;
        }
        try {
            long ended = System.nanoTime();
            BaselineSession session = soleSession();
            if (session != null && session.recordProjection(
                ended, elapsedUs(startedNanos, ended), tombstones,
                BaselineSession.judgmentCount(snapshot)
            )) {
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // Publication order and failure behavior remain unchanged.
        }
    }

    public static long rootCommitStarted() {
        return unscopedStarted();
    }

    public static void recordRootCommit(long startedNanos, boolean changed) {
        if (!ENABLED || startedNanos == 0L) {
            return;
        }
        try {
            long ended = System.nanoTime();
            BaselineSession session = soleSession();
            if (session != null && session.recordRootCommit(
                ended, elapsedUs(startedNanos, ended), changed
            )) {
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // Root receipt and exception semantics remain unchanged.
        }
    }

    public static long retryTickStarted(MinecraftServer server) {
        if (!ENABLED || server == null) {
            return 0L;
        }
        try {
            BaselineSession session = SESSIONS.get(server);
            return session != null && session.measuring() ? System.nanoTime() : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public static void recordRetryTick(
        MinecraftServer server,
        long startedNanos,
        int queued,
        int processed,
        int attempts,
        int remaining
    ) {
        if (!ENABLED || server == null || startedNanos == 0L) {
            return;
        }
        try {
            long ended = System.nanoTime();
            BaselineSession session = SESSIONS.get(server);
            if (session != null && session.recordRetryTick(
                ended, elapsedUs(startedNanos, ended), queued, processed, attempts, remaining
            )) {
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // Retry scheduling and budget behavior remain unchanged.
        }
    }

    public static void recordTerminalInvocation(Entity entity) {
        if (!ENABLED || entity == null) {
            return;
        }
        try {
            if (!(entity.level() instanceof ServerLevel level)) {
                return;
            }
            BaselineSession session = SESSIONS.get(level.getServer());
            if (session != null && session.recordTerminalInvocation(System.nanoTime())) {
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // Terminal lifecycle behavior remains authoritative.
        }
    }

    /** Flushes a complete or explicitly incomplete run without blocking server shutdown. */
    public static void serverStopped(MinecraftServer server) {
        if (!ENABLED || server == null) {
            return;
        }
        try {
            BaselineSession session = SESSIONS.remove(server);
            refreshUnscopedSession();
            if (session != null) {
                session.finish(System.nanoTime());
                archiveAsync(session);
            }
        } catch (Exception ignored) {
            // Shutdown cleanup must not depend on baseline archiving.
        }
    }

    private static long unscopedStarted() {
        if (!ENABLED) {
            return 0L;
        }
        try {
            BaselineSession session = soleSession();
            return session != null && session.measuring() ? System.nanoTime() : 0L;
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static BaselineSession soleSession() {
        return unscopedSession;
    }

    private static BaselineSession session(MinecraftServer server) {
        BaselineSession value = SESSIONS.computeIfAbsent(
            server, ignored -> {
                var runtime = ServerRuntimeRegistry.runtime(server);
                return new BaselineSession(
                    WARMUP_SECONDS,
                    MEASUREMENT_SECONDS,
                    SAMPLE_CAPACITY,
                    runtime.rawObservationEnabled(),
                    runtime.rawObservationFlagSource().name()
                );
            }
        );
        unscopedSession = SESSIONS.size() == 1 ? value : null;
        return value;
    }

    private static void refreshUnscopedSession() {
        if (SESSIONS.size() != 1) {
            unscopedSession = null;
            return;
        }
        unscopedSession = SESSIONS.values().iterator().next();
    }

    private static void archiveAsync(BaselineSession session) {
        if (session == null || !session.scheduleArchive()) {
            return;
        }
        Thread writer = new Thread(() -> {
            try {
                BaselineArchiveWriter.write(session);
            } catch (Exception failure) {
                session.archiveFailed();
                LOGGER.error(
                    "RONOVA_BASELINE archive failed: {}", failure.toString()
                );
            }
        }, "Ronova-Baseline-Archive");
        writer.setDaemon(false);
        writer.start();
    }

    private static long elapsedUs(long startedNanos, long endedNanos) {
        return Math.max(0L, endedNanos - startedNanos) / 1_000L;
    }

    private static boolean booleanSetting(
        String property, String environment, boolean fallback
    ) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environment);
        }
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value);
    }

    private static long longSetting(
        String property, String environment, long fallback, long minimum, long maximum
    ) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environment);
        }
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(minimum, Math.min(maximum, Long.parseLong(value)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
