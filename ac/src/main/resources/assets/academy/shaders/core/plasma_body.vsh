#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 camspace;

void main() {
    vec4 cam = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * cam;
    camspace = cam.xyz;
}
