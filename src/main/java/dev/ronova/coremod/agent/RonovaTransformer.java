package dev.ronova.coremod.agent;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

final class RonovaTransformer implements ClassFileTransformer {
    private static final String ENTITY = "net/minecraft/world/entity/Entity";
    private static final String LIVING = "net/minecraft/world/entity/LivingEntity";
    private static final String LEVEL = "net/minecraft/world/level/Level";
    private static final String SERVER_LEVEL = "net/minecraft/server/level/ServerLevel";
    private static final String CLIENT_LEVEL = "net/minecraft/client/multiplayer/ClientLevel";
    private static final String MINECRAFT = "net/minecraft/client/Minecraft";
    private static final String KEY_MAPPING = "net/minecraft/client/KeyMapping";
    private static final String MOUSE_HANDLER = "net/minecraft/client/MouseHandler";
    private static final String KEYBOARD_HANDLER = "net/minecraft/client/KeyboardHandler";
    private static final String SCREEN = "net/minecraft/client/gui/screens/Screen";
    private static final String OVERLAY = "net/minecraft/client/gui/screens/Overlay";
    private static final String HOOKS = "dev/ronova/coremod/agent/BootstrapHooks";
    private static final Set<String> REMOVE_NAMES = Set.of("remove", "m_142687_");
    private static final Set<String> REVIVE_NAMES = Set.of("revive");
    private static final Set<String> HURT_NAMES = Set.of("hurt", "m_6469_");
    private static final Set<String> DIE_NAMES = Set.of("die", "m_6667_");
    private static final Set<String> HEALTH_NAMES = Set.of("setHealth", "m_21153_");
    private static final Set<String> REMOVAL_FIELD_NAMES = Set.of("removalReason", "f_146795_");
    private static final Set<String> ADDED_FIELD_NAMES = Set.of("isAddedToWorld");
    private static final Set<String> DEAD_FIELD_NAMES = Set.of("dead", "f_20890_");
    private static final Set<String> DEATH_TIME_FIELD_NAMES = Set.of("deathTime", "f_20919_");
    private static final Set<String> RENDER_NAMES = Set.of("render", "m_7392_");
    private static final Set<String> QUERY_COLLECTION_TYPES = Set.of(
        "java/lang/Iterable", "java/util/Collection", "java/util/List", "java/util/Set"
    );
    private static final Set<String> SCREEN_SINK_NAMES = Set.of(
        "setScreen", "m_91152_", "forceSetScreen", "m_91346_",
        "setOverlay", "m_91150_", "clearLevel", "m_91320_", "m_91399_"
    );
    private static final Set<String> KEY_SINK_NAMES = Set.of("releaseAll", "m_90847_");
    private static final Set<String> MOUSE_SINK_NAMES = Set.of("releaseMouse", "m_91602_");
    @SuppressWarnings("unused")
    private final String phase;

    RonovaTransformer(String phase) {
        this.phase = phase;
    }

    static boolean isTarget(String internalName) {
        return EntityDataAccessorContract.OWNER.equals(internalName) || NbtFactoryContract.OWNER.equals(internalName) || NbtCopyContract.OWNER.equals(internalName) || RawAccessorContract.registered(internalName) || ENTITY.equals(internalName) || LIVING.equals(internalName)
            || LEVEL.equals(internalName) || SERVER_LEVEL.equals(internalName)
            || CLIENT_LEVEL.equals(internalName);
    }

    static boolean shouldInspect(String internalName, byte[] classBytes) {
        if(JdkTaskBoundaryContract.enabled()&&JdkTaskBoundaryContract.registered(internalName))return classBytes!=null;
        if (internalName == null || classBytes == null || internalName.startsWith("java/")
            || internalName.startsWith("jdk/") || internalName.startsWith("sun/")) {
            return false;
        }
        return isTarget(internalName)
            || hasEntityReference(classBytes)
            || (containsAscii(classBytes, "net/minecraft/client/renderer/entity/")
                && (containsAscii(classBytes, "render") || containsAscii(classBytes, "m_7392_")))
            || hasClientControlReference(classBytes)
            || containsAscii(classBytes, "f_146795_")
            || containsAscii(classBytes, "isAddedToWorld")
            || containsAscii(classBytes, "f_20890_")
            || containsAscii(classBytes, "f_20919_")
            || containsAscii(classBytes, "removalReason")
            || containsAscii(classBytes, "deathTime");
    }

