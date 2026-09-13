#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 source = texture(Sampler0, texCoord);
    float gray = (source.r + source.g + source.b) / 3.0;
    vec4 color = vec4(gray, gray, gray, source.a)
        * vec4(1.0, 1.0, 1.0, vertexColor.a)
        * ColorModulator;
    if (color.a < 0.001) {
        discard;
    }
    fragColor = color;
}
