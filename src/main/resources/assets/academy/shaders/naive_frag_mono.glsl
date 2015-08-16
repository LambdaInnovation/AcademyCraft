#version 120

in vec4 Color;
in vec2 UV;

uniform sampler2D sampler;

void main() {
    vec4 result = Color * texture2D(sampler, UV);
    float c = (result.r + result.g + result.b) / 3;
    gl_FragColor = vec4(c, c, c, result.a);
}