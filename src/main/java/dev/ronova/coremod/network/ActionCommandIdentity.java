package dev.ronova.coremod.network;

import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.root.api.CommitReceipt;
import dev.ronova.coremod.root.api.DominionCommand;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

/**
 * Derives the durable Root identity of one client action.  The identity is
 * stable across a server restart while remaining fenced to one world, player,
 * client session and sequence. The action kind is bound into the command
 * payload domain, so changing action under the same sequence produces a
 * command-id conflict instead of a second durable command.
 */
public final class ActionCommandIdentity {
    private static final int DOMAIN = 0x52313641; // R16A

    private ActionCommandIdentity() {}

    public static UUID derive(
        RealmId realmId,
        UUID playerId,
        long sessionId,
        long sequence
    ) {
        Objects.requireNonNull(realmId, "realmId");
        Objects.requireNonNull(playerId, "playerId");
        if (sessionId <= 0L || sequence <= 0L) {
            throw new IllegalArgumentException("positive action session and sequence required");
        }

        ByteBuffer canonical = ByteBuffer.allocate(52);
        canonical.putInt(DOMAIN);
        putUuid(canonical, realmId.value());
        putUuid(canonical, playerId);
        canonical.putLong(sessionId);
        canonical.putLong(sequence);
        byte[] digest = sha256(canonical.array());

        // RFC 9562 UUIDv8: locally defined SHA-256 name format with RFC variant.
        digest[6] = (byte) ((digest[6] & 0x0f) | 0x80);
        digest[8] = (byte) ((digest[8] & 0x3f) | 0x80);
        ByteBuffer value = ByteBuffer.wrap(digest);
        return new UUID(value.getLong(), value.getLong());
    }

    private static void putUuid(ByteBuffer output, UUID value) {
        output.putLong(value.getMostSignificantBits());
        output.putLong(value.getLeastSignificantBits());
    }

    public static String commandDomain(RonovaAction action) {
        return switch (Objects.requireNonNull(action, "action")) {
            case PRIMARY -> "ronova.action.v1.primary";
            case AREA -> "ronova.action.v1.area";
            case LOADED -> "ronova.action.v1.loaded";
        };
    }

    public static boolean matches(CommitReceipt receipt, RonovaAction action) {
        return receipt != null
            && receipt.command() instanceof DominionCommand.CommitJudgmentBatch batch
            && batch.batch().commandDomain().equals(commandDomain(action));
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
