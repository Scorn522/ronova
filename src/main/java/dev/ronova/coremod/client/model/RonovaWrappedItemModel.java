package dev.ronova.coremod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Delegating baked model with the same generated-item geometry path used by Buer. */
public abstract class RonovaWrappedItemModel implements RonovaPerspectiveModel {
    private static final ItemModelGenerator ITEM_MODEL_GENERATOR = new ItemModelGenerator();
    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ResourceLocation DYNAMIC_MODEL =
        ResourceLocation.fromNamespaceAndPath("ronova", "dynamic_mask");

    protected final BakedModel wrapped;
    protected RonovaPerspectiveModelState parentState = RonovaPerspectiveModelState.IDENTITY;
    @Nullable protected LivingEntity entity;
    @Nullable protected ClientLevel world;

    private final ItemOverrides overrideList = new ItemOverrides() {
        @Override
        public BakedModel resolve(
            BakedModel model,
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity livingEntity,
            int seed
        ) {
            entity = livingEntity;
            world = level != null
                ? level
                : livingEntity != null && livingEntity.level() instanceof ClientLevel clientLevel
                    ? clientLevel
                    : null;
            if (!isMasked()) {
                return model;
            }
            BakedModel resolved = wrapped.getOverrides().resolve(
                wrapped, stack, world, livingEntity, seed);
            return resolved == null ? wrapped : resolved;
        }
    };

    protected RonovaWrappedItemModel(BakedModel wrapped) {
        this.wrapped = wrapped;
    }

    /** Converts a mask sprite to the standard generated-item front/back/edge quads. */
    public static List<BakedQuad> bakeItem(List<TextureAtlasSprite> sprites) {
        List<BakedQuad> quads = new ArrayList<>();
        for (int layer = 0; layer < sprites.size(); layer++) {
            TextureAtlasSprite sprite = sprites.get(layer);
            List<BlockElement> elements = ITEM_MODEL_GENERATOR.processFrames(
                layer, "layer" + layer, sprite.contents());
            for (BlockElement element : elements) {
                for (Map.Entry<Direction, BlockElementFace> face : element.faces.entrySet()) {
                    quads.add(FACE_BAKERY.bakeQuad(
                        element.from,
                        element.to,
                        face.getValue(),
                        sprite,
                        face.getKey(),
                        RonovaPerspectiveModelState.IDENTITY,
                        element.rotation,
                        element.shade,
                        DYNAMIC_MODEL
                    ));
                }
            }
        }
        return quads;
    }

    protected boolean isMasked() {
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(
        @Nullable BlockState state,
        @Nullable Direction side,
        RandomSource random
    ) {
        return wrapped.getQuads(state, side, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return wrapped.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return wrapped.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return wrapped.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return wrapped.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return wrapped.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrideList;
    }

    protected final void renderWrapped(
        ItemStack stack,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay,
        boolean fabulous
    ) {
        renderWrapped(
            stack, poseStack, bufferSource, packedLight, packedOverlay,
            fabulous, Function.identity());
    }

    protected final void renderWrapped(
        ItemStack stack,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        int packedOverlay,
        boolean fabulous,
        Function<VertexConsumer, VertexConsumer> consumerTransform
    ) {
        BakedModel resolved = wrapped.getOverrides().resolve(
            wrapped, stack, world, entity, 0);
        if (resolved == null) {
            resolved = wrapped;
        }

        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        for (BakedModel pass : resolved.getRenderPasses(stack, fabulous)) {
            for (RenderType renderType : pass.getRenderTypes(stack, fabulous)) {
                VertexConsumer consumer = consumerTransform.apply(
                    bufferSource.getBuffer(renderType));
                itemRenderer.renderModelLists(
                    pass, stack, packedLight, packedOverlay, poseStack, consumer);
            }
        }
    }

    @Override
    public RonovaPerspectiveModelState getModelState() {
        return parentState;
    }
}
