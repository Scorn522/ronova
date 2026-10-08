package dev.ronova.coremod.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;

/** Freezes the approved sword, portrait mask, eye atlas, and custom-render contract. */
public final class RonovaBladeModelSelfTest {
    private static final String ASSET_ROOT = "assets/ronova/";
    private static final String SWORD_SHA256 =
        "9E6158E16DE9563814B8CB2D641A9EDFFF5E1238570FAEA390303C6DA63EB275";
    private static final String MASK_SHA256 =
        "6FAC3D35E6FA13060D85ECD09E322CB38A749402D7F290C87A44A2D8CF9AD28D";
    private static final String PORTRAIT_SHA256 =
        "134382E39A42056A83719F3C492F0DB40C00AE73765FB1133F56ABEF3A47A592";
    private static final String EYE_SHA256 =
        "BFFFBCB6B5BF463A30B49E4F3666818F6A9CA802AD071BA7203425D8347B7F5D";

    private RonovaBladeModelSelfTest() {}

    public static void main(String[] args) throws Exception {
        verifiesMaskedHandheldModel();
        verifiesSwordMaskAndPortrait();
        verifiesEyeAtlas();
        verifiesShaders();
        verifiesRendererClasses();
        verifiesClientWiring();
        verifiesTooltipControls();
        verifiesLegacyRendererRemoved();
        System.out.println(
            "RonovaBladeModelSelfTest: PASS "
                + "(masked portrait + component Tooltip + legacy renderer removed)"
        );
    }

    private static void verifiesMaskedHandheldModel() throws Exception {
        JsonObject model = JsonParser.parseString(
            text(ASSET_ROOT + "models/item/ronova_blade.json")
        ).getAsJsonObject();
        require("minecraft:item/handheld".equals(model.get("parent").getAsString()),
            "blade model is not based on the vanilla handheld model");
        require("ronova:masked_layer_loader".equals(model.get("loader").getAsString()),
            "masked geometry loader changed");
        JsonObject textures = model.getAsJsonObject("textures");
        require("ronova:item/ronova_blade".equals(textures.get("layer0").getAsString()),
            "base blade texture changed");
        require("ronova:item/ronova_layer".equals(textures.get("layer1").getAsString()),
            "portrait layer texture changed");
        JsonObject masked = model.getAsJsonObject("masked");
        require("ronova:item/ronova_mask".equals(masked.get("mask").getAsString()),
            "portrait mask texture changed");
        require(Math.abs(masked.get("parallax_scale").getAsFloat() - 0.09F) < 0.0001F,
            "portrait parallax scale changed");
        require(Math.abs(masked.get("layer_offset_y").getAsFloat() - 0.08F) < 0.0001F,
            "portrait vertical framing changed");
        require(!model.has("elements"), "legacy 84-cube geometry returned");
    }

    private static void verifiesSwordMaskAndPortrait() throws Exception {
        BufferedImage sword = imageAndHash(
            ASSET_ROOT + "textures/item/ronova_blade.png", SWORD_SHA256
        );
        require(sword.getWidth() == 32 && sword.getHeight() == 32,
            "sword texture is not 32x32");
        int visibleSwordPixels = 0;
        for (int y = 0; y < sword.getHeight(); y++) {
            for (int x = 0; x < sword.getWidth(); x++) {
                if (((sword.getRGB(x, y) >>> 24) & 0xFF) != 0) visibleSwordPixels++;
            }
        }
        require(visibleSwordPixels > 30 && visibleSwordPixels < 512,
            "sword alpha silhouette is implausible: " + visibleSwordPixels);

        BufferedImage mask = imageAndHash(
            ASSET_ROOT + "textures/item/ronova_mask.png", MASK_SHA256
        );
        require(mask.getWidth() == 32 && mask.getHeight() == 32,
            "mask texture is not 32x32");
        int black = 0;
        int active = 0;
        boolean[] levels = new boolean[256];
        for (int y = 0; y < mask.getHeight(); y++) {
            for (int x = 0; x < mask.getWidth(); x++) {
                int rgb = mask.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                require(r == g && g == b, "mask is no longer grayscale");
                require(((rgb >>> 24) & 0xFF) == ((sword.getRGB(x, y) >>> 24) & 0xFF),
                    "mask alpha no longer matches the full sword silhouette");
                levels[r] = true;
                if (r == 0) black++; else active++;
            }
        }
        int levelCount = 0;
        for (boolean present : levels) if (present) levelCount++;
        require(black > 800 && active > 40 && levelCount >= 4,
            "mask no longer carries the intended multi-level energy data");

        BufferedImage portrait = imageAndHash(
            ASSET_ROOT + "textures/item/ronova_layer.png", PORTRAIT_SHA256
        );
        require(portrait.getWidth() == 1324 && portrait.getHeight() == 1847,
            "Ronova portrait dimensions changed");
    }

