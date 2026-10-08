package dev.ronova.coremod.baseline;

import dev.ronova.coremod.network.RonovaAction;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Freezes disabled-by-default and bounded-storage baseline invariants. */
public final class BaselineTelemetrySelfTest {
    private BaselineTelemetrySelfTest() {}

    public static void main(String[] args) throws Exception {
        String explicitProperty = System.getProperty("ronova.baseline.enabled");
        String explicitEnvironment = System.getenv("RONOVA_BASELINE_ENABLED");
        if ((explicitProperty == null || explicitProperty.isBlank())
            && (explicitEnvironment == null || explicitEnvironment.isBlank())) {
            require(!BaselineTelemetry.enabled(),
                "baseline telemetry must be disabled unless explicitly enabled");
        }

        BaselineSession session = new BaselineSession(0L, 300L, 4);
        long now = System.nanoTime();
        session.tickHead(now);
        for (int i = 0; i < 6; i++) {
            session.recordAction(
                now + i + 1L, RonovaAction.PRIMARY, 10L + i,
                2 + i, 1 + i, 3 + i, 4 + i, 5 + i
            );
            session.recordAdmission(now + i + 1L, false, (i & 1) == 0, 20L + i);
            session.recordProjection(now + i + 1L, 30L + i, 3 + i, 2 + i);
            session.recordRetryTick(now + i + 1L, 40L + i, i, i, i, i);
        }
        boolean incompleteSnapshotRejected = false;
        try {
            session.snapshot();
        } catch (IllegalStateException expected) {
            incompleteSnapshotRejected = true;
        }
        require(incompleteSnapshotRejected,
            "archive snapshot must wait for an immutable complete session");
        session.finish(now + 1_000_000L);
        session.recordAction(
            now + 1_000_001L, RonovaAction.PRIMARY, 99L,
            99, 99, 99, 99, 99
        );
        session.tickHead(now + 1_000_002L);
        session.tickTail(null, now + 1_000_003L);
        BaselineSession.Snapshot snapshot = session.snapshot();
        require(snapshot.actions().get(RonovaAction.PRIMARY).totalCount() == 6L,
            "action count was not retained");
        require(snapshot.actions().get(RonovaAction.PRIMARY).elapsedUs().length == 4,
            "action sample capacity is not bounded");
        require(snapshot.actions().get(RonovaAction.PRIMARY).dropped() == 2L,
            "overflowed action samples were not counted");
        require(snapshot.admission().accepted() == 3L
                && snapshot.admission().rejected() == 3L,
            "admission outcomes were not counted");
        require(snapshot.projections().elapsedUs().length == 4,
            "projection sample capacity is not bounded");
        require(snapshot.retries().processedTotal() == 15L,
            "retry processed total was not retained");
        require(snapshot.phase() == BaselineSession.Phase.COMPLETE,
            "completed session did not publish COMPLETE before archive snapshot");
        Method snapshotMethod = BaselineSession.class.getDeclaredMethod("snapshot");
        require(!Modifier.isSynchronized(snapshotMethod.getModifiers()),
            "archive snapshot must not contend for the session monitor");

        Method serverStopped = BaselineTelemetry.class.getMethod(
            "serverStopped", net.minecraft.server.MinecraftServer.class
        );
        require(Modifier.isPublic(serverStopped.getModifiers())
                && Modifier.isStatic(serverStopped.getModifiers()),
            "server stop archive hook is missing");

        Path temporaryParent = Path.of("build", "tmp").toAbsolutePath().normalize();
        Files.createDirectories(temporaryParent);
        Path gameDirectory = Files.createTempDirectory(
            temporaryParent, "baseline-telemetry-selftest-"
        ).toAbsolutePath().normalize();
        require(gameDirectory.startsWith(temporaryParent),
            "temporary baseline directory escaped build/tmp");
        try {
            Path baselineRoot = gameDirectory.resolve("baseline");
            BaselineArchiveWriter.write(session, gameDirectory);
            Path runDirectory;
            try (var runs = Files.list(baselineRoot.resolve("7.13").resolve("runs"))) {
                runDirectory = runs.findFirst()
                    .orElseThrow(() -> new AssertionError("baseline archive directory missing"));
            }
            require(Files.isRegularFile(runDirectory.resolve("BASELINE-ENVIRONMENT.json")),
                "environment archive missing");
            require(Files.isRegularFile(runDirectory.resolve("BASELINE-SUMMARY.json")),
                "summary archive missing");
            require(Files.isRegularFile(runDirectory.resolve("BASELINE-ACTIONS.csv")),
                "action CSV missing");
            require(Files.isRegularFile(runDirectory.resolve("BASELINE-TICKS.csv")),
                "tick CSV missing");
            String summary = Files.readString(runDirectory.resolve("BASELINE-SUMMARY.json"));
            require(summary.contains("\"latency_unit\": \"us\"")
                    && summary.contains("\"PRIMARY\"")
                    && summary.contains("NOT_AVAILABLE_IN_7_13"),
                "summary schema or availability markers are incomplete");
        } finally {
            deleteTree(gameDirectory);
        }

        System.out.println(
            "BaselineTelemetrySelfTest: PASS "
                + "(disabled default, bounded samples, explicit shutdown archive)"
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
