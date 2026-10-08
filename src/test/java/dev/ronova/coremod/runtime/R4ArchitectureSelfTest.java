package dev.ronova.coremod.runtime;

import dev.ronova.coremod.integrity.EnforcementProofSource;
import dev.ronova.coremod.nativebridge.NativeIntegrityBridge;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** Freezes the R4 semantic lease boundary and Native binary contract. */
public final class R4ArchitectureSelfTest {
    private R4ArchitectureSelfTest() {}

    public static void main(String[] args) throws Exception {
        requireConstruction(
            "dev/ronova/coremod/bootstrap/DominionCompositionRoot",
            "dev/ronova/coremod/integrity/EnforcementRuntime"
        );
        requireCall(
            "dev/ronova/coremod/runtime/RuntimeCoordinator",
            "dev/ronova/coremod/integrity/EnforcementRuntime", "pulse"
        );
        for (String hotOwner : Set.of(
            "dev/ronova/coremod/engine/AuthorityGuard",
            "dev/ronova/coremod/engine/JudgmentExecutor",
            "dev/ronova/coremod/engine/SpawnFirewall",
            "dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime"
        )) {
            requireNoCallPrefix(hotOwner, "dev/ronova/coremod/nativebridge/NativeIntegrityBridge");
        }
        require(EnforcementProofSource.class.isAssignableFrom(NativeIntegrityBridge.class),
            "Native integrity adapter must implement the proof-source boundary");
        Method state = NativeIntegrityBridge.class.getDeclaredMethod("state0");
        require(Modifier.isPrivate(state.getModifiers()) && Modifier.isStatic(state.getModifiers())
                && Modifier.isNative(state.getModifiers()),
            "semantic lease JNI surface must remain one private static method");
        require("14".equals(dev.ronova.coremod.network.RonovaChannel.PROTOCOL),
            "current strength and projection contracts require network protocol 14");
        verifyNativeBinary();
        System.out.println(
            "R4ArchitectureSelfTest: PASS "
                + "(semantic lease boundary, cold JNI projection, Native markers)"
        );
    }

    private static void verifyNativeBinary() throws Exception {
        String resource = "native/windows-x86_64/ronova.dll";
        byte[] binary;
        try (InputStream input = R4ArchitectureSelfTest.class.getClassLoader()
            .getResourceAsStream(resource)) {
            require(input != null, "missing Native integrity resource");
            binary = input.readAllBytes();
        }
        require(binary.length > 64 * 1024 && binary[0] == 'M' && binary[1] == 'Z',
            "Native integrity resource is not a Windows x64 PE candidate");
        String symbols = new String(binary, StandardCharsets.ISO_8859_1);
        require(symbols.contains(
            "Java_dev_ronova_coremod_nativebridge_NativeIntegrityBridge_state0"
        ), "Native semantic status export is missing");
        for (String marker : Set.of(
            "(Ljava/lang/String;[BLjava/lang/ClassLoader;)Z",
            "dev/ronova/coremod/agent/NativeTransformBridge",
            "net/minecraft/world/entity/Entity",
            "net/minecraft/world/entity/LivingEntity",
            "net/minecraft/world/level/Level",
            "net/minecraft/server/level/ServerLevel",
            "net/minecraft/client/multiplayer/ClientLevel"
        )) {
            require(symbols.contains(marker), "Native semantic marker missing: " + marker);
        }
    }

    private static void requireConstruction(String owner, String target) throws Exception {
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof TypeInsnNode type
                    && type.getOpcode() == org.objectweb.asm.Opcodes.NEW
                    && target.equals(type.desc)) return;
            }
        }
        throw new AssertionError(owner + " must construct " + target);
    }

    private static void requireCall(String owner, String target, String name) throws Exception {
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && target.equals(call.owner) && name.equals(call.name)) return;
            }
        }
        throw new AssertionError(owner + " must call " + target + '.' + name);
    }

    private static void requireNoCallPrefix(String owner, String targetPrefix) throws Exception {
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && call.owner.startsWith(targetPrefix)) {
                    throw new AssertionError(owner + " leaked cold JNI dependency on " + call.owner);
                }
            }
        }
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = R4ArchitectureSelfTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            require(input != null, "missing production class: " + internalName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);
            return node;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