    private static void verifiesEyeAtlas() throws Exception {
        BufferedImage atlas = imageAndHash(
            ASSET_ROOT + "textures/gui/ronova_eye_atlas.png", EYE_SHA256
        );
        require(atlas.getWidth() == 2560 && atlas.getHeight() == 256,
            "five-frame eye atlas dimensions changed");
        require(atlas.getWidth() % 5 == 0,
            "eye atlas can no longer be divided into five exact cells");
        int transparent = 0;
        int visible = 0;
        for (int y = 0; y < atlas.getHeight(); y += 8) {
            for (int x = 0; x < atlas.getWidth(); x += 8) {
                int alpha = (atlas.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha == 0) transparent++;
                if (alpha > 16) visible++;
            }
        }
        require(transparent > 1000 && visible > 1000,
            "eye atlas lost either transparency or visible eye frames");
    }

    private static void verifiesShaders() throws Exception {
        JsonObject masked = JsonParser.parseString(
            text(ASSET_ROOT + "shaders/core/masked_layer.json")
        ).getAsJsonObject();
        JsonObject tooltip = JsonParser.parseString(
            text(ASSET_ROOT + "shaders/core/ronova_tooltip.json")
        ).getAsJsonObject();
        require("ronova:masked_layer".equals(masked.get("fragment").getAsString()),
            "masked-layer fragment shader id changed");
        require("ronova:ronova_tooltip".equals(tooltip.get("fragment").getAsString()),
            "tooltip fragment shader id changed");
        require(uniformNames(masked).containsAll(List.of(
                "MaskUVs", "LayerUVs", "ParallaxScale", "LayerOffsetY", "CameraForward",
                "OculusActive", "DiscardTransparent")),
            "masked portrait uniforms missing");
        require(uniformNames(tooltip).containsAll(List.of("Time", "TooltipSize", "ShiftOpen")),
            "tooltip animation uniforms missing");

        String maskedFragment = text(ASSET_ROOT + "shaders/core/masked_layer.fsh");
        String tooltipFragment = text(ASSET_ROOT + "shaders/core/ronova_tooltip.fsh");
        require(maskedFragment.contains("LayerSampler")
                && maskedFragment.contains("maskColor.r")
                && maskedFragment.contains("LayerOffsetY * layerHeight")
                && maskedFragment.contains("textureSize(LayerSampler, 0)"),
            "portrait sampling, mask clipping, or atlas guard missing");
        require(tooltipFragment.contains("vec4 eye")
                && tooltipFragment.contains("centerVeil")
                && tooltipFragment.contains("borderPulse"),
            "tooltip eye/readability/border layers missing");
    }

    private static void verifiesRendererClasses() {
        for (String path : List.of(
            "dev/ronova/coremod/client/render/RonovaShaders.class",
            "dev/ronova/coremod/client/render/RonovaMaskedLayerShaders.class",
            "dev/ronova/coremod/client/render/RonovaItemDecorator.class",
            "dev/ronova/coremod/client/model/RonovaItemModel.class",
            "dev/ronova/coremod/client/model/RonovaMaskedLayerModelLoader.class",
            "dev/ronova/coremod/client/model/RonovaMaskedLayerBakedModel.class",
            "dev/ronova/coremod/mixin/ItemRendererMixin.class"
        )) {
            try (InputStream input = RonovaBladeModelSelfTest.class.getClassLoader()
                .getResourceAsStream(path)) {
                require(input != null, "missing packaged renderer class: " + path);
            } catch (Exception exception) {
                throw new AssertionError("could not inspect renderer class: " + path, exception);
            }
        }
    }

