package dev.ronova.coremod.persistence;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Guards mapped calls and override declarations on a Minecraft subclass. */
public final class InheritedMappedMemberSelfTest {
    private static final String LEDGER =
        "dev/ronova/coremod/persistence/DominionLedger";
    private static final Map<String, Integer> EXPECTED_DIRTY_MARKS = Map.ofEntries(
        Map.entry("<init>", 1),
        Map.entry("load", 1),
        Map.entry("bindOwner", 1),
        Map.entry("unbindOwner", 1),
        Map.entry("recordBatch", 1),
        Map.entry("record", 1),
        Map.entry("appendCommand", 1),
        Map.entry("mirrorFromDurableRoot", 1),
        Map.entry("claimLifecycleIntegration", 1),
        Map.entry("clearTombstones", 1),
        Map.entry("clearTombstone", 1)
    );

    private InheritedMappedMemberSelfTest() {}

    public static void main(String[] args) throws Exception {
        try (InputStream input = InheritedMappedMemberSelfTest.class.getClassLoader()
            .getResourceAsStream(LEDGER + ".class")) {
            require(input != null, "missing production DominionLedger.class");
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, 0);

            Map<String, Integer> dirtyMarks = new LinkedHashMap<>();
            int ownerFieldAccesses = 0;
            int savedDataOverrides = 0;
            for (MethodNode method : node.methods) {
                if ("(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;"
                    .equals(method.desc)) {
                    require(!"save".equals(method.name),
                        "SavedData override declaration retained development name: save");
                    if ("m_7176_".equals(method.name)) {
                        savedDataOverrides++;
                    }
                }
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof FieldInsnNode field
                        && LEDGER.equals(field.owner)
                        && "Ljava/util/Set;".equals(field.desc)) {
                        require("owners".equals(field.name),
                            "application field was globally remapped: "
                                + field.name + " in " + method.name);
                        ownerFieldAccesses++;
                    }
                    if (!(instruction instanceof MethodInsnNode invocation)
                        || !LEDGER.equals(invocation.owner)
                        || !"()V".equals(invocation.desc)) {
                        continue;
                    }
                    if ("setDirty".equals(invocation.name)
                        || "m_77762_".equals(invocation.name)) {
                        require("m_77762_".equals(invocation.name),
                            "SavedData.setDirty was not reobfuscated: "
                                + invocation.name + " in " + method.name);
                        dirtyMarks.merge(method.name, 1, Integer::sum);
                    }
                }
            }
            require(dirtyMarks.equals(EXPECTED_DIRTY_MARKS),
                "unexpected mapped dirty-marker inventory: expected "
                    + EXPECTED_DIRTY_MARKS + ", found " + dirtyMarks);
            require(ownerFieldAccesses >= 7,
                "expected at least seven owner-field accesses, found " + ownerFieldAccesses);
            require(savedDataOverrides == 1,
                "expected one mapped SavedData.save override, found " + savedDataOverrides);
        }
        System.out.println(
            "InheritedMappedMemberSelfTest: PASS "
                + "(1 override declaration, 11 inherited calls across "
                + "11 durable mutation methods, "
                + "application owner fields preserved)"
        );
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
