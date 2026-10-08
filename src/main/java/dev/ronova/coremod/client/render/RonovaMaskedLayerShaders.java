package dev.ronova.coremod.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ronova.coremod.RonovaDominion;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Shader and render-state owner for the portrait-inside-the-blade pass. */
public final class RonovaMaskedLayerShaders extends RenderType {
    private static final ResourceLocation SHADER_ID =
        ResourceLocation.fromNamespaceAndPath("ronova", "masked_layer");

    private static ShaderInstance maskedShader;
    private static RenderType maskedRenderType;
    private static RenderType alwaysRenderType;

    private RonovaMaskedLayerShaders(
        String name,
        VertexFormat format,
        VertexFormat.Mode mode,
        int bufferSize,
        boolean affectsCrumbling,
        boolean sortOnUpload,
        Runnable setupState,
        Runnable clearState
    ) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload,
            setupState, clearState);
    }

    public static void register(RegisterShadersEvent event) throws IOException {
        maskedRenderType = null;
        alwaysRenderType = null;
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(), SHADER_ID, DefaultVertexFormat.BLOCK),
            shader -> maskedShader = shader
        );
    }

    public static ShaderInstance shader() {
        return maskedShader;
    }

    public static RenderType renderType() {
        if (maskedRenderType == null) {
            maskedRenderType = create(
                "ronova:masked_layer",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                2_097_152,
                true,
                false,
                CompositeState.builder()
                    .setShaderState(shaderState(false))
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setLightmapState(LIGHTMAP)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(NO_TEXTURE)
                    .createCompositeState(true)
            );
        }
        return maskedRenderType;
    }

    public static RenderType alwaysRenderType() {
        if (alwaysRenderType == null) {
            alwaysRenderType = create(
                "ronova:masked_layer_always",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                2_097_152,
                true,
                false,
                CompositeState.builder()
                    .setShaderState(shaderState(true))
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setLightmapState(LIGHTMAP)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setTextureState(NO_TEXTURE)
                    .createCompositeState(true)
            );
        }
        return alwaysRenderType;
    }

    private static RenderStateShard.ShaderStateShard shaderState(boolean always) {
        return new RenderStateShard.ShaderStateShard(RonovaMaskedLayerShaders::shader) {
            @Override
            public void setupRenderState() {
                super.setupRenderState();
                forceUnlockDepthColor();
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.depthMask(true);
                if (always) {
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                }
            }
        };
    }

    public static void configureLayer(
        ResourceLocation layerLocation,
        TextureAtlasSprite maskSprite,
        TextureAtlasSprite layerSprite,
        float parallaxScale,
        float layerOffsetY
    ) {
        ShaderInstance shader = maskedShader;
        if (shader == null) {
            return;
        }
        try {
            Minecraft minecraft = Minecraft.getInstance();
            int blockAtlas = minecraft.getTextureManager()
                .getTexture(InventoryMenu.BLOCK_ATLAS).getId();
            int layerAtlas = minecraft.getTextureManager()
                .getTexture(layerSprite.atlasLocation()).getId();
            shader.setSampler("Sampler0", blockAtlas);
            shader.setSampler("LayerSampler", layerAtlas);

            set(shader, "MaskUVs",
                maskSprite.getU0(), maskSprite.getV0(),
                maskSprite.getU1(), maskSprite.getV1());
            set(shader, "LayerUVs",
                layerSprite.getU0(), layerSprite.getV0(),
                layerSprite.getU1(), layerSprite.getV1());
            set(shader, "ParallaxScale", parallaxScale);
            set(shader, "LayerOffsetY", layerOffsetY);
            set(shader, "DiscardTransparent", 0.0F);
            configureCamera(shader);
        } catch (RuntimeException exception) {
            RonovaDominion.LOGGER.error(
                "Could not configure masked layer {}", layerLocation, exception);
        }
    }

    public static void setDiscardTransparent(boolean enabled) {
        ShaderInstance shader = maskedShader;
        if (shader != null) {
            set(shader, "DiscardTransparent", enabled ? 1.0F : 0.0F);
        }
    }

    public static boolean isShaderPackInUse() {
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            return Boolean.TRUE.equals(
                apiClass.getMethod("isShaderPackInUse").invoke(api));
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void resetCapturedRenderState() {
        if (!isShaderPackInUse()) {
            return;
        }
        try {
            Class<?> stateClass = Class.forName(
                "net.irisshaders.iris.uniforms.CapturedRenderingState");
            Object state = stateClass.getField("INSTANCE").get(null);
            stateClass.getMethod("setCurrentRenderedItem", int.class).invoke(state, 0);
            stateClass.getMethod("setCurrentBlockEntity", int.class).invoke(state, 0);
        } catch (Throwable ignored) {
            // Oculus/Iris is optional; vanilla rendering remains valid.
        }
    }

    private static void configureCamera(ShaderInstance shader) {
        Vector3f forward = Minecraft.getInstance().gameRenderer
            .getMainCamera().getLookVector();
        set(shader, "CameraForward", forward.x, forward.y, forward.z);

        boolean shaderPack = isShaderPackInUse();
        set(shader, "OculusActive", shaderPack ? 1.0F : 0.0F);
        if (!shaderPack) {
            return;
        }
        Matrix4f modelView = capturedMatrix("getGbufferModelView");
        set(shader, "GbufferModelViewMat", modelView);
    }

    private static Matrix4f capturedMatrix(String getterName) {
        try {
            Class<?> stateClass = Class.forName(
                "net.irisshaders.iris.uniforms.CapturedRenderingState");
            Field instanceField = stateClass.getField("INSTANCE");
            Object state = instanceField.get(null);
            Method getter = stateClass.getMethod(getterName);
            Object result = getter.invoke(state);
            return result instanceof Matrix4f matrix ? matrix : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void forceUnlockDepthColor() {
        try {
            Class<?> storage = Class.forName(
                "net.irisshaders.iris.gl.blending.DepthColorStorage");
            storage.getMethod("unlockDepthColor").invoke(null);
        } catch (Throwable ignored) {
            // No shader-pack depth lock is present.
        }
    }

    private static void set(ShaderInstance shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    private static void set(
        ShaderInstance shader,
        String name,
        float x,
        float y,
        float z
    ) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z);
        }
    }

    private static void set(
        ShaderInstance shader,
        String name,
        float x,
        float y,
        float z,
        float w
    ) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(x, y, z, w);
        }
    }

    private static void set(ShaderInstance shader, String name, Matrix4f matrix) {
        if (matrix == null) {
            return;
        }
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            float[] values = new float[16];
            matrix.get(values);
            uniform.set(values);
        }
    }
}
