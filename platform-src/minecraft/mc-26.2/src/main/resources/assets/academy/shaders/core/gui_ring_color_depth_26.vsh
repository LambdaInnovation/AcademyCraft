#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;

void main() {
    vec4 clip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    clip.z = -0.25 * clip.w;
    gl_Position = clip;
    vertexColor = Color;
}
