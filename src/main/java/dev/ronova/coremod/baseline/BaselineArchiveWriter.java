package dev.ronova.coremod.baseline;

import com.mojang.logging.LogUtils;
import dev.ronova.coremod.agent.AgentHooks;
import dev.ronova.coremod.baseline.BaselineSession.ActionSnapshot;
import dev.ronova.coremod.baseline.BaselineSession.AdmissionSnapshot;
import dev.ronova.coremod.baseline.BaselineSession.ProjectionSnapshot;
import dev.ronova.coremod.baseline.BaselineSession.RangeSnapshot;
import dev.ronova.coremod.baseline.BaselineSession.RetrySnapshot;
import dev.ronova.coremod.baseline.BaselineSession.RootCommitSnapshot;
import dev.ronova.coremod.baseline.BaselineSession.Snapshot;
import dev.ronova.coremod.baseline.BaselineSession.TickSnapshot;
import dev.ronova.coremod.nativebridge.NativeBridge;
import dev.ronova.coremod.network.RonovaAction;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/** Writes one immutable session snapshot outside the server tick thread. */
final class BaselineArchiveWriter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_VERSION = "0.2.0-alpha.7.13";
    private static final String MINECRAFT_VERSION = "1.20.1";
    private static final String FORGE_VERSION = "47.4.22";
    private static final DateTimeFormatter RUN_ID = DateTimeFormatter
        .ofPattern("uuuuMMdd-HHmmss-SSS", Locale.ROOT)
        .withZone(ZoneOffset.UTC);

    private BaselineArchiveWriter() {}

    static void write(BaselineSession session) throws Exception {
        write(session, FMLPaths.GAMEDIR.get().toAbsolutePath());
    }

    static void write(BaselineSession session, Path gameDirectory) throws Exception {
        Snapshot snapshot = session.snapshot();
        Instant timestamp = Instant.ofEpochMilli(snapshot.createdEpochMillis());
        Path directory = gameDirectory.toAbsolutePath()
            .resolve("baseline").resolve("7.13").resolve("runs")
            .resolve(RUN_ID.format(timestamp) + "Z");
        Files.createDirectories(directory);
        writeEnvironment(directory.resolve("BASELINE-ENVIRONMENT.json"), snapshot, timestamp);
        writeSummary(directory.resolve("BASELINE-SUMMARY.json"), snapshot, timestamp);
        writeActions(directory.resolve("BASELINE-ACTIONS.csv"), snapshot);
        writeTicks(directory.resolve("BASELINE-TICKS.csv"), snapshot.ticks());
        writeAdmission(directory.resolve("BASELINE-ADMISSION.csv"), snapshot.admission());
        writeProjections(directory.resolve("BASELINE-PROJECTION.csv"), snapshot.projections());
        writeRetries(directory.resolve("BASELINE-RETRIES.csv"), snapshot.retries());
        writeRootCommits(directory.resolve("BASELINE-ROOT-COMMITS.csv"), snapshot.rootCommits());
        writeReadme(directory.resolve("BASELINE-README.md"), snapshot, timestamp);
        LOGGER.info(
            "RONOVA_BASELINE archive={} phase={} reason={} ticks={} actions={} admission={} publications={}",
            directory, snapshot.phase(), snapshot.completionReason(),
            snapshot.ticks().totalCount(), actionCount(snapshot),
            snapshot.admission().totalCount(), snapshot.projections().totalCount()
        );
    }

    private static void writeEnvironment(
        Path path, Snapshot snapshot, Instant timestamp
    ) throws Exception {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long physicalMemory = physicalMemory();
        String sourceHash = sourceHash();
        StringBuilder json = new StringBuilder(1_024);
        json.append("{\n")
            .append("  \"schema\": \"ronova-baseline-7.13-v1\",\n")
            .append("  \"timestamp_utc\": ").append(quote(timestamp.toString())).append(",\n")
            .append("  \"ronova_version\": ").append(quote(modVersion())).append(",\n")
            .append("  \"minecraft_version\": ").append(quote(MINECRAFT_VERSION)).append(",\n")
            .append("  \"forge_version\": ").append(quote(FORGE_VERSION)).append(",\n")
            .append("  \"java_version\": ").append(quote(property("java.version"))).append(",\n")
            .append("  \"jvm_vendor\": ").append(quote(property("java.vendor"))).append(",\n")
            .append("  \"jvm_name\": ").append(quote(property("java.vm.name"))).append(",\n")
            .append("  \"jvm_arguments\": ").append(stringArray(
                ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(String[]::new)
            )).append(",\n")
            .append("  \"os\": {\n")
            .append("    \"name\": ").append(quote(property("os.name"))).append(",\n")
            .append("    \"version\": ").append(quote(property("os.version"))).append(",\n")
            .append("    \"architecture\": ").append(quote(property("os.arch"))).append("\n")
            .append("  },\n")
            .append("  \"cpu\": {\n")
            .append("    \"available_processors\": ")
            .append(Runtime.getRuntime().availableProcessors()).append(",\n")
            .append("    \"identifier\": ").append(quote(environment("PROCESSOR_IDENTIFIER"))).append("\n")
            .append("  },\n")
            .append("  \"memory\": {\n")
            .append("    \"jvm_max_bytes\": ").append(maxMemory).append(",\n")
            .append("    \"physical_bytes\": ")
            .append(physicalMemory >= 0L ? Long.toString(physicalMemory) : "null")
            .append("\n  },\n")
            .append("  \"git_commit\": \"NOT_AVAILABLE_NO_GIT_METADATA\",\n")
            .append("  \"source_hash_sha256\": ").append(quote(sourceHash)).append(",\n")
            .append("  \"raw_observation\": {\n")
            .append("    \"enabled\": ").append(snapshot.rawObservationEnabled()).append(",\n")
            .append("    \"mode\": ").append(quote(
                snapshot.rawObservationEnabled() ? "RAW" : "LEGACY"
            )).append(",\n")
            .append("    \"flag_source\": ")
            .append(quote(snapshot.rawObservationFlagSource())).append(",\n")
            .append("    \"watch_mode\": ").append(quote(
                snapshot.rawObservationEnabled()
                    ? "PASSIVE_LEGACY_MIRROR" : "PASSIVE_DISABLED"
            )).append("\n  },\n")
            .append("  \"native_status\": {\n")
            .append("    \"state\": ").append(quote(NativeBridge.state().name())).append(",\n")
            .append("    \"capability_mask_hex\": ")
            .append(quote("0x" + Integer.toHexString(NativeBridge.capabilityMask()))).append("\n")
            .append("  },\n")
            .append("  \"agent_status\": {\n")
            .append("    \"early\": ").append(AgentHooks.isAgentActive()).append(",\n")
            .append("    \"late\": ").append(AgentHooks.isLateGuardActive()).append("\n")
            .append("  },\n")
            .append("  \"measurement\": {\n")
            .append("    \"warmup_seconds\": ").append(snapshot.configuredWarmupSeconds()).append(",\n")
            .append("    \"measurement_seconds\": ").append(snapshot.configuredMeasurementSeconds()).append(",\n")
            .append("    \"actual_measurement_millis\": ").append(snapshot.actualMeasurementMillis()).append(",\n")
            .append("    \"sample_capacity_per_stream\": ").append(snapshot.sampleCapacity()).append(",\n")
            .append("    \"completed_session_snapshot\": \"IMMUTABLE_NO_SESSION_MONITOR\",\n")
            .append("    \"disabled_overhead\": \"UNVERIFIED_NOT_BENCHMARKED\",\n")
            .append("    \"enabled_overhead\": \"UNVERIFIED_WORKLOAD_DEPENDENT\",\n")
            .append("    \"phase\": ").append(quote(snapshot.phase().name())).append(",\n")
            .append("    \"completion_reason\": ").append(quote(snapshot.completionReason())).append("\n")
            .append("  }\n")
            .append("}\n");
        writeUtf8(path, json);
    }

    private static void writeSummary(
        Path path, Snapshot snapshot, Instant timestamp
    ) throws IOException {
        StringBuilder json = new StringBuilder(4_096);
        json.append("{\n")
            .append("  \"schema\": \"ronova-baseline-7.13-v1\",\n")
            .append("  \"timestamp_utc\": ").append(quote(timestamp.toString())).append(",\n")
            .append("  \"latency_unit\": \"us\",\n")
            .append("  \"percentile_method\": \"nearest_rank\",\n")
            .append("  \"live_status\": \"LIVE_BASELINE_PENDING\",\n")
            .append("  \"phase\": ").append(quote(snapshot.phase().name())).append(",\n")
            .append("  \"completion_reason\": ").append(quote(snapshot.completionReason())).append(",\n")
            .append("  \"tick_hook_observed\": ").append(snapshot.tickHookObserved()).append(",\n")
            .append("  \"raw_observation\": {\"enabled\": ")
            .append(snapshot.rawObservationEnabled())
            .append(", \"mode\": ").append(quote(
                snapshot.rawObservationEnabled() ? "RAW" : "LEGACY"
            )).append(", \"flag_source\": ")
            .append(quote(snapshot.rawObservationFlagSource())).append("},\n")
            .append("  \"passive_watch\": {\"mode\": ").append(quote(
                snapshot.rawObservationEnabled()
                    ? "PASSIVE_LEGACY_MIRROR" : "PASSIVE_DISABLED"
            )).append(", \"entries\": ");
        appendRange(json, snapshot.passiveWatchEntries());
        json.append(", \"notification_failures\": ")
            .append(snapshot.passiveWatchNotificationFailures()).append("},\n")
            .append("  \"tick\": ");
        appendLatency(json, snapshot.ticks().totalCount(), snapshot.ticks().dropped(),
            snapshot.ticks().elapsedUs());
        json.append(",\n");
        for (RonovaAction action : RonovaAction.values()) {
            ActionSnapshot actionSnapshot = snapshot.actions().get(action);
            json.append("  ").append(quote(action.name())).append(": ");
            appendAction(json, actionSnapshot);
            json.append(",\n");
        }
        json.append("  \"admission\": ");
        appendAdmission(json, snapshot.admission());
        json.append(",\n  \"projection_publish\": ");
        appendProjection(json, snapshot.projections());
        json.append(",\n  \"root_commit\": ");
        appendRootCommit(json, snapshot.rootCommits());
        json.append(",\n  \"tombstones\": ");
        appendRange(json, snapshot.tombstones());
        json.append(",\n  \"candidates\": ");
        appendRange(json, snapshot.candidates());
        json.append(",\n  \"topology\": ");
        appendRange(json, snapshot.topology());
        json.append(",\n  \"quarantine_objects\": ");
        appendRange(json, snapshot.quarantineObjects());
        json.append(",\n  \"retry\": ");
        appendRetry(json, snapshot.retries(), snapshot.terminalInvocations());
        json.append("\n}\n");
        writeUtf8(path, json);
    }

    private static void writeActions(Path path, Snapshot snapshot) throws IOException {
        try (BufferedWriter writer = writer(path)) {
            writer.write("action,sample,elapsed_us,candidates,judged,tombstones,topology,quarantine_objects\n");
            for (RonovaAction action : RonovaAction.values()) {
                ActionSnapshot samples = snapshot.actions().get(action);
                for (int i = 0; i < samples.elapsedUs().length; i++) {
                    writer.write(action.name());
                    writer.write(',');
                    writer.write(Integer.toString(i + 1));
                    writer.write(',');
                    writer.write(Long.toString(samples.elapsedUs()[i]));
                    writer.write(',');
                    writer.write(Integer.toString(samples.candidates()[i]));
                    writer.write(',');
                    writer.write(Integer.toString(samples.judged()[i]));
                    writer.write(',');
                    writer.write(Integer.toString(samples.tombstones()[i]));
                    writer.write(',');
                    writer.write(Integer.toString(samples.topology()[i]));
                    writer.write(',');
                    writer.write(Integer.toString(samples.quarantineObjects()[i]));
                    writer.newLine();
                }
            }
        }
    }

    private static void writeTicks(Path path, TickSnapshot samples) throws IOException {
        try (BufferedWriter writer = writer(path)) {
            writer.write("sample,elapsed_us,tombstones,topology,quarantine_objects,pending_retries\n");
            for (int i = 0; i < samples.elapsedUs().length; i++) {
                writer.write(Integer.toString(i + 1));
                writer.write(',');
                writer.write(Long.toString(samples.elapsedUs()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.tombstones()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.topology()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.quarantineObjects()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.pendingRetries()[i]));
                writer.newLine();
            }
        }
    }

    private static void writeAdmission(Path path, AdmissionSnapshot samples) throws IOException {
        try (BufferedWriter writer = writer(path)) {
            writer.write("sample,path,rejected,elapsed_us\n");
            for (int i = 0; i < samples.elapsedUs().length; i++) {
                writer.write(Integer.toString(i + 1));
                writer.write(',');
                writer.write(samples.nbt()[i] ? "NBT" : "OBJECT");
                writer.write(',');
                writer.write(Boolean.toString(samples.rejectedSamples()[i]));
                writer.write(',');
                writer.write(Long.toString(samples.elapsedUs()[i]));
                writer.newLine();
            }
        }
    }

    private static void writeProjections(Path path, ProjectionSnapshot samples)
        throws IOException {
        try (BufferedWriter writer = writer(path)) {
            writer.write("sample,elapsed_us,snapshot_tombstones,snapshot_judgments\n");
            for (int i = 0; i < samples.elapsedUs().length; i++) {
                writer.write(Integer.toString(i + 1));
                writer.write(',');
                writer.write(Long.toString(samples.elapsedUs()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.tombstones()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.judgments()[i]));
                writer.newLine();
            }
        }
    }

    private static void writeRetries(Path path, RetrySnapshot samples) throws IOException {
        try (BufferedWriter writer = writer(path)) {
            writer.write("sample,elapsed_us,queued,processed,attempts,remaining\n");
            for (int i = 0; i < samples.elapsedUs().length; i++) {
                writer.write(Integer.toString(i + 1));
                writer.write(',');
                writer.write(Long.toString(samples.elapsedUs()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.queued()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.processed()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.attempts()[i]));
                writer.write(',');
                writer.write(Integer.toString(samples.remaining()[i]));
                writer.newLine();
            }
        }
    }

    private static void writeRootCommits(Path path, RootCommitSnapshot samples)
        throws IOException {
        try (BufferedWriter writer = writer(path)) {
            writer.write("sample,elapsed_us,changed\n");
            for (int i = 0; i < samples.elapsedUs().length; i++) {
                writer.write(Integer.toString(i + 1));
                writer.write(',');
                writer.write(Long.toString(samples.elapsedUs()[i]));
                writer.write(',');
                writer.write(Boolean.toString(samples.changed()[i]));
                writer.newLine();
            }
        }
    }

    private static void writeReadme(
        Path path, Snapshot snapshot, Instant timestamp
    ) throws IOException {
        String text = """
            # Ronova 7.13 Live Baseline Run

            Status: `LIVE_BASELINE_PENDING`

            This directory is an archive generated by the opt-in 7.13 baseline tooling.
            It does not claim a live pass until the operator verifies the scenarios and
            records the environment as the intended comparison host.

            ## Window

            - Started (UTC): `%s`
            - Configured warmup: `%d seconds`
            - Configured measurement: `%d seconds`
            - Actual recorded measurement: `%d ms`
            - Sample capacity per stream: `%d`
            - Final phase: `%s`
            - Completion reason: `%s`
            - Latency unit: `us`

            WARMUP samples are excluded from every percentile and CSV file.
            The completed session is immutable before archive construction, so the archive
            snapshot does not acquire the server session monitor.

            Telemetry overhead is not separately benchmarked. Disabled-mode cost is
            `UNVERIFIED_NOT_BENCHMARKED`; enabled-mode cost is
            `UNVERIFIED_WORKLOAD_DEPENDENT` and must be interpreted with the baseline.

            ## Measurement meaning

            - `BASELINE-TICKS.csv`: real `MinecraftServer.tickServer` HEAD-to-TAIL latency.
            - `BASELINE-ACTIONS.csv`: existing `RONOVA_ACTION elapsed_us`, copied to the
              archive with candidates, judged, tombstones, topology and quarantine scale.
            - `BASELINE-ADMISSION.csv`: current 7.13 object and NBT admission path latency.
            - `BASELINE-PROJECTION.csv`: current projection build/publish cycle latency.
            - `BASELINE-ROOT-COMMITS.csv`: Root commit lane latency; projection timing is
              reported independently and remains nested where the existing lane publishes.
            - `BASELINE-RETRIES.csv`: finite retry queue observations; the retry policy is unchanged.
            - Terminal timing: `NOT_AVAILABLE_IN_7_13`; invocation count is measured.

            `RONOVA_ACTION elapsed_us` is Action latency. It must never be interpreted as
            Minecraft server tick latency. Only `BASELINE-TICKS.csv` supplies tick latency.

            ## Required live scenarios

            1. Idle: enter the world, stand/move normally, do not attack.
            2. PRIMARY: repeat normal PRIMARY Judgment Actions during measurement.
            3. AREA: repeat AREA Actions during measurement.
            4. LOADED: execute LOADED Actions during measurement.
            5. Tombstone accumulation: without changing Tombstone semantics, gradually
               increase tombstones and repeat Actions so latency remains paired with scale.

            Do not merge startup or world-load samples into the measurement window. Restart
            the game/server for a clean run if scenario control was lost during WARMUP.
            """.formatted(
                timestamp, snapshot.configuredWarmupSeconds(),
                snapshot.configuredMeasurementSeconds(), snapshot.actualMeasurementMillis(),
                snapshot.sampleCapacity(),
                snapshot.phase(), snapshot.completionReason()
            );
        writeUtf8(path, text);
    }

    private static void appendAction(StringBuilder json, ActionSnapshot snapshot) {
        json.append('{')
            .append("\"count\": ").append(snapshot.totalCount()).append(", ")
            .append("\"dropped_samples\": ").append(snapshot.dropped()).append(", ")
            .append("\"p50_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.50))).append(", ")
            .append("\"p95_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.95))).append(", ")
            .append("\"p99_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.99))).append(", ")
            .append("\"max_us\": ").append(numberOrNull(max(snapshot.elapsedUs()))).append(", ")
            .append("\"candidates\": ");
        appendAggregate(json, snapshot.candidates());
        json.append(", \"judged\": ");
        appendAggregate(json, snapshot.judged());
        json.append(", \"tombstones\": ");
        appendAggregate(json, snapshot.tombstones());
        json.append('}');
    }

    private static void appendAdmission(StringBuilder json, AdmissionSnapshot snapshot) {
        json.append('{')
            .append("\"calls\": ").append(snapshot.totalCount()).append(", ")
            .append("\"accepted\": ").append(snapshot.accepted()).append(", ")
            .append("\"rejected\": ").append(snapshot.rejected()).append(", ")
            .append("\"object_calls\": ").append(snapshot.objectCalls()).append(", ")
            .append("\"nbt_calls\": ").append(snapshot.nbtCalls()).append(", ")
            .append("\"dropped_samples\": ").append(snapshot.dropped()).append(", ")
            .append("\"p50_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.50))).append(", ")
            .append("\"p95_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.95))).append(", ")
            .append("\"p99_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.99))).append(", ")
            .append("\"max_us\": ").append(numberOrNull(max(snapshot.elapsedUs())))
            .append('}');
    }

    private static void appendProjection(StringBuilder json, ProjectionSnapshot snapshot) {
        json.append('{')
            .append("\"publish_count\": ").append(snapshot.totalCount()).append(", ")
            .append("\"dropped_samples\": ").append(snapshot.dropped()).append(", ")
            .append("\"p50_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.50))).append(", ")
            .append("\"p95_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.95))).append(", ")
            .append("\"p99_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.99))).append(", ")
            .append("\"max_us\": ").append(numberOrNull(max(snapshot.elapsedUs()))).append(", ")
            .append("\"snapshot_tombstones\": ");
        appendAggregate(json, snapshot.tombstones());
        json.append(", \"snapshot_judgments\": ");
        appendAggregate(json, snapshot.judgments());
        json.append('}');
    }

    private static void appendRootCommit(StringBuilder json, RootCommitSnapshot snapshot) {
        json.append('{')
            .append("\"count\": ").append(snapshot.totalCount()).append(", ")
            .append("\"changed\": ").append(snapshot.changedCount()).append(", ")
            .append("\"unchanged\": ").append(snapshot.totalCount() - snapshot.changedCount()).append(", ")
            .append("\"dropped_samples\": ").append(snapshot.dropped()).append(", ")
            .append("\"p50_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.50))).append(", ")
            .append("\"p95_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.95))).append(", ")
            .append("\"p99_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.99))).append(", ")
            .append("\"max_us\": ").append(numberOrNull(max(snapshot.elapsedUs())))
            .append('}');
    }

    private static void appendRetry(
        StringBuilder json, RetrySnapshot snapshot, long terminalInvocations
    ) {
        json.append('{')
            .append("\"tick_samples\": ").append(snapshot.totalCount()).append(", ")
            .append("\"processed_total\": ").append(snapshot.processedTotal()).append(", ")
            .append("\"attempts_total\": ").append(snapshot.attemptsTotal()).append(", ")
            .append("\"terminal_enforcement_invocations\": ").append(terminalInvocations).append(", ")
            .append("\"terminal_latency\": \"NOT_AVAILABLE_IN_7_13\", ")
            .append("\"dropped_samples\": ").append(snapshot.dropped()).append(", ")
            .append("\"p50_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.50))).append(", ")
            .append("\"p95_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.95))).append(", ")
            .append("\"p99_us\": ").append(numberOrNull(percentile(snapshot.elapsedUs(), 0.99))).append(", ")
            .append("\"max_us\": ").append(numberOrNull(max(snapshot.elapsedUs()))).append(", ")
            .append("\"queued\": ");
        appendAggregate(json, snapshot.queued());
        json.append(", \"processed_per_tick\": ");
        appendAggregate(json, snapshot.processed());
        json.append(", \"attempts_per_tick\": ");
        appendAggregate(json, snapshot.attempts());
        json.append(", \"remaining\": ");
        appendAggregate(json, snapshot.remaining());
        json.append('}');
    }

    private static void appendLatency(
        StringBuilder json, long count, long dropped, long[] values
    ) {
        json.append('{')
            .append("\"count\": ").append(count).append(", ")
            .append("\"dropped_samples\": ").append(dropped).append(", ")
            .append("\"p50_us\": ").append(numberOrNull(percentile(values, 0.50))).append(", ")
            .append("\"p95_us\": ").append(numberOrNull(percentile(values, 0.95))).append(", ")
            .append("\"p99_us\": ").append(numberOrNull(percentile(values, 0.99))).append(", ")
            .append("\"max_us\": ").append(numberOrNull(max(values)))
            .append('}');
    }

    private static void appendAggregate(StringBuilder json, int[] values) {
        if (values.length == 0) {
            json.append("{\"count\": 0, \"sum\": 0, \"min\": null, \"max\": null}");
            return;
        }
        long sum = 0L;
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int value : values) {
            sum += value;
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        json.append("{\"count\": ").append(values.length)
            .append(", \"sum\": ").append(sum)
            .append(", \"min\": ").append(minimum)
            .append(", \"max\": ").append(maximum).append('}');
    }

    private static void appendRange(StringBuilder json, RangeSnapshot range) {
        if (!range.available()) {
            json.append("{\"min\": null, \"max\": null}");
            return;
        }
        json.append("{\"min\": ").append(range.minimum())
            .append(", \"max\": ").append(range.maximum()).append('}');
    }

    private static Long percentile(long[] source, double percentile) {
        if (source.length == 0) {
            return null;
        }
        long[] values = source.clone();
        Arrays.sort(values);
        int index = (int) Math.ceil(percentile * values.length) - 1;
        return values[Math.max(0, Math.min(values.length - 1, index))];
    }

    private static Long max(long[] values) {
        if (values.length == 0) {
            return null;
        }
        long maximum = Long.MIN_VALUE;
        for (long value : values) {
            maximum = Math.max(maximum, value);
        }
        return maximum;
    }

    private static String numberOrNull(Long value) {
        return value == null ? "null" : Long.toString(value);
    }

    private static long actionCount(Snapshot snapshot) {
        long count = 0L;
        for (ActionSnapshot action : snapshot.actions().values()) {
            count += action.totalCount();
        }
        return count;
    }

    private static String modVersion() {
        String version = BaselineArchiveWriter.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? MOD_VERSION : version;
    }

    private static String sourceHash() {
        try {
            Path path = Paths.get(BaselineArchiveWriter.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (Files.isRegularFile(path)) {
                updateDigest(digest, path);
            } else if (Files.isDirectory(path)) {
                try (Stream<Path> paths = Files.walk(path)) {
                    for (Path file : paths.filter(Files::isRegularFile).sorted().toList()) {
                        digest.update(path.relativize(file).toString()
                            .replace('\\', '/').getBytes(StandardCharsets.UTF_8));
                        digest.update((byte) 0);
                        updateDigest(digest, file);
                    }
                }
            } else {
                return "NOT_AVAILABLE_CODE_SOURCE_MISSING";
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception ignored) {
            return "NOT_AVAILABLE_HASH_FAILED";
        }
    }

    private static void updateDigest(MessageDigest digest, Path path) throws IOException {
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
    }

    private static long physicalMemory() {
        try {
            Object bean = ManagementFactory.getOperatingSystemMXBean();
            var method = bean.getClass().getMethod("getTotalMemorySize");
            Object result = method.invoke(bean);
            return result instanceof Number number ? number.longValue() : -1L;
        } catch (Exception ignored) {
            return -1L;
        }
    }

    private static String property(String name) {
        return System.getProperty(name, "NOT_AVAILABLE");
    }

    private static String environment(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? "NOT_AVAILABLE" : value;
    }

    private static String stringArray(String[] values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                json.append(", ");
            }
            json.append(quote(values[i]));
        }
        return json.append(']').toString();
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static BufferedWriter writer(Path path) throws IOException {
        return Files.newBufferedWriter(
            path, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
    }

    private static void writeUtf8(Path path, CharSequence content) throws IOException {
        Files.writeString(
            path, content, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        );
    }
}
