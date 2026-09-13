#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 foreground = texture(Sampler0, vec2(texCoord.x + vertexColor.r, texCoord.y));
    float mask = texture(Sampler1, texCoord).a;
    vec3 highlight = vec3(1.0, 0.3, 0.1) * vertexColor.g;
    fragColor = vec4(foreground.rgb + highlight, foreground.a * mask * vertexColor.a)
        * ColorModulator;
}
