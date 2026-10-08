package dev.ronova.coremod.runtime;

import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Freezes the R3.2 spawn, client and network component boundaries. */
public final class R32ArchitectureSelfTest {
    private R32ArchitectureSelfTest() {}

    public static void main(String[] args) throws Exception {
        requireCallPrefix(
            "dev/ronova/coremod/engine/SpawnFirewall",
            "dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime"
        );
        requireNoCallPrefix(
            "dev/ronova/coremod/engine/SpawnFirewall",
            "dev/ronova/coremod/projection/"
        );
        requireNoCallPrefix(
            "dev/ronova/coremod/engine/SpawnFirewall",
            "dev/ronova/coremod/util/"
        );
        requireNoCallPrefix(
            "dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime",
            "dev/ronova/coremod/bootstrap/ServerRuntimeRegistry"
        );
        requireCallPrefix(
            "dev/ronova/coremod/bootstrap/DominionCompositionRoot",
            "dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime"
        );

        requireCallPrefix(
            "dev/ronova/coremod/client/ClientRuntime",
            "dev/ronova/coremod/client/ClientActionController"
        );
        requireCallPrefix(
            "dev/ronova/coremod/client/ClientRuntime",
            "dev/ronova/coremod/client/ClientInputController"
        );
        requireCallPrefix(
            "dev/ronova/coremod/client/ClientRuntime",
            "dev/ronova/coremod/client/ClientRecoveryController"
        );
        requireNoCallPrefix(
            "dev/ronova/coremod/client/ClientRuntime", "org/lwjgl/glfw/GLFW"
        );
        requireNoCallPrefix(
            "dev/ronova/coremod/client/ClientRuntime", "dev/ronova/coremod/mixin/"
        );
        requireNoCallPrefix(
            "dev/ronova/coremod/client/ClientRecoveryController",
            "dev/ronova/coremod/network/"
        );

        requireCallPrefix(
            "dev/ronova/coremod/network/RonovaNetwork",
            "dev/ronova/coremod/network/RonovaChannel"
        );
        requireCallPrefix(
            "dev/ronova/coremod/network/RonovaNetwork",
            "dev/ronova/coremod/network/ServerActionService"
        );
        requireCallPrefix(
            "dev/ronova/coremod/network/RonovaNetwork",
            "dev/ronova/coremod/network/QuarantineSyncService"
        );
        requireNoCallPrefix(
            "dev/ronova/coremod/network/RonovaNetwork",
            "net/minecraftforge/network/simple/SimpleChannel"
        );
        requireNoCallPrefix(
            "dev/ronova/coremod/network/RonovaNetwork",
            "dev/ronova/coremod/engine/JudgmentEngine"
        );
        requireNoCallPrefix(
            "dev/ronova/coremod/network/RonovaChannel",
            "dev/ronova/coremod/engine/"
        );
        require("14".equals(dev.ronova.coremod.network.RonovaChannel.PROTOCOL),
            "current strength and projection contracts require network protocol 14");

        System.out.println(
            "R32ArchitectureSelfTest: PASS "
                + "(composed spawn, client controllers, transport/service split)"
        );
    }

    private static void requireCallPrefix(String owner, String targetPrefix) throws Exception {
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && call.owner.startsWith(targetPrefix)) {
                    return;
                }
            }
        }
        throw new AssertionError(owner + " must delegate to " + targetPrefix);
    }

    private static void requireNoCallPrefix(String owner, String targetPrefix) throws Exception {
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && call.owner.startsWith(targetPrefix)) {
                    throw new AssertionError(owner + " leaked dependency on " + call.owner);
                }
            }
        }
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = R32ArchitectureSelfTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            require(input != null, "missing production class: " + internalName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
