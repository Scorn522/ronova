package dev.ronova.coremod.network;

import java.io.InputStream;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Guards the exact Forge 47.4.22 SimpleChannel registration JVM descriptor. */
public final class ForgeNetworkAbiSelfTest {
    private static final String CHANNEL =
        "net/minecraftforge/network/simple/SimpleChannel";
    private static final String CONTEXT = "net/minecraftforge/network/NetworkEvent$Context";
    private static final String DISTRIBUTOR = "net/minecraftforge/network/PacketDistributor";
    private static final String REGISTER_DESCRIPTOR =
        "(ILjava/lang/Class;Ljava/util/function/BiConsumer;"
            + "Ljava/util/function/Function;Ljava/util/function/BiConsumer;"
            + "Ljava/util/Optional;)"
            + "Lnet/minecraftforge/network/simple/IndexedMessageCodec$MessageHandler;";
    private static final String ENQUEUE_DESCRIPTOR =
        "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;";
    private static final String SEND_DESCRIPTOR =
        "(Lnet/minecraftforge/network/PacketDistributor$PacketTarget;"
            + "Ljava/lang/Object;)V";
    private static final String SEND_TO_SERVER_DESCRIPTOR = "(Ljava/lang/Object;)V";
    private static final String TARGET_DESCRIPTOR =
        "(Ljava/util/function/Supplier;)"
            + "Lnet/minecraftforge/network/PacketDistributor$PacketTarget;";
    private static final List<String> PACKETS = List.of(
        "RonovaPackets$ActionRequest", "RonovaPackets$OwnerState", "RonovaPackets$JudgmentFeedback", "RonovaPackets$QuarantineSync",
        "RonovaPackets$BlockBreakRequest", "RonovaPackets$ProjectionResyncRequest",
        "StrengthControlPackets$RequestPacket", "StrengthControlPackets$SnapshotPacket", "ProjectionAcknowledgement"
    );

    private ForgeNetworkAbiSelfTest() {}

    public static void main(String[] args) throws Exception {
        String resource = "dev/ronova/coremod/network/RonovaChannel.class";
        try (InputStream input = ForgeNetworkAbiSelfTest.class.getClassLoader()
            .getResourceAsStream(resource)) {
            require(input != null, "missing production RonovaChannel.class");
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);

            int registrations = 0;
            int sends = 0;
            int serverSends = 0;
            int targets = 0;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (!(instruction instanceof MethodInsnNode invocation)) {
                        continue;
                    }
                    if (CHANNEL.equals(invocation.owner)
                        && "registerMessage".equals(invocation.name)) {
                        registrations++;
                        require(REGISTER_DESCRIPTOR.equals(invocation.desc),
                            "wrong SimpleChannel.registerMessage descriptor: "
                                + invocation.desc);
                    } else if (CHANNEL.equals(invocation.owner)
                        && "send".equals(invocation.name)) {
                        sends++;
                        require(SEND_DESCRIPTOR.equals(invocation.desc),
                            "wrong SimpleChannel.send descriptor: " + invocation.desc);
                    } else if (CHANNEL.equals(invocation.owner)
                        && "sendToServer".equals(invocation.name)) {
                        serverSends++;
                        require(SEND_TO_SERVER_DESCRIPTOR.equals(invocation.desc),
                            "wrong SimpleChannel.sendToServer descriptor: "
                                + invocation.desc);
                    } else if (DISTRIBUTOR.equals(invocation.owner)
                        && "with".equals(invocation.name)) {
                        targets++;
                        require(TARGET_DESCRIPTOR.equals(invocation.desc),
                            "wrong PacketDistributor.with descriptor: "
                                + invocation.desc);
                    }
                }
            }
            require(registrations == 9,
                "expected 9 packet registrations, found " + registrations);
            require(sends == 1,
                "expected one centralized server-to-client send, found " + sends);
            require(serverSends == 1,
                "expected one centralized client-to-server send, found " + serverSends);
            require(targets == sends,
                "expected one packet target per server-to-client send, found " + targets);
        }

        int enqueues = 0;
        for (String packet : PACKETS) {
            String packetResource = "dev/ronova/coremod/network/" + packet + ".class";
            try (InputStream input = ForgeNetworkAbiSelfTest.class.getClassLoader()
                .getResourceAsStream(packetResource)) {
                require(input != null, "missing packet class: " + packet);
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, 0);
                for (MethodNode method : node.methods) {
                    for (AbstractInsnNode instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode invocation
                            && CONTEXT.equals(invocation.owner)
                            && "enqueueWork".equals(invocation.name)) {
                            enqueues++;
                            require(ENQUEUE_DESCRIPTOR.equals(invocation.desc),
                                "wrong NetworkEvent.Context.enqueueWork descriptor in "
                                    + packet + ": " + invocation.desc);
                        }
                    }
                }
            }
        }
        require(enqueues == PACKETS.size(),
            "expected " + PACKETS.size() + " packet enqueues, found " + enqueues);
        System.out.println(
            "ForgeNetworkAbiSelfTest: PASS "
                + "(9 registrations, 9 handlers, 2 centralized sends, 1 target)"
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