    @Override
    public byte[] transform(
        Module module,
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer
    ) {
        if (!shouldInspect(className, classfileBuffer)) {
            return null;
        }
        if(JdkTaskBoundaryContract.registered(className))return JdkTaskBoundaryContract.transform(className,classfileBuffer);
        try {
            ClassNode node = new ClassNode();
            // Preserve ModLauncher's frames. Recomputing every frame in Entity during
            // premain is unsafe because its transforming module hierarchy is not yet
            // visible to the system class loader.
            new ClassReader(classfileBuffer).accept(node, 0);
            boolean changed = false;
            changed |= RawAccessorContract.repair(node);
            changed |= EntityDataAccessorContract.repair(node);
            changed |= NbtCopyContract.patch(node);
            changed |= NbtFactoryContract.patch(node);
            if (ENTITY.equals(className)) {
                changed |= patchEntity(node);
            } else if (LIVING.equals(className)) {
                changed |= patchLiving(node);
            }
            if (LEVEL.equals(className) || SERVER_LEVEL.equals(className) || CLIENT_LEVEL.equals(className)) {
                changed |= patchQueryReturns(node);
            }
            if (!className.startsWith("net/minecraft/")
                && !className.startsWith("dev/ronova/coremod/")) {
                changed |= patchExternalFieldWrites(node);
                changed |= patchExternalQueryCollections(node);
                changed |= patchExternalControlEntrypoints(node);
                changed |= patchExternalControlGuards(node);
                changed |= patchExternalRendererEntries(node);
            }
            if (!changed) {
                return null;
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean patchEntity(ClassNode node) {
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (REMOVE_NAMES.contains(method.name)
                && method.desc.endsWith("Entity$RemovalReason;)V")
                && !SemanticGateVerifier.guarded(method, "shouldBlockRemoval", Opcodes.ALOAD)) {
                method.instructions.insert(blockRemoval());
                changed = true;
            } else if (REVIVE_NAMES.contains(method.name)
                && "()V".equals(method.desc)
                && !SemanticGateVerifier.guarded(method, "shouldBlockRevive", 0)) {
                method.instructions.insert(blockVoid("shouldBlockRevive", "(Ljava/lang/Object;)Z"));
                changed = true;
            }
        }
        return changed;
    }

    private static boolean patchLiving(ClassNode node) {
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (HURT_NAMES.contains(method.name)
                && method.desc.endsWith("DamageSource;F)Z")
                && !SemanticGateVerifier.guarded(method, "shouldBlockDamage", 0)) {
                method.instructions.insert(blockBoolean("shouldBlockDamage"));
                changed = true;
            } else if (DIE_NAMES.contains(method.name)
                && method.desc.endsWith("DamageSource;)V")
                && !SemanticGateVerifier.guarded(method, "shouldBlockDeath", 0)) {
                method.instructions.insert(blockVoid("shouldBlockDeath", "(Ljava/lang/Object;)Z"));
                changed = true;
            } else if (HEALTH_NAMES.contains(method.name)
                && "(F)V".equals(method.desc)
                && !SemanticGateVerifier.guarded(method, "shouldBlockHealth", Opcodes.FLOAD)) {
                method.instructions.insert(blockHealth());
                changed = true;
            }
        }
        return changed;
    }

    private static boolean patchQueryReturns(ClassNode node) {
        boolean changed = false;
        for (MethodNode method : node.methods) {
            // Raw accessors are internal observation ports, not public query views.
            // Filtering them would hide precisely the records reconciliation must see.
            if (method.name.startsWith("ronova$")) continue;
            String returnType = method.desc.substring(method.desc.lastIndexOf(')') + 1);
            String hook = null;
            String cast = null;
            if ("Ljava/util/List;".equals(returnType) || "Ljava/lang/Iterable;".equals(returnType)) {
                hook = "filterQuarantinedResult";
                cast = returnType.substring(1, returnType.length() - 1);
            } else if ("Lnet/minecraft/world/entity/Entity;".equals(returnType)) {
                hook = "filterQuarantinedEntity";
                cast = ENTITY;
            }
            if (hook == null) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() == Opcodes.ARETURN && !SemanticGateVerifier.filteredReturn(method, instruction, hook, cast)) {
                    InsnList filter = new InsnList();
                    filter.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC, HOOKS, hook,
                        "(Ljava/lang/Object;)Ljava/lang/Object;", false
                    ));
                    filter.add(new TypeInsnNode(Opcodes.CHECKCAST, cast));
                    method.instructions.insertBefore(instruction, filter);
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean patchExternalFieldWrites(ClassNode node) {
        boolean changed = false;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof FieldInsnNode field) || field.getOpcode() != Opcodes.PUTFIELD) {
                    continue;
                }
                String hook = null;
                String descriptor = null;
                if (REMOVAL_FIELD_NAMES.contains(field.name)
                    && field.desc.endsWith("Entity$RemovalReason;")) {
                    hook = "writeRemovalReason";
                    descriptor = "(Ljava/lang/Object;Ljava/lang/Object;)V";
                } else if (ADDED_FIELD_NAMES.contains(field.name) && "Z".equals(field.desc)) {
                    hook = "writeAddedToWorld";
                    descriptor = "(Ljava/lang/Object;Z)V";
                } else if (DEAD_FIELD_NAMES.contains(field.name) && "Z".equals(field.desc)) {
                    hook = "writeDead";
                    descriptor = "(Ljava/lang/Object;Z)V";
                } else if (DEATH_TIME_FIELD_NAMES.contains(field.name) && "I".equals(field.desc)) {
                    hook = "writeDeathTime";
                    descriptor = "(Ljava/lang/Object;I)V";
                }
                if (hook != null) {
                    method.instructions.set(field, new MethodInsnNode(
                        Opcodes.INVOKESTATIC, HOOKS, hook, descriptor, false
                    ));
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Filters entity-aware results at the producer as well as at Level. This is
     * important when another transformer replaces a whole Minecraft query body
     * with a call into its own helper class.
     */
    private static boolean patchExternalQueryCollections(ClassNode node) {
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (containsHook(method, "filterQuarantinedResult")) {
                continue;
            }
            Type returnType = Type.getReturnType(method.desc);
            if (returnType.getSort() != Type.OBJECT) {
                continue;
            }
            String internalName = returnType.getInternalName();
            if (!QUERY_COLLECTION_TYPES.contains(internalName)) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.ARETURN) {
                    continue;
                }
                InsnList filter = new InsnList();
                filter.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC, HOOKS, "filterQuarantinedResult",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", false
                ));
                filter.add(new TypeInsnNode(Opcodes.CHECKCAST, internalName));
                method.instructions.insertBefore(instruction, filter);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Stops external control loops at their own boundary. This is deliberately
     * sink-based: ordinary helpers that merely carry Minecraft objects are not
     * altered, and the gate is active only during client authority recovery.
     */
    private static boolean patchExternalControlEntrypoints(ClassNode node) {
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if ("<init>".equals(method.name) || "<clinit>".equals(method.name)
                || Type.getReturnType(method.desc).getSort() != Type.VOID
                || containsHook(method, "shouldSuppressExternalControl")
                || !containsControlSink(method)) {
                continue;
            }
            method.instructions.insert(blockStaticVoid("shouldSuppressExternalControl"));
            changed = true;
        }
        return changed;
    }

    /** Neutralizes guard-shaped external hooks that would suppress recovery input or UI. */
    private static boolean patchExternalControlGuards(ClassNode node) {
        boolean changed = false;
        for (MethodNode method : node.methods) {
            if (Type.getReturnType(method.desc).getSort() != Type.BOOLEAN
                || !isControlGuardName(method.name)
                || !hasControlArgument(method.desc)
                || containsHook(method, "shouldSuppressExternalControl")) {
                continue;
            }
            method.instructions.insert(blockStaticBoolean("shouldSuppressExternalControl"));
            changed = true;
        }
        return changed;
    }

    /** Suppresses detached or custom-pipeline entity renderers without relying on Forge events. */
    private static boolean patchExternalRendererEntries(ClassNode node) {
        boolean changed = false;
        boolean authorityVisualClass = hasAuthorityVisualCondition(node);
        for (MethodNode method : node.methods) {
            if (!(RENDER_NAMES.contains(method.name) || method.name.startsWith("render"))
                || Type.getReturnType(method.desc).getSort() != Type.VOID
                || containsHook(method, "shouldSuppressRender")
                || containsHook(method, "shouldSuppressExternalVisual")) {
                continue;
            }
            Type[] arguments = Type.getArgumentTypes(method.desc);
            if (arguments.length == 0) {
                if (!authorityVisualClass) {
                    continue;
                }
                method.instructions.insert(blockStaticVoid("shouldSuppressExternalVisual"));
                changed = true;
                continue;
            }
            if (arguments[0].getSort() != Type.OBJECT) {
                if (authorityVisualClass) {
                    method.instructions.insert(blockStaticVoid("shouldSuppressExternalVisual"));
                    changed = true;
                }
                continue;
            }
            if (!authorityVisualClass && !isEntityRenderSubject(arguments[0].getInternalName())) {
                continue;
            }
            int entitySlot = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
            method.instructions.insert(blockArgumentVoid("shouldSuppressRender", entitySlot));
            changed = true;
        }
        return changed;
    }

    private static InsnList blockVoid(String hook, String descriptor) {
        LabelNode allow = new LabelNode();
        InsnList code = new InsnList();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, descriptor, false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, allow));
        code.add(new InsnNode(Opcodes.RETURN));
        code.add(allow);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        return code;
    }

    private static InsnList blockBoolean(String hook) {
        LabelNode allow = new LabelNode();
        InsnList code = new InsnList();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, "(Ljava/lang/Object;)Z", false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, allow));
        code.add(new InsnNode(Opcodes.ICONST_0));
        code.add(new InsnNode(Opcodes.IRETURN));
        code.add(allow);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        return code;
    }

    private static InsnList blockHealth() {
        LabelNode allow = new LabelNode();
        InsnList code = new InsnList();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.FLOAD, 1));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC, HOOKS, "shouldBlockHealth", "(Ljava/lang/Object;F)Z", false
        ));
        code.add(new JumpInsnNode(Opcodes.IFEQ, allow));
        code.add(new InsnNode(Opcodes.RETURN));
        code.add(allow);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        return code;
    }

    private static InsnList blockRemoval() {
        LabelNode allow = new LabelNode();
        InsnList code = new InsnList();
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC, HOOKS, "shouldBlockRemoval",
            "(Ljava/lang/Object;Ljava/lang/Object;)Z", false
        ));
        code.add(new JumpInsnNode(Opcodes.IFEQ, allow));
        code.add(new InsnNode(Opcodes.RETURN));
        code.add(allow);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        return code;
    }

    private static InsnList blockArgumentVoid(String hook, int argumentSlot) {
        LabelNode allow = new LabelNode();
        InsnList code = new InsnList();
        code.add(new VarInsnNode(Opcodes.ALOAD, argumentSlot));
        code.add(new MethodInsnNode(
            Opcodes.INVOKESTATIC, HOOKS, hook, "(Ljava/lang/Object;)Z", false
        ));
        code.add(new JumpInsnNode(Opcodes.IFEQ, allow));
        code.add(new InsnNode(Opcodes.RETURN));
        code.add(allow);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        return code;
    }

    private static InsnList blockStaticVoid(String hook) {
        LabelNode allow = new LabelNode();
        InsnList code = new InsnList();
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, "()Z", false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, allow));
        code.add(new InsnNode(Opcodes.RETURN));
        code.add(allow);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        return code;
    }

    private static InsnList blockStaticBoolean(String hook) {
        LabelNode allow = new LabelNode();
        InsnList code = new InsnList();
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, "()Z", false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, allow));
        code.add(new InsnNode(Opcodes.ICONST_0));
        code.add(new InsnNode(Opcodes.IRETURN));
        code.add(allow);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        return code;
    }

    private static boolean containsHook(MethodNode method, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && HOOKS.equals(call.owner) && name.equals(call.name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAscii(byte[] source, String value) {
        byte[] needle = value.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        outer:
        for (int start = 0; start <= source.length - needle.length; start++) {
            for (int offset = 0; offset < needle.length; offset++) {
                if (source[start + offset] != needle[offset]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean hasEntityReference(byte[] classBytes) {
        return containsAscii(classBytes, "net/minecraft/world/entity/")
            || containsAscii(classBytes, "net/minecraft/world/level/entity/EntityAccess")
            || (containsAscii(classBytes, "/entity/") && containsAscii(classBytes, "Entity"));
    }

    private static boolean hasClientControlReference(byte[] classBytes) {
        return containsAscii(classBytes, MINECRAFT)
            || containsAscii(classBytes, KEY_MAPPING)
            || containsAscii(classBytes, MOUSE_HANDLER)
            || containsAscii(classBytes, KEYBOARD_HANDLER)
            || containsAscii(classBytes, SCREEN)
            || containsAscii(classBytes, OVERLAY);
    }

    private static boolean containsControlSink(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (!(instruction instanceof MethodInsnNode call)) {
                continue;
            }
            if ((MINECRAFT.equals(call.owner) && SCREEN_SINK_NAMES.contains(call.name))
                || (KEY_MAPPING.equals(call.owner) && KEY_SINK_NAMES.contains(call.name))
                || (MOUSE_HANDLER.equals(call.owner) && MOUSE_SINK_NAMES.contains(call.name))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isControlGuardName(String name) {
        return name != null && (name.startsWith("guard")
            || name.startsWith("shouldBlock") || name.startsWith("shouldCancel"));
    }

    private static boolean hasControlArgument(String descriptor) {
        for (Type argument : Type.getArgumentTypes(descriptor)) {
            if (argument.getSort() != Type.OBJECT) {
                continue;
            }
            String name = argument.getInternalName();
            if (MINECRAFT.equals(name) || KEY_MAPPING.equals(name)
                || MOUSE_HANDLER.equals(name) || KEYBOARD_HANDLER.equals(name)
                || SCREEN.equals(name) || OVERLAY.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAuthorityVisualCondition(ClassNode node) {
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (!(instruction instanceof MethodInsnNode call)
                    || Type.getReturnType(call.desc).getSort() != Type.BOOLEAN) {
                    continue;
                }
                String owner = call.owner == null ? "" : call.owner;
                if (owner.contains("Death") || owner.contains("Execution")
                    || owner.contains("Lifecycle") || owner.contains("Authority")
                    || owner.contains("Control") || owner.contains("Guard")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isEntityRenderSubject(String internalName) {
        if (internalName == null) {
            return false;
        }
        int separator = internalName.lastIndexOf('/');
        String simpleName = separator < 0 ? internalName : internalName.substring(separator + 1);
        return ENTITY.equals(internalName) || LIVING.equals(internalName)
            || internalName.startsWith("net/minecraft/world/entity/")
            || internalName.startsWith("net/minecraft/client/player/")
            || simpleName.endsWith("Entity") || simpleName.endsWith("EntityAccess")
            || simpleName.endsWith("Player");
    }

}
