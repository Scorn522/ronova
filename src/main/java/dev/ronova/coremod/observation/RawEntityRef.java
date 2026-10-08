package dev.ronova.coremod.observation;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.UUID;

/** Weak identity reference with immutable raw lookup keys. */
final class RawEntityRef<T, L> extends WeakReference<T> {
    private final int identityHash;
    private final UUID uuid;
    private final int entityId;
    private final L levelKey;
    private boolean reaped;

    RawEntityRef(
        T value,
        ReferenceQueue<T> queue,
        UUID uuid,
        int entityId,
        L levelKey
    ) {
        super(Objects.requireNonNull(value, "value"), Objects.requireNonNull(queue, "queue"));
        identityHash = System.identityHashCode(value);
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.entityId = entityId;
        this.levelKey = Objects.requireNonNull(levelKey, "levelKey");
    }

    UUID uuid() {
        return uuid;
    }

    int entityId() {
        return entityId;
    }

    L levelKey() {
        return levelKey;
    }

    boolean keysMatch(UUID candidateUuid, int candidateId, L candidateLevel) {
        return uuid.equals(candidateUuid)
            && entityId == candidateId
            && levelKey.equals(candidateLevel);
    }

    synchronized boolean reaped() {
        return reaped;
    }

    /** Returns true once when this reference leaves its registered queue. */
    synchronized boolean markReaped() {
        if (reaped) {
            return false;
        }
        reaped = true;
        return true;
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
        if (!(other instanceof RawEntityRef<?, ?> reference)) {
            return false;
        }
        Object value = get();
        return value != null && value == reference.get();
    }
}

/** Mutable, non-owning HashMap probe reused while WeakTopologyCore is locked. */
final class RawEntityLookup<T> {
    private T value;
    private int identityHash;

    void set(T value) {
        this.value = Objects.requireNonNull(value, "value");
        identityHash = System.identityHashCode(value);
    }

    void clear() {
        value = null;
        identityHash = 0;
    }

    @Override
    public int hashCode() {
        return identityHash;
    }

    @Override
    public boolean equals(Object other) {
        return value != null
            && other instanceof RawEntityRef<?, ?> reference
            && value == reference.get();
    }
}
