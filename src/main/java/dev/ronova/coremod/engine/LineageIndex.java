package dev.ronova.coremod.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Identity plus spatial lineage index. Identity and global lookups are O(1);
 * local lookups inspect only occupied cells that can overlap the query point.
 */
public final class LineageIndex<T extends LineageRecord> {
    private static final double CELL_SIZE = 64.0D;
    private static final String GLOBAL_DIMENSION = "\u0000global";

    private final LinkedHashMap<UUID, T> identities = new LinkedHashMap<>();
    private final Map<LineageKey, Integer> globalCounts = new HashMap<>();
    private final Map<LineageKey, LocalBucket<T>> localBuckets = new HashMap<>();

    public synchronized T put(T record) {
        Objects.requireNonNull(record, "record");
        T previous = identities.remove(record.uuid());
        if (previous != null) {
            removeLineage(previous);
        }
        identities.put(record.uuid(), record);
        addLineage(record);
        return previous;
    }

    public synchronized T remove(UUID uuid) {
        T removed = identities.remove(uuid);
        if (removed != null) {
            removeLineage(removed);
        }
        return removed;
    }

    /** Atomically replaces a projection so readers never observe a partial snapshot. */
    public synchronized void replaceAll(Iterable<? extends T> records) {
        identities.clear();
        globalCounts.clear();
        localBuckets.clear();
        if (records == null) {
            return;
        }
        for (T record : records) {
            if (record != null) {
                T previous = identities.remove(record.uuid());
                if (previous != null) {
                    removeLineage(previous);
                }
                identities.put(record.uuid(), record);
                addLineage(record);
            }
        }
    }

    public synchronized boolean matches(
        UUID uuid,
        String type,
        String dimension,
        double x,
        double y,
        double z
    ) {
        if (uuid != null && identities.containsKey(uuid)) {
            return true;
        }
        if (globalCounts.containsKey(globalKey(type))) {
            return true;
        }
        LineageKey key = localKey(dimension, type);
        LocalBucket<T> bucket = localBuckets.get(key);
        return bucket != null && bucket.matches(x, y, z);
    }

    /**
     * Matches a non-identity lineage without applying its spatial radius. This
     * is reserved for detached client-side proxy objects that are not members
     * of the world topology and therefore cannot carry the original UUID.
     */
    public synchronized boolean matchesDetachedLineage(String type, String dimension) {
        if (globalCounts.containsKey(globalKey(type))) {
            return true;
        }
        LineageKey key = localKey(dimension, type);
        LocalBucket<T> bucket = localBuckets.get(key);
        return bucket != null && !bucket.isEmpty();
    }

    public synchronized boolean contains(UUID uuid) {
        return uuid != null && identities.containsKey(uuid);
    }

    public synchronized T get(UUID uuid) {
        return uuid == null ? null : identities.get(uuid);
    }

    public synchronized int size() {
        return identities.size();
    }

    public synchronized List<T> values() {
        return List.copyOf(identities.values());
    }

    public synchronized void clear() {
        identities.clear();
        globalCounts.clear();
        localBuckets.clear();
    }

    public synchronized int trimToSize(int maximum) {
        return removeEldestToSize(maximum).size();
    }

    public synchronized List<T> removeEldestToSize(int maximum) {
        List<T> removed = new ArrayList<>();
        int boundedMaximum = Math.max(0, maximum);
        while (identities.size() > boundedMaximum) {
            UUID eldest = identities.keySet().iterator().next();
            T record = remove(eldest);
            if (record != null) {
                removed.add(record);
            }
        }
        return List.copyOf(removed);
    }

    private void addLineage(T record) {
        if (record.scope() == JudgmentScope.IDENTITY) {
            return;
        }
        if (record.scope() == JudgmentScope.GLOBAL_LINEAGE) {
            globalCounts.merge(globalKey(record.type()), 1, Integer::sum);
        } else {
            localBuckets.computeIfAbsent(
                localKey(record.dimension(), record.type()), ignored -> new LocalBucket<>()
            ).add(record);
        }
    }

    private void removeLineage(T record) {
        if (record.scope() == JudgmentScope.IDENTITY) {
            return;
        }
        if (record.scope() == JudgmentScope.GLOBAL_LINEAGE) {
            LineageKey key = globalKey(record.type());
            globalCounts.computeIfPresent(key, (ignored, count) -> count <= 1 ? null : count - 1);
            return;
        }
        LineageKey key = localKey(record.dimension(), record.type());
        LocalBucket<T> bucket = localBuckets.get(key);
        if (bucket != null) {
            bucket.remove(record);
            if (bucket.isEmpty()) {
                localBuckets.remove(key);
            }
        }
    }

