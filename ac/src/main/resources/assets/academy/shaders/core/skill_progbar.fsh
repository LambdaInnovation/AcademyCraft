#version 150

// Radial wipe: Sampler1's red channel is a per-pixel threshold, so raising
// Progress sweeps the ring art in. `discard` rather than a transparent write —
// a fragment that never reaches the framebuffer cannot be turned opaque by a
// stray blend state, and it leaves the depth buffer alone.
uniform sampler2D Sampler0;   // ring art
uniform sampler2D Sampler1;   // radial threshold mask
uniform vec4 ColorModulator;
uniform float Progress;

in vec2 uv;

out vec4 fragColor;

void main() {
    float threshold = texture(Sampler1, uv).r;
    if (Progress <= threshold) {
        discard;
    }
    fragColor = texture(Sampler0, uv) * ColorModulator;
}
