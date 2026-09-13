#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 uv;

void main() {
    uv = UV0;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
