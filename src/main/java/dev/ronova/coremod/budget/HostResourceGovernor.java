package dev.ronova.coremod.budget;

import java.util.*;

/** Accounting only for registered allocations. It never removes Truth or infers opponent ownership. */
public final class HostResourceGovernor {
    public enum Priority { NORMAL, RECOVERY }
    public enum Accounting { UNOBSERVED, PARTIAL_ESTIMATE, PARTIAL_EXACT }
    public enum Denial { NONE, ACCOUNTING_UNAVAILABLE, SOFT_LIMIT, HARD_LIMIT, HOST_PRESSURE }
    public record Budget(long soft, long hard, long recoveryReserve) {
        public Budget {
            if (soft < 0 || hard <= 0 || recoveryReserve < 0 || recoveryReserve > hard || soft > hard - recoveryReserve)
                throw new IllegalArgumentException("invalid resource budget");
        }
    }
    public record HostSample(Long usage, Long limit, long sampledAtNanos) {
        public HostSample {
            if (usage != null && usage < 0 || limit != null && limit <= 0) throw new IllegalArgumentException("invalid host sample");
        }
        public Double pressure() { return usage == null || limit == null ? null : Math.min(1D, (double) usage / limit); }
    }
    public record Row(HostResourceDomain domain, Accounting accounting, Long ownedUsage, Long ownedPeak,
        Long allocatedTotal, Long releasedTotal, Double allocationRatePerSecond, long lastReleaseRevision, Budget budget,
        HostSample host, Denial throttleState, boolean recoveryHeadroomInUse, Denial lastDenial, long denied, Set<String> accountingSources) {}
    public record Request(Reservation reservation, Denial denial, HostResourceDomain domain) {
        public boolean granted() { return reservation != null; }
    }
    public final class Reservation implements AutoCloseable {
        private final Map<HostResourceDomain, Long> charges;
        private boolean closed;
        private Reservation(Map<HostResourceDomain, Long> charges) { this.charges = Map.copyOf(charges); }
        @Override public void close() {
            synchronized (HostResourceGovernor.this) {
                if (closed) return;
                closed = true;
                for (var entry : charges.entrySet()) {
                    Cell cell = cells.get(entry.getKey()); cell.used -= entry.getValue();
                    cell.released = saturatedAdd(cell.released, entry.getValue()); cell.lastRelease = advanceRevision();
                }
            }
        }
    }
    private static final class Cell {
        final Budget budget;
        Accounting accounting = Accounting.UNOBSERVED;
        final Set<String> sources = new TreeSet<>();
        long used, peak, allocated, released, lastRelease, denied;
        HostSample host = new HostSample(null, null, 0);
        Denial lastDenial = Denial.NONE;
        Cell(Budget budget) { this.budget = budget; }
    }
    private final EnumMap<HostResourceDomain, Cell> cells = new EnumMap<>(HostResourceDomain.class);
    private long revision;
    private final long startedAt = System.nanoTime();
    public HostResourceGovernor() { this(defaultBudgets()); }
    public HostResourceGovernor(Map<HostResourceDomain, Budget> budgets) {
        for (HostResourceDomain domain : HostResourceDomain.values()) cells.put(domain,
            new Cell(Objects.requireNonNull(budgets.get(domain), "missing resource budget " + domain)));
    }
    public synchronized void declareAccounting(HostResourceDomain domain, Accounting accounting, String source) {
        Objects.requireNonNull(accounting); Objects.requireNonNull(source);
        if (source.isBlank() || source.length() > 128 || accounting == Accounting.UNOBSERVED) throw new IllegalArgumentException("invalid accounting source");
        Cell cell = cells.get(Objects.requireNonNull(domain));
        if (!cell.sources.contains(source) && cell.sources.size() >= 32) throw new IllegalStateException("accounting source limit");
        cell.sources.add(source);
        if (cell.accounting == Accounting.UNOBSERVED || accounting == Accounting.PARTIAL_ESTIMATE) cell.accounting = accounting;
        advanceRevision();
    }
    public synchronized void observeHost(HostResourceDomain domain, HostSample sample) {
        cells.get(Objects.requireNonNull(domain)).host = Objects.requireNonNull(sample); advanceRevision();
    }
    public synchronized Request reserve(Map<HostResourceDomain, Long> requested, Priority priority) {
        Objects.requireNonNull(priority); Objects.requireNonNull(requested);
        EnumMap<HostResourceDomain, Long> charges = new EnumMap<>(HostResourceDomain.class);
        for (var entry : requested.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) throw new IllegalArgumentException("resource charge must be positive");
            charges.put(Objects.requireNonNull(entry.getKey()), entry.getValue());
        }
        if (charges.isEmpty()) throw new IllegalArgumentException("empty resource request");
        for (var entry : charges.entrySet()) {
            Cell cell = cells.get(entry.getKey());
            Denial denial = cell.accounting == Accounting.UNOBSERVED ? Denial.ACCOUNTING_UNAVAILABLE
                : entry.getValue() > cell.budget.hard - cell.used ? Denial.HARD_LIMIT
                : priority == Priority.NORMAL && entry.getValue() > cell.budget.soft - cell.used ? Denial.SOFT_LIMIT
                : priority == Priority.NORMAL && cell.host.pressure() != null && cell.host.pressure() >= 0.90D ? Denial.HOST_PRESSURE : Denial.NONE;
            if (denial != Denial.NONE) {
                cell.lastDenial = denial; cell.denied = saturatedAdd(cell.denied, 1); advanceRevision();
                return new Request(null, denial, entry.getKey());
            }
        }
        for (var entry : charges.entrySet()) {
            Cell cell = cells.get(entry.getKey()); cell.used += entry.getValue();
            cell.peak = Math.max(cell.peak, cell.used); cell.allocated = saturatedAdd(cell.allocated, entry.getValue());
            cell.lastDenial = Denial.NONE;
        }
        advanceRevision(); return new Request(new Reservation(charges), Denial.NONE, null);
    }
    public synchronized long revision() { return revision; }
    public synchronized List<Row> snapshot() {
        List<Row> result = new ArrayList<>();
        cells.forEach((domain, c) -> result.add(new Row(domain, c.accounting,
            c.accounting == Accounting.UNOBSERVED ? null : c.used,
            c.accounting == Accounting.UNOBSERVED ? null : c.peak,
            c.accounting == Accounting.UNOBSERVED ? null : c.allocated,
            c.accounting == Accounting.UNOBSERVED ? null : c.released,
            c.accounting == Accounting.UNOBSERVED ? null : c.allocated * 1_000_000_000D / Math.max(1L, System.nanoTime() - startedAt),
            c.lastRelease, c.budget, c.host,
            c.accounting == Accounting.UNOBSERVED ? Denial.ACCOUNTING_UNAVAILABLE : c.used >= c.budget.hard ? Denial.HARD_LIMIT
                : c.used >= c.budget.soft ? Denial.SOFT_LIMIT : c.host.pressure() != null && c.host.pressure() >= 0.90D ? Denial.HOST_PRESSURE : Denial.NONE,
            c.used > c.budget.soft, c.lastDenial, c.denied, Set.copyOf(c.sources))));
        return List.copyOf(result);
    }
    public static Map<HostResourceDomain, Budget> defaultBudgets() {
        var budgets = new EnumMap<HostResourceDomain, Budget>(HostResourceDomain.class);
        for (var domain : HostResourceDomain.values()) {
            long hard = domain.unit() == HostResourceDomain.Unit.BYTES ? 16L * 1024 * 1024 : 1024;
            if (domain == HostResourceDomain.THREAD) hard = 32;
            budgets.put(domain, new Budget(hard / 2, hard, hard / 8));
        }
        return Map.copyOf(budgets);
    }
    private long advanceRevision() { revision = saturatedAdd(revision, 1); return revision; }
    private static long saturatedAdd(long a, long b) { return a > Long.MAX_VALUE - b ? Long.MAX_VALUE : a + b; }
}
