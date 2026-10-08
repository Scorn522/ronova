package dev.ronova.coremod.observation;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * Object-identity evidence for an observed seed and its actual copies.
 * Equal content, UUIDs and class names never create a relationship. This index
 * observes data flow; its answer is neither a Judgment nor permission to write.
 */
public final class CopyProvenanceIndex<K> implements AutoCloseable {
    public enum State { UNOBSERVED, RESOLVED, AMBIGUOUS, STALE }
    public record Resolution<K>(State state, K sourceKey, Object seed, int copyDepth, boolean evidenceVerified) {
        public Resolution(State state, K sourceKey, Object seed) { this(state, sourceKey, seed, 0, false); }
    }
    private static final int ENTRY_BYTES = 384;
    private static final int REAP_SLICE = 16;
    private final int capacity;
    private final IntFunction<Runnable> reserve;
    private final Consumer<K> coverageLoss;
    private final ReferenceQueue<Object> collected = new ReferenceQueue<>();
    private final Map<IdentityRef, Entry<K>> entries = new HashMap<>();
    private final Map<K, java.util.Set<IdentityRef>> bySource = new HashMap<>();
    private long copied, conflicts, rejected, reaped;
    private boolean closed;

    public CopyProvenanceIndex(int capacity, IntFunction<Runnable> reserve,
                               Consumer<K> coverageLoss) {
        if (capacity < 1) throw new IllegalArgumentException("positive capacity required");
        this.capacity = capacity;
        this.reserve = Objects.requireNonNull(reserve);
        this.coverageLoss = Objects.requireNonNull(coverageLoss);
    }

    public synchronized boolean track(K sourceKey, Object seed) {
        return track(sourceKey, seed, false);
    }
    public synchronized boolean track(K sourceKey, Object seed, boolean evidenceVerified) {
        Objects.requireNonNull(sourceKey);
        Objects.requireNonNull(seed);
        if (closed) return false;
        reap();
        Entry<K> existing = entries.get(new IdentityRef(seed));
        if (existing != null) {
            if (!existing.ambiguous && existing.key.equals(sourceKey)
                && existing.seed.get() == seed && existing.copyDepth == 0) {
                // This is a new direct seed observation. Existing descendants keep their own evidence.
                existing.evidenceVerified = evidenceVerified;
                return true;
            }
            conflict(existing, sourceKey);
            return false;
        }
        return insert(seed, sourceKey, new WeakReference<>(seed), 0, evidenceVerified);
    }

    /** Called only after a real copy operation; returns no modified object. */
    public synchronized void copied(Object source, Object result) {
        copied(source, result, Integer.MAX_VALUE, false);
    }
    public synchronized void copied(Object source, Object result, int maximumDepth, boolean evidenceVerified) {
        if (maximumDepth < 1) throw new IllegalArgumentException("positive copy depth required");
        if (closed || source == null || result == null || source == result) return;
        reap();
        Entry<K> origin = entries.get(new IdentityRef(source));
        if (origin == null) {
            Entry<K> target = entries.get(new IdentityRef(result));
            if (target != null) conflict(target, target.key);
            return;
        }
        if (origin.copyDepth >= maximumDepth) {
            // A reused output must not keep an older shallow proof for this new out-of-scope value.
            Entry<K> target = entries.get(new IdentityRef(result));
            if (target != null) conflict(target, origin.key);
            return;
        }
        if (origin.ambiguous || origin.copyDepth == 0 && origin.seed.get() == null) {
            // Do not silently let an ambiguous input overwrite an existing proof.
            Entry<K> target = entries.get(new IdentityRef(result));
            if (target != null) conflict(target, origin.key);
            loss(origin.key);
            return;
        }
        Entry<K> target = entries.get(new IdentityRef(result));
        if (target != null) {
            if (target.ambiguous || !target.key.equals(origin.key)
                || target.seed != origin.seed) conflict(target, origin.key);
            else {
                target.copyDepth = Math.max(target.copyDepth, origin.copyDepth + 1);
                target.evidenceVerified &= origin.evidenceVerified && evidenceVerified;
            }
            return;
        }
        if (insert(result, origin.key, origin.seed, origin.copyDepth + 1, origin.evidenceVerified && evidenceVerified)) copied = increment(copied);
    }

    public synchronized Resolution<K> resolve(Object value) {
        if (closed || value == null) return new Resolution<>(State.UNOBSERVED, null, null);
        reap();
        Entry<K> entry = entries.get(new IdentityRef(value));
        if (entry == null) return new Resolution<>(State.UNOBSERVED, null, null);
        if (entry.ambiguous) return new Resolution<>(State.AMBIGUOUS, null, null);
        Object seed = entry.seed.get();
        // The shared weak-reference object is the lineage token. Collecting the original
        // seed does not erase a copy edge witnessed while its actual objects were alive.
        return new Resolution<>(seed == null && entry.copyDepth == 0 ? State.STALE : State.RESOLVED,
            entry.key, seed, entry.copyDepth, entry.evidenceVerified);
    }

