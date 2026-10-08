package dev.ronova.coremod.extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Bootstrap-only registry. Freezing prevents late class-load order from
 * changing Root interpretation in a running realm.
 */
public final class VersionedExtensionRegistry {
    private final Map<VersionedExtensionId, TreeMap<Integer, VersionedExtensionCodec<?>>> codecs =
        new LinkedHashMap<>();
    private boolean frozen;

    public synchronized void register(VersionedExtensionCodec<?> codec) {
        Objects.requireNonNull(codec, "codec");
        if (frozen) {
            throw new IllegalStateException("extension registry is frozen");
        }
        if (codec.schemaVersion() <= 0) {
            throw new IllegalArgumentException("extension schema version must be positive");
        }
        TreeMap<Integer, VersionedExtensionCodec<?>> versions =
            codecs.computeIfAbsent(codec.id(), ignored -> new TreeMap<>());
        if (versions.putIfAbsent(codec.schemaVersion(), codec) != null) {
            throw new IllegalArgumentException(
                "duplicate extension codec " + codec.id().canonical()
                    + "@" + codec.schemaVersion()
            );
        }
    }

    public synchronized void freeze() {
        frozen = true;
    }

    public synchronized boolean frozen() {
        return frozen;
    }

    public synchronized Resolution resolve(VersionedExtensionEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        TreeMap<Integer, VersionedExtensionCodec<?>> versions = codecs.get(envelope.id());
        if (versions == null) {
            return new Resolution(Status.UNKNOWN_EXTENSION, null, null);
        }
        VersionedExtensionCodec<?> codec = versions.get(envelope.schemaVersion());
        if (codec == null) {
            return new Resolution(Status.UNSUPPORTED_SCHEMA, null, null);
        }
        try {
            Object value = codec.decode(envelope);
            if (value == null || !codec.valueType().isInstance(value)) {
                return new Resolution(Status.INVALID_PAYLOAD, codec, null);
            }
            return new Resolution(Status.RESOLVED, codec, value);
        } catch (RuntimeException invalid) {
            return new Resolution(Status.INVALID_PAYLOAD, codec, null);
        }
    }

    public synchronized List<String> registeredKeys() {
        List<String> keys = new ArrayList<>();
        codecs.forEach((id, versions) -> versions.keySet().forEach(
            schema -> keys.add(id.canonical() + "@" + schema)
        ));
        Collections.sort(keys);
        return List.copyOf(keys);
    }

    public enum Status {
        RESOLVED,
        UNKNOWN_EXTENSION,
        UNSUPPORTED_SCHEMA,
        INVALID_PAYLOAD
    }

    public record Resolution(
        Status status, VersionedExtensionCodec<?> codec, Object value
    ) {
        public Resolution {
            Objects.requireNonNull(status, "status");
        }

        public boolean resolved() {
            return status == Status.RESOLVED;
        }
    }
}
