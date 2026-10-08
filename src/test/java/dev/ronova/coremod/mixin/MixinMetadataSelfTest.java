package dev.ronova.coremod.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

/** Guards the CLASS-retained @Mixin metadata required by Mixin 0.8.5 PREPARE. */
public final class MixinMetadataSelfTest {
    private static final String MIXIN_DESCRIPTOR = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final List<String> MIXINS = List.of(
        "EntityStateAccessor",
        "LivingEntityStateAccessor",
        "SynchedEntityDataStateAccessor",
        "SynchedDataItemStateAccessor",
        "PersistentEntityManagerAccessor",
        "PersistentEntityAdmissionMixin",
        "ServerLevelAccessor",
        "EntityProtectionMixin",
        "EntityTypeNbtAdmissionMixin",
        "EntityTypeFactoryAdmissionMixin",
        "ConnectionProtectionMixin",
        "LivingEntityProtectionMixin",
        "MinecraftServerRuntimeMixin",
        "ServerConnectionProtectionMixin",
        "ServerLevelAdmissionMixin",
        "ServerLevelTickAdmissionMixin",
        "ClientLevelAccessor",
        "ItemRendererMixin",
        "MinecraftClientRuntimeMixin",
        "MinecraftScreenGuardMixin",
        "MouseHandlerInputMixin"
    );

    private MixinMetadataSelfTest() {}

    public static void main(String[] args) throws Exception {
        verifiesMixinConfiguration();
        verifiesItemRendererRefmap();
        verifiesTerminalStateRefmap();
        for (String simpleName : MIXINS) {
            String resource = "dev/ronova/coremod/mixin/" + simpleName + ".class";
            try (InputStream input = MixinMetadataSelfTest.class.getClassLoader()
                .getResourceAsStream(resource)) {
                require(input != null, "missing mixin class: " + simpleName);
                ClassNode node = new ClassNode();
                new ClassReader(input).accept(node, ClassReader.SKIP_CODE);
                require(hasMixin(node.invisibleAnnotations),
                    "@Mixin must be RuntimeInvisible: " + simpleName);
                require(!hasMixin(node.visibleAnnotations),
                    "@Mixin must not be RuntimeVisible: " + simpleName);
            }
        }
        System.out.println("MixinMetadataSelfTest: PASS (" + MIXINS.size() + " mixins)");
    }

    private static void verifiesMixinConfiguration() throws Exception {
        try (InputStream input = MixinMetadataSelfTest.class.getClassLoader()
            .getResourceAsStream("ronova.mixins.json")) {
            require(input != null, "missing ronova.mixins.json");
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (String simpleName : MIXINS) {
                require(json.contains("\"" + simpleName + "\""),
                    "mixin is not registered: " + simpleName);
            }
        }
    }

    private static void verifiesTerminalStateRefmap() throws Exception {
        try (InputStream input = MixinMetadataSelfTest.class.getClassLoader().getResourceAsStream("ronova.refmap.json")) {
            require(input != null, "missing generated refmap");
            var mappings = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("mappings");
            for (var entry : java.util.Map.of(
                "EntityStateAccessor", List.of("entityData", "removalReason", "levelCallback"),
                "LivingEntityStateAccessor", List.of("dead", "DATA_HEALTH_ID"),
                "SynchedEntityDataStateAccessor", List.of("entity", "itemsById", "lock", "isDirty"),
                "SynchedDataItemStateAccessor", List.of("accessor", "value", "dirty")).entrySet()) {
                var fields = mappings.getAsJsonObject("dev/ronova/coremod/mixin/" + entry.getKey());
                require(fields != null, "missing terminal state mappings: " + entry.getKey());
                for (String field : entry.getValue()) require(fields.has(field)
                    && fields.get(field).getAsString().startsWith("f_"), "unmapped terminal state field: " + entry.getKey() + "." + field);
            }
        }
    }

    private static void verifiesItemRendererRefmap() throws Exception {
        try (InputStream input = MixinMetadataSelfTest.class.getClassLoader()
            .getResourceAsStream("ronova.refmap.json")) {
            require(input != null, "missing ronova.refmap.json");
            JsonObject root = JsonParser.parseString(
                new String(input.readAllBytes(), StandardCharsets.UTF_8)
            ).getAsJsonObject();
            String descriptor = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;"
                + "Lnet/minecraft/world/item/ItemStack;"
                + "Lnet/minecraft/world/item/ItemDisplayContext;Z"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                + "Lnet/minecraft/world/level/Level;III)V";
            String expected = "Lnet/minecraft/client/renderer/entity/ItemRenderer;"
                + "m_269491_(Lnet/minecraft/world/entity/LivingEntity;"
                + "Lnet/minecraft/world/item/ItemStack;"
                + "Lnet/minecraft/world/item/ItemDisplayContext;Z"
                + "Lcom/mojang/blaze3d/vertex/PoseStack;"
                + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                + "Lnet/minecraft/world/level/Level;III)V";
            for (String section : List.of("mappings", "data")) {
                JsonObject mappings = root.getAsJsonObject(section);
                if ("data".equals(section)) {
                    mappings = mappings.getAsJsonObject("searge");
                }
                JsonObject mixin = mappings.getAsJsonObject(
                    "dev/ronova/coremod/mixin/ItemRendererMixin");
                require(mixin != null, "ItemRendererMixin refmap entry is missing: " + section);
                require(expected.equals(mixin.get(descriptor).getAsString()),
                    "ItemRendererMixin refmap target changed: " + section);
                JsonObject ticks = mappings.getAsJsonObject("dev/ronova/coremod/mixin/ServerLevelTickAdmissionMixin");
                require(ticks != null && ticks.has("tickPassenger") && ticks.has("tickNonPassenger"),
                    "both server tick gates require packaged refmap entries: " + section);
                require(("Lnet/minecraft/server/level/ServerLevel;m_8662_(Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/entity/Entity;)V").equals(ticks.get("tickPassenger").getAsString()),
                    "passenger tick refmap differs from the 1.20.1 mapping: " + section);
            }
        }
    }

    private static boolean hasMixin(List<AnnotationNode> annotations) {
        return annotations != null
            && annotations.stream().anyMatch(annotation -> MIXIN_DESCRIPTOR.equals(annotation.desc));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
