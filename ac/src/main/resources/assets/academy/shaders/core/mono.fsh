#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 uv;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, uv);
    float gray = (color.r + color.g + color.b) / 3.0;
    // Honour ColorModulator so callers can fade the quad (RenderSystem.setShaderColor);
    // the skill tree relies on it for the unlearned-icon reveal.
    fragColor = vec4(gray, gray, gray, color.a) * ColorModulator;
}
