package dev.ronova.coremod.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class AgentTransformerSelfTest {
    public static void main(String[] args) throws Exception {
        verifyEntityRemovalPatch();
        verifyEntityRevivePatch();
        verifyLivingSemanticProofPatch();
        verifyGenericDirectWritePatch();
        verifyQueryFilterPatch();
        verifyExternalQueryProducerPatch();
        verifyExternalSingularCarrierPreserved();
        verifyExternalControlPatches();
        verifyExternalRendererPatch();
        verifyTerminalTransitionGates();
        verifyUnrelatedEntityFramesArePreserved();
        verifyStartupPolicyRoundTrip();
        verifyExternalFixtures(args);
        System.out.println("AgentTransformerSelfTest: PASS");
    }

    private static void verifyEntityRevivePatch() {
        ClassWriter input = new ClassWriter(0);
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC,
            "net/minecraft/world/entity/Entity", null, "java/lang/Object", null);
        var method = input.visitMethod(Opcodes.ACC_PUBLIC, "revive", "()V", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 1);
        method.visitEnd();
        input.visitEnd();

        byte[] output = new RonovaTransformer("revive-test").transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "net/minecraft/world/entity/Entity", null, null, input.toByteArray()
        );
        require(output != null && hasHook(output, "shouldBlockRevive"),
            "quarantine revive gate missing");
    }

    private static void verifyGenericDirectWritePatch() {
        ClassWriter input = new ClassWriter(0);
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "example/ForeignLifecycle", null, "java/lang/Object", null);
        var method = input.visitMethod(
            Opcodes.ACC_PUBLIC, "forceRemoval",
            "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
            null, null
        );
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 1);
        method.visitVarInsn(Opcodes.ALOAD, 2);
        method.visitFieldInsn(
            Opcodes.PUTFIELD, "net/minecraft/world/entity/Entity", "f_146795_",
            "Lnet/minecraft/world/entity/Entity$RemovalReason;"
        );
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(2, 3);
        method.visitEnd();
        input.visitEnd();

        RonovaTransformer transformer = new RonovaTransformer("direct-write-test");
        byte[] output = transformer.transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "example/ForeignLifecycle", null, null, input.toByteArray()
        );
        require(output != null, "foreign direct field write was not transformed");
        require(hasHook(output, "writeRemovalReason"), "direct removal write gate missing");
        require(transformer.transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "example/ForeignLifecycle", null, null, output
        ) == null, "direct write transformation must be idempotent");
    }

    private static void verifyQueryFilterPatch() {
        for (String owner : List.of(
            "net/minecraft/world/level/Level",
            "net/minecraft/server/level/ServerLevel",
            "net/minecraft/client/multiplayer/ClientLevel"
        )) {
            ClassWriter input = new ClassWriter(0);
            input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
            var method = input.visitMethod(
                Opcodes.ACC_PUBLIC, "getAllEntities", "()Ljava/lang/Iterable;", null, null
            );
            method.visitCode();
            method.visitInsn(Opcodes.ACONST_NULL);
            method.visitInsn(Opcodes.ARETURN);
            method.visitMaxs(1, 1);
            method.visitEnd();
            input.visitEnd();

            byte[] output = new RonovaTransformer("query-test").transform(
                null, AgentTransformerSelfTest.class.getClassLoader(),
                owner, null, null, input.toByteArray()
            );
            require(output != null && hasHook(output, "filterQuarantinedResult"),
                "query quarantine filter missing: " + owner);
        }
    }

    private static void verifyLivingSemanticProofPatch() {
        ClassWriter input = new ClassWriter(0);
        String owner = "net/minecraft/world/entity/LivingEntity";
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        addDefaultMethod(input, "hurt", "(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            Opcodes.ICONST_1, Opcodes.IRETURN, 3);
        addDefaultMethod(input, "die", "(Lnet/minecraft/world/damagesource/DamageSource;)V",
            -1, Opcodes.RETURN, 2);
        addDefaultMethod(input, "setHealth", "(F)V", -1, Opcodes.RETURN, 2);
        input.visitEnd();

        byte[] output = new RonovaTransformer("living-proof-test").transform(
            null, AgentTransformerSelfTest.class.getClassLoader(), owner,
            null, null, input.toByteArray()
        );
        require(output != null && hasHook(output, "shouldBlockDamage")
                && hasHook(output, "shouldBlockDeath") && hasHook(output, "shouldBlockHealth"),
            "LivingEntity semantic proof set is incomplete");
    }

    private static void addDefaultMethod(
        ClassWriter owner, String name, String descriptor,
        int valueOpcode, int returnOpcode, int locals
    ) {
        var method = owner.visitMethod(Opcodes.ACC_PUBLIC, name, descriptor, null, null);
        method.visitCode();
        if (valueOpcode >= 0) method.visitInsn(valueOpcode);
        method.visitInsn(returnOpcode);
        method.visitMaxs(valueOpcode >= 0 ? 1 : 0, locals);
        method.visitEnd();
    }

    private static void verifyExternalQueryProducerPatch() {
        ClassWriter input = new ClassWriter(0);
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC,
            "example/ForeignEntityIndex", null, "java/lang/Object", null);
        var method = input.visitMethod(
            Opcodes.ACC_PUBLIC, "entities",
            "(Lnet/minecraft/world/entity/Entity;)Ljava/util/Set;", null, null
        );
        method.visitCode();
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 2);
        method.visitEnd();
        input.visitEnd();

        byte[] output = new RonovaTransformer("external-query-test").transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "example/ForeignEntityIndex", null, null, input.toByteArray()
        );
        require(output != null && hasHook(output, "filterQuarantinedResult"),
            "external query producer filter missing");
    }

    private static void verifyExternalSingularCarrierPreserved() {
        ClassWriter input = new ClassWriter(0);
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC,
            "example/ForeignEntityEvent", null, "java/lang/Object", null);
        var method = input.visitMethod(
            Opcodes.ACC_PUBLIC, "getEntity",
            "()Lnet/minecraft/world/entity/Entity;", null, null
        );
        method.visitCode();
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();
        input.visitEnd();

        byte[] output = new RonovaTransformer("singular-carrier-test").transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "example/ForeignEntityEvent", null, null, input.toByteArray()
        );
        require(output == null,
            "singular identity/event carriers must never be rewritten to null");
    }

    private static void verifyExternalControlPatches() {
        ClassWriter input = new ClassWriter(0);
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC,
            "example/ForeignClientControl", null, "java/lang/Object", null);

        var maintain = input.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "maintain", "()V", null, null);
        maintain.visitCode();
        maintain.visitMethodInsn(Opcodes.INVOKESTATIC,
            "net/minecraft/client/KeyMapping", "m_90847_", "()V", false);
        maintain.visitInsn(Opcodes.RETURN);
        maintain.visitMaxs(0, 0);
        maintain.visitEnd();

        var guard = input.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            "guardMouse", "(Lnet/minecraft/client/MouseHandler;)Z", null, null);
        guard.visitCode();
        guard.visitInsn(Opcodes.ICONST_1);
        guard.visitInsn(Opcodes.IRETURN);
        guard.visitMaxs(1, 1);
        guard.visitEnd();
        input.visitEnd();

        RonovaTransformer transformer = new RonovaTransformer("client-control-test");
        byte[] output = transformer.transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "example/ForeignClientControl", null, null, input.toByteArray()
        );
        require(output != null && hasHook(output, "shouldSuppressExternalControl"),
            "external client-control boundary gate missing");
        require(transformer.transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "example/ForeignClientControl", null, null, output
        ) == null, "client-control transformation must be idempotent");
    }

    private static void verifyExternalRendererPatch() {
        ClassWriter input = new ClassWriter(0);
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC,
            "example/DetachedEntityRenderer", null, "java/lang/Object", null);
        var method = input.visitMethod(
            Opcodes.ACC_PUBLIC, "render", "(Lexample/entity/DetachedEntity;F)V", null, null
        );
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 3);
        method.visitEnd();
        input.visitEnd();

        byte[] output = new RonovaTransformer("external-render-test").transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "example/DetachedEntityRenderer", null, null, input.toByteArray()
        );
        require(output != null && hasHook(output, "shouldSuppressRender"),
            "detached entity renderer gate missing");
    }

    private static void verifyTerminalTransitionGates() {
        Object owner = new Object();
        Object quarantined = new Object();
        BootstrapHooks.installPredicate(value -> value == owner);
        BootstrapHooks.installRemovalPredicate(
            (value, reason) -> value == owner && !"normal-unload".equals(reason)
        );
        BootstrapHooks.installQuarantinePredicate(value -> value == quarantined);
        BootstrapHooks.installEntityPredicate(value -> value == owner || value == quarantined);
        BootstrapHooks.installExternalControlSuppression(() -> true);
        try {
            require(BootstrapHooks.shouldBlockRevive(quarantined),
                "quarantined object must reject revive");
            require(!BootstrapHooks.shouldBlockRevive(owner),
                "protected owner must remain revivable");
            require(BootstrapHooks.shouldBlockHealth(owner, 0.0F),
                "protected owner must reject terminal health");
            require(BootstrapHooks.shouldBlockHealth(quarantined, 20.0F),
                "quarantined object must reject live health");
            require(BootstrapHooks.shouldBlockRemoval(owner, "hostile"),
                "protected owner must reject hostile removal");
            require(!BootstrapHooks.shouldBlockRemoval(owner, "normal-unload"),
                "normal lifecycle removal must remain available");

            Set<Object> source = new LinkedHashSet<>(List.of("prefix", quarantined, "suffix"));
            Object filtered = BootstrapHooks.filterQuarantinedResult(source);
            require(filtered instanceof Set<?>, "set query shape must be preserved");
            require(filtered.equals(new LinkedHashSet<>(List.of("prefix", "suffix"))),
                "set query filter must preserve non-quarantined elements and order");
            require(!BootstrapHooks.shouldSuppressRender(owner),
                "control recovery must not suppress ordinary entity rendering");
            require(BootstrapHooks.shouldSuppressExternalVisual(),
                "external non-entity visual recovery gate must be active");
        } finally {
            BootstrapHooks.installPredicate(ignored -> false);
            BootstrapHooks.installRemovalPredicate((value, reason) -> false);
            BootstrapHooks.installQuarantinePredicate(ignored -> false);
            BootstrapHooks.installEntityPredicate(ignored -> false);
            BootstrapHooks.installExternalControlSuppression(() -> false);
        }
    }

    private static void verifyExternalFixtures(String[] args) throws Exception {
        require(args.length % 3 == 0,
            "external fixtures use triples: <class-file> <internal-name> <expected-hook>");
        for (int index = 0; index < args.length; index += 3) {
            byte[] input = Files.readAllBytes(Path.of(args[index]));
            String internalName = args[index + 1];
            String expectedHook = args[index + 2];
            RonovaTransformer transformer = new RonovaTransformer("external-fixture");
            byte[] output = transformer.transform(
                null, AgentTransformerSelfTest.class.getClassLoader(),
                internalName, null, null, input
            );
            if ("NONE".equals(expectedHook)) {
                require(output == null,
                    "external fixture must remain untouched: " + internalName);
                continue;
            }
            if (expectedHook.startsWith("ABSENT:")) {
                String forbiddenHook = expectedHook.substring("ABSENT:".length());
                require(output == null || !hasHook(output, forbiddenHook),
                    "external fixture contains forbidden hook: " + internalName
                        + " -> " + forbiddenHook);
                if (output != null) {
                    require(transformer.transform(
                        null, AgentTransformerSelfTest.class.getClassLoader(),
                        internalName, null, null, output
                    ) == null, "external fixture transformation must be idempotent: " + internalName);
                }
                continue;
            }
            require(output != null && hasHook(output, expectedHook),
                "external fixture hook missing: " + internalName + " -> " + expectedHook);
            require(transformer.transform(
                null, AgentTransformerSelfTest.class.getClassLoader(),
                internalName, null, null, output
            ) == null, "external fixture transformation must be idempotent: " + internalName);
        }
    }

    private static void verifyUnrelatedEntityFramesArePreserved() {
        final String entity = "net/minecraft/world/entity/Entity";
        ClassWriter input = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String first, String second) {
                if (first.equals("test/SpecialEntity") || second.equals("test/SpecialEntity")) {
                    return entity;
                }
                return super.getCommonSuperClass(first, second);
            }
        };
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, entity, null, "java/lang/Object", null);

        var remove = input.visitMethod(
            Opcodes.ACC_PUBLIC, "remove", "(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", null, null
        );
        remove.visitCode();
        remove.visitInsn(Opcodes.RETURN);
        remove.visitMaxs(0, 0);
        remove.visitEnd();

        var self = input.visitMethod(Opcodes.ACC_PUBLIC, "self", "()Lnet/minecraft/world/entity/Entity;", null, null);
        self.visitCode();
        self.visitVarInsn(Opcodes.ALOAD, 0);
        self.visitInsn(Opcodes.ARETURN);
        self.visitMaxs(0, 0);
        self.visitEnd();

        var unrelated = input.visitMethod(Opcodes.ACC_PUBLIC, "m_280447_", "()V", null, null);
        var alternate = new org.objectweb.asm.Label();
        var merge = new org.objectweb.asm.Label();
        unrelated.visitCode();
        unrelated.visitInsn(Opcodes.ICONST_0);
        unrelated.visitJumpInsn(Opcodes.IFEQ, alternate);
        unrelated.visitVarInsn(Opcodes.ALOAD, 0);
        unrelated.visitJumpInsn(Opcodes.GOTO, merge);
        unrelated.visitLabel(alternate);
        unrelated.visitInsn(Opcodes.ACONST_NULL);
        unrelated.visitTypeInsn(Opcodes.CHECKCAST, "test/SpecialEntity");
        unrelated.visitLabel(merge);
        unrelated.visitVarInsn(Opcodes.ASTORE, 1);
        unrelated.visitVarInsn(Opcodes.ALOAD, 1);
        unrelated.visitMethodInsn(Opcodes.INVOKEVIRTUAL, entity, "self", "()Lnet/minecraft/world/entity/Entity;", false);
        unrelated.visitInsn(Opcodes.POP);
        unrelated.visitInsn(Opcodes.RETURN);
        unrelated.visitMaxs(0, 0);
        unrelated.visitEnd();
        input.visitEnd();

        byte[] output = new RonovaTransformer("frame-test").transform(
            null, AgentTransformerSelfTest.class.getClassLoader(), entity, null, null, input.toByteArray()
        );
        require(output != null, "frame fixture was not transformed");

        ClassNode node = new ClassNode();
        new ClassReader(output).accept(node, 0);
        boolean entityMergeFrame = false;
        for (MethodNode method : node.methods) {
            if (!method.name.equals("m_280447_")) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof FrameNode frame && frame.stack != null && frame.stack.contains(entity)) {
                    entityMergeFrame = true;
                }
            }
        }
        require(entityMergeFrame, "unrelated Entity merge frame was widened or discarded");
    }

    private static void verifyEntityRemovalPatch() {
        ClassWriter input = new ClassWriter(0);
        input.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "net/minecraft/world/entity/Entity", null, "java/lang/Object", null);
        var method = input.visitMethod(
            Opcodes.ACC_PUBLIC,
            "remove",
            "(Lnet/minecraft/world/entity/Entity$RemovalReason;)V",
            null,
            null
        );
        method.visitCode();
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 2);
        method.visitEnd();
        input.visitEnd();

        RonovaTransformer transformer = new RonovaTransformer("test");
        byte[] output = transformer.transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "net/minecraft/world/entity/Entity", null, null, input.toByteArray()
        );
        require(output != null, "target class was not transformed");

        ClassNode node = new ClassNode();
        new ClassReader(output).accept(node, 0);
        boolean hookFound = false;
        for (MethodNode candidate : node.methods) {
            for (AbstractInsnNode instruction : candidate.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("dev/ronova/coremod/agent/BootstrapHooks")
                    && call.name.equals("shouldBlockRemoval")) {
                    hookFound = true;
                }
            }
        }
        require(hookFound, "removal hook missing from transformed bytecode");

        byte[] second = transformer.transform(
            null, AgentTransformerSelfTest.class.getClassLoader(),
            "net/minecraft/world/entity/Entity", null, null, output
        );
        require(second == null, "transformer must be idempotent");
    }

    private static void verifyStartupPolicyRoundTrip() throws Exception {
        Path directory = Files.createTempDirectory("ronova-policy-test");
        Path file = directory.resolve("config.properties");
        StartupPolicy expected = new StartupPolicy(StartupPolicy.ProtectionMode.DUAL_AGENT, true, 30_000);
        expected.save(file);
        StartupPolicy actual = StartupPolicy.load(file);
        require(expected.equals(actual), "startup policy round-trip mismatch");
    }

    private static boolean hasHook(byte[] bytes, String name) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("dev/ronova/coremod/agent/BootstrapHooks")
                    && call.name.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
