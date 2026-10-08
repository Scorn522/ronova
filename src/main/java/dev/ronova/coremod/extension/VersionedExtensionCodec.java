package dev.ronova.coremod.extension;

/** Codec registered before Root publication; decode must be deterministic. */
public interface VersionedExtensionCodec<T> {
    VersionedExtensionId id();

    int schemaVersion();

    Class<T> valueType();

    VersionedExtensionEnvelope encode(T value, long revision);

    T decode(VersionedExtensionEnvelope envelope);
}
