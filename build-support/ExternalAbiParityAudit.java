package dev.ronova.coremod.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Detects method/field descriptor drift against a prior ForgeGradle-built artifact. */
public final class ExternalAbiParityAudit {
    private static final String INTERNAL_PREFIX = "dev/ronova/coremod/";

    private ExternalAbiParityAudit() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: <baseline.jar> <candidate.jar>");
        }
        References baseline = scan(Path.of(args[0]));
        References candidate = scan(Path.of(args[1]));
        List<String> mismatches = new ArrayList<>();
        int commonMethodNames = compareUniqueMethodDescriptors(
            baseline.methodDescriptors, candidate.methodDescriptors, mismatches);
        int commonMethods = compareMethods(baseline.methods, candidate.methods, mismatches);
        int commonFields = compareFields(baseline.fields, candidate.fields, mismatches);
        if (!mismatches.isEmpty()) {
            throw new AssertionError("external ABI drift:\n  "
                + String.join("\n  ", mismatches));
        }
        System.out.println("ExternalAbiParityAudit: PASS (" + commonMethodNames
            + " common method names, " + commonMethods
            + " common argument shapes, " + commonFields + " common fields)");
    }

    private static int compareUniqueMethodDescriptors(
        Map<MethodNameKey, Set<String>> baseline,
        Map<MethodNameKey, Set<String>> candidate,
        List<String> mismatches
    ) {
        int common = 0;
        for (Map.Entry<MethodNameKey, Set<String>> entry : candidate.entrySet()) {
            Set<String> expected = baseline.get(entry.getKey());
            if (expected == null) {
                continue;
            }
            common++;
            Set<String> actual = entry.getValue();
            if (expected.size() == 1 && actual.size() == 1 && !expected.equals(actual)) {
                mismatches.add(entry.getKey() + " descriptor " + actual
                    + ", expected " + expected);
            }
        }
        return common;
    }

    private static int compareMethods(
        Map<MethodKey, Set<String>> baseline,
        Map<MethodKey, Set<String>> candidate,
        List<String> mismatches
    ) {
        int common = 0;
        for (Map.Entry<MethodKey, Set<String>> entry : candidate.entrySet()) {
            Set<String> expected = baseline.get(entry.getKey());
            if (expected == null) {
                continue;
            }
            common++;
            Set<String> actual = entry.getValue();
            if (expected.size() == 1 && actual.size() == 1 && !expected.equals(actual)) {
                mismatches.add(entry.getKey() + " returns " + actual + ", expected " + expected);
            }
        }
        return common;
    }

    private static int compareFields(
        Map<FieldKey, Set<String>> baseline,
        Map<FieldKey, Set<String>> candidate,
        List<String> mismatches
    ) {
        int common = 0;
        for (Map.Entry<FieldKey, Set<String>> entry : candidate.entrySet()) {
            Set<String> expected = baseline.get(entry.getKey());
            if (expected == null) {
                continue;
            }
            common++;
            if (!expected.equals(entry.getValue())) {
                mismatches.add(entry.getKey() + " has " + entry.getValue()
                    + ", expected " + expected);
            }
        }
        return common;
    }

    private static References scan(Path archive) throws IOException {
        References references = new References();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            for (ZipEntry entry : zip.stream()
                .filter(item -> !item.isDirectory())
                .filter(item -> item.getName().startsWith(INTERNAL_PREFIX))
                .filter(item -> item.getName().endsWith(".class"))
                .sorted((left, right) -> left.getName().compareTo(right.getName()))
                .toList()) {
                try (InputStream input = zip.getInputStream(entry)) {
                    new ClassReader(input).accept(visitor(references),
                        ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
        }
        return references;
    }

    private static ClassVisitor visitor(References references) {
        return new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                int access, String name, String descriptor, String signature, String[] exceptions
            ) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(
                        int opcode, String owner, String methodName, String methodDescriptor,
                        boolean isInterface
                    ) {
                        recordMethod(references, owner, methodName, methodDescriptor);
                    }

                    @Override
                    public void visitFieldInsn(
                        int opcode, String owner, String fieldName, String fieldDescriptor
                    ) {
                        recordField(references, owner, fieldName, fieldDescriptor);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(
                        String dynamicName, String dynamicDescriptor, Handle bootstrap,
                        Object... arguments
                    ) {
                        for (Object argument : arguments) {
                            if (argument instanceof Handle handle) {
                                recordHandle(references, handle);
                            }
                        }
                    }
                };
            }
        };
    }

    private static void recordHandle(References references, Handle handle) {
        switch (handle.getTag()) {
            case Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC,
                 Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC ->
                recordField(references, handle.getOwner(), handle.getName(), handle.getDesc());
            default -> recordMethod(
                references, handle.getOwner(), handle.getName(), handle.getDesc());
        }
    }

    private static void recordMethod(
        References references, String owner, String name, String descriptor
    ) {
        if (owner.startsWith(INTERNAL_PREFIX)) {
            return;
        }
        int end = descriptor.indexOf(')');
        if (end < 0) {
            throw new IllegalArgumentException("invalid method descriptor: " + descriptor);
        }
        MethodKey key = new MethodKey(owner, name, descriptor.substring(0, end + 1));
        references.methods.computeIfAbsent(key, ignored -> new TreeSet<>())
            .add(descriptor.substring(end + 1));
        references.methodDescriptors
            .computeIfAbsent(new MethodNameKey(owner, name), ignored -> new TreeSet<>())
            .add(descriptor);
    }

    private static void recordField(
        References references, String owner, String name, String descriptor
    ) {
        if (!owner.startsWith(INTERNAL_PREFIX)) {
            references.fields.computeIfAbsent(new FieldKey(owner, name), ignored -> new TreeSet<>())
                .add(descriptor);
        }
    }

    private static final class References {
        private final Map<MethodKey, Set<String>> methods = new HashMap<>();
        private final Map<MethodNameKey, Set<String>> methodDescriptors = new HashMap<>();
        private final Map<FieldKey, Set<String>> fields = new HashMap<>();
    }

    private record MethodNameKey(String owner, String name) {}

    private record MethodKey(String owner, String name, String arguments) {}

    private record FieldKey(String owner, String name) {}
}
