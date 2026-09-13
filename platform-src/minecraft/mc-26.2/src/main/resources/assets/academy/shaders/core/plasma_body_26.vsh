#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in vec3 Normal;

out vec2 plasmaUv;
out vec4 plasmaColor;
out vec3 plasmaNeighbor2;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    plasmaUv = UV0;
    plasmaColor = Color;
    plasmaNeighbor2 = Normal;
}
