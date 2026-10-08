package dev.ronova.coremod.budget;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class HostResourceGovernorSelfTest {
    public static void main(String[] args) throws Exception {
        var budgets = new EnumMap<HostResourceDomain, HostResourceGovernor.Budget>(HostResourceDomain.class);
        for (var domain : HostResourceDomain.values()) budgets.put(domain, new HostResourceGovernor.Budget(60, 100, 20));
        var governor = new HostResourceGovernor(budgets);
        require(governor.snapshot().size() == 15 && governor.snapshot().stream().allMatch(row -> row.ownedUsage() == null),
            "unobserved resources were reported as zero owned usage");
        var unknown = governor.reserve(Map.of(HostResourceDomain.JNI_JNA_NATIVE, 1L), HostResourceGovernor.Priority.NORMAL);
        require(!unknown.granted() && unknown.denial() == HostResourceGovernor.Denial.ACCOUNTING_UNAVAILABLE, "unregistered allocator bypassed accounting");
        governor.declareAccounting(HostResourceDomain.JAVA_HEAP, HostResourceGovernor.Accounting.PARTIAL_EXACT, "test-counter");
        var normal = governor.reserve(Map.of(HostResourceDomain.JAVA_HEAP, 60L), HostResourceGovernor.Priority.NORMAL);
        require(normal.granted(), "soft budget unavailable");
        require(!governor.reserve(Map.of(HostResourceDomain.JAVA_HEAP, 1L), HostResourceGovernor.Priority.NORMAL).granted(), "normal work consumed recovery space");
        var recovery = governor.reserve(Map.of(HostResourceDomain.JAVA_HEAP, 40L), HostResourceGovernor.Priority.RECOVERY);
        require(recovery.granted() && !governor.reserve(Map.of(HostResourceDomain.JAVA_HEAP, 1L), HostResourceGovernor.Priority.RECOVERY).granted(),
            "recovery reserve unavailable or hard cap bypassed");
        normal.reservation().close(); normal.reservation().close(); recovery.reservation().close();
        require(row(governor).ownedUsage() == 0 && row(governor).ownedPeak() == 100 && row(governor).releasedTotal() == 100,
            "reservation lifecycle leaked or double released");
        var atomic = governor.reserve(Map.of(HostResourceDomain.JAVA_HEAP, 30L, HostResourceDomain.THREAD, 1L), HostResourceGovernor.Priority.NORMAL);
        require(!atomic.granted() && row(governor).ownedUsage() == 0, "multi-domain rejection left a partial charge");
        governor.observeHost(HostResourceDomain.JAVA_HEAP, new HostResourceGovernor.HostSample(95L, 100L, 1));
        require(governor.reserve(Map.of(HostResourceDomain.JAVA_HEAP, 1L), HostResourceGovernor.Priority.NORMAL).denial()
            == HostResourceGovernor.Denial.HOST_PRESSURE && row(governor).ownedUsage() == 0, "host pressure was attributed as owned usage");
        recovery = governor.reserve(Map.of(HostResourceDomain.JAVA_HEAP, 1L), HostResourceGovernor.Priority.RECOVERY);
        require(recovery.granted(), "host pressure suppressed bounded recovery"); recovery.reservation().close();
        governor.observeHost(HostResourceDomain.JAVA_HEAP, new HostResourceGovernor.HostSample(20L, 100L, 2));
        var pool = Executors.newFixedThreadPool(8); var requests = new ArrayList<Future<?>>(); AtomicInteger successes = new AtomicInteger();
        try {
            for (int i = 0; i < 8; i++) requests.add(pool.submit(() -> {
                for (int n = 0; n < 1000; n++) {
                    var r = governor.reserve(Map.of(HostResourceDomain.JAVA_HEAP, 1L), HostResourceGovernor.Priority.NORMAL);
                    if (r.granted()) { successes.incrementAndGet(); r.reservation().close(); }
                }
            }));
            for (var future : requests) future.get(10, TimeUnit.SECONDS);
        } finally { pool.shutdownNow(); }
        require(successes.get() > 0 && row(governor).ownedUsage() == 0 && row(governor).ownedPeak() <= 100, "concurrent quota/release violated hard bounds");
        JvmResourceSampler.sample(governor, System.nanoTime());
        require(row(governor).host().usage() != null && governor.snapshot().stream()
            .filter(r -> r.domain() == HostResourceDomain.GPU_UPLOAD).allMatch(r -> r.host().usage() == null),
            "JMX sampler fabricated unavailable GPU usage");
        System.out.println("HostResourceGovernorSelfTest: PASS (15 domains, unknown attribution, atomic quotas, reserve, host pressure, release/race, JMX)");
    }
    private static HostResourceGovernor.Row row(HostResourceGovernor governor) {
        return governor.snapshot().stream().filter(r -> r.domain() == HostResourceDomain.JAVA_HEAP).findFirst().orElseThrow();
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
