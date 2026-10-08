package dev.ronova.coremod.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/** Weak set whose membership is based on object identity rather than equals(). */
public final class WeakIdentitySet<T> {
    private final ReferenceQueue<T> stale = new ReferenceQueue<>();
    private final Map<IdentityReference<T>, Boolean> entries = new HashMap<>();

    public synchronized boolean add(T value) {
        if (value == null) {
            return false;
        }
        drain();
        return entries.put(new IdentityReference<>(value, stale), Boolean.TRUE) == null;
    }

    /** Adds by identity while reclaiming at most {@code maxStale} old keys. */
    public synchronized boolean addBounded(T value, int maxStale) {
        if (value == null) {
            return false;
        }
        if (maxStale < 0) {
            throw new IllegalArgumentException("maxStale must be non-negative");
        }
        drain(maxStale);
        return entries.put(new IdentityReference<>(value, stale), Boolean.TRUE) == null;
    }

    public synchronized boolean contains(T value) {
        if (value == null) {
            return false;
        }
        drain();
        return entries.containsKey(new IdentityReference<>(value));
    }

    public synchronized boolean remove(T value) {
        if (value == null) {
            return false;
        }
        drain();
        return entries.remove(new IdentityReference<>(value)) != null;
    }

    public synchronized void removeIf(Predicate<? super T> predicate) {
        drain();
        Iterator<IdentityReference<T>> iterator = entries.keySet().iterator();
        while (iterator.hasNext()) {
            T value = iterator.next().get();
            if (value == null || predicate.test(value)) {
                iterator.remove();
            }
        }
    }

    public synchronized int size() {
        drain();
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
        while (stale.poll() != null) {
            // Drain queued references so the next population starts cleanly.
        }
    }

    private void drain() {
        drain(Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    private void drain(int maxStale) {
        for (int drained = 0; drained < maxStale; drained++) {
            IdentityReference<T> reference = (IdentityReference<T>) stale.poll();
            if (reference == null) {
                return;
            }
            entries.remove(reference);
        }
    }

    private static final class IdentityReference<T> extends WeakReference<T> {
        private final int identityHash;

        private IdentityReference(T value) {
            super(value);
            identityHash = System.identityHashCode(value);
        }

        private IdentityReference(T value, ReferenceQueue<T> queue) {
            super(value, queue);
            identityHash = System.identityHashCode(value);
        }

        @Override
        public int hashCode() {
            return identityHash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof IdentityReference<?> reference)) {
                return false;
            }
            Object value = get();
            return value != null && value == reference.get();
        }
    }
}
