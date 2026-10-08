package dev.ronova.coremod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ronova.coremod.client.render.RonovaMaskedLayerShaders;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Vanilla base item plus one mask-clipped portrait layer. */
public final class RonovaMaskedLayerBakedModel extends RonovaWrappedItemModel
    implements RonovaItemRenderer {

    private final List<ResourceLocation> maskSprites;
    private final List<ResourceLocation> layerSprites;
    private final float parallaxScale;
    private final float layerOffsetY;

    public RonovaMaskedLayerBakedModel(
        BakedModel wrapped,
        List<ResourceLocation> maskSprites,
        List<ResourceLocation> layerSprites,
        float parallaxScale,
        float layerOffsetY
    ) {
        super(wrapped);
        this.maskSprites = List.copyOf(maskSprites);
        this.layerSprites = List.copyOf(layerSprites);
        this.parallaxScale = parallaxScale;
        this.layerOffsetY = layerOffsetY;
    }

    public List<ResourceLocation> getMaskSprites() {
        return maskSprites;
    }

    @Override
    protected boolean isMasked() {
        return true;
    }

    @Override
    public void renderItem(
        ItemStack stack,
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay
    ) {
        // Match Buer: the pixel-art frame stays readable regardless of world light.
        renderWrapped(
            stack, poseStack, bufferSource,
            LightTexture.FULL_BRIGHT, packedOverlay, true);

        if (maskSprites.isEmpty()
            || layerSprites.isEmpty()
            || RonovaMaskedLayerShaders.shader() == null) {
            return;
        }

        if (bufferSource instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch();
        }

        Minecraft minecraft = Minecraft.getInstance();
        var atlas = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite maskSprite = atlas.apply(maskSprites.get(0));
        TextureAtlasSprite layerSprite = atlas.apply(layerSprites.get(0));
        RonovaMaskedLayerShaders.configureLayer(
            layerSprites.get(0), maskSprite, layerSprite,
            parallaxScale, layerOffsetY);

        List<BakedQuad> maskQuads = bakeItem(List.of(maskSprite));
        boolean outline = bufferSource instanceof OutlineBufferSource;
        try {
            RonovaMaskedLayerShaders.setDiscardTransparent(outline);
            if (outline) {
                MultiBufferSource.BufferSource immediate =
                    minecraft.renderBuffers().bufferSource();
                VertexConsumer consumer = immediate.getBuffer(
                    RonovaMaskedLayerShaders.alwaysRenderType());
                minecraft.getItemRenderer().renderQuadList(
                    poseStack,
                    consumer,
                    maskQuads,
                    stack,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay
                );
                immediate.endBatch();
            } else {
                VertexConsumer consumer = bufferSource.getBuffer(
                    RonovaMaskedLayerShaders.renderType());
                minecraft.getItemRenderer().renderQuadList(
                    poseStack,
                    consumer,
                    maskQuads,
                    stack,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay
                );
            }
        } finally {
            RonovaMaskedLayerShaders.setDiscardTransparent(false);
        }
    }
}
