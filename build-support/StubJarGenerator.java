package dev.ronova.coremod.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Creates compile-only ABI stubs from the last verified release. The output is
 * never packaged; it exists solely to let an offline build type-check changed
 * sources when Forge's Maven repository is unavailable.
 */
public final class StubJarGenerator {
    private static final String PROJECT_PREFIX = "dev/ronova/coremod/";
    private static final Map<String, Owner> OWNERS = new HashMap<>();
    private static final Set<String> INTERFACES = new HashSet<>();
    private static final Map<String, String> REVERSE_METHODS = new HashMap<>();
    private static final Map<String, String> REVERSE_FIELDS = new HashMap<>();
    private static final Map<String, String> GLOBAL_REVERSE_METHODS = new HashMap<>();
    private static final Map<String, String> GLOBAL_REVERSE_FIELDS = new HashMap<>();

    private StubJarGenerator() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                "usage: <verified-jar> <mojmap-to-srg-tsrg> <output-directory>"
            );
        }
        Path output = Path.of(args[2]);
        Files.createDirectories(output);
        loadMappings(Path.of(args[1]));
        collect(Path.of(args[0]));
        addKnownTypes();
        for (Owner owner : OWNERS.values()) {
            write(owner, output);
        }
        System.out.println("compile-only ABI stubs: " + OWNERS.size());
    }

    private static void loadMappings(Path mappings) throws IOException {
        String owner = null;
        for (String line : Files.readAllLines(mappings, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("tsrg2")) {
                continue;
            }
            if (!line.startsWith("\t")) {
                String[] columns = line.trim().split("\\s+");
                owner = columns.length >= 1 ? columns[0] : null;
                continue;
            }
            if (line.startsWith("\t\t") || owner == null) {
                continue;
            }
            String[] columns = line.trim().split("\\s+");
            if (columns.length == 2) {
                REVERSE_FIELDS.put(owner + "#" + columns[1], columns[0]);
                addUnambiguous(GLOBAL_REVERSE_FIELDS, columns[1], columns[0]);
            } else if (columns.length >= 3 && columns[1].startsWith("(")) {
                REVERSE_METHODS.put(owner + "#" + columns[2] + columns[1], columns[0]);
                addUnambiguous(GLOBAL_REVERSE_METHODS, columns[2] + columns[1], columns[0]);
            }
        }
    }

    private static void addUnambiguous(Map<String, String> target, String key, String value) {
        target.merge(key, value, (left, right) -> left.equals(right) ? left : "");
    }

    private static void collect(Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith(PROJECT_PREFIX) || !entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream input = jar.getInputStream(entry)) {
                    ClassNode node = new ClassNode();
                    new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    addType(node.superName);
                    node.interfaces.forEach(StubJarGenerator::addType);
                    node.fields.forEach(field -> addDescriptor(field.desc));
                    for (MethodNode method : node.methods) {
                        addMethodDescriptor(method.desc);
                        for (AbstractInsnNode instruction : method.instructions) {
                            if (instruction instanceof FieldInsnNode field) {
                                addField(field.owner, field.name, field.desc,
                                    field.getOpcode() == Opcodes.GETSTATIC || field.getOpcode() == Opcodes.PUTSTATIC);
                            } else if (instruction instanceof MethodInsnNode call) {
                                addMethod(call.owner, call.name, call.desc,
                                    call.getOpcode() == Opcodes.INVOKESTATIC,
                                    call.getOpcode() == Opcodes.INVOKEINTERFACE);
                            } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                                addMethodDescriptor(dynamic.desc);
                                addHandle(dynamic.bsm);
                                for (Object argument : dynamic.bsmArgs) {
                                    if (argument instanceof Handle handle) {
                                        addHandle(handle);
                                    } else if (argument instanceof Type type) {
                                        addType(type);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void addHandle(Handle handle) {
        boolean field = handle.getTag() >= Opcodes.H_GETFIELD && handle.getTag() <= Opcodes.H_PUTSTATIC;
        if (field) {
            addField(handle.getOwner(), handle.getName(), handle.getDesc(),
                handle.getTag() == Opcodes.H_GETSTATIC || handle.getTag() == Opcodes.H_PUTSTATIC);
        } else {
            addMethod(handle.getOwner(), handle.getName(), handle.getDesc(),
                handle.getTag() == Opcodes.H_INVOKESTATIC,
                handle.getTag() == Opcodes.H_INVOKEINTERFACE);
        }
    }

    private static void addKnownTypes() {
        hierarchy("net/minecraft/world/entity/LivingEntity", "net/minecraft/world/entity/Entity");
        hierarchy("net/minecraft/world/entity/player/Player", "net/minecraft/world/entity/LivingEntity");
        hierarchy("net/minecraft/server/level/ServerPlayer", "net/minecraft/world/entity/player/Player");
        hierarchy("net/minecraft/client/player/AbstractClientPlayer", "net/minecraft/world/entity/player/Player");
        hierarchy("net/minecraft/client/player/LocalPlayer", "net/minecraft/client/player/AbstractClientPlayer");
        hierarchy("net/minecraftforge/entity/PartEntity", "net/minecraft/world/entity/Entity");
        hierarchy("net/minecraft/server/level/ServerLevel", "net/minecraft/world/level/Level");
        hierarchy("net/minecraft/client/multiplayer/ClientLevel", "net/minecraft/world/level/Level");
        hierarchy("net/minecraft/core/BlockPos", "net/minecraft/core/Vec3i");
        hierarchy("net/minecraft/client/gui/screens/DeathScreen", "net/minecraft/client/gui/screens/Screen");
        hierarchy("net/minecraft/world/phys/BlockHitResult", "net/minecraft/world/phys/HitResult");
        hierarchy("net/minecraft/nbt/CompoundTag", "net/minecraft/nbt/Tag");
        hierarchy("net/minecraft/nbt/ListTag", "net/minecraft/nbt/Tag");
        hierarchy("org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",
            "org/spongepowered/asm/mixin/injection/callback/CallbackInfo");
        markInterface("net/minecraft/world/level/entity/EntityAccess");
        markInterface("net/minecraft/network/protocol/Packet");
        markInterface("net/minecraft/network/FriendlyByteBuf$Reader");
        markInterface("net/minecraft/network/FriendlyByteBuf$Writer");
        markInterface("org/slf4j/Logger");

        addMethod("net/minecraft/network/Connection", "isConnected", "()Z", false, false);
        addField("net/minecraft/world/entity/Entity", "tickCount", "I", false);
        addField("net/minecraft/server/network/ServerGamePacketListenerImpl", "connection",
            "Lnet/minecraft/network/Connection;", false);
        addMethod("org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable",
            "getReturnValue", "()Ljava/lang/Object;", false, false);
        addMethod("net/minecraft/world/entity/EntityType", "create",
            "(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;", false, false);
        addMethod("net/minecraft/world/entity/EntityType", "create",
            "(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", true, false);
        addMethod("net/minecraft/world/level/entity/PersistentEntitySectionManager", "addEntity",
            "(Lnet/minecraft/world/level/entity/EntityAccess;Z)Z", false, false);
        addMethod("net/minecraft/world/level/entity/PersistentEntitySectionManager", "startTracking",
            "(Lnet/minecraft/world/level/entity/EntityAccess;)V", false, false);
        addMethod("net/minecraft/world/level/entity/PersistentEntitySectionManager", "startTicking",
            "(Lnet/minecraft/world/level/entity/EntityAccess;)V", false, false);
        addMethod("net/minecraft/server/level/ServerLevel", "tickNonPassenger",
            "(Lnet/minecraft/world/entity/Entity;)V", false, false);
        addField("net/minecraft/world/entity/Entity$RemovalReason", "UNLOADED_WITH_PLAYER",
            "Lnet/minecraft/world/entity/Entity$RemovalReason;", true);
        addField("net/minecraft/world/entity/Entity$RemovalReason", "CHANGED_DIMENSION",
            "Lnet/minecraft/world/entity/Entity$RemovalReason;", true);
    }

    private static void hierarchy(String name, String parent) {
        Owner owner = owner(name);
        owner.parent = parent;
        addType(parent);
    }

    private static void markInterface(String name) {
        INTERFACES.add(name);
        owner(name).isInterface = true;
    }

    private static void addField(String owner, String name, String desc, boolean isStatic) {
        if (isExternal(owner)) {
            String mappedName = REVERSE_FIELDS.get(owner + "#" + name);
            if (mappedName == null) {
                mappedName = GLOBAL_REVERSE_FIELDS.get(name);
            }
            if (mappedName == null || mappedName.isEmpty()) {
                mappedName = name;
            }
            owner(owner).fields.putIfAbsent(mappedName + desc, new Field(mappedName, desc, isStatic));
            addDescriptor(desc);
        }
    }

    private static void addMethod(String owner, String name, String desc, boolean isStatic, boolean isInterface) {
        if (isExternal(owner)) {
            Owner target = owner(owner);
            target.isInterface |= isInterface;
            String mappedName = REVERSE_METHODS.get(owner + "#" + name + desc);
            if (mappedName == null) {
                mappedName = GLOBAL_REVERSE_METHODS.get(name + desc);
            }
            if (mappedName == null || mappedName.isEmpty()) {
                mappedName = name;
            }
            target.methods.putIfAbsent(mappedName + desc, new Method(mappedName, desc, isStatic));
            addMethodDescriptor(desc);
        }
    }

    private static void addMethodDescriptor(String descriptor) {
        for (Type argument : Type.getArgumentTypes(descriptor)) {
            addType(argument);
        }
        addType(Type.getReturnType(descriptor));
    }

    private static void addDescriptor(String descriptor) {
        addType(Type.getType(descriptor));
    }

    private static void addType(Type type) {
        if (type.getSort() == Type.ARRAY) {
            addType(type.getElementType());
        } else if (type.getSort() == Type.OBJECT) {
            addType(type.getInternalName());
        } else if (type.getSort() == Type.METHOD) {
            addMethodDescriptor(type.getDescriptor());
        }
    }

    private static void addType(String name) {
        if (isExternal(name)) {
            owner(name);
            int split = name.lastIndexOf('$');
            if (split > 0) {
                addType(name.substring(0, split));
            }
        }
    }

    private static Owner owner(String name) {
        return OWNERS.computeIfAbsent(name, Owner::new);
    }

    private static boolean isExternal(String name) {
        return name != null && !name.startsWith("java/") && !name.startsWith("javax/")
            && !name.startsWith("jdk/") && !name.startsWith("sun/")
            && !name.startsWith(PROJECT_PREFIX) && !name.startsWith("org/objectweb/asm/");
    }

    private static void write(Owner owner, Path output) throws IOException {
        int access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER;
        if (owner.isInterface || INTERFACES.contains(owner.name)) {
            access = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        }
        String parent = owner.parent == null ? "java/lang/Object" : owner.parent;
        String signature = signature(owner.name, parent);
        String[] interfaces = interfaces(owner.name);
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, access, owner.name, signature, parent, interfaces);
        addInnerClassMetadata(writer, owner.name, access);
        for (Owner candidate : OWNERS.values()) {
            if (candidate.name.startsWith(owner.name + "$")) {
                int split = candidate.name.lastIndexOf('$');
                if (candidate.name.substring(0, split).equals(owner.name)) {
                    writer.visitInnerClass(candidate.name, owner.name,
                        candidate.name.substring(split + 1), Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC);
                }
            }
        }

        if (!owner.isInterface) {
            var constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
            constructor.visitCode();
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, parent, "<init>", "()V", false);
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(1, 1);
            constructor.visitEnd();
        }
        for (Field field : owner.fields.values()) {
            int fieldAccess = Opcodes.ACC_PUBLIC | (field.isStatic ? Opcodes.ACC_STATIC : 0);
            writer.visitField(fieldAccess, field.name, field.desc, null, null).visitEnd();
        }
        for (Method method : owner.methods.values()) {
            if ("<clinit>".equals(method.name)) {
                continue;
            }
            if ("<init>".equals(method.name)) {
                if (owner.isInterface || "()V".equals(method.desc)) {
                    continue;
                }
                var constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", method.desc, null, null);
                constructor.visitCode();
                constructor.visitVarInsn(Opcodes.ALOAD, 0);
                constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, parent, "<init>", "()V", false);
                constructor.visitInsn(Opcodes.RETURN);
                int locals = 1;
                for (Type argument : Type.getArgumentTypes(method.desc)) {
                    locals += argument.getSize();
                }
                constructor.visitMaxs(1, locals);
                constructor.visitEnd();
                continue;
            }
            int methodAccess = Opcodes.ACC_PUBLIC | (method.isStatic ? Opcodes.ACC_STATIC : 0);
            if (owner.isInterface) {
                writer.visitMethod(methodAccess | Opcodes.ACC_ABSTRACT,
                    method.name, method.desc, null, null).visitEnd();
                continue;
            }
            var visitor = writer.visitMethod(methodAccess, method.name, method.desc, null, null);
            visitor.visitCode();
            emitDefault(visitor, Type.getReturnType(method.desc));
            visitor.visitMaxs(2, method.isStatic ? 0 : 1);
            visitor.visitEnd();
        }
        writer.visitEnd();
        Path target = output.resolve(owner.name + ".class");
        Files.createDirectories(target.getParent());
        Files.write(target, writer.toByteArray());
    }

    private static void emitDefault(org.objectweb.asm.MethodVisitor visitor, Type type) {
        switch (type.getSort()) {
            case Type.VOID -> visitor.visitInsn(Opcodes.RETURN);
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                visitor.visitInsn(Opcodes.ICONST_0);
                visitor.visitInsn(Opcodes.IRETURN);
            }
            case Type.LONG -> {
                visitor.visitInsn(Opcodes.LCONST_0);
                visitor.visitInsn(Opcodes.LRETURN);
            }
            case Type.FLOAT -> {
                visitor.visitInsn(Opcodes.FCONST_0);
                visitor.visitInsn(Opcodes.FRETURN);
            }
            case Type.DOUBLE -> {
                visitor.visitInsn(Opcodes.DCONST_0);
                visitor.visitInsn(Opcodes.DRETURN);
            }
            default -> {
                visitor.visitInsn(Opcodes.ACONST_NULL);
                visitor.visitInsn(Opcodes.ARETURN);
            }
        }
    }

    private static String signature(String name, String parent) {
        return switch (name) {
            case "net/minecraft/resources/ResourceKey" ->
                "<T:Ljava/lang/Object;>Ljava/lang/Object;";
            case "net/minecraft/world/entity/EntityType" ->
                "<T:Lnet/minecraft/world/entity/Entity;>Ljava/lang/Object;";
            case "net/minecraftforge/entity/PartEntity" ->
                "<T:Lnet/minecraft/world/entity/Entity;>Lnet/minecraft/world/entity/Entity;";
            case "net/minecraft/world/level/entity/PersistentEntitySectionManager" ->
                "<T::Lnet/minecraft/world/level/entity/EntityAccess;>Ljava/lang/Object;";
            case "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable" ->
                "<T:Ljava/lang/Object;>Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
            case "com/mojang/brigadier/CommandDispatcher" ->
                "<S:Ljava/lang/Object;>Ljava/lang/Object;";
            case "net/minecraftforge/client/event/RenderLivingEvent$Pre" ->
                "<T:Ljava/lang/Object;M:Ljava/lang/Object;>Ljava/lang/Object;";
            case "net/minecraft/network/FriendlyByteBuf$Reader" ->
                "<T:Ljava/lang/Object;>Ljava/lang/Object;"
                    + "Ljava/util/function/Function<"
                    + "Lnet/minecraft/network/FriendlyByteBuf;TT;>;";
            case "net/minecraft/network/FriendlyByteBuf$Writer" ->
                "<T:Ljava/lang/Object;>Ljava/lang/Object;"
                    + "Ljava/util/function/BiConsumer<"
                    + "Lnet/minecraft/network/FriendlyByteBuf;TT;>;";
            default -> null;
        };
    }

    private static String[] interfaces(String name) {
        List<String> result = new ArrayList<>();
        if ("net/minecraft/world/entity/Entity".equals(name)) {
            result.add("net/minecraft/world/level/entity/EntityAccess");
        }
        if ("net/minecraft/nbt/ListTag".equals(name)) {
            result.add("java/lang/Iterable");
        }
        if ("net/minecraft/network/protocol/game/ClientboundPlayerCombatKillPacket".equals(name)) {
            result.add("net/minecraft/network/protocol/Packet");
        }
        if ("net/minecraft/network/FriendlyByteBuf$Reader".equals(name)) {
            result.add("java/util/function/Function");
        }
        if ("net/minecraft/network/FriendlyByteBuf$Writer".equals(name)) {
            result.add("java/util/function/BiConsumer");
        }
        return result.toArray(String[]::new);
    }

    private static void addInnerClassMetadata(ClassWriter writer, String name, int access) {
        int split = name.lastIndexOf('$');
        if (split > 0 && split + 1 < name.length()) {
            writer.visitInnerClass(name, name.substring(0, split), name.substring(split + 1),
                access | Opcodes.ACC_STATIC);
        }
    }

    private static final class Owner {
        private final String name;
        private String parent;
        private boolean isInterface;
        private final Map<String, Field> fields = new HashMap<>();
        private final Map<String, Method> methods = new HashMap<>();

        private Owner(String name) {
            this.name = name;
        }
    }

    private record Field(String name, String desc, boolean isStatic) {}
    private record Method(String name, String desc, boolean isStatic) {}
}
