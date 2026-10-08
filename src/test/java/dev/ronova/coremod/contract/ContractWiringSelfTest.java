package dev.ronova.coremod.contract;

import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Ensures the contract is an execution boundary rather than unused model code. */
public final class ContractWiringSelfTest {
    private static final String CORE = "dev/ronova/coremod/core/LocalDominionCore";
    private static final String ADAPTER =
        "dev/ronova/coremod/adapter/MinecraftSubjectAdapter";

    private ContractWiringSelfTest() {}

    public static void main(String[] args) throws Exception {
        requireCalls("dev/ronova/coremod/engine/AuthorityDecisionResolver", ADAPTER, "from", 1);
        requireCalls(
            "dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime", ADAPTER, "from", 2
        );
        requireCalls("dev/ronova/coremod/engine/JudgmentExecutor", CORE, "decide", 1);
        requireCalls("dev/ronova/coremod/engine/JudgmentExecutor", ADAPTER, "from", 1);
        requireCalls("dev/ronova/coremod/engine/ServerQuarantineView", ADAPTER, "from", 1);
        requireCalls("dev/ronova/coremod/engine/ClientQuarantineView", ADAPTER, "from", 1);
        requireNoCalls("dev/ronova/coremod/engine/AuthorityDecisionResolver", CORE);
        requireNoCalls("dev/ronova/coremod/engine/spawn/SpawnAdmissionRuntime", CORE);
        requireNoCalls("dev/ronova/coremod/projection/AdmissionProjection", CORE);
        requireNoCalls("dev/ronova/coremod/projection/PresenceProjection", CORE);
        requireNoCalls("dev/ronova/coremod/presence/PresencePolicy", CORE);
        requireNoCalls("dev/ronova/coremod/presence/OwnerImmunity", CORE);
        System.out.println(
            "ContractWiringSelfTest: PASS "
                + "(projected Authority/Admission/Presence, Root Judgment)"
        );
    }

    private static void requireNoCalls(String owner, String targetOwner) throws Exception {
        ClassNode node = read(owner);
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && targetOwner.equals(call.owner)) {
                    throw new AssertionError(owner + " must not call " + targetOwner);
                }
            }
        }
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
        try (InputStream input = ContractWiringSelfTest.class.getClassLoader()
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