    private static LineageKey globalKey(String type) {
        return new LineageKey(GLOBAL_DIMENSION, type);
    }

    private static LineageKey localKey(String dimension, String type) {
        return new LineageKey(dimension, type);
    }

    private record LineageKey(String dimension, String type) {
        private LineageKey {
            dimension = Objects.requireNonNullElse(dimension, "");
            type = Objects.requireNonNullElse(type, "");
        }
    }

    private static final class LocalBucket<T extends LineageRecord> {
        private final Map<CellKey, List<T>> cells = new HashMap<>();
        private final TreeMap<Double, Integer> finiteRadii = new TreeMap<>();
        private final List<T> unbounded = new ArrayList<>();

        private void add(T record) {
            double radius = normalizedRadius(record.radius());
            if (Double.isInfinite(radius)) {
                unbounded.add(record);
                return;
            }
            cells.computeIfAbsent(CellKey.at(record.x(), record.y(), record.z()), ignored -> new ArrayList<>())
                .add(record);
            finiteRadii.merge(radius, 1, Integer::sum);
        }

        private void remove(T record) {
            double radius = normalizedRadius(record.radius());
            if (Double.isInfinite(radius)) {
                unbounded.remove(record);
                return;
            }
            CellKey cell = CellKey.at(record.x(), record.y(), record.z());
            List<T> records = cells.get(cell);
            if (records != null) {
                records.remove(record);
                if (records.isEmpty()) {
                    cells.remove(cell);
                }
            }
            finiteRadii.computeIfPresent(radius, (ignored, count) -> count <= 1 ? null : count - 1);
        }

        private boolean matches(double x, double y, double z) {
            if (!unbounded.isEmpty()) {
                return true;
            }
            if (finiteRadii.isEmpty()) {
                return false;
            }
            double reach = finiteRadii.lastKey();
            long minX = cell(x - reach);
            long maxX = cell(x + reach);
            long minY = cell(y - reach);
            long maxY = cell(y + reach);
            long minZ = cell(z - reach);
            long maxZ = cell(z + reach);

            if (regionCellCountAtMost(minX, maxX, minY, maxY, minZ, maxZ, cells.size())) {
                for (long cellX = minX; cellX <= maxX; cellX++) {
                    for (long cellY = minY; cellY <= maxY; cellY++) {
                        for (long cellZ = minZ; cellZ <= maxZ; cellZ++) {
                            if (matches(cells.get(new CellKey(cellX, cellY, cellZ)), x, y, z)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            for (Map.Entry<CellKey, List<T>> entry : cells.entrySet()) {
                CellKey cell = entry.getKey();
                if (cell.x() >= minX && cell.x() <= maxX
                    && cell.y() >= minY && cell.y() <= maxY
                    && cell.z() >= minZ && cell.z() <= maxZ
                    && matches(entry.getValue(), x, y, z)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isEmpty() {
            return cells.isEmpty() && unbounded.isEmpty();
        }

        private static <T extends LineageRecord> boolean matches(
            List<T> records,
            double x,
            double y,
            double z
        ) {
            if (records == null) {
                return false;
            }
            for (T record : records) {
                double radius = normalizedRadius(record.radius());
                double dx = x - record.x();
                double dy = y - record.y();
                double dz = z - record.z();
                if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                    return true;
                }
            }
            return false;
        }

        private static boolean regionCellCountAtMost(
            long minX,
            long maxX,
            long minY,
            long maxY,
            long minZ,
            long maxZ,
            int limit
        ) {
            long xCount = rangeLength(minX, maxX);
            long yCount = rangeLength(minY, maxY);
            long zCount = rangeLength(minZ, maxZ);
            long boundedLimit = Math.max(1L, limit);
            if (xCount > boundedLimit || yCount > boundedLimit || zCount > boundedLimit) {
                return false;
            }
            long xy = xCount * yCount;
            return xy <= boundedLimit && zCount <= boundedLimit / Math.max(1L, xy);
        }

        private static long rangeLength(long minimum, long maximum) {
            if (maximum < minimum || maximum == Long.MAX_VALUE || minimum == Long.MIN_VALUE) {
                return Long.MAX_VALUE;
            }
            return maximum - minimum + 1L;
        }

        private static double normalizedRadius(double radius) {
            return Double.isNaN(radius) ? 0.0D : Math.max(0.0D, radius);
        }
    }

    private record CellKey(long x, long y, long z) {
        private static CellKey at(double x, double y, double z) {
            return new CellKey(cell(x), cell(y), cell(z));
        }
    }

    private static long cell(double coordinate) {
        return (long) Math.floor(coordinate / CELL_SIZE);
    }
}
