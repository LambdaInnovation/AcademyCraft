#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

in vec2 plasmaUv;
in vec4 plasmaColor;
in vec3 plasmaNeighbor2;

out vec4 fragColor;

float plasma_noise(vec3 p, float phase) {
    float angular = sin(atan(p.y, p.x) * 7.0 + phase * 1.7);
    float ripples = sin(length(p) * 16.0 - phase * 2.4);
    float cells = sin(dot(p, vec3(13.7, 19.3, 11.1)) + phase);
    return (angular + ripples + cells) / 3.0;
}

float sample_depth(int sliceIndex) {
    if (sliceIndex == 0) return -1.50;
    if (sliceIndex == 1) return -0.90;
    if (sliceIndex == 2) return -0.30;
    if (sliceIndex == 3) return 0.30;
    if (sliceIndex == 4) return 0.90;
    return 1.50;
}

float neighbor_radius(int radiusCode) {
    if (radiusCode == 1) return 0.50;
    if (radiusCode == 2) return 1.00;
    if (radiusCode == 3) return 1.50;
    if (radiusCode == 4) return 2.00;
    return 0.0;
}

float ball_field(vec3 samplePosition, vec3 center, float radius) {
    vec3 delta = samplePosition - center;
    return radius * radius / max(dot(delta, delta), 0.08);
}

void main() {
    // SubmitNodeCollector does not expose the old per-draw ball-array UBO.
    // UV integer bands therefore carry the slice and two radius codes.
    // Color.rgb and Normal carry the two nearest offsets in primary-radius
    // units. This still falls short of 1.21.1's all-ball 20-step ray march.
    int sliceIndex = int(floor(plasmaUv.x * 0.5));
    int radiusPairCode = int(floor(plasmaUv.y * 0.5));
    int radiusCode = radiusPairCode % 5;
    int secondRadiusCode = radiusPairCode / 5;
    vec2 localUv = mod(plasmaUv, 2.0);
    vec2 planePosition = (localUv * 2.0 - 1.0) * 1.8;
    vec3 samplePosition = vec3(planePosition, sample_depth(sliceIndex));
    vec3 neighborPosition = plasmaColor.rgb * 8.0 - 4.0;
    vec3 secondNeighborPosition = plasmaNeighbor2 * 4.0;
    float neighborRadius = neighbor_radius(radiusCode);
    float secondNeighborRadius = neighbor_radius(secondRadiusCode);

    float phase = GameTime * 120.0;
    float noise = plasma_noise(samplePosition, phase);
    float density = ball_field(samplePosition, vec3(0.0), 1.0);
    if (radiusCode != 0) {
        density += ball_field(samplePosition, neighborPosition, neighborRadius);
    }
    if (secondRadiusCode != 0) {
        // These extra terms make adjacent balls bridge and deform one another
        // instead of rendering as independent translucent sprites.
        density += ball_field(
            samplePosition, secondNeighborPosition, secondNeighborRadius);
    }

    float noisyDensity = density * (1.0 + noise * 0.10);
    float shell = smoothstep(0.22, 0.78, noisyDensity);
    float core = smoothstep(0.72, 1.85, noisyDensity);
    float sparks = pow(max(0.0,
        sin((samplePosition.x - samplePosition.y + samplePosition.z) * 24.0
            + phase * 3.0)), 14.0) * shell;
    // Six ordered samples approximate front-to-back integration. The alpha
    // is deliberately per-sample so translucent blending performs accumulation.
    float opacity = clamp(shell * 0.16 + core * 0.087 + sparks * 0.033, 0.0, 0.30)
        * plasmaColor.a;
    if (opacity < 0.01) {
        discard;
    }

    vec3 cyan = vec3(0.43, 0.82, 1.0);
    vec3 magenta = vec3(0.98, 0.51, 0.92);
    vec3 color = mix(cyan, magenta, clamp(1.0 - density * 0.42 + noise * 0.12, 0.0, 1.0));
    color += vec3(0.30, 0.36, 0.50) * core
        + sparks * vec3(0.55, 0.72, 1.0);
    fragColor = vec4(color, opacity) * ColorModulator;
}
