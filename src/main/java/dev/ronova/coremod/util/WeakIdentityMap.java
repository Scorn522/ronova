package dev.ronova.coremod.util;

import java.lang.ref.*;
import java.util.*;
import java.util.function.Predicate;

/** Weak keys compared by physical identity, even when a target overrides equals/hashCode. */
public final class WeakIdentityMap<K, V> {
    private final ReferenceQueue<K> collected = new ReferenceQueue<>();
    private final Map<Key<K>, V> entries = new HashMap<>();
    private void drain() { for (Object dead; (dead = collected.poll()) != null;) entries.remove(dead); }
    public synchronized V get(K key) { drain(); return entries.get(new Key<>(Objects.requireNonNull(key), null)); }
    public synchronized V computeIfAbsent(K key, java.util.function.Function<K, V> factory) {
        V found = get(key); if (found != null) return found;
        V created = Objects.requireNonNull(factory.apply(key)); put(key, created); return created;
    }
    public synchronized void put(K key, V value) { drain(); entries.put(new Key<>(Objects.requireNonNull(key), collected), Objects.requireNonNull(value)); }
    public synchronized void remove(K key) { drain(); entries.remove(new Key<>(Objects.requireNonNull(key), null)); }
    public synchronized void removeIf(Predicate<K> test) { drain(); entries.keySet().removeIf(key -> { K value = key.get(); return value == null || test.test(value); }); }
    public synchronized void clear() { entries.clear(); drain(); }
    public synchronized int size() { drain(); return entries.size(); }
    private static final class Key<K> extends WeakReference<K> {
        private final int hash;
        Key(K value, ReferenceQueue<K> queue) { super(value, queue); hash = System.identityHashCode(value); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) { Object value = get(); return this == other || other instanceof Key<?> key && value != null && value == key.get(); }
    }
}
