#version 150

uniform sampler2D Sampler0;
uniform sampler2D LayerSampler;
uniform vec4 MaskUVs;
uniform vec4 LayerUVs;
uniform float ParallaxScale;
uniform float LayerOffsetY;
uniform vec3 CameraForward;
uniform float OculusActive;
uniform mat4 GbufferModelViewMat;
uniform float DiscardTransparent;

in vec4 vertexColor;
in vec4 baseColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 maskColor = texture(Sampler0, texCoord0);

    float maskWidth = MaskUVs.z - MaskUVs.x;
    float maskHeight = MaskUVs.w - MaskUVs.y;
    float maskU = (texCoord0.x - MaskUVs.x) / maskWidth;
    float maskV = (texCoord0.y - MaskUVs.y) / maskHeight;

    float layerWidth = LayerUVs.z - LayerUVs.x;
    float layerHeight = LayerUVs.w - LayerUVs.y;
    float maskAspect = maskWidth / maskHeight;
    float layerAspect = layerWidth / layerHeight;

    float scaleU;
    float scaleV;
    float offsetU;
    float offsetV;
    if (maskAspect > layerAspect) {
        scaleV = 1.0;
        scaleU = layerAspect / maskAspect;
        offsetU = (1.0 - scaleU) * 0.5;
        offsetV = 0.0;
    } else {
        scaleU = 1.0;
        scaleV = maskAspect / layerAspect;
        offsetU = 0.0;
        offsetV = (1.0 - scaleV) * 0.5;
    }

    float layerU = mix(LayerUVs.x, LayerUVs.z, offsetU + maskU * scaleU);
    float layerV = mix(LayerUVs.y, LayerUVs.w, offsetV + maskV * scaleV);
    // Positive V sampling moves the portrait itself upward inside the blade.
    layerV += LayerOffsetY * layerHeight;

    vec2 fromCenter = vec2(maskU, maskV) - 0.5;
    vec2 parallaxOffset;
    if (OculusActive > 0.5) {
        vec3 forward = normalize(mat3(GbufferModelViewMat) * CameraForward);
        parallaxOffset = -forward.xy * ParallaxScale
            + fromCenter * ParallaxScale * 0.1;
    } else {
        parallaxOffset = -CameraForward.xy * ParallaxScale
            + fromCenter * ParallaxScale * 0.1;
    }
    layerU += parallaxOffset.x * layerWidth;
    layerV += parallaxOffset.y * layerHeight;

    // Keep parallax inside the stitched portrait sprite instead of sampling a
    // neighbouring atlas entry, a small safety fix over the original Buer pass.
    vec2 halfTexel = 0.5 / vec2(textureSize(LayerSampler, 0));
    layerU = clamp(layerU, LayerUVs.x + halfTexel.x, LayerUVs.z - halfTexel.x);
    layerV = clamp(layerV, LayerUVs.y + halfTexel.y, LayerUVs.w - halfTexel.y);

    vec4 layerColor = texture(LayerSampler, vec2(layerU, layerV));
    float maskAlpha = maskColor.r;

    float unlitStrength = clamp(maskColor.g * 2.0, 0.0, 1.0);
    vec4 lighting = mix(vertexColor, baseColor, mix(0.3, 1.0, unlitStrength));
    vec4 color = layerColor * lighting;
    color.a *= maskAlpha;

    if (DiscardTransparent > 0.5) {
        if (color.a < 0.02) {
            discard;
        }
        fragColor = vec4(color.rgb, 1.0);
        return;
    }

    if (color.a < 0.001) {
        discard;
    }
    fragColor = color;
}
