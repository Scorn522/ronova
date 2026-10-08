package dev.ronova.coremod.engine;

import dev.ronova.coremod.util.WeakIdentitySet;
import java.util.UUID;

/** Standalone regression checks for the hot-path indexes; no game runtime required. */
public final class RuntimeIndexSelfTest {
    private RuntimeIndexSelfTest() {}

    public static void main(String[] args) {
        verifiesIdentityAndLineageSemantics();
        verifiesBoundedSpatialLookupAtMaximumLedgerSize();
        verifiesWeakIdentitySemantics();
        System.out.println("RuntimeIndexSelfTest: all checks passed");
    }

    private static void verifiesIdentityAndLineageSemantics() {
        LineageIndex<TestRecord> index = new LineageIndex<>();
        UUID identity = UUID.randomUUID();
        index.put(record(identity, "mob:a", "world:a", 0, 0, 0, 0, JudgmentScope.IDENTITY));
        require(index.matches(identity, "mob:b", "world:b", 500, 500, 500),
            "identity must match independently of location");

        UUID local = UUID.randomUUID();
        index.put(record(local, "mob:a", "world:a", -1, 4, -1, 64, JudgmentScope.LOCAL_LINEAGE));
        require(index.matchesDetachedLineage("mob:a", "world:a"),
            "detached proxy must inherit a non-identity local lineage");
        require(index.matches(UUID.randomUUID(), "mob:a", "world:a", 20, 4, 20),
            "local lineage must match inside its sphere");
        require(!index.matches(UUID.randomUUID(), "mob:a", "world:a", 100, 4, 100),
            "local lineage must not match outside its sphere");

        UUID globalOne = UUID.randomUUID();
        UUID globalTwo = UUID.randomUUID();
        index.put(record(globalOne, "mob:g", "world:a", 0, 0, 0, 0, JudgmentScope.GLOBAL_LINEAGE));
        index.put(record(globalTwo, "mob:g", "world:a", 0, 0, 0, 0, JudgmentScope.GLOBAL_LINEAGE));
        require(index.matches(UUID.randomUUID(), "mob:g", "world:b", 0, 0, 0),
            "global lineage must cross dimensions");
        require(index.matchesDetachedLineage("mob:g", "world:c"),
            "detached global lineage must cross dimensions");
        index.remove(globalOne);
        require(index.matches(UUID.randomUUID(), "mob:g", "world:b", 0, 0, 0),
            "removing one global record must preserve the remaining lineage");
        index.remove(globalTwo);
        require(!index.matches(UUID.randomUUID(), "mob:g", "world:b", 0, 0, 0),
            "removing the last global record must clear the lineage");

        require(!index.matches(UUID.randomUUID(), "mob:a", "world:b", -1, 4, -1),
            "local lineage must remain dimension-scoped");

        index.put(record(local, "mob:a", "world:a", 0, 0, 0, 0, JudgmentScope.IDENTITY));
        require(!index.matchesDetachedLineage("mob:a", "world:a"),
            "identity-only records must not suppress unrelated detached proxies");
        require(!index.matches(UUID.randomUUID(), "mob:a", "world:a", 20, 4, 20),
            "replacing a UUID must de-index its previous local lineage");

        UUID snapshotIdentity = UUID.randomUUID();
        index.replaceAll(java.util.List.of(record(
            snapshotIdentity, "mob:snapshot", "world:s", 1, 2, 3, 0,
            JudgmentScope.IDENTITY
        )));
        require(index.size() == 1 && index.matches(
            snapshotIdentity, "ignored", "ignored", 0, 0, 0
        ), "snapshot replacement must install the complete new projection");
        require(!index.matches(identity, "mob:g", "world:a", 0, 0, 0),
            "snapshot replacement must remove every previous identity and lineage");
    }

    private static void verifiesBoundedSpatialLookupAtMaximumLedgerSize() {
        LineageIndex<TestRecord> index = new LineageIndex<>();
        for (int value = 0; value < 65_536; value++) {
            index.put(record(
                new UUID(0L, value + 1L), "mob:stress", "world:a",
                value * 128.0D, 64.0D, 0.0D, 64.0D, JudgmentScope.LOCAL_LINEAGE
            ));
        }
        long started = System.nanoTime();
        for (int value = 0; value < 20_000; value++) {
            require(!index.matches(
                new UUID(1L, value + 1L), "mob:stress", "world:a",
                -10_000.0D, 64.0D, value % 32
            ), "far-away spatial miss must remain a miss");
        }
        long elapsedMicros = (System.nanoTime() - started) / 1_000L;
        require(index.size() == 65_536, "stress population must remain intact");
        System.out.println("RuntimeIndexSelfTest: 20k/65k spatial misses in "
            + elapsedMicros + " us");
    }

    private static void verifiesWeakIdentitySemantics() {
        WeakIdentitySet<EqualObject> set = new WeakIdentitySet<>();
        EqualObject first = new EqualObject(7);
        EqualObject second = new EqualObject(7);
        require(first.equals(second), "test objects must be value-equal");
        require(set.add(first) && set.add(second), "identity set must retain equal but distinct objects");
        require(set.size() == 2, "identity set must contain both objects");
        set.remove(first);
        require(!set.contains(first) && set.contains(second), "identity removal must be exact");
    }

    private static TestRecord record(
        UUID uuid,
        String type,
        String dimension,
        double x,
        double y,
        double z,
        double radius,
        JudgmentScope scope
    ) {
        return new TestRecord(uuid, type, dimension, x, y, z, radius, scope);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record TestRecord(
        UUID uuid,
        String type,
        String dimension,
        double x,
        double y,
        double z,
        double radius,
        JudgmentScope scope
    ) implements LineageRecord {}

    private record EqualObject(int value) {}
}
