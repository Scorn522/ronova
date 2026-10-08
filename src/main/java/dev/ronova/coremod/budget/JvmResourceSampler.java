package dev.ronova.coremod.budget;

import java.lang.management.*;
import java.util.*;

/** Public JMX measurements only. Unavailable limits and uninstrumented native/GPU usage remain unknown. */
public final class JvmResourceSampler {
    private JvmResourceSampler() {}
    public static void sample(HostResourceGovernor governor, long now) {
        EnumMap<HostResourceDomain, HostResourceGovernor.HostSample> samples = new EnumMap<>(HostResourceDomain.class);
        for (var domain : HostResourceDomain.values()) samples.put(domain, new HostResourceGovernor.HostSample(null, null, now));
        try {
            MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            samples.put(HostResourceDomain.JAVA_HEAP, usage(heap, now));
            long codeUsed = 0, codeMax = 0; boolean codeSeen = false, codeBounded = true;
            for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
                if (pool.getName().equals("Metaspace")) samples.put(HostResourceDomain.METASPACE, usage(pool.getUsage(), now));
                if (pool.getName().contains("CodeHeap") || pool.getName().equals("Code Cache")) {
                    MemoryUsage value = pool.getUsage(); if (value == null) continue;
                    codeSeen = true; codeUsed = Math.addExact(codeUsed, value.getUsed());
                    if (value.getMax() <= 0) codeBounded = false; else codeMax = Math.addExact(codeMax, value.getMax());
                }
            }
            if (codeSeen) samples.put(HostResourceDomain.CODE_CACHE,
                new HostResourceGovernor.HostSample(codeUsed, codeBounded && codeMax > 0 ? codeMax : null, now));
        } catch (RuntimeException unavailable) { /* Unmeasured fields stay unknown. */ }
        try {
            for (BufferPoolMXBean pool : ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)) {
                HostResourceDomain domain = pool.getName().equals("direct") ? HostResourceDomain.DIRECT_BUFFER
                    : pool.getName().equals("mapped") ? HostResourceDomain.MAPPED_BUFFER : null;
                if (domain != null) samples.put(domain, new HostResourceGovernor.HostSample(
                    pool.getMemoryUsed() < 0 ? null : pool.getMemoryUsed(), null, now));
            }
        } catch (RuntimeException unavailable) { }
        try { samples.put(HostResourceDomain.THREAD, new HostResourceGovernor.HostSample((long) ManagementFactory.getThreadMXBean().getThreadCount(), null, now)); }
        catch (RuntimeException unavailable) { }
        try { samples.put(HostResourceDomain.CLASS, new HostResourceGovernor.HostSample((long) ManagementFactory.getClassLoadingMXBean().getLoadedClassCount(), null, now)); }
        catch (RuntimeException unavailable) { }
        samples.forEach(governor::observeHost);
    }
    private static HostResourceGovernor.HostSample usage(MemoryUsage value, long now) {
        return value == null ? new HostResourceGovernor.HostSample(null, null, now)
            : new HostResourceGovernor.HostSample(value.getUsed(), value.getMax() > 0 ? value.getMax() : null, now);
    }
}
