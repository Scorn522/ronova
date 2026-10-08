package dev.ronova.coremod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import dev.ronova.coremod.RonovaDominion;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ModelEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Final inventory-model wrapper that preserves Buer's context-specific sword transforms. */
public final class RonovaItemModel extends RonovaWrappedItemModel
    implements RonovaItemRenderer {

    private static final Transformation FLIP_X = new Transformation(
        null, null, new Vector3f(-1.0F, 1.0F, 1.0F), null);
    private static final RonovaPerspectiveModelState HANDHELD = createHandheldState();

    public RonovaItemModel(BakedModel wrapped) {
        super(wrapped);
        parentState = HANDHELD;
    }

    public static void wrapInventoryModel(
        ModelEvent.ModifyBakingResult event,
        ResourceLocation itemId
    ) {
        ModelResourceLocation key = new ModelResourceLocation(
            itemId.getNamespace(), itemId.getPath(), "inventory");
        BakedModel original = event.getModels().get(key);
        if (original == null) {
            RonovaDominion.LOGGER.error("Ronova blade model was not baked: {}", key);
            return;
        }
        if (!(original instanceof RonovaItemModel)) {
            event.getModels().put(key, new RonovaItemModel(original));
        }
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
        if (wrapped instanceof RonovaItemRenderer renderer) {
            renderer.renderItem(
                stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        renderWrapped(
            stack, poseStack, bufferSource, packedLight, packedOverlay, true);
    }

    @Override
    public RonovaPerspectiveModelState getModelState() {
        return HANDHELD;
    }

    private static RonovaPerspectiveModelState createHandheldState() {
        Map<ItemDisplayContext, Transformation> transforms =
            new EnumMap<>(ItemDisplayContext.class);

        transforms.put(ItemDisplayContext.GROUND,
            create(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F));
        transforms.put(ItemDisplayContext.FIXED,
            create(0.0F, 0.0F, 0.0F, 0.0F, 180.0F, 0.0F, 1.0F));
        transforms.put(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            create(0.0F, 4.0F, 0.5F, 0.0F, -90.0F, 55.0F, 0.85F));
        transforms.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
            create(0.0F, 4.0F, 0.5F, 0.0F, 90.0F, -55.0F, 0.85F));

        Transformation firstPerson =
            create(1.13F, 3.2F, 1.13F, 0.0F, -90.0F, 25.0F, 0.68F);
        transforms.put(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, firstPerson);
        transforms.put(ItemDisplayContext.FIRST_PERSON_LEFT_HAND, flipLeft(firstPerson));

        return new RonovaPerspectiveModelState(transforms);
    }

    private static Transformation create(
        float x,
        float y,
        float z,
        float rotationX,
        float rotationY,
        float rotationZ,
        float scale
    ) {
        Quaternionf rotation = new Quaternionf().rotationXYZ(
            (float)Math.toRadians(rotationX),
            (float)Math.toRadians(rotationY),
            (float)Math.toRadians(rotationZ));
        return new Transformation(
            new Vector3f(x / 16.0F, y / 16.0F, z / 16.0F),
            rotation,
            new Vector3f(scale, scale, scale),
            null
        );
    }

    private static Transformation flipLeft(Transformation transformation) {
        return FLIP_X.compose(transformation).compose(FLIP_X);
    }
}
