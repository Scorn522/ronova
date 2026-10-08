package dev.ronova.coremod.network;

import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Guards the erased SAM descriptor used by FriendlyByteBuf.Writer lambdas. */
public final class FriendlyByteBufLambdaAbiSelfTest {
    private static final String CLASS_RESOURCE =
        "dev/ronova/coremod/network/RonovaPackets$QuarantineSync.class";
    private static final String WRITER_FACTORY_DESCRIPTOR =
        "()Lnet/minecraft/network/FriendlyByteBuf$Writer;";
    private static final String WRITER_SAM_DESCRIPTOR =
        "(Ljava/lang/Object;Ljava/lang/Object;)V";
    private static final String WRITE_MARK_DESCRIPTOR =
        "(Lnet/minecraft/network/FriendlyByteBuf;"
            + "Ldev/ronova/coremod/engine/JudgmentMark;)V";

    private FriendlyByteBufLambdaAbiSelfTest() {}

    public static void main(String[] args) throws Exception {
        try (InputStream input = FriendlyByteBufLambdaAbiSelfTest.class.getClassLoader()
            .getResourceAsStream(CLASS_RESOURCE)) {
            require(input != null, "missing QuarantineSync production class");
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);

            int writerFactories = 0;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (!(instruction instanceof InvokeDynamicInsnNode dynamic)
                        || !"accept".equals(dynamic.name)
                        || !WRITER_FACTORY_DESCRIPTOR.equals(dynamic.desc)) {
                        continue;
                    }
                    writerFactories++;
                    require(dynamic.bsmArgs.length == 3,
                        "unexpected LambdaMetafactory argument count: "
                            + dynamic.bsmArgs.length);
                    require(dynamic.bsmArgs[0] instanceof Type,
                        "writer SAM argument is not a method type");
                    Type sam = (Type) dynamic.bsmArgs[0];
                    require(WRITER_SAM_DESCRIPTOR.equals(sam.getDescriptor()),
                        "wrong FriendlyByteBuf.Writer erased SAM descriptor: "
                            + sam.getDescriptor());
                    require(dynamic.bsmArgs[1] instanceof Handle,
                        "writer implementation argument is not a method handle");
                    Handle implementation = (Handle) dynamic.bsmArgs[1];
                    require("dev/ronova/coremod/network/RonovaPackets".equals(
                            implementation.getOwner())
                            && "writeMark".equals(implementation.getName())
                            && WRITE_MARK_DESCRIPTOR.equals(implementation.getDesc()),
                        "unexpected writer implementation handle: " + implementation);
                    require(dynamic.bsmArgs[2] instanceof Type,
                        "writer instantiated argument is not a method type");
                    Type instantiated = (Type) dynamic.bsmArgs[2];
                    require(WRITE_MARK_DESCRIPTOR.equals(instantiated.getDescriptor()),
                        "wrong instantiated writer descriptor: "
                            + instantiated.getDescriptor());
                }
            }
            require(writerFactories == 1,
                "expected one FriendlyByteBuf.Writer factory, found " + writerFactories);
        }
        System.out.println(
            "FriendlyByteBufLambdaAbiSelfTest: PASS "
                + "(BiConsumer erasure and writeMark adaptation)"
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
