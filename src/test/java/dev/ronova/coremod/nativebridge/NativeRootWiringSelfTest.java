package dev.ronova.coremod.nativebridge;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Keeps the R3 loader, JNI protocol, root coordinator and gameplay lanes separated. */
public final class NativeRootWiringSelfTest {
    private NativeRootWiringSelfTest() {}

    public static void main(String[] args) throws Exception {
        requireNoCall(
            "dev/ronova/coremod/nativebridge/NativeBridge",
            "dev/ronova/coremod/nativebridge/NativeSnapshotCodec"
        );
        requireCall(
            "dev/ronova/coremod/nativebridge/NativeRootBridge",
            "dev/ronova/coremod/nativebridge/NativeSnapshotCodec", "encode"
        );
        requireCall(
            "dev/ronova/coremod/nativebridge/NativeDominionRoot",
            "dev/ronova/coremod/core/LocalDominionCore", "install"
        );
        requireCall(
            "dev/ronova/coremod/projection/ProjectionCoordinator",
            "dev/ronova/coremod/nativebridge/NativeDominionRoot", "publish"
        );
        requireCall(
            "dev/ronova/coremod/runtime/DominionServerRuntime",
            "dev/ronova/coremod/projection/ProjectionCoordinator", "reconcileNative"
        );

        Set<String> nativeMethods = java.util.Arrays.stream(NativeRootBridge.class.getDeclaredMethods())
            .filter(method -> Modifier.isNative(method.getModifiers()))
            .peek(method -> require(
                Modifier.isPrivate(method.getModifiers()) && Modifier.isStatic(method.getModifiers()),
                "JNI method must remain private static: " + method.getName()
            ))
            .map(Method::getName)
            .collect(Collectors.toSet());
        require(nativeMethods.equals(Set.of(
            "installSnapshot", "exportSnapshot", "decide0", "capabilities",
            "state", "acknowledge0", "reset0", "bootstrap", "startWatchdog0", "shutdown"
        )), "unexpected Native root JNI surface: " + nativeMethods);
        require(new NativeDominionRoot.Publication(
            NativeDominionRoot.PublishState.FAILED, 0L, "failed"
        ).enforced(), "an installed Root must fail closed on publication failure");
        require(!new NativeDominionRoot.Publication(
            NativeDominionRoot.PublishState.UNAVAILABLE, 0L, "unavailable"
        ).enforced(), "an unavailable Root must preserve Java fallback");
        requireNativeBinary();
        RuntimeArtifactSourceSelfTest.main(new String[0]);

        System.out.println(
            "NativeRootWiringSelfTest: PASS "
                + "(loader isolation, JNI surface, cutover, publish, reconcile)"
        );
    }

    private static void requireNativeBinary() throws Exception {
        String resource = "native/windows-x86_64/ronova_root.dll";
        byte[] binary;
        try (InputStream input = NativeRootWiringSelfTest.class.getClassLoader()
            .getResourceAsStream(resource)) {
            require(input != null, "missing Native root resource: " + resource);
            binary = input.readAllBytes();
        }
        require(binary.length > 64 * 1024, "Native root binary is unexpectedly small");
        require(binary[0] == 'M' && binary[1] == 'Z', "Native root is not a PE binary");
        String symbols = new String(binary, StandardCharsets.ISO_8859_1);
        for (String method : Set.of(
            "installSnapshot", "exportSnapshot", "decide0", "capabilities",
            "state", "acknowledge0", "reset0", "bootstrap", "startWatchdog0", "shutdown"
        )) {
            require(symbols.contains(
                "Java_dev_ronova_coremod_nativebridge_NativeRootBridge_" + method
            ), "Native root is missing JNI export: " + method);
        }
    }

    private static void requireCall(String owner, String target, String name) throws Exception {
        require(countCalls(owner, target, name) > 0,
            owner + " must call " + target + '.' + name);
    }

    private static void requireNoCall(String owner, String target) throws Exception {
        ClassNode node = read(owner);
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && target.equals(call.owner)) {
                    throw new AssertionError(owner + " leaked dependency on " + target);
                }
            }
        }
    }

    private static int countCalls(String owner, String target, String name) throws Exception {
        int count = 0;
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && target.equals(call.owner) && name.equals(call.name)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = NativeRootWiringSelfTest.class.getClassLoader()
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
