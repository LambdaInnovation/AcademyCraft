#version 150

uniform sampler2D Sampler0;   // front_overload.png
uniform sampler2D Sampler1;   // mask.png
uniform vec4 ColorModulator;
uniform float ScrollOffset;
uniform float HighlightAlpha;

in vec2 uv;
out vec4 fragColor;

void main() {
    vec2 scrollUv = vec2(uv.x + ScrollOffset, uv.y);
    vec4 fgColor = texture(Sampler0, scrollUv);
    float mask = texture(Sampler1, uv).a;
    vec3 highlight = vec3(1.0, 0.3, 0.1) * HighlightAlpha;
    fragColor = vec4(fgColor.rgb + highlight, fgColor.a * mask) * ColorModulator;
}
