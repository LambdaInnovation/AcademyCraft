#version 150

// Fog-free counterpart of the vanilla text shader: texture × vertex colour ×
// ColorModulator, no linear_fog. Every vanilla 1.20.1 shader includes fog, so
// the MineDetect ore highlights (drawn while the skill's own blindness fog is
// active) need this custom program — upstream HandlerRender disables GL_FOG
// for the whole mineview pass for exactly this reason.

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    fragColor = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
}
