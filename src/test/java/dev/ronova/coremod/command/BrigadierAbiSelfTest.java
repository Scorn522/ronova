package dev.ronova.coremod.command;

import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Guards the exact Brigadier command-registration JVM descriptor. */
public final class BrigadierAbiSelfTest {
    private static final String DISPATCHER = "com/mojang/brigadier/CommandDispatcher";
    private static final String REGISTER_DESCRIPTOR =
        "(Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;)"
            + "Lcom/mojang/brigadier/tree/LiteralCommandNode;";

    private BrigadierAbiSelfTest() {}

    public static void main(String[] args) throws Exception {
        String resource = "dev/ronova/coremod/command/DominionCommands.class";
        try (InputStream input = BrigadierAbiSelfTest.class.getClassLoader()
            .getResourceAsStream(resource)) {
            require(input != null, "missing production DominionCommands.class");
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);

            int registrations = 0;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode invocation
                        && DISPATCHER.equals(invocation.owner)
                        && "register".equals(invocation.name)) {
                        registrations++;
                        require(REGISTER_DESCRIPTOR.equals(invocation.desc),
                            "wrong CommandDispatcher.register descriptor: "
                                + invocation.desc);
                    }
                }
            }
            require(registrations == 1,
                "expected one root command registration, found " + registrations);
        }
        System.out.println("BrigadierAbiSelfTest: PASS (1 registration)");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
