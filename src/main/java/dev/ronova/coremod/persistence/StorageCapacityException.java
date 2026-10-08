package dev.ronova.coremod.persistence;

import dev.ronova.coremod.root.api.StorageCapacity;

/** Preflight rejection before a new Root image or effect intent has been accepted. */
final class StorageCapacityException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final StorageCapacity capacity;
    StorageCapacityException(StorageCapacity capacity) {
        super("durable storage preflight blocked: " + capacity.state());
        this.capacity = capacity;
    }
    StorageCapacity capacity() { return capacity; }
}
