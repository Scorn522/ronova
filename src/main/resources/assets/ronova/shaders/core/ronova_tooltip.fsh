#version 150

uniform sampler2D Sampler0;
uniform float Time;
uniform vec2 TooltipSize;
uniform float ShiftOpen;

in vec2 texCoord0;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

vec4 eye(vec2 uv, vec2 center, vec2 scale, float frame) {
    vec2 local = (uv - center) / scale + 0.5;
    float inside = step(0.0, local.x) * step(local.x, 1.0)
                 * step(0.0, local.y) * step(local.y, 1.0);
    // Keep bilinear filtering inside the selected 512x256 cell. Without this
    // half-texel inset a blink can borrow a bright edge from its neighbour.
    vec2 cellUv = clamp(local, vec2(0.5 / 512.0, 0.5 / 256.0),
        vec2(1.0 - 0.5 / 512.0, 1.0 - 0.5 / 256.0));
    vec2 atlasUv = vec2((clamp(frame, 0.0, 4.0) + cellUv.x) * 0.2, cellUv.y);
    return texture(Sampler0, atlasUv) * inside;
}

void main() {
    vec2 uv = texCoord0;
    float t = Time;
    vec2 safeSize = max(TooltipSize, vec2(1.0));

    vec3 top = vec3(0.055, 0.020, 0.050);
    vec3 bottom = vec3(0.018, 0.006, 0.015);
    vec3 color = mix(top, bottom, uv.y);

    vec2 fogUv = uv * vec2(5.2, 3.1);
    fogUv.x += t * 0.055;
    fogUv.y += sin(t * 0.13 + uv.x * 5.0) * 0.24;
    float fogA = noise(fogUv);
    float fogB = noise(fogUv * 1.9 - vec2(t * 0.041, t * 0.026));
    float fog = smoothstep(0.43, 0.86, fogA * 0.62 + fogB * 0.48);
    color += vec3(0.23, 0.010, 0.045) * fog * (0.75 + ShiftOpen * 0.32);

    float cycle = mod(t, 5.4);
    float frame = ShiftOpen > 0.5 ? 3.0 : 2.0;
    if (ShiftOpen < 0.5 && cycle > 4.68 && cycle <= 5.02) {
        float closing = clamp((cycle - 4.68) / 0.34, 0.0, 1.0);
        frame = floor(mix(2.0, 0.0, closing) + 0.5);
    } else if (ShiftOpen < 0.5 && cycle > 5.02) {
        float opening = clamp((cycle - 5.02) / 0.30, 0.0, 1.0);
        frame = floor(mix(0.0, 2.0, opening) + 0.5);
    }

    vec4 mainEye = eye(uv, vec2(0.79, 0.22), vec2(0.46, 0.27), frame);
    vec4 leftEye = eye(uv, vec2(0.10, 0.79), vec2(0.24, 0.15), max(frame - 1.0, 1.0));
    vec4 rightEye = eye(uv, vec2(0.93, 0.74), vec2(0.20, 0.13), max(frame - 1.0, 1.0));
    float pulse = 0.88 + 0.12 * sin(t * 1.35);
    float mainStrength = (0.13 + ShiftOpen * 0.13) * pulse;
    float dimStrength = 0.055 + ShiftOpen * 0.035;
    color = mix(color, mainEye.rgb, mainEye.a * mainStrength);
    color = mix(color, leftEye.rgb, leftEye.a * dimStrength);
    color = mix(color, rightEye.rgb, rightEye.a * dimStrength);

    vec2 particleGrid = floor(uv * vec2(58.0, 24.0) + vec2(0.0, -t * 1.7));
    float particleSeed = hash(particleGrid);
    float particle = step(0.988, particleSeed);
    particle *= 0.38 + 0.22 * sin(t * 1.8 + particleSeed * 18.0);
    color += vec3(0.92, 0.08, 0.20) * particle * (0.12 + ShiftOpen * 0.08);

    float centerVeil = 1.0 - smoothstep(0.08, 0.68, length((uv - 0.5) * vec2(0.76, 1.0)));
    color *= 1.0 - centerVeil * (0.20 + ShiftOpen * 0.08);
    float readingVeil = 1.0 - smoothstep(0.52, 0.88, uv.x);
    color *= 1.0 - readingVeil * (0.055 + ShiftOpen * 0.025);
    float vignette = 1.0 - smoothstep(0.28, 0.94, length((uv - 0.5) * vec2(1.15, 1.0)));
    color *= 0.66 + 0.34 * vignette;

    float edgePixels = min(
        min(uv.x, 1.0 - uv.x) * safeSize.x,
        min(uv.y, 1.0 - uv.y) * safeSize.y
    );
    float outerBorder = 1.0 - smoothstep(0.38, 0.92, edgePixels);
    float innerBorder = smoothstep(0.72, 1.08, edgePixels)
        * (1.0 - smoothstep(1.12, 1.88, edgePixels));
    float borderPulse = 0.82 + 0.18 * sin(t * 1.18 + uv.x * 5.0 - uv.y * 3.0);
    vec3 outerColor = vec3(0.31, 0.010, 0.065);
    vec3 innerColor = mix(vec3(0.40, 0.020, 0.085), vec3(0.82, 0.075, 0.18), borderPulse);
    color = mix(color, outerColor, outerBorder * 0.94);
    color = mix(color, innerColor, innerBorder * (0.28 + ShiftOpen * 0.07));

    vec2 aspect = vec2(safeSize.x / safeSize.y, 1.0);
    float cornerGlow = 0.0;
    cornerGlow += exp(-length((uv - vec2(0.0, 0.0)) * aspect) * 34.0);
    cornerGlow += exp(-length((uv - vec2(1.0, 0.0)) * aspect) * 34.0);
    cornerGlow += exp(-length((uv - vec2(0.0, 1.0)) * aspect) * 34.0);
    cornerGlow += exp(-length((uv - vec2(1.0, 1.0)) * aspect) * 34.0);
    color += vec3(0.44, 0.025, 0.10) * cornerGlow * (0.32 + borderPulse * 0.14);

    fragColor = vec4(color, 0.965);
}
