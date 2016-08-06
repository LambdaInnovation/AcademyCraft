#version 330

uniform vec2 screenSize;

// per-vertex
layout (location=0) in vec2 vertexPos;
layout (location=1) in vec2 uv;

// per-instance
layout (location=2) in vec2 offset;
layout (location=3) in float size;
layout (location=4) in float alpha;

out float v_alpha;
out vec2 v_uv;

void main() {
    vec2 pos = (vertexPos * size) + offset;

    gl_Position = vec4(pos / screenSize - vec2(0.5), 0, 1);

    v_alpha = alpha;
    v_uv = uv;
}