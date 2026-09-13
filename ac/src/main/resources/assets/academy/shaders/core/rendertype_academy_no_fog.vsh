#version 150

// Fog-free textured quad: position + color + UV passthrough. UV2 (lightmap)
// is declared to satisfy the POSITION_COLOR_TEX_LIGHTMAP vertex format but
// never sampled — the mineview highlight must show true texture colours
// without lightmap modulation, matching upstream's ShaderSimple semantics.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    vertexColor = Color;
    texCoord0 = UV0;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
