package dev.ronova.coremod.runtime;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Observation nonce only. Root alone assigns a qualified incarnation and binding revision. */
public final class PhysicalIncarnationTracker {
    private final ReferenceQueue<Object> collected = new ReferenceQueue<>();
    private final Map<Key, UUID> nonces = new HashMap<>();
    public synchronized UUID nonce(Object physicalObject) {
        Objects.requireNonNull(physicalObject, "physicalObject");
        for (Object dead; (dead = collected.poll()) != null;) nonces.remove(dead);
        Key lookup = new Key(physicalObject, null);
        UUID known = nonces.get(lookup);
        if (known != null) return known;
        UUID created = UUID.randomUUID(); nonces.put(new Key(physicalObject, collected), created); return created;
    }
    public synchronized void clear() { nonces.clear(); while (collected.poll() != null) {} }
    private static final class Key extends WeakReference<Object> {
        private final int hash;
        Key(Object value, ReferenceQueue<Object> queue) { super(value, queue); hash = System.identityHashCode(value); }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || other instanceof Key key && get() != null && get() == key.get();
        }
    }
}
