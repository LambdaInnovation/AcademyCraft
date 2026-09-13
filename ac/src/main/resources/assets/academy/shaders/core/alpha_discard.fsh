#version 150

// Depth-only stamp: drawn with the colour mask off, so the fragment colour is
// irrelevant and no ColorModulator is declared. Only the discard matters.
uniform sampler2D Sampler0;
uniform float AlphaThreshold;

in vec2 uv;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, uv);
    if (color.a < AlphaThreshold) {
        discard;
    }
    fragColor = color;
}