    private static void verifiesClientWiring() throws Exception {
        String clientClass = binaryText("dev/ronova/coremod/client/RonovaClient.class");
        require(clientClass.contains("RonovaMaskedLayerShaders"),
            "masked-layer shader registration is missing");
        require(clientClass.contains("masked_layer_loader"),
            "masked geometry loader registration is missing");
        require(clientClass.contains("RonovaTooltipLineData")
                && clientClass.contains("RonovaTooltipControlData"),
            "Tooltip component factory registration is missing");
        require(clientClass.contains("wrapInventoryModel"),
            "blade inventory model wrapping is missing");
    }

    private static void verifiesTooltipControls() throws Exception {
        byte[] tooltipText = binaryBytes(
            "dev/ronova/coremod/client/tooltip/RonovaTooltipText.class");
        require(containsUtf8(tooltipText, "左键")
                && containsUtf8(tooltipText, "右键")
                && containsUtf8(tooltipText, "Shift + 右键"),
            "Tooltip control labels changed");
        for (String className : List.of(
            "dev/ronova/coremod/client/tooltip/RonovaTooltipControlData.class",
            "dev/ronova/coremod/client/tooltip/RonovaTooltipControlComponent.class",
            "dev/ronova/coremod/client/tooltip/RonovaTooltipLineData.class",
            "dev/ronova/coremod/client/tooltip/RonovaTooltipLineComponent.class"
        )) {
            require(binaryBytes(className) != null, "missing Tooltip component class: " + className);
        }
    }

    private static void verifiesLegacyRendererRemoved() {
        for (String path : List.of(
            "dev/ronova/coremod/client/render/RonovaBladeRenderer.class",
            "dev/ronova/coremod/client/render/RonovaRenderTypes.class",
            ASSET_ROOT + "shaders/core/ronova_blade_energy.json",
            ASSET_ROOT + "shaders/core/ronova_blade_energy.vsh",
            ASSET_ROOT + "shaders/core/ronova_blade_energy.fsh",
            ASSET_ROOT + "font/ui/lore.json"
        )) {
            try (InputStream input = RonovaBladeModelSelfTest.class.getClassLoader()
                .getResourceAsStream(path)) {
                require(input == null, "legacy visual resource is still packaged: " + path);
            } catch (Exception exception) {
                throw new AssertionError("could not inspect legacy resource: " + path, exception);
            }
        }
    }

    private static String binaryText(String path) throws Exception {
        return new String(binaryBytes(path), StandardCharsets.ISO_8859_1);
    }

    private static byte[] binaryBytes(String path) throws Exception {
        try (InputStream input = resource(path)) {
            return input.readAllBytes();
        }
    }

    private static boolean containsUtf8(byte[] bytes, String text) {
        byte[] needle = text.getBytes(StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i <= bytes.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (bytes[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static List<String> uniformNames(JsonObject shader) {
        JsonArray uniforms = shader.getAsJsonArray("uniforms");
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        uniforms.forEach(element -> names.add(element.getAsJsonObject().get("name").getAsString()));
        return names;
    }

    private static BufferedImage imageAndHash(String path, String expectedHash) throws Exception {
        byte[] bytes;
        try (InputStream input = resource(path)) {
            bytes = input.readAllBytes();
        }
        String actual = HexFormat.of().withUpperCase().formatHex(
            MessageDigest.getInstance("SHA-256").digest(bytes)
        );
        require(expectedHash.equals(actual), "resource bytes changed: " + path);
        try (InputStream input = resource(path)) {
            BufferedImage image = ImageIO.read(input);
            require(image != null, "could not decode image: " + path);
            return image;
        }
    }

    private static String text(String path) throws Exception {
        try (InputStream input = resource(path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InputStream resource(String path) {
        InputStream input = RonovaBladeModelSelfTest.class.getClassLoader()
            .getResourceAsStream(path);
        require(input != null, "missing packaged resource: " + path);
        return input;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
