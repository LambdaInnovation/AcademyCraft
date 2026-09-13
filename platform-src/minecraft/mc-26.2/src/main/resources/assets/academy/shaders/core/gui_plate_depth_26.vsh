#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord;
out vec4 vertexColor;

void main() {
    vec4 clip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    clip.z = -0.5 * clip.w;
    gl_Position = clip;
    texCoord = UV0;
    vertexColor = Color;
}
