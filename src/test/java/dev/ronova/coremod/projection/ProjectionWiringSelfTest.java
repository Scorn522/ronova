package dev.ronova.coremod.projection;

import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Prevents R2 projections from becoming unused parallel model code. */
public final class ProjectionWiringSelfTest {
    private ProjectionWiringSelfTest() {}

    public static void main(String[] args) throws Exception {
        requireCalls(
            "dev/ronova/coremod/engine/AuthorityDecisionResolver",
            "dev/ronova/coremod/bootstrap/ServerRuntimeRegistry", "runtime", 1
        );
        requireCalls(
            "dev/ronova/coremod/engine/SpawnFirewall",
            "dev/ronova/coremod/bootstrap/ServerRuntimeRegistry", "runtime", 3
        );
        requireCalls(
            "dev/ronova/coremod/engine/JudgmentEngine",
            "dev/ronova/coremod/engine/TargetDiscovery", "ray", 1
        );
        requireCalls(
            "dev/ronova/coremod/engine/JudgmentEngine",
            "dev/ronova/coremod/engine/TargetDiscovery", "area", 1
        );
        requireCalls(
            "dev/ronova/coremod/engine/JudgmentEngine",
            "dev/ronova/coremod/engine/TargetDiscovery", "loaded", 1
        );
        requireCalls(
            "dev/ronova/coremod/engine/JudgmentEngine",
            "dev/ronova/coremod/engine/JudgmentExecutor", "prepare", 1
        );
        requireCalls(
            "dev/ronova/coremod/engine/JudgmentEngine",
            "dev/ronova/coremod/engine/JudgmentExecutor", "applyCommitted", 1
        );
        requireCalls(
            "dev/ronova/coremod/engine/JudgmentExecutor",
            "dev/ronova/coremod/root/api/CommittedJudgmentGate", "runQualified", 1
        );
        requireCalls(
            "dev/ronova/coremod/runtime/RootCommandBus",
            "dev/ronova/coremod/projection/ProjectionCoordinator", "publish", 2
        );
        requireCalls(
            "dev/ronova/coremod/network/ServerActionService",
            "dev/ronova/coremod/bootstrap/ServerRuntimeRegistry", "runtime", 1
        );
        requireCalls(
            "dev/ronova/coremod/network/QuarantineSyncService",
            "dev/ronova/coremod/bootstrap/ServerRuntimeRegistry", "runtime", 1
        );
        System.out.println(
            "ProjectionWiringSelfTest: PASS "
                + "(Authority, Capability, Admission, Presence, Discovery, Execution)"
        );
    }

    private static void requireCalls(
        String owner, String targetOwner, String targetName, int minimum
    ) throws Exception {
        ClassNode node = read(owner);
        int count = 0;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && targetOwner.equals(call.owner) && targetName.equals(call.name)) {
                    count++;
                }
            }
        }
        require(count >= minimum,
            owner + " must call " + targetOwner + '.' + targetName
                + " at least " + minimum + " time(s), found " + count);
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = ProjectionWiringSelfTest.class.getClassLoader()
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
