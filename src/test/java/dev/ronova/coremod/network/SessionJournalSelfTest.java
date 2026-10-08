package dev.ronova.coremod.network;

import dev.ronova.coremod.contract.RealmId;
import dev.ronova.coremod.root.api.JudgmentBatch;
import java.io.InputStream;
import java.util.UUID;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Standalone checks for bounded multi-session request idempotence state. */
public final class SessionJournalSelfTest {
    private static final String ACTION_ID =
        "dev/ronova/coremod/network/ActionCommandIdentity";
    private static final String ACTION_SERVICE =
        "dev/ronova/coremod/network/ServerActionService";
    private static final String ENGINE = "dev/ronova/coremod/engine/JudgmentEngine";
    private static final String REPORT =
        "dev/ronova/coremod/engine/JudgmentEngine$JudgmentReport";
    private static final String ROOT_BUS = "dev/ronova/coremod/runtime/RootCommandBus";
    private static final String BATCH = "dev/ronova/coremod/root/api/JudgmentBatch";

    private SessionJournalSelfTest() {}

    public static void main(String[] args) throws Exception {
        SessionJournal<String> journal = new SessionJournal<>(2);
        journal.put(11L, "first");
        journal.put(12L, "second");
        require("first".equals(journal.get(11L)), "existing session receipt must replay");
        journal.put(13L, "third");
        require(journal.get(12L) == null, "least-recently-used session must be evicted");
        require("first".equals(journal.get(11L)), "recently accessed session must remain");
        require("third".equals(journal.get(13L)), "new session must remain");
        journal.put(13L, "replacement");
        require("replacement".equals(journal.get(13L)), "same-session receipt must update");
        verifiesDurableActionIdentity();
        verifiesRealmFencedProtocol();
        verifiesDurableIngressWiring();
        System.out.println(
            "SessionJournalSelfTest: PASS "
                + "(bounded sessions, stable restart identity, protocol-9 Realm fencing)"
        );
    }

    private static void verifiesDurableActionIdentity() {
        RealmId realm = new RealmId(UUID.fromString(
            "11111111-2222-3333-4444-555555555555"
        ));
        UUID player = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        UUID expected = UUID.fromString("0e2158c9-cc2a-842b-bb84-03c6f5070fc1");
        UUID first = ActionCommandIdentity.derive(
            realm, player, 77L, 9L
        );
        UUID replay = ActionCommandIdentity.derive(
            realm, player, 77L, 9L
        );
        require(first.equals(expected) && replay.equals(expected),
            "same client action did not retain its canonical Root command id");
        require(first.version() == 8 && first.variant() == 2,
            "action identity is not an RFC-variant UUIDv8 value");
        require(!first.equals(ActionCommandIdentity.derive(
            realm, player, 77L, 10L
        )), "sequence was omitted from the durable action identity");
        require(!first.equals(ActionCommandIdentity.derive(
            realm, new UUID(1L, 2L), 77L, 9L
        )), "player was omitted from the durable action identity");
        require(!first.equals(ActionCommandIdentity.derive(
            new RealmId(new UUID(3L, 4L)), player, 77L, 9L
        )), "RealmId was omitted from the durable action identity");
        String areaHash = JudgmentBatch.empty(
            first, ActionCommandIdentity.commandDomain(RonovaAction.AREA)
        ).canonicalPayloadHash();
        String primaryHash = JudgmentBatch.empty(
            first, ActionCommandIdentity.commandDomain(RonovaAction.PRIMARY)
        ).canonicalPayloadHash();
        require(!areaHash.equals(primaryHash),
            "action kind was omitted from the immutable command payload");
    }

    private static void verifiesDurableIngressWiring() throws Exception {
        ClassNode service = read(ACTION_SERVICE);
        require(countCalls(service, ACTION_ID, "derive") == 1,
            "sequenced ingress does not derive exactly one stable Root command id");
        require(countCalls(service, ACTION_ID, "matches") == 1,
            "recovered ingress does not reject a changed action domain");
        require(countCalls(service, ROOT_BUS, "recover") >= 1,
            "sequenced ingress rediscovered targets before durable receipt recovery");
        require(countCalls(service, ENGINE, "reportFromReceipt") == 1,
            "durable ingress recovery does not reconstruct its original result");
        require(countCalls(service, REPORT, "resolved") >= 3,
            "an unresolved Root commit can still reach a client acknowledgement path");

        ClassNode engine = read(ENGINE);
        require(countCalls(engine, BATCH, "empty") >= 1,
            "an authorized zero-target action has no durable no-op receipt");
    }

    private static void verifiesRealmFencedProtocol() throws Exception {
        require("14".equals(RonovaChannel.PROTOCOL),
            "Realm-fenced action replay requires protocol 10");
        require(RonovaPackets.ActionRequest.class.getRecordComponents().length == 4
                && RonovaPackets.ActionRequest.class.getRecordComponents()[0]
                    .getType() == UUID.class
                && "realmId".equals(
                    RonovaPackets.ActionRequest.class.getRecordComponents()[0].getName()
                ),
            "ActionRequest does not carry its expected Realm identity");
        require(RonovaPackets.OwnerState.class.getRecordComponents().length == 6
                && RonovaPackets.OwnerState.class.getRecordComponents()[2]
                    .getType() == UUID.class
                && "realmId".equals(
                    RonovaPackets.OwnerState.class.getRecordComponents()[2].getName()
                ),
            "OwnerState does not publish the server Realm identity");
        require(RonovaPackets.OwnerState.class.getRecordComponents()[3].getType()==UUID.class
            &&"streamId".equals(RonovaPackets.OwnerState.class.getRecordComponents()[3].getName())
            &&"contextRevision".equals(RonovaPackets.OwnerState.class.getRecordComponents()[4].getName())
            &&"ownerRevision".equals(RonovaPackets.OwnerState.class.getRecordComponents()[5].getName()),
            "OwnerState lacks connection stream and authorization revisions");
        ClassNode service = read(ACTION_SERVICE);
        require(countCalls(
            service,
            "dev/ronova/coremod/network/RonovaPackets$ActionRequest",
            "realmId"
        ) >= 2, "server ingress does not validate and consume request RealmId");
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = SessionJournalSelfTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            require(input != null, "missing production class: " + internalName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static int countCalls(ClassNode node, String owner, String name) {
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && owner.equals(call.owner) && name.equals(call.name)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
