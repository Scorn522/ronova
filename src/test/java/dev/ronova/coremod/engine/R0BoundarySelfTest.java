package dev.ronova.coremod.engine;

import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Structural regression checks for durable Authority and revocation boundaries. */
public final class R0BoundarySelfTest {
    private static final String BINDER = "dev/ronova/coremod/authority/AuthorityBinder";
    private static final String AUTHORITY_VIEW = "dev/ronova/coremod/authority/AuthorityView";
    private static final String ACTION_SERVICE = "dev/ronova/coremod/network/ServerActionService";
    private static final String COMMAND_BUS = "dev/ronova/coremod/runtime/RootCommandBus";
    private static final String STATE = "dev/ronova/coremod/engine/JudgmentState";
    private static final String QUARANTINE = "dev/ronova/coremod/engine/QuarantineView";

    private R0BoundarySelfTest() {}

    public static void main(String[] args) throws Exception {
        ClassNode blade = read("dev/ronova/coremod/content/RonovaBladeItem");
        require(countCalls(blade, BINDER, "restore") == 1,
            "the blade must enter only the explicit restore boundary");

        ClassNode binder = read(BINDER);
        MethodNode restore = method(binder, "restore");
        MethodNode grant = method(binder, "grant");
        MethodNode unbind = method(binder, "unbind");
        require(countCalls(restore, COMMAND_BUS, "commit") == 0,
            "runtime restore must never mutate durable ownership");
        require(countCalls(grant, COMMAND_BUS, "commit") == 1,
            "administrative grant must use the unified Root mutation boundary");
        require(countCalls(unbind, COMMAND_BUS, "commit") == 1,
            "administrative unbind must use the unified Root mutation boundary");

        ClassNode network = read(ACTION_SERVICE);
        require(countCalls(network, BINDER, null) == 0,
            "network actions must not bind, restore, or clear Authority state");
        require(countCalls(network, "dev/ronova/coremod/engine/AuthorityGuard", null) == 0,
            "network actions must not re-enter the compatibility facade");
        require(countCalls(method(network, "hasAuthority"), AUTHORITY_VIEW, "hasCapability") == 1,
            "network actions must validate capabilities through AuthorityView");
        require(countCalls(method(network, "executeTargeted"), ACTION_SERVICE, "hasAuthority") == 1,
            "targeted actions must use the pure Authority gate");
        require(countCalls(method(network, "executeBlockBreak"), ACTION_SERVICE, "hasAuthority") == 1,
            "block breaking must use the pure Authority gate");
        require(countCalls(method(network, "canExecute"), ACTION_SERVICE, "hasAuthority") == 1,
            "sequenced actions must use the pure Authority gate");

        ClassNode firewall = read("dev/ronova/coremod/engine/SpawnFirewall");
        require(countCalls(firewall, STATE, "clearIdentity") == 1,
            "UUID revocation must clear the terminal identity state");
        require(countCalls(firewall, STATE, "clearServer") >= 1,
            "full revocation must clear terminal server state");

        ClassNode presence = read("dev/ronova/coremod/network/RonovaPackets$QuarantineSync");
        require(countCalls(presence,"dev/ronova/coremod/client/ClientRuntime","acceptProjection")==1,
            "network projection must enter the context/revision assembler");
        require(countCalls(presence,QUARANTINE,null)==0,"packet bypassed atomic projection assembly");
        require(countCalls(read("dev/ronova/coremod/client/ClientRuntime"),QUARANTINE,"replaceClientMarks")==1,
            "committed projection must publish through one atomic replacement");
        require(java.util.Set.of(dev.ronova.coremod.network.ProjectionFrame.Mode.values()).equals(java.util.Set.of(
            dev.ronova.coremod.network.ProjectionFrame.Mode.APPEND,dev.ronova.coremod.network.ProjectionFrame.Mode.REPLACE,
            dev.ronova.coremod.network.ProjectionFrame.Mode.REMOVE,dev.ronova.coremod.network.ProjectionFrame.Mode.CLEAR)),
            "versioned projection lost an original update/revocation operation");

        System.out.println("R0BoundarySelfTest: PASS");
    }

    private static ClassNode read(String internalName) throws Exception {
        try (InputStream input = R0BoundarySelfTest.class.getClassLoader()
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
            count += countCalls(method, owner, name);
        }
        return count;
    }

    private static int countCalls(MethodNode method, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode invocation
                && owner.equals(invocation.owner)
                && (name == null || name.equals(invocation.name))) {
                count++;
            }
        }
        return count;
    }

    private static MethodNode method(ClassNode node, String name) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name)) {
                return method;
            }
        }
        throw new AssertionError("missing method " + node.name + "." + name);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
