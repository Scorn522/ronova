package dev.ronova.coremod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ronova.coremod.client.model.RonovaItemRenderer;
import dev.ronova.coremod.client.render.RonovaMaskedLayerShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the final baked model own the base and masked passes without invoking BEWLR. */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Shadow
    protected abstract BakedModel getModel(
        ItemStack stack,
        Level level,
        LivingEntity entity,
        int seed
    );

    @Inject(
        method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void ronova$renderMaskedItem(
        LivingEntity entity,
        ItemStack stack,
        ItemDisplayContext displayContext,
        boolean leftHand,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        Level level,
        int packedLight,
        int packedOverlay,
        int seed,
        CallbackInfo callback
    ) {
        BakedModel model = getModel(stack, level, entity, seed);
        if (!(model instanceof RonovaItemRenderer)) {
            return;
        }

        poseStack.pushPose();
        try {
            BakedModel transformed = ForgeHooksClient.handleCameraTransforms(
                poseStack, model, displayContext, leftHand);
            if (!(transformed instanceof RonovaItemRenderer transformedRenderer)) {
                return;
            }

            callback.cancel();
            RonovaMaskedLayerShaders.resetCapturedRenderState();
            poseStack.translate(-0.5D, -0.5D, -0.5D);
            transformedRenderer.renderItem(
                stack,
                displayContext,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
            );
        } finally {
            poseStack.popPose();
        }
    }
}
