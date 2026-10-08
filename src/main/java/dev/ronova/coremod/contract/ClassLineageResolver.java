package dev.ronova.coremod.contract;

import dev.ronova.coremod.budget.*;
import dev.ronova.coremod.engine.JudgmentMark;
import dev.ronova.coremod.engine.JudgmentScope;
import dev.ronova.coremod.root.api.SubjectQualification;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Predicate;

/** A class name is only a hint; actual Class identity and assignability establish JVM lineage. */
public final class ClassLineageResolver {
    public record Match(SubjectKey anchor, long bindingRevision, String provenance) {}
    private record Origin(SubjectQualification qualification, JudgmentMark mark, WeakReference<Class<?>> type,
                          HostResourceGovernor.Reservation reservation) {}
    private final LinkedHashMap<SubjectKey, Origin> origins = new LinkedHashMap<>();
    private final int limit;
    private final HostResourceGovernor resources;
    private long unresolvedRegistrations;
    public ClassLineageResolver(int limit, HostResourceGovernor resources) {
        if (limit < 1 || limit > 4096) throw new IllegalArgumentException("invalid origin observation budget");
        this.limit = limit; this.resources = resources;
        if (resources != null) resources.declareAccounting(HostResourceDomain.JAVA_HEAP,
            HostResourceGovernor.Accounting.PARTIAL_ESTIMATE, "class-lineage-origin/1024-bytes");
    }
    public synchronized boolean remember(SubjectQualification qualification, JudgmentMark mark, Class<?> observed,
                                          Predicate<SubjectQualification> current) {
        Objects.requireNonNull(qualification); Objects.requireNonNull(mark); Objects.requireNonNull(observed); Objects.requireNonNull(current);
        if (!qualification.terminal() || qualification.revisions() == null || qualification.revisions().activeIncarnation() == null
            || !qualification.subjectKey().subjectId().equals(mark.uuid()) || mark.scope() == JudgmentScope.IDENTITY
            || !current.test(qualification)) return false;
        origins.entrySet().removeIf(entry -> {
            var origin = entry.getValue();
            if (origin.type().get() != null && current.test(origin.qualification())) return false;
            release(origin); return true;
        });
        var previous = origins.get(qualification.subjectKey());
        if (previous != null) {
            if (!previous.qualification().equals(qualification) || previous.type().get() != observed) return false;
            origins.put(qualification.subjectKey(), new Origin(qualification, mark, previous.type(), previous.reservation())); return true;
        }
        if (origins.size() >= limit) { unresolvedRegistrations++; return false; }
        HostResourceGovernor.Reservation reservation = null;
        if (resources != null) {
            var allocated = resources.reserve(Map.of(HostResourceDomain.JAVA_HEAP, 1024L), HostResourceGovernor.Priority.RECOVERY);
            if (!allocated.granted()) { unresolvedRegistrations++; return false; }
            reservation = allocated.reservation();
        }
        origins.put(qualification.subjectKey(), new Origin(qualification, mark, new WeakReference<>(observed), reservation)); return true;
    }
    public synchronized Match resolve(Class<?> candidate, SubjectIdentity subject, Predicate<SubjectQualification> current) {
        if (candidate == null || subject == null) return null;
        for (var origin : origins.values()) {
            Class<?> source = origin.type().get();
            if (source == null || !source.isAssignableFrom(candidate) || !current.test(origin.qualification())) continue;
            var mark = origin.mark();
            if (mark.scope() == JudgmentScope.LOCAL_LINEAGE) {
                double dx = subject.x() - mark.x(), dy = subject.y() - mark.y(), dz = subject.z() - mark.z();
                double radius = Math.max(0, mark.radius());
                if (!mark.dimension().equals(subject.dimension()) || !Double.isFinite(dx + dy + dz)
                    || Math.hypot(Math.hypot(dx, dy), dz) > radius) continue;
            }
            return new Match(origin.qualification().subjectKey(), origin.qualification().targetEpoch().generation(),
                "root-bound-class-identity/assignability/v1");
        }
        return null;
    }
    public synchronized long unresolvedRegistrations() { return unresolvedRegistrations; }
    public synchronized void forget(UUID subjectId) {
        origins.entrySet().removeIf(entry -> { if (!entry.getKey().subjectId().equals(subjectId)) return false; release(entry.getValue()); return true; });
    }
    public synchronized void clear() { origins.values().forEach(ClassLineageResolver::release); origins.clear(); unresolvedRegistrations = 0; }
    private static void release(Origin origin) { if (origin.reservation() != null) origin.reservation().close(); }
}
