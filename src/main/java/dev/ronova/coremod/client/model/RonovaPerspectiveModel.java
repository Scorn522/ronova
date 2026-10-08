package dev.ronova.coremod.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

/** Applies Buer-style item transforms before the custom baked-model renderer runs. */
public interface RonovaPerspectiveModel extends BakedModel {
    RonovaPerspectiveModelState getModelState();

    @Override
    default BakedModel applyTransform(
        ItemDisplayContext displayContext,
        PoseStack poseStack,
        boolean leftHand
    ) {
        RonovaPerspectiveModelState state = getModelState();
        if (state == null) {
            getTransforms().getTransform(displayContext).apply(leftHand, poseStack);
            return this;
        }

        Transformation transform = state.getTransform(displayContext);
        Vector3f translation = transform.getTranslation();
        Vector3f scale = transform.getScale();
        poseStack.translate(translation.x(), translation.y(), translation.z());
        poseStack.mulPose(transform.getLeftRotation());
        poseStack.scale(scale.x(), scale.y(), scale.z());
        poseStack.mulPose(transform.getRightRotation());
        if (leftHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
        return this;
    }
}
