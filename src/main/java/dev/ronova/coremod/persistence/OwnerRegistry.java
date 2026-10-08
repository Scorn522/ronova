package dev.ronova.coremod.persistence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Shadow-side owner registry extracted from the legacy ledger vocabulary. */
public final class OwnerRegistry {
    private final LinkedHashSet<UUID> owners = new LinkedHashSet<>();

    public synchronized boolean contains(UUID uuid) {
        return uuid != null && owners.contains(uuid);
    }

    public synchronized boolean bind(UUID uuid) {
        return owners.add(Objects.requireNonNull(uuid, "uuid"));
    }

    public synchronized boolean unbind(UUID uuid) {
        return uuid != null && owners.remove(uuid);
    }

    public synchronized int size() {
        return owners.size();
    }

    public synchronized List<UUID> snapshot() {
        return List.copyOf(owners);
    }

    public synchronized List<UUID> sortedSnapshot() {
        List<UUID> result = new ArrayList<>(owners);
        result.sort(UUID::compareTo);
        return List.copyOf(result);
    }

    public synchronized void replaceAll(Iterable<UUID> values) {
        owners.clear();
        if (values == null) {
            return;
        }
        for (UUID value : values) {
            if (value != null) {
                owners.add(value);
            }
        }
    }

    public synchronized void clear() {
        owners.clear();
    }
}
