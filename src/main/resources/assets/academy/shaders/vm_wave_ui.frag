#version 330 core

uniform sampler2D tex;

in float v_alpha;
in vec2 v_uv;

out vec4 fragColor;

void main() {
	fragColor = texture(tex, v_uv) * v_alpha;
}
