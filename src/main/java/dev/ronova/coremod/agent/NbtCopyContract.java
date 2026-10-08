package dev.ronova.coremod.agent;

import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** One registered Minecraft copy boundary, independent of any third-party producer. */
final class NbtCopyContract {
    static final String OWNER = "net/minecraft/nbt/CompoundTag";
    private static final String HOOKS = "dev/ronova/coremod/agent/BootstrapHooks";
    private static final String DESCRIPTOR = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
    private static final Set<String> NAMES = Set.of("copy", "m_6426_");
    private NbtCopyContract() {}

    static boolean patch(ClassNode node) {
        if (!OWNER.equals(node.name)) return false;
        boolean changed = false;
        for (MethodNode method : node.methods) if (matches(method)) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.ARETURN || guarded(method, instruction)) continue;
                InsnList trace = new InsnList();
                trace.add(new VarInsnNode(Opcodes.ALOAD, 0));
                trace.add(new InsnNode(Opcodes.SWAP));
                trace.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "observeNbtCopy", DESCRIPTOR, false));
                trace.add(new TypeInsnNode(Opcodes.CHECKCAST, OWNER));
                method.instructions.insertBefore(instruction, trace);
                changed = true;
            }
        }
        return changed;
    }

    static boolean verify(ClassNode node) {
        String fieldName = null;
        for (FieldNode field : node.fields) if (Set.of("tags", "f_128329_").contains(field.name)) {
            if (fieldName != null || !field.desc.equals("Ljava/util/Map;") || (field.access & Opcodes.ACC_STATIC) != 0) return false;
            fieldName = field.name;
        }
        if (fieldName == null) return false;
        int methods = 0, returns = 0;
        for (MethodNode method : node.methods) if (matches(method)) {
            methods++;
            int methodReturns = 0;
            for (AbstractInsnNode instruction : method.instructions) if (instruction.getOpcode() == Opcodes.ARETURN) {
                if (!guarded(method, instruction)) return false;
                methodReturns++;
            }
            if (methodReturns != 1 || !registeredCopyBody(method, fieldName)) return false;
            returns += methodReturns;
        }
        return methods == 1 && returns == 1 && registeredMapConstructor(node, fieldName);
    }

    /** Build-defined 1.20.1 algorithm; merely retaining the hook cannot attest an all-return replacement. */
    private static boolean registeredCopyBody(MethodNode method, String fieldName) {
        var code = instructions(method);
        if (!method.tryCatchBlocks.isEmpty() || code.size() != 15) return false;
        return variable(code.get(0), Opcodes.ALOAD, 0)
            && field(code.get(1), Opcodes.GETFIELD, fieldName)
            && copyFunction(code.get(2))
            && method(code.get(3), Opcodes.INVOKESTATIC, "com/google/common/collect/Maps", "transformValues",
                "(Ljava/util/Map;Lcom/google/common/base/Function;)Ljava/util/Map;")
            && method(code.get(4), Opcodes.INVOKESTATIC, "com/google/common/collect/Maps", "newHashMap", "(Ljava/util/Map;)Ljava/util/HashMap;")
            && variable(code.get(5), Opcodes.ASTORE, 1)
            && code.get(6) instanceof TypeInsnNode created && created.getOpcode() == Opcodes.NEW && created.desc.equals(OWNER)
            && code.get(7).getOpcode() == Opcodes.DUP && variable(code.get(8), Opcodes.ALOAD, 1)
            && method(code.get(9), Opcodes.INVOKESPECIAL, OWNER, "<init>", "(Ljava/util/Map;)V")
            && guarded(method, code.get(14)) && code.get(14).getOpcode() == Opcodes.ARETURN;
    }
    private static boolean registeredMapConstructor(ClassNode node, String fieldName) {
        for (var method : node.methods) if (method.name.equals("<init>") && method.desc.equals("(Ljava/util/Map;)V")) {
            var code = instructions(method);
            return method.tryCatchBlocks.isEmpty() && code.size() == 6
                && variable(code.get(0), Opcodes.ALOAD, 0)
                && method(code.get(1), Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V")
                && variable(code.get(2), Opcodes.ALOAD, 0) && variable(code.get(3), Opcodes.ALOAD, 1)
                && field(code.get(4), Opcodes.PUTFIELD, fieldName) && code.get(5).getOpcode() == Opcodes.RETURN;
        }
        return false;
    }
    private static List<AbstractInsnNode> instructions(MethodNode method) {
        var result = new ArrayList<AbstractInsnNode>();
        for (var node : method.instructions) if (node.getOpcode() >= 0) result.add(node);
        return result;
    }
    private static boolean field(AbstractInsnNode instruction, int opcode, String fieldName) {
        return instruction instanceof FieldInsnNode field && field.getOpcode() == opcode && field.owner.equals(OWNER)
            && fieldName.equals(field.name) && field.desc.equals("Ljava/util/Map;");
    }
    private static boolean variable(AbstractInsnNode instruction, int opcode, int slot) {
        return instruction instanceof VarInsnNode variable && variable.getOpcode() == opcode && variable.var == slot;
    }
    private static boolean method(AbstractInsnNode instruction, int opcode, String owner, String name, String descriptor) {
        return instruction instanceof MethodInsnNode call && call.getOpcode() == opcode && !call.itf
            && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(descriptor);
    }
    private static boolean copyFunction(AbstractInsnNode instruction) {
        if (!(instruction instanceof InvokeDynamicInsnNode call) || !call.name.equals("apply")
            || !call.desc.equals("()Lcom/google/common/base/Function;") || call.bsmArgs.length != 3) return false;
        Handle bootstrap = call.bsm;
        if (bootstrap.getTag() != Opcodes.H_INVOKESTATIC || bootstrap.isInterface()
            || !bootstrap.getOwner().equals("java/lang/invoke/LambdaMetafactory") || !bootstrap.getName().equals("metafactory")
            || !bootstrap.getDesc().equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;")) return false;
        return Type.getMethodType("(Ljava/lang/Object;)Ljava/lang/Object;").equals(call.bsmArgs[0])
            && call.bsmArgs[1] instanceof Handle target && target.getTag() == Opcodes.H_INVOKEINTERFACE && target.isInterface()
            && target.getOwner().equals("net/minecraft/nbt/Tag") && NAMES.contains(target.getName())
            && target.getDesc().equals("()Lnet/minecraft/nbt/Tag;")
            && Type.getMethodType("(Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;").equals(call.bsmArgs[2]);
    }

    private static boolean matches(MethodNode method) {
        return NAMES.contains(method.name) && method.desc.equals("()L" + OWNER + ";")
            && (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0;
    }

    private static boolean guarded(MethodNode method, AbstractInsnNode ret) {
        AbstractInsnNode cast = previous(ret), call = previous(cast), swap = previous(call), receiver = previous(swap);
        if (!(cast instanceof TypeInsnNode type) || type.getOpcode() != Opcodes.CHECKCAST || !type.desc.equals(OWNER)
            || !(call instanceof MethodInsnNode invoke) || invoke.getOpcode() != Opcodes.INVOKESTATIC || invoke.itf
            || !invoke.owner.equals(HOOKS) || !invoke.name.equals("observeNbtCopy") || !invoke.desc.equals(DESCRIPTOR)
            || swap == null || swap.getOpcode() != Opcodes.SWAP
            || !(receiver instanceof VarInsnNode load) || load.getOpcode() != Opcodes.ALOAD || load.var != 0) return false;
        int start = method.instructions.indexOf(receiver), end = method.instructions.indexOf(ret);
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof JumpInsnNode jump && inside(method, jump.label, start, end)) return false;
            if (instruction instanceof TableSwitchInsnNode table && (inside(method, table.dflt, start, end)
                || table.labels.stream().anyMatch(label -> inside(method, label, start, end)))) return false;
            if (instruction instanceof LookupSwitchInsnNode table && (inside(method, table.dflt, start, end)
                || table.labels.stream().anyMatch(label -> inside(method, label, start, end)))) return false;
        }
        for (TryCatchBlockNode handler : method.tryCatchBlocks)
            if (inside(method, handler.handler, start, end)) return false;
        return true;
    }

    private static boolean inside(MethodNode method, LabelNode label, int start, int end) {
        int position = method.instructions.indexOf(label);
        return position > start && position <= end;
    }
    private static AbstractInsnNode previous(AbstractInsnNode value) {
        if (value == null) return null;
        do { value = value.getPrevious(); } while (value != null && value.getOpcode() < 0);
        return value;
    }
}
