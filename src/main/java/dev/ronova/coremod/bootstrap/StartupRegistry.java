package dev.ronova.coremod.bootstrap;

import java.util.*;
import java.util.function.*;

/** Process-local lifecycle slots; a failed constructor is never retried implicitly. */
final class StartupRegistry<K, V> {
    private final Map<K,V> ready = new WeakHashMap<>();
    private final Map<K,Throwable> failed = new WeakHashMap<>();
    private final Set<K> closed = Collections.newSetFromMap(new WeakHashMap<>());

    synchronized V get(K key) { return ready.get(key); }
    synchronized Collection<V> values() { return List.copyOf(ready.values()); }
    synchronized V remove(K key) { closed.add(key); return ready.remove(key); }
    synchronized V getOrCreate(K key, Function<K,V> factory, Consumer<K> rollback) {
        V value = ready.get(key);
        if (value != null) return value;
        Throwable prior = failed.get(key);
        if (prior instanceof RuntimeException error) throw error;
        if (prior instanceof Error error) throw error;
        if (closed.contains(key)) throw new IllegalStateException("server runtime has already been released");
        try {
            value = Objects.requireNonNull(factory.apply(key), "runtime factory result");
            ready.put(key,value);
            return value;
        } catch (RuntimeException | Error failure) {
            failed.put(key,failure);
            try { rollback.accept(key); }
            catch (RuntimeException | Error cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
}
