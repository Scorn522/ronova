package dev.ronova.coremod.nativebridge;

import dev.ronova.coremod.contract.CapabilitySet;
import dev.ronova.coremod.contract.DominionDecision;
import dev.ronova.coremod.contract.DominionEpoch;
import dev.ronova.coremod.contract.DominionSnapshot;
import dev.ronova.coremod.contract.SubjectIdentity;
import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;

/** Golden checks for the Java/Native R3 snapshot wire contract. */
public final class NativeSnapshotCodecSelfTest {
    private NativeSnapshotCodecSelfTest() {}

    public static void main(String[] args) throws Exception {
        DominionEpoch current = new DominionEpoch(3L, 19L);
        SubjectIdentity owner = subject(
            UUID.fromString("11111111-2222-3333-4444-555555555555"),
            "minecraft:player", "test.Owner", true
        );
        SubjectIdentity zombie = subject(
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
            "minecraft:zombie", "test.Zombie", false
        );
        DominionSnapshot snapshot = new DominionSnapshot(current, List.of(
            new DominionDecision.Authority(
                owner, current, true, CapabilitySet.OWNER_DEFAULT, "owner"
            ),
            new DominionDecision.Judgment(
                zombie, new DominionEpoch(3L, 12L), true,
                DominionDecision.LineageScope.GLOBAL_LINEAGE, 0.0D, "judged"
            ),
            new DominionDecision.Admission(zombie, current, false, "blocked"),
            new DominionDecision.Presence(zombie, current, false, "hidden")
        ));

        NativeSnapshotCodec.Encoded first = NativeSnapshotCodec.encode(snapshot);
        NativeSnapshotCodec.Encoded second = NativeSnapshotCodec.encode(snapshot);
        require(first.fingerprint() == second.fingerprint(), "fingerprint is not deterministic");
        require(first.sha256().equals(second.sha256()), "SHA-256 is not deterministic");
        require(NativeSnapshotCodec.decode(first.payload()).equals(snapshot),
            "snapshot round trip changed contract state");

        byte[] defensive = first.payload();
        defensive[0] ^= 0x7f;
        require(NativeSnapshotCodec.decode(first.payload()).equals(snapshot),
            "encoded payload leaked mutable storage");

        byte[] trailing = java.util.Arrays.copyOf(first.payload(), first.payload().length + 1);
        boolean rejected = false;
        try {
            NativeSnapshotCodec.decode(trailing);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "trailing payload bytes were accepted");

        if (args.length == 1) {
            Files.write(Path.of(args[0]), first.payload());
        }

        System.out.println(
            "NativeSnapshotCodecSelfTest: PASS "
                + "(schema, epoch, four domains, fingerprint, defensive copy)"
        );
    }

    private static SubjectIdentity subject(
        UUID uuid, String type, String runtimeClass, boolean player
    ) {
        return new SubjectIdentity(
            uuid, type, runtimeClass, "minecraft:overworld",
            1.25D, 64.0D, -3.5D, player
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
