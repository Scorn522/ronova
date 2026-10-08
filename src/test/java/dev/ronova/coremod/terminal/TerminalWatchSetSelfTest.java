package dev.ronova.coremod.terminal;

import dev.ronova.coremod.engine.JudgmentEngine;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** 7.14 passive compatibility plus 7.16 durable terminal takeover gates. */
public final class TerminalWatchSetSelfTest {
    private static final String JUDGMENT = "dev/ronova/coremod/engine/JudgmentEngine";
    private static final String EXECUTOR = "dev/ronova/coremod/engine/JudgmentExecutor";
    private static final String WATCH = "dev/ronova/coremod/terminal/TerminalWatchSet";
    private static final String DURABLE =
        "dev/ronova/coremod/terminal/DurableTerminalWatchSet";
    private static final String FIREWALL = "dev/ronova/coremod/engine/SpawnFirewall";
    private static final String TERMINAL = "dev/ronova/coremod/engine/TerminalLifecycle";
    private static final String ADMISSION =
        "dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime";
    private static final String COORDINATOR =
        "dev/ronova/coremod/runtime/RuntimeCoordinator";
    private static final String COMPOSITION =
        "dev/ronova/coremod/bootstrap/DominionCompositionRoot";
    private static final ResourceKey<Level> TEST_OVERWORLD = testDimension("overworld");
    private static final ResourceKey<Level> TEST_NETHER = testDimension("nether");

