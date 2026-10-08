package dev.ronova.coremod.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

/** Reads a vanilla item model while reserving layer1 for a mask-driven shader pass. */
public final class RonovaMaskedLayerModelLoader
    implements IGeometryLoader<RonovaMaskedLayerModelLoader.MaskedLayerGeometry> {

    public static final RonovaMaskedLayerModelLoader INSTANCE =
        new RonovaMaskedLayerModelLoader();

    private RonovaMaskedLayerModelLoader() {}

    @Override
    public MaskedLayerGeometry read(
        JsonObject json,
        JsonDeserializationContext deserializationContext
    ) throws JsonParseException {
        JsonObject masked = json.getAsJsonObject("masked");
        if (masked == null) {
            throw new JsonParseException("Missing 'masked' object");
        }

        List<String> maskTextures = new ArrayList<>();
        JsonElement maskElement = masked.get("mask");
        if (maskElement == null) {
            throw new JsonParseException("Missing 'masked.mask' texture");
        }
        if (maskElement.isJsonArray()) {
            JsonArray masks = maskElement.getAsJsonArray();
            for (JsonElement entry : masks) {
                maskTextures.add(entry.getAsString());
            }
        } else {
            maskTextures.add(GsonHelper.getAsString(masked, "mask"));
        }

        float parallaxScale = masked.has("parallax_scale")
            ? masked.get("parallax_scale").getAsFloat()
            : 0.09F;
        float layerOffsetY = masked.has("layer_offset_y")
            ? masked.get("layer_offset_y").getAsFloat()
            : 0.08F;

        JsonObject vanillaJson = json.deepCopy();
        vanillaJson.remove("masked");
        vanillaJson.remove("loader");

        List<String> layerTextures = new ArrayList<>();
        if (vanillaJson.has("textures")) {
            JsonObject textures = vanillaJson.getAsJsonObject("textures");
            if (textures.has("layer1")) {
                layerTextures.add(textures.get("layer1").getAsString());
                textures.remove("layer1");
            }
        }
        if (layerTextures.isEmpty()) {
            throw new JsonParseException("Masked model requires textures.layer1");
        }

        BlockModel baseModel = deserializationContext.deserialize(
            vanillaJson, BlockModel.class);
        return new MaskedLayerGeometry(
            baseModel, layerTextures, maskTextures, parallaxScale, layerOffsetY);
    }

    public static final class MaskedLayerGeometry
        implements IUnbakedGeometry<MaskedLayerGeometry> {

        private final BlockModel baseModel;
        private final List<String> layerTextures;
        private final List<String> maskTextures;
        private final float parallaxScale;
        private final float layerOffsetY;

        private MaskedLayerGeometry(
            BlockModel baseModel,
            List<String> layerTextures,
            List<String> maskTextures,
            float parallaxScale,
            float layerOffsetY
        ) {
            this.baseModel = baseModel;
            this.layerTextures = List.copyOf(layerTextures);
            this.maskTextures = List.copyOf(maskTextures);
            this.parallaxScale = parallaxScale;
            this.layerOffsetY = layerOffsetY;
        }

        @Override
        public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides,
            ResourceLocation modelLocation
        ) {
            BakedModel bakedBase = baseModel.bake(
                baker, baseModel, spriteGetter, modelState, modelLocation, true);
            List<ResourceLocation> layers = layerTextures.stream()
                .map(ResourceLocation::parse)
                .toList();
            List<ResourceLocation> masks = maskTextures.stream()
                .map(ResourceLocation::parse)
                .toList();
            return new RonovaMaskedLayerBakedModel(
                bakedBase, masks, layers, parallaxScale, layerOffsetY);
        }

        @Override
        public void resolveParents(
            Function<ResourceLocation, UnbakedModel> modelGetter,
            IGeometryBakingContext context
        ) {
            baseModel.resolveParents(modelGetter);
        }
    }
}
