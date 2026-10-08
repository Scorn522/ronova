package dev.ronova.coremod.nativebridge;

import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Canonical, loader-neutral snapshot wire format shared with the Native root. */
public final class NativeSnapshotCodec {
    static final int MAGIC = 0x41585233; // AXR3
    static final int SCHEMA = 1;
    private static final int MAX_PAYLOAD = 64 * 1024 * 1024;
    private static final int MAX_DECISIONS = 131_072;
    private static final int MAX_STRING_BYTES = 1_048_576;
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private NativeSnapshotCodec() {}

    public static Encoded encode(DominionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.decisions().size() > MAX_DECISIONS) {
            throw new IllegalArgumentException("native snapshot exceeds decision limit");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(MAGIC);
                output.writeInt(SCHEMA);
                output.writeLong(snapshot.epoch().generation());
                output.writeLong(snapshot.epoch().sequence());
                output.writeInt(snapshot.decisions().size());
                for (DominionDecision decision : snapshot.decisions()) {
                    writeDecision(output, decision);
                }
            }
            byte[] payload = bytes.toByteArray();
            if (payload.length > MAX_PAYLOAD) {
                throw new IllegalArgumentException("native snapshot exceeds payload limit");
            }
            return new Encoded(payload, fingerprint(payload), sha256(payload));
        } catch (IOException impossible) {
            throw new IllegalStateException("in-memory snapshot encoding failed", impossible);
        }
    }

    public static DominionSnapshot decode(byte[] payload) {
        if (payload == null || payload.length == 0 || payload.length > MAX_PAYLOAD) {
            throw new IllegalArgumentException("invalid native snapshot payload size");
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            if (input.readInt() != MAGIC || input.readInt() != SCHEMA) {
                throw new IllegalArgumentException("unsupported native snapshot schema");
            }
            DominionEpoch epoch = new DominionEpoch(input.readLong(), input.readLong());
            int count = input.readInt();
            if (count < 0 || count > MAX_DECISIONS) {
                throw new IllegalArgumentException("invalid native decision count");
            }
            List<DominionDecision> decisions = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                decisions.add(readDecision(input));
            }
            if (input.available() != 0) {
                throw new IllegalArgumentException("trailing native snapshot bytes");
            }
            return new DominionSnapshot(epoch, decisions);
        } catch (EOFException truncated) {
            throw new IllegalArgumentException("truncated native snapshot payload", truncated);
        } catch (IOException malformed) {
            throw new IllegalArgumentException("malformed native snapshot payload", malformed);
        }
    }

    public static long fingerprint(DominionSnapshot snapshot) {
        return encode(snapshot).fingerprint();
    }

    static long fingerprint(byte[] payload) {
        long value = FNV_OFFSET;
        for (byte current : payload) {
            value ^= current & 0xffL;
            value *= FNV_PRIME;
        }
        return value;
    }

    private static void writeDecision(DataOutputStream output, DominionDecision decision)
        throws IOException {
        output.writeByte(decision.domain().ordinal());
        writeSubject(output, decision.subject());
        output.writeLong(decision.epoch().generation());
        output.writeLong(decision.epoch().sequence());
        if (decision instanceof DominionDecision.Authority authority) {
            output.writeBoolean(authority.authorized());
            output.writeLong(authority.capabilities().mask());
        } else if (decision instanceof DominionDecision.Judgment judgment) {
            output.writeBoolean(judgment.committed());
            output.writeByte(judgment.scope().ordinal());
            output.writeDouble(judgment.radius());
        } else if (decision instanceof DominionDecision.Admission admission) {
            output.writeBoolean(admission.admitted());
        } else if (decision instanceof DominionDecision.Presence presence) {
            output.writeBoolean(presence.present());
        } else {
            throw new IllegalArgumentException("unsupported decision: " + decision);
        }
        writeString(output, decision.reason());
    }

    private static DominionDecision readDecision(DataInputStream input) throws IOException {
        int domain = input.readUnsignedByte();
        SubjectIdentity subject = readSubject(input);
        DominionEpoch decisionEpoch = new DominionEpoch(input.readLong(), input.readLong());
        return switch (domain) {
            case 0 -> new DominionDecision.Authority(
                subject, decisionEpoch, input.readBoolean(),
                new CapabilitySet(input.readLong()), readString(input)
            );
            case 1 -> new DominionDecision.Judgment(
                subject, decisionEpoch, input.readBoolean(),
                readScope(input.readUnsignedByte()), input.readDouble(), readString(input)
            );
            case 2 -> new DominionDecision.Admission(
                subject, decisionEpoch, input.readBoolean(), readString(input)
            );
            case 3 -> new DominionDecision.Presence(
                subject, decisionEpoch, input.readBoolean(), readString(input)
            );
            default -> throw new IllegalArgumentException("unknown native decision domain: " + domain);
        };
    }

    private static void writeSubject(DataOutputStream output, SubjectIdentity subject)
        throws IOException {
        UUID uuid = subject.uuid();
        output.writeBoolean(uuid != null);
        output.writeLong(uuid == null ? 0L : uuid.getMostSignificantBits());
        output.writeLong(uuid == null ? 0L : uuid.getLeastSignificantBits());
        writeString(output, subject.registryType());
        writeString(output, subject.runtimeClass());
        writeString(output, subject.dimension());
        output.writeDouble(subject.x());
        output.writeDouble(subject.y());
        output.writeDouble(subject.z());
        output.writeBoolean(subject.player());
    }

    private static SubjectIdentity readSubject(DataInputStream input) throws IOException {
        boolean identified = input.readBoolean();
        long most = input.readLong();
        long least = input.readLong();
        return new SubjectIdentity(
            identified ? new UUID(most, least) : null,
            readString(input), readString(input), readString(input),
            input.readDouble(), input.readDouble(), input.readDouble(), input.readBoolean()
        );
    }

    private static DominionDecision.LineageScope readScope(int ordinal) {
        DominionDecision.LineageScope[] values = DominionDecision.LineageScope.values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("unknown native lineage scope: " + ordinal);
        }
        return values[ordinal];
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = Objects.toString(value, "").getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) {
            throw new IllegalArgumentException("native snapshot string exceeds limit");
        }
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > MAX_STRING_BYTES || length > input.available()) {
            throw new IllegalArgumentException("invalid native snapshot string length");
        }
        return new String(input.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static String sha256(byte[] payload) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public record Encoded(byte[] payload, long fingerprint, String sha256) {
        public Encoded {
            payload = payload == null ? new byte[0] : payload.clone();
            sha256 = Objects.requireNonNull(sha256, "sha256");
        }

        @Override
        public byte[] payload() {
            return payload.clone();
        }
    }
}