    private TerminalWatchSetSelfTest() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ResourceKey<Level> testDimension(String path) {
        try {
            Constructor<ResourceKey> constructor = ResourceKey.class.getDeclaredConstructor(
                ResourceLocation.class, ResourceLocation.class
            );
            constructor.setAccessible(true);
            return (ResourceKey<Level>) constructor.newInstance(
                ResourceLocation.fromNamespaceAndPath("ronova_test", "dimension"),
                ResourceLocation.fromNamespaceAndPath("ronova_test", path)
            );
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static void main(String[] args) throws Exception {
        verifiesPassiveSnapshotsAndDimensionKeys();
        verifiesBoundedCompletionHistory();
        verifiesIndependentClear();
        verifiesStatelessNoop();
        verifiesDurableTakeover();
        System.out.println(
            "TerminalWatchSetSelfTest: PASS "
                + "(legacy diagnostics + durable tombstone worker takeover)"
        );
    }

    private static void verifiesPassiveSnapshotsAndDimensionKeys() {
        UUID identity = UUID.randomUUID();
        TerminalWatchSet.LegacyRetryKey overworld = new TerminalWatchSet.LegacyRetryKey(
            identity, TEST_OVERWORLD
        );
        TerminalWatchSet.LegacyRetryKey nether = new TerminalWatchSet.LegacyRetryKey(
            identity, TEST_NETHER
        );

        PassiveTerminalWatchSet watch = new PassiveTerminalWatchSet();
        watch.observeScheduled(overworld, 0, 101L);
        watch.observeAttempt(overworld, 0, true, true);
        watch.observeScheduled(overworld, 1, 103L);
        watch.observeCompleted(overworld, TerminalWatchSet.Completion.LEGACY_ATTEMPTS_EXHAUSTED);
        watch.observeScheduled(nether, 0, 201L);
        watch.observeAttempt(nether, 0, false, false);
        watch.observeCompleted(nether, TerminalWatchSet.Completion.ENTITY_UNRESOLVED);

        TerminalWatchSet.WatchSnapshot snapshot = watch.snapshot(identity);
        TerminalWatchMetrics metrics = watch.metrics();
        require(snapshot.mode() == TerminalWatchSet.Mode.PASSIVE_LEGACY_MIRROR,
            "legacy diagnostic mode is mislabeled");
        require(snapshot.size() == 2 && watch.size() == 0
                && metrics.activeEntries() == 0 && metrics.retainedDiagnostics() == 2,
            "completed diagnostics stayed active or dimensions were merged");
        require(metrics.scheduledNotifications() == 3L
                && metrics.attemptNotifications() == 2L
                && metrics.completionNotifications() == 2L,
            "legacy diagnostic totals are incorrect");
        TerminalWatchSet.EntrySnapshot overworldEntry = snapshot.entries().get(overworld);
        require(overworldEntry.schedules().size() == 2
                && overworldEntry.schedules().get(1).nextTick() == 103L,
            "legacy schedule facts were not preserved");
        require(overworldEntry.attempts().get(0).entityResolved()
                && overworldEntry.attempts().get(0).reasserted(),
            "legacy attempt facts were not preserved");
        require(snapshot.entries().get(nether).completions().equals(
            List.of(TerminalWatchSet.Completion.ENTITY_UNRESOLVED)
        ), "completion facts were not dimension-specific");
        assertImmutable(snapshot.entries(), overworldEntry);

        watch.observeScheduled(overworld, 0, 301L);
        TerminalWatchSet.WatchSnapshot reopened = watch.snapshot(identity);
        require(watch.size() == 1 && watch.metrics().activeEntries() == 1
                && watch.metrics().retainedDiagnostics() == 1,
            "a reopened legacy diagnostic did not replace completed history");
        require(reopened.entries().get(overworld).completions().isEmpty()
                && reopened.entries().get(overworld).schedules().size() == 1
                && reopened.entries().get(nether).completions().size() == 1,
            "targeted legacy snapshot mixed unrelated state");
        require(watch.snapshot(UUID.randomUUID()).size() == 0,
            "targeted snapshot returned an unrelated UUID");
    }

    private static void verifiesBoundedCompletionHistory() {
        PassiveTerminalWatchSet watch = new PassiveTerminalWatchSet();
        UUID first = new UUID(0L, 1L);
        UUID latest = null;
        int total = PassiveTerminalWatchSet.MAX_DIAGNOSTIC_HISTORY + 17;
        for (int index = 0; index < total; index++) {
            latest = new UUID(0L, index + 1L);
            TerminalWatchSet.LegacyRetryKey key = new TerminalWatchSet.LegacyRetryKey(
                latest, TEST_OVERWORLD
            );
            watch.observeScheduled(key, 0, index + 1L);
            watch.observeCompleted(
                key, TerminalWatchSet.Completion.LEGACY_ATTEMPTS_EXHAUSTED
            );
        }
        TerminalWatchMetrics metrics = watch.metrics();
        require(metrics.activeEntries() == 0 && watch.size() == 0,
            "completed diagnostic entries remained active");
        require(metrics.retainedDiagnostics() == PassiveTerminalWatchSet.MAX_DIAGNOSTIC_HISTORY,
            "completed diagnostic history is not bounded");
        require(watch.snapshot(first).size() == 0,
            "oldest completed diagnostic was not evicted");
        require(watch.snapshot(latest).size() == 1,
            "latest completed diagnostic was not retained");
    }

    private static void assertImmutable(
        Map<TerminalWatchSet.LegacyRetryKey, TerminalWatchSet.EntrySnapshot> entries,
        TerminalWatchSet.EntrySnapshot entry
    ) {
        boolean mapImmutable = false;
        try {
            entries.clear();
        } catch (UnsupportedOperationException expected) {
            mapImmutable = true;
        }
        boolean listImmutable = false;
        try {
            entry.schedules().clear();
        } catch (UnsupportedOperationException expected) {
            listImmutable = true;
        }
        require(mapImmutable && listImmutable, "watch snapshots must be deeply immutable");
    }

    private static void verifiesIndependentClear() {
        TerminalWatchSet.LegacyRetryKey key = new TerminalWatchSet.LegacyRetryKey(
            UUID.randomUUID(), TEST_OVERWORLD
        );
        PassiveTerminalWatchSet serverA = new PassiveTerminalWatchSet();
        PassiveTerminalWatchSet serverB = new PassiveTerminalWatchSet();
        serverA.observeScheduled(key, 0, 1L);
        serverB.observeScheduled(key, 0, 1L);
        serverA.clear();
        require(serverA.snapshot(key.uuid()).size() == 0
                && serverB.snapshot(key.uuid()).size() == 1,
            "clearing one server diagnostic changed another server");
    }

    private static void verifiesStatelessNoop() {
        TerminalWatchSet.LegacyRetryKey key = new TerminalWatchSet.LegacyRetryKey(
            UUID.randomUUID(), TEST_OVERWORLD
        );
        NoopTerminalWatchSet noop = NoopTerminalWatchSet.INSTANCE;
        noop.observeScheduled(key, 0, 1L);
        noop.observeAttempt(key, 0, true, true);
        noop.observeCompleted(key, TerminalWatchSet.Completion.SERVER_RELEASED);
        noop.clear();
        TerminalWatchSet.WatchSnapshot snapshot = noop.snapshot(key.uuid());
        TerminalWatchMetrics metrics = noop.metrics();
        require(snapshot.mode() == TerminalWatchSet.Mode.PASSIVE_DISABLED
                && snapshot.size() == 0 && noop.size() == 0
                && metrics.activeEntries() == 0
                && metrics.scheduledNotifications() == 0L,
            "disabled watch retained state or used the wrong label");
        for (Field field : NoopTerminalWatchSet.class.getDeclaredFields()) {
            require(Modifier.isStatic(field.getModifiers()),
                "no-op watch has mutable instance state: " + field.getName());
        }
    }

    private static void verifiesDurableTakeover() throws Exception {
        require(TerminalWatchSet.Mode.valueOf("DURABLE_TOMBSTONE")
                == TerminalWatchSet.Mode.DURABLE_TOMBSTONE,
            "durable watch mode is missing");
        for (String required : List.of(
            "track", "observeIncarnation", "maintain", "clearIdentity"
        )) {
            boolean found = false;
            for (Method method : TerminalWatchSet.class.getDeclaredMethods()) {
                found |= required.equals(method.getName());
            }
            require(found, "durable watch API is missing " + required);
        }
        for (Field field : JudgmentEngine.class.getDeclaredFields()) {
            require(!field.getName().startsWith("RETRY_")
                    && !field.getName().contains("PENDING_RETRIES"),
                "finite retry authority survived cutover: " + field.getName());
        }
        for (Method method : JudgmentEngine.class.getDeclaredMethods()) {
            require(!method.getName().equals("tickInternal")
                    && !method.getName().equals("scheduleRetry")
                    && !method.getName().equals("isolatePassiveWatchNotification"),
                "legacy retry method survived cutover: " + method.getName());
        }

        ClassNode judgment = read(JUDGMENT);
        require(countCalls(judgment, WATCH, "observeScheduled") == 0
                && countCalls(judgment, WATCH, "observeAttempt") == 0
                && countCalls(judgment, WATCH, "observeCompleted") == 0,
            "JudgmentEngine still drives the finite retry mirror");
        ClassNode executor = read(EXECUTOR);
        require(countCalls(executor, WATCH, "track") >= 1,
            "committed judgment is not registered with durable terminal watch");
        ClassNode admission = read(ADMISSION);
        require(countCalls(admission, WATCH, "observeIncarnation") >= 1
                && countCalls(admission, WATCH, "clearIdentity") >= 1,
            "Admission does not promote reinsertion or release exact tombstones");
        require(countCalls(admission, TERMINAL, "completeCommitted") >= 2,
            "Admission recovery can mark a committed entity without completing "
                + "its once-only lifecycle integration");
        ClassNode coordinator = read(COORDINATOR);
        require(countCalls(coordinator, WATCH, "maintain") == 1
                && countCalls(coordinator, JUDGMENT, "tick") == 0,
            "runtime maintenance did not transfer to TerminalWatchSet");
        ClassNode composition = read(COMPOSITION);
        require(countCalls(composition, DURABLE, "<init>") == 1,
            "composition root does not install exactly one durable watch");
        ClassNode durable = read(DURABLE);
        require(countCalls(durable, FIREWALL, "reassert") >= 1,
            "durable worker never reasserts a rediscovered incarnation");
        require(countCalls(durable, "java/util/Map", "clear") >= 1,
            "server release does not clear durable watch state");
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = TerminalWatchSetSelfTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            require(input != null, "missing production class: " + internalName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static int countCalls(ClassNode node, String owner, String name) {
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && owner.equals(call.owner) && name.equals(call.name)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
