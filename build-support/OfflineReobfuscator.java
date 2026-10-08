package dev.ronova.coremod.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Offline equivalent of ForgeGradle's member-name reobfuscation pass. */
public final class OfflineReobfuscator {
    private final Map<String, String> methods = new HashMap<>();
    private final Map<String, String> fields = new HashMap<>();
    private final Map<String, String> globalMethods = new HashMap<>();
    private final Map<String, String> globalFields = new HashMap<>();
    private final Set<String> mappedClasses = new HashSet<>();
    private final Set<String> applicationClasses = new HashSet<>();
    private final Map<String, Set<String>> parents = new HashMap<>();
    private int changedMembers;

    private OfflineReobfuscator(Path mappings) throws IOException {
        loadMappings(mappings);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                "usage: <mappings.tsrg> <input-classes> <output-classes> "
                    + "[hierarchy-classes ...]"
            );
        }
        Path input = Path.of(args[1]);
        OfflineReobfuscator reobfuscator = new OfflineReobfuscator(Path.of(args[0]));
        // javac may emit an inherited invocation with the concrete application
        // subclass as its symbolic owner. Load the classes being transformed as
        // part of the hierarchy before consulting optional Minecraft/Forge ABI
        // roots, otherwise calls such as DominionLedger.setDirty() cannot walk
        // back to SavedData's mapped runtime name.
        reobfuscator.loadHierarchy(input, true);
        for (int index = 3; index < args.length; index++) {
            reobfuscator.loadHierarchy(Path.of(args[index]), false);
        }
        reobfuscator.run(input, Path.of(args[2]));
        System.out.println("reobfuscated member references: " + reobfuscator.changedMembers);
    }

    private void loadMappings(Path path) throws IOException {
        String owner = null;
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("tsrg2")) {
                continue;
            }
            if (!line.startsWith("\t")) {
                String[] columns = line.trim().split("\\s+");
                owner = columns.length == 0 ? null : columns[0];
                if (owner != null) {
                    mappedClasses.add(owner);
                }
                continue;
            }
            if (line.startsWith("\t\t") || owner == null) {
                continue;
            }
            String[] columns = line.trim().split("\\s+");
            if (columns.length == 2) {
                fields.put(owner + "#" + columns[0], columns[1]);
                addUnambiguous(globalFields, columns[0], columns[1]);
            } else if (columns.length >= 3 && columns[1].startsWith("(")) {
                methods.put(owner + "#" + columns[0] + columns[1], columns[2]);
                addUnambiguous(globalMethods, columns[0] + columns[1], columns[2]);
            }
        }
    }

    private static void addUnambiguous(Map<String, String> map, String key, String value) {
        map.merge(key, value, (left, right) -> left.equals(right) ? left : "");
    }

    private void loadHierarchy(Path classes, boolean application) throws IOException {
        try (Stream<Path> paths = Files.walk(classes)) {
            for (Path source : paths.filter(path -> path.toString().endsWith(".class")).toList()) {
                ClassReader reader = new ClassReader(Files.readAllBytes(source));
                if (application) {
                    applicationClasses.add(reader.getClassName());
                }
                Set<String> direct = parents.computeIfAbsent(reader.getClassName(), ignored -> new HashSet<>());
                if (reader.getSuperName() != null) {
                    direct.add(reader.getSuperName());
                }
                for (String interfaceName : reader.getInterfaces()) {
                    direct.add(interfaceName);
                }
            }
        }
    }

    private void run(Path input, Path output) throws IOException {
        try (Stream<Path> paths = Files.walk(input)) {
            for (Path source : paths.filter(path -> path.toString().endsWith(".class")).toList()) {
                Path relative = input.relativize(source);
                Path target = output.resolve(relative);
                Files.createDirectories(target.getParent());
                Files.write(target, transform(Files.readAllBytes(source)));
            }
        }
    }

    private byte[] transform(byte[] input) {
        ClassNode node = new ClassNode();
        new ClassReader(input).accept(node, 0);
        for (MethodNode method : node.methods) {
            // A method declaration that overrides a mapped Minecraft/Forge
            // parent must be renamed together with its call sites. Mapping only
            // MethodInsnNode owners leaves the subclass with the development
            // name and causes AbstractMethodError when the runtime dispatches
            // through the reobfuscated abstract parent (for example,
            // SavedData.save(CompoundTag) -> m_7176_).
            String mappedDeclaration = mapMethod(node.name, method.name, method.desc);
            if (!mappedDeclaration.equals(method.name)) {
                method.name = mappedDeclaration;
                changedMembers++;
            }
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call) {
                    String mapped = mapMethod(call.owner, call.name, call.desc);
                    if (!mapped.equals(call.name)) {
                        call.name = mapped;
                        changedMembers++;
                    }
                } else if (instruction instanceof FieldInsnNode field) {
                    String mapped = mapField(field.owner, field.name);
                    if (!mapped.equals(field.name)) {
                        field.name = mapped;
                        changedMembers++;
                    }
                } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                    for (int index = 0; index < dynamic.bsmArgs.length; index++) {
                        dynamic.bsmArgs[index] = mapConstant(dynamic.bsmArgs[index]);
                    }
                } else if (instruction instanceof LdcInsnNode constant) {
                    constant.cst = mapConstant(constant.cst);
                }
            }
        }
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private Object mapConstant(Object value) {
        if (value instanceof Handle handle) {
            return mapHandle(handle);
        }
        if (value instanceof ConstantDynamic dynamic) {
            Object[] arguments = new Object[dynamic.getBootstrapMethodArgumentCount()];
            for (int index = 0; index < arguments.length; index++) {
                arguments[index] = mapConstant(dynamic.getBootstrapMethodArgument(index));
            }
            return new ConstantDynamic(
                dynamic.getName(), dynamic.getDescriptor(),
                mapHandle(dynamic.getBootstrapMethod()), arguments
            );
        }
        return value;
    }

    private Handle mapHandle(Handle handle) {
        boolean field = handle.getTag() >= Opcodes.H_GETFIELD && handle.getTag() <= Opcodes.H_PUTSTATIC;
        String mapped = field
            ? mapField(handle.getOwner(), handle.getName())
            : mapMethod(handle.getOwner(), handle.getName(), handle.getDesc());
        if (mapped.equals(handle.getName())) {
            return handle;
        }
        changedMembers++;
        return new Handle(
            handle.getTag(), handle.getOwner(), mapped, handle.getDesc(), handle.isInterface()
        );
    }

    private String mapMethod(String owner, String name, String descriptor) {
        String mapped = lookupHierarchy(methods, owner, name + descriptor);
        if (mapped == null && !applicationClasses.contains(owner)
            && belongsToMappedHierarchy(owner)) {
            mapped = globalMethods.get(name + descriptor);
        }
        return mapped == null || mapped.isEmpty() ? name : mapped;
    }

    private String mapField(String owner, String name) {
        String mapped = lookupHierarchy(fields, owner, name);
        if (mapped == null && !applicationClasses.contains(owner)
            && belongsToMappedHierarchy(owner)) {
            mapped = globalFields.get(name);
        }
        return mapped == null || mapped.isEmpty() ? name : mapped;
    }

    private boolean belongsToMappedHierarchy(String owner) {
        ArrayDeque<String> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.add(owner);
        while (!pending.isEmpty()) {
            String candidate = pending.removeFirst();
            if (!visited.add(candidate)) {
                continue;
            }
            if (mappedClasses.contains(candidate)) {
                return true;
            }
            pending.addAll(parents.getOrDefault(candidate, Set.of()));
        }
        return false;
    }

    private String lookupHierarchy(Map<String, String> mappings, String owner, String member) {
        ArrayDeque<String> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.add(owner);
        while (!pending.isEmpty()) {
            String candidate = pending.removeFirst();
            if (!visited.add(candidate)) {
                continue;
            }
            String mapped = mappings.get(candidate + "#" + member);
            if (mapped != null) {
                return mapped;
            }
            pending.addAll(parents.getOrDefault(candidate, Set.of()));
        }
        return null;
    }
}