    /** Does not retain copies or require the original seed to remain alive. */
    public synchronized boolean hasLiveCopies(K key) {
        reap();
        var references=bySource.get(Objects.requireNonNull(key));
        if(references==null)return false;
        for(var reference:references) {
            var entry=entries.get(reference);
            if(reference.get()!=null&&entry!=null&&entry.copyDepth>0)return true;
        }
        return false;
    }

    private boolean insert(Object value, K key, WeakReference<Object> seed, int depth, boolean evidenceVerified) {
        if (entries.size() >= capacity) { loss(key); return false; }
        Runnable release = reserve.apply(ENTRY_BYTES);
        if (release == null) { loss(key); return false; }
        boolean retained = false;
        IdentityRef reference = new IdentityRef(value, collected);
        try {
            entries.put(reference, new Entry<>(key, seed, release, depth, evidenceVerified));
            bySource.computeIfAbsent(key, ignored -> new java.util.HashSet<>()).add(reference);
            retained = true;
            return true;
        } finally {
            if (!retained) { entries.remove(reference); unlink(key, reference); release.run(); }
        }
    }

    private void conflict(Entry<K> existing, K other) {
        if (!existing.ambiguous) {
            existing.ambiguous = true;
            conflicts = increment(conflicts);
            coverageLoss.accept(existing.key);
        }
        if (!existing.key.equals(other)) coverageLoss.accept(other);
    }

    private void loss(K key) {
        rejected = increment(rejected);
        coverageLoss.accept(key);
    }

    private void reap() {
        for (int i = 0; i < REAP_SLICE; i++) {
            IdentityRef ref = (IdentityRef) collected.poll();
            if (ref == null) return;
            Entry<K> entry = entries.remove(ref);
            if (entry != null) { unlink(entry.key, ref); entry.release.run(); reaped = increment(reaped); }
        }
    }

    public synchronized int size() { return entries.size(); }
    public synchronized long copyCount() { return copied; }
    public synchronized long conflictCount() { return conflicts; }
    public synchronized long rejectedCount() { return rejected; }
    public synchronized long reapedCount() { return reaped; }
    /** Retire only direct seed admission; already observed successors retain their own lineage. */
    public synchronized void retireSeeds(K key) {
        Objects.requireNonNull(key);
        var references=bySource.get(key);
        if(references==null)return;
        RuntimeException failure=null;
        var iterator=references.iterator();
        while(iterator.hasNext()) {
            var reference=iterator.next();var entry=entries.get(reference);
            if(entry==null){iterator.remove();continue;}
            if(entry.copyDepth!=0)continue;
            iterator.remove();entries.remove(reference);
            try {entry.release.run();} catch(RuntimeException error) {
                if(failure==null)failure=error;else if(failure!=error)failure.addSuppressed(error);
            }
        }
        if(references.isEmpty())bySource.remove(key);
        if(failure!=null)throw failure;
    }
    /** Retire a source revision without making the same surviving seed inherit its old authority. */
    public synchronized void forget(K key) {
        Objects.requireNonNull(key);
        var references = bySource.remove(key);
        RuntimeException failure = null;
        if (references != null) for (var reference : references) {
            Entry<K> entry = entries.remove(reference);
            if (entry != null) try { entry.release.run(); }
            catch (RuntimeException error) {
                if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }
    private void unlink(K key, IdentityRef ref) {
        var references = bySource.get(key);
        if (references != null) { references.remove(ref); if (references.isEmpty()) bySource.remove(key); }
    }

    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        RuntimeException failure = null;
        for (Entry<K> entry : entries.values()) {
            try { entry.release.run(); }
            catch (RuntimeException error) {
                if (failure == null) failure = error; else if (failure != error) failure.addSuppressed(error);
            }
        }
        entries.clear();
        bySource.clear();
        if (failure != null) throw failure;
    }

    private static long increment(long value) { return value == Long.MAX_VALUE ? value : value + 1; }
    private static final class Entry<K> {
        final K key;
        final WeakReference<Object> seed;
        final Runnable release;
        boolean ambiguous;
        int copyDepth;
        boolean evidenceVerified;
        Entry(K key, WeakReference<Object> seed, Runnable release, int depth, boolean evidenceVerified) {
            this.key = key; this.seed = seed; this.release = release;
            this.copyDepth = depth; this.evidenceVerified = evidenceVerified;
        }
    }
    private static final class IdentityRef extends WeakReference<Object> {
        private final int hash;
        IdentityRef(Object value) { super(value); hash = System.identityHashCode(value); }
        IdentityRef(Object value, ReferenceQueue<Object> queue) {
            super(value, queue); hash = System.identityHashCode(value);
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof IdentityRef ref && get() != null && get() == ref.get();
        }
    }
}
