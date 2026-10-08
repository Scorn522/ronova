package dev.ronova.coremod.runtime;

import dev.ronova.coremod.contract.Capability;
import dev.ronova.coremod.contract.CapabilitySet;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Freezes R3.1 dependency direction, stable capability IDs and facade boundaries. */
public final class R31ArchitectureSelfTest {
    private R31ArchitectureSelfTest() {}

    public static void main(String[] args) throws Exception {
        requireNoCall(
            "dev/ronova/coremod/persistence/DominionLedger",
            "dev/ronova/coremod/core/LocalDominionCore"
        );
        requireNoCall(
            "dev/ronova/coremod/persistence/LocalDominionStore",
            "dev/ronova/coremod/projection/ProjectionCoordinator"
        );
        requireNoCall(
            "dev/ronova/coremod/engine/AuthorityDecisionResolver",
            "dev/ronova/coremod/core/LocalDominionCore"
        );
        requireNoCall(
            "dev/ronova/coremod/engine/SpawnFirewall",
            "dev/ronova/coremod/core/LocalDominionCore"
        );
        requireNoCall(
            "dev/ronova/coremod/projection/PresenceProjection",
            "dev/ronova/coremod/core/LocalDominionCore"
        );
        requireNoCall(
            "dev/ronova/coremod/presence/PresencePolicy",
            "dev/ronova/coremod/core/LocalDominionCore"
        );
        requireNoCall(
            "dev/ronova/coremod/engine/ServerQuarantineView",
            "dev/ronova/coremod/core/LocalDominionCore"
        );
        requireNoCall(
            "dev/ronova/coremod/engine/ClientQuarantineView",
            "dev/ronova/coremod/core/LocalDominionCore"
        );
        requireNoCallName(
            "dev/ronova/coremod/mixin/EntityTypeFactoryAdmissionMixin",
            "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",
            "setReturnValue"
        );
        requireCall(
            "dev/ronova/coremod/runtime/RootCommandBus",
            "dev/ronova/coremod/root/api/DominionRootGateway", "commit"
        );
        requireCall(
            "dev/ronova/coremod/runtime/RootCommandBus",
            "dev/ronova/coremod/projection/ProjectionCoordinator", "publish"
        );
        requireNoOwnerPrefix(
            "dev/ronova/coremod/engine/AuthorityGuard",
            "dev/ronova/coremod/mixin/"
        );

        Set<Integer> ids = new HashSet<>();
        for (Capability capability : Capability.values()) {
            require(ids.add(capability.stableId()), "duplicate capability id");
            require(CapabilitySet.of(capability).mask() == (1L << capability.stableId()),
                "capability bit does not match stable id: " + capability);
        }
        require(CapabilitySet.OWNER_DEFAULT.mask() == 0x7ffL,
            "owner capability mask drifted from alpha.7.10");

        System.out.println(
            "R31ArchitectureSelfTest: PASS "
                + "(one-way dependencies, projected hot paths, silent spawn gate, stable IDs)"
        );
    }

    private static void requireCall(String owner, String target, String name) throws Exception {
        int calls = 0;
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && target.equals(call.owner) && name.equals(call.name)) calls++;
            }
        }
        require(calls > 0, owner + " must call " + target + '.' + name);
    }

    private static void requireNoCall(String owner, String target) throws Exception {
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && target.equals(call.owner)) {
                    throw new AssertionError(owner + " leaked dependency on " + target);
                }
            }
        }
    }

    private static void requireNoCallName(
        String owner, String target, String name
    ) throws Exception {
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && target.equals(call.owner) && name.equals(call.name)) {
                    throw new AssertionError(
                        owner + " leaked forbidden call " + target + '.' + name
                    );
                }
            }
        }
    }

    private static void requireNoOwnerPrefix(String owner, String prefix) throws Exception {
        for (MethodNode method : read(owner).methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call && call.owner.startsWith(prefix)) {
                    throw new AssertionError(owner + " leaked gameplay implementation " + call.owner);
                }
            }
        }
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = R31ArchitectureSelfTest.class.getClassLoader()
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
