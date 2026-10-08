package dev.ronova.coremod.extension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/**
 * Bounded opaque payload carried by Root commands and receipts. Unknown
 * extensions remain preserved data; they never become Authority by themselves.
 */
public final class VersionedExtensionEnvelope {
    public static final int MAX_PAYLOAD_BYTES = 1_048_576;

    private final VersionedExtensionId id;
    private final int schemaVersion;
    private final long revision;
    private final byte[] payload;
    private final String payloadHash;

    public VersionedExtensionEnvelope(
        VersionedExtensionId id,
        int schemaVersion,
        long revision,
        byte[] payload,
        String payloadHash
    ) {
        this.id = Objects.requireNonNull(id, "id");
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("schema version must be positive");
        }
        if (revision < 0L) {
            throw new IllegalArgumentException("extension revision must be non-negative");
        }
        byte[] copy = payload == null ? new byte[0] : payload.clone();
        if (copy.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("extension payload exceeds hard limit");
        }
        String expected = sha256(copy);
        String supplied = Objects.toString(payloadHash, "").trim().toLowerCase(Locale.ROOT);
        if (!expected.equals(supplied)) {
            throw new IllegalArgumentException("extension payload hash mismatch");
        }
        this.schemaVersion = schemaVersion;
        this.revision = revision;
        this.payload = copy;
        this.payloadHash = expected;
    }

    public static VersionedExtensionEnvelope create(
        VersionedExtensionId id, int schemaVersion, long revision, byte[] payload
    ) {
        byte[] safe = payload == null ? new byte[0] : payload.clone();
        return new VersionedExtensionEnvelope(
            id, schemaVersion, revision, safe, sha256(safe)
        );
    }

    public static VersionedExtensionEnvelope utf8(
        VersionedExtensionId id, int schemaVersion, long revision, String payload
    ) {
        return create(
            id, schemaVersion, revision,
            Objects.toString(payload, "").getBytes(StandardCharsets.UTF_8)
        );
    }

    public VersionedExtensionId id() { return id; }
    public int schemaVersion() { return schemaVersion; }
    public long revision() { return revision; }
    public byte[] payload() { return payload.clone(); }
    public String payloadHash() { return payloadHash; }

    public String canonicalKey() {
        return id.canonical() + "@" + schemaVersion + "#" + revision + "/" + payloadHash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof VersionedExtensionEnvelope envelope)) {
            return false;
        }
        return schemaVersion == envelope.schemaVersion
            && revision == envelope.revision
            && id.equals(envelope.id)
            && payloadHash.equals(envelope.payloadHash)
            && Arrays.equals(payload, envelope.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, schemaVersion, revision, payloadHash);
        return 31 * result + Arrays.hashCode(payload);
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
