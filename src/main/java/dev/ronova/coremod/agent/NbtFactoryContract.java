package dev.ronova.coremod.agent;

import java.util.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** The registered static NBT factory seam. The original Mixin remains an independent fallback. */
final class NbtFactoryContract {
    static final String OWNER = "net/minecraft/world/entity/EntityType";
    private static final String DESC = "(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;";
    private static final String HOOKS = "dev/ronova/coremod/agent/BootstrapHooks";
    private static boolean target(MethodNode method) {
        return Set.of("create", "m_20642_").contains(method.name) && method.desc.equals(DESC)
            && (method.access & Opcodes.ACC_STATIC) != 0
            && (method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0;
    }
    static boolean patch(ClassNode node) {
        if (!node.name.equals(OWNER)) return false;
        boolean changed = false;
        for (MethodNode method : node.methods) if (target(method) && !guarded(method)) {
            LabelNode body = new LabelNode(); InsnList guard = new InsnList();
            guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
            guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
            guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "rejectSerializedSpawn",
                "(Ljava/lang/Object;Ljava/lang/Object;)Z", false));
            guard.add(new JumpInsnNode(Opcodes.IFEQ, body));
            guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false));
            guard.add(new InsnNode(Opcodes.ARETURN)); guard.add(body);
            guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            method.instructions.insert(guard); changed = true;
        }
        return changed;
    }
    static boolean verify(ClassNode node) {
        int count = 0;
        for (MethodNode method : node.methods) if (target(method)) {
            if (!guarded(method)) return false; count++;
        }
        return count == 1;
    }
    private static boolean guarded(MethodNode method) {
        List<AbstractInsnNode> code = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) if (instruction.getOpcode() >= 0) code.add(instruction);
        if (code.size() < 7 || !load(code.get(0), 1) || !load(code.get(1), 0)
            || !call(code.get(2), HOOKS, "rejectSerializedSpawn", "(Ljava/lang/Object;Ljava/lang/Object;)Z")
            || !(code.get(3) instanceof JumpInsnNode jump) || jump.getOpcode() != Opcodes.IFEQ
            || !call(code.get(4), "java/util/Optional", "empty", "()Ljava/util/Optional;")
            || code.get(5).getOpcode() != Opcodes.ARETURN) return false;
        // The false branch must enter the original body immediately after the denial return.
        boolean found = false;
        for (var cursor = code.get(5).getNext(); cursor != null && cursor.getOpcode() < 0; cursor = cursor.getNext())
            if (cursor == jump.label) found = true;
        if (!found) return false;
        int end = method.instructions.indexOf(code.get(5));
        for (var handler : method.tryCatchBlocks) if (method.instructions.indexOf(handler.start) <= end
            || method.instructions.indexOf(handler.handler) <= end) return false;
        for (var instruction : method.instructions) {
            if (instruction == jump) continue;
            if (instruction instanceof JumpInsnNode branch && method.instructions.indexOf(branch.label) <= end) return false;
            if (instruction instanceof TableSwitchInsnNode table) {
                if (method.instructions.indexOf(table.dflt) <= end) return false;
                for (var label : table.labels) if (method.instructions.indexOf(label) <= end) return false;
            }
            if (instruction instanceof LookupSwitchInsnNode lookup) {
                if (method.instructions.indexOf(lookup.dflt) <= end) return false;
                for (var label : lookup.labels) if (method.instructions.indexOf(label) <= end) return false;
            }
        }
        return true;
    }
    private static boolean load(AbstractInsnNode node, int slot) {
        return node instanceof VarInsnNode value && value.getOpcode() == Opcodes.ALOAD && value.var == slot;
    }
    private static boolean call(AbstractInsnNode node, String owner, String name, String descriptor) {
        return node instanceof MethodInsnNode value && value.getOpcode() == Opcodes.INVOKESTATIC
            && !value.itf && value.owner.equals(owner) && value.name.equals(name) && value.desc.equals(descriptor);
    }
}
