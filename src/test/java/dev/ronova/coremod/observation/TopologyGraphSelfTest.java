package dev.ronova.coremod.observation;

import dev.ronova.coremod.budget.BudgetCategory;
import dev.ronova.coremod.budget.BudgetReceipt;
import dev.ronova.coremod.budget.FrameBudgetGovernor;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Standalone checks for the generic core used by the per-server raw graph. */
public final class TopologyGraphSelfTest {
    private TopologyGraphSelfTest() {}

    public static void main(String[] args) throws Exception {
        Object serverA = new Object();
        Object serverB = new Object();
        WeakTopologyCore<Probe, String, Object> graph = new WeakTopologyCore<>(
            serverA,
            Probe::owner,
            Probe::dimension,
            Probe::uuid,
            Probe::entityId,
            Probe::indexable
        );

        UUID shared = UUID.randomUUID();
        Probe source = new Probe(serverA, "overworld", shared, 41, true, false);
        Probe equalButDistinct = new Probe(serverA, "overworld", shared, 42, true, false);
        Probe sameId = new Probe(serverA, "overworld", UUID.randomUUID(), 41, true, false);
        Probe otherDimension = new Probe(serverA, "nether", shared, 43, true, true);
        Probe foreign = new Probe(serverB, "overworld", shared, 44, true, false);
        Probe excluded = new Probe(serverA, "overworld", UUID.randomUUID(), 45, false, false);

        require(source.equals(equalButDistinct), "test probes must be value-equal by UUID");
        require(graph.observe(source), "first identity was not indexed");
        require(!graph.observe(source), "same physical identity was indexed twice");
        require(graph.observe(equalButDistinct), "equal but distinct identity was collapsed");
        require(graph.observe(sameId), "entity-id relation probe was not indexed");
        require(graph.observe(otherDimension), "same UUID in another dimension was not indexed");
        require(!graph.observe(foreign), "foreign server object entered the graph");
        require(!graph.observe(excluded), "non-indexable object entered the graph");
        require(graph.size() == 4, "raw graph population is incorrect");

        List<Probe> overworld = graph.snapshot("overworld");
        require(overworld.size() == 3 && !containsIdentity(overworld, otherDimension),
            "level index merged dimensions");
        require(graph.snapshot("nether").size() == 1
                && containsIdentity(graph.snapshot("nether"), otherDimension),
            "dimension-specific raw snapshot is incorrect");
        require(graph.snapshot(shared).size() == 3,
            "UUID index did not retain distinct physical observations across dimensions");
        require(containsIdentity(graph.snapshot(shared), otherDimension),
            "terminal marker contaminated raw retention");

        List<Probe> related = graph.related(source);
        require(containsIdentity(related, equalButDistinct)
                && containsIdentity(related, sameId),
            "UUID/entity-id relation buckets were not combined");
        require(!containsIdentity(related, source)
                && !containsIdentity(related, otherDimension),
            "related lookup returned source or a different dimension");

        verifiesBoundedStaleMaintenance(serverA);
        verifiesSparseStaleMaintenance(serverA);
        verifiesMaintenanceHasNoFullIndexTraversal();

        equalButDistinct.dimension = "nether";
        require(graph.observe(equalButDistinct), "changed lookup keys were not reindexed");
        require(!containsIdentity(graph.snapshot("overworld"), equalButDistinct)
                && containsIdentity(graph.snapshot("nether"), equalButDistinct),
            "level-key migration left a stale bucket");

        Probe stale = new Probe(serverA, "overworld", UUID.randomUUID(), 99, true, false);
        WeakReference<Probe> witness = new WeakReference<>(stale);
        require(graph.observe(stale), "stale-reference probe was not indexed");
        require(graph.simulateCollectionForTest(stale), "test seam could not enqueue stale ref");
        require(witness.get() == stale && graph.size() == 4,
            "queued weak reference was not purged independently of caller ownership");

        boolean immutable = false;
        try {
            graph.snapshot("overworld").clear();
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        require(immutable, "raw snapshots must be immutable");

        graph.clear();
        require(graph.size() == 0, "clear did not isolate the next server lifetime");
        System.out.println("TopologyGraphSelfTest: PASS");
    }

    private static void verifiesBoundedStaleMaintenance(Object owner) {
        WeakTopologyCore<Probe, String, Object> graph = new WeakTopologyCore<>(
            owner,
            Probe::owner,
            Probe::dimension,
            Probe::uuid,
            Probe::entityId,
            Probe::indexable
        );
        List<Probe> stale = new ArrayList<>(300);
        for (int index = 0; index < 300; index++) {
            Probe probe = new Probe(
                owner,
                "overworld",
                new UUID(0L, 1_000L + index),
                1_000 + index,
                true,
                false
            );
            require(graph.observe(probe), "bounded-maintenance probe was not indexed");
            stale.add(probe);
        }
        for (Probe probe : stale) {
            require(graph.enqueueCollectionForTest(probe),
                "bounded-maintenance probe could not be queued");
        }

        FrameBudgetGovernor governor = new FrameBudgetGovernor(false);
        governor.beginTick(1L);
        var firstBudget = governor.budget(BudgetCategory.OBSERVATION);
        WeakTopologyCore.StaleDrain firstDrain = graph.maintainStale(firstBudget);
        BudgetReceipt first = firstBudget.finish(
            firstDrain.processed(), firstDrain.deferred(),
            firstDrain.carryIn(), firstDrain.carryOut()
        );
        require(first.attempted() == 256L && first.processed() == 256L
                && first.deferred() == 0L && first.carryIn() == 257L
                && first.carryOut() == 1L && graph.size() == 44,
            "stale maintenance exceeded its bounded discovery window or lost carry work");

        governor.beginTick(2L);
        var secondBudget = governor.budget(BudgetCategory.OBSERVATION);
        WeakTopologyCore.StaleDrain secondDrain = graph.maintainStale(secondBudget);
        BudgetReceipt second = secondBudget.finish(
            secondDrain.processed(), secondDrain.deferred(),
            secondDrain.carryIn(), secondDrain.carryOut()
        );
        require(second.attempted() == 44L && second.processed() == 44L
                && second.deferred() == 0L && second.carryIn() == 44L
                && second.carryOut() == 0L && graph.size() == 0,
            "stale maintenance did not resume and finish the carried queue");
    }

    private static void verifiesSparseStaleMaintenance(Object owner) {
        final int liveCount = 200_000;
        WeakTopologyCore<Probe, String, Object> graph = new WeakTopologyCore<>(
            owner,
            Probe::owner,
            Probe::dimension,
            Probe::uuid,
            Probe::entityId,
            Probe::indexable
        );
        UUID shared = UUID.fromString("00000000-0000-0000-0000-000000000714");
        List<Probe> live = new ArrayList<>(liveCount + 3);
        for (int index = 0; index < liveCount + 3; index++) {
            Probe probe = new Probe(
                owner, "overworld", shared, 0, true, false
            );
            require(graph.observe(probe),
                "sparse-stale live reference was not indexed: " + index);
            live.add(probe);
        }
        for (int index = liveCount; index < live.size(); index++) {
            require(graph.enqueueCollectionForTest(live.get(index)),
                "sparse-stale reference could not be queued");
        }

        Probe.resetAccessorCalls();
        FrameBudgetGovernor governor = new FrameBudgetGovernor(false);
        governor.beginTick(714L);
        var budget = governor.budget(BudgetCategory.OBSERVATION);
        WeakTopologyCore.StaleDrain drain = graph.maintainStale(budget);
        BudgetReceipt receipt = budget.finish(
            drain.processed(), drain.deferred(), drain.carryIn(), drain.carryOut()
        );
        require(receipt.processed() == 3L && receipt.deferred() == 0L
                && receipt.carryIn() == 3L && receipt.carryOut() == 0L,
            "sparse stale maintenance did not process only discovered stale work");
        require(graph.size() == liveCount,
            "sparse stale maintenance removed live references or retained stale ones");
        require(Probe.accessorCalls() == 0L,
            "maintenance revisited live objects instead of using queued stale references");
        graph.clear();
    }

    private static void verifiesMaintenanceHasNoFullIndexTraversal() throws Exception {
        String internalName = "dev/ronova/coremod/observation/WeakTopologyCore";
        ClassNode node = new ClassNode();
        try (InputStream input = TopologyGraphSelfTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            require(input != null, "missing WeakTopologyCore.class");
            new ClassReader(input).accept(node, 0);
        }

        Map<String, MethodNode> methods = new HashMap<>();
        for (MethodNode method : node.methods) {
            methods.put(method.name + method.desc, method);
        }
        ArrayDeque<MethodNode> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        for (MethodNode method : node.methods) {
            if ("maintainStale".equals(method.name)) {
                pending.addLast(method);
            }
        }
        require(!pending.isEmpty(), "missing bounded stale maintenance method");

        while (!pending.isEmpty()) {
            MethodNode method = pending.removeFirst();
            String methodKey = method.name + method.desc;
            if (!visited.add(methodKey)) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)) {
                    continue;
                }
                boolean bulkIndexTraversal = (
                    "java/util/Map".equals(call.owner)
                        || "java/util/HashMap".equals(call.owner)
                ) && Set.of("keySet", "values", "entrySet", "forEach")
                    .contains(call.name);
                require(!bulkIndexTraversal,
                    "stale maintenance reaches full index traversal via "
                        + call.owner + "." + call.name + " in " + method.name);
                if (internalName.equals(call.owner)) {
                    MethodNode target = methods.get(call.name + call.desc);
                    if (target != null) {
                        pending.addLast(target);
                    }
                }
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static boolean containsIdentity(List<Probe> values, Probe expected) {
        for (Probe value : values) {
            if (value == expected) {
                return true;
            }
        }
        return false;
    }

    private static final class Probe {
        private static long accessorCalls;

        private final Object owner;
        private String dimension;
        private final UUID uuid;
        private final int entityId;
        private final boolean indexable;
        @SuppressWarnings("unused")
        private final boolean terminal;

        private Probe(
            Object owner,
            String dimension,
            UUID uuid,
            int entityId,
            boolean indexable,
            boolean terminal
        ) {
            this.owner = owner;
            this.dimension = dimension;
            this.uuid = uuid;
            this.entityId = entityId;
            this.indexable = indexable;
            this.terminal = terminal;
        }

        static void resetAccessorCalls() { accessorCalls = 0L; }

        static long accessorCalls() { return accessorCalls; }

        Object owner() {
            accessorCalls++;
            return owner;
        }

        String dimension() {
            accessorCalls++;
            return dimension;
        }

        UUID uuid() {
            accessorCalls++;
            return uuid;
        }

        int entityId() {
            accessorCalls++;
            return entityId;
        }

        boolean indexable() {
            accessorCalls++;
            return indexable;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Probe probe && uuid.equals(probe.uuid);
        }

        @Override
        public int hashCode() {
            return uuid.hashCode();
        }
    }
}
