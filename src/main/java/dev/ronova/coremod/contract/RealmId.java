package dev.ronova.coremod.contract;

import java.util.Objects;
import java.util.UUID;

/** Stable identity of one world lineage; it is correlation data, not ownership proof. */
public record RealmId(UUID value) {
    public RealmId {
        Objects.requireNonNull(value, "value");
    }

    public static RealmId random() {
        return new RealmId(UUID.randomUUID());
    }

    public static RealmId fromString(String value) {
        String normalized = Objects.requireNonNull(value, "value").trim();
        UUID parsed = UUID.fromString(normalized);
        if (!parsed.toString().equalsIgnoreCase(normalized)) {
            throw new IllegalArgumentException("realm id must be a canonical UUID");
        }
        return new RealmId(parsed);
    }

    public String canonical() {
        return value.toString();
    }

    @Override
    public String toString() {
        return canonical();
    }
}
