#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    if (vertexColor.r <= texture(Sampler1, texCoord).r) {
        discard;
    }
    fragColor = texture(Sampler0, texCoord)
        * vec4(1.0, 1.0, 1.0, vertexColor.a)
        * ColorModulator;
}
