package dev.ronova.coremod.agent;

import java.util.ArrayList;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Registered leaf read used by the independent terminal health write path. */
final class EntityDataAccessorContract {
    static final String OWNER = "net/minecraft/network/syncher/EntityDataAccessor";
    private static final Set<String> FIELDS = Set.of("id", "f_135010_");
    private static final Set<String> METHODS = Set.of("getId", "m_135015_");
    private static final int UNSUPPORTED = Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT
        | Opcodes.ACC_NATIVE | Opcodes.ACC_SYNCHRONIZED;
    private EntityDataAccessorContract() {}

    static boolean verify(ClassNode node) {
        if (!OWNER.equals(node.name)) return false;
        FieldNode field = field(node); MethodNode method = getter(node);
        return field != null && method != null && exact(method, field);
    }
    static boolean repair(ClassNode node) {
        if (!OWNER.equals(node.name)) return false;
        FieldNode field = field(node); MethodNode method = getter(node);
        if (field == null || method == null || (method.access & UNSUPPORTED) != 0
            || (method.access & Opcodes.ACC_PUBLIC) == 0 || exact(method, field)) return false;
        var code = new InsnList();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new FieldInsnNode(Opcodes.GETFIELD, OWNER, field.name, "I"));
        code.add(new InsnNode(Opcodes.IRETURN));
        method.instructions = code; method.tryCatchBlocks.clear(); method.localVariables = null;
        method.visibleLocalVariableAnnotations = null; method.invisibleLocalVariableAnnotations = null;
        method.maxLocals = 1; method.maxStack = 1; return true;
    }
    private static FieldNode field(ClassNode node) {
        FieldNode found = null;
        for (var field : node.fields) if (FIELDS.contains(field.name)) {
            if (found != null || !field.desc.equals("I") || (field.access & Opcodes.ACC_STATIC) != 0) return null;
            found = field;
        }
        return found;
    }
    private static MethodNode getter(ClassNode node) {
        MethodNode found = null;
        for (var method : node.methods) if (METHODS.contains(method.name) && method.desc.equals("()I")) {
            if (found != null) return null; found = method;
        }
        return found;
    }
    private static boolean exact(MethodNode method, FieldNode field) {
        if ((method.access & UNSUPPORTED) != 0 || (method.access & Opcodes.ACC_PUBLIC) == 0
            || !method.tryCatchBlocks.isEmpty()) return false;
        var code = new ArrayList<AbstractInsnNode>();
        for (var instruction : method.instructions) if (instruction.getOpcode() >= 0) code.add(instruction);
        return code.size() == 3
            && code.get(0) instanceof VarInsnNode receiver && receiver.getOpcode() == Opcodes.ALOAD && receiver.var == 0
            && code.get(1) instanceof FieldInsnNode read && read.getOpcode() == Opcodes.GETFIELD
            && read.owner.equals(OWNER) && read.name.equals(field.name) && read.desc.equals("I")
            && code.get(2).getOpcode() == Opcodes.IRETURN;
    }
}
