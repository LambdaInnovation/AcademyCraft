#version 120

varying vec4 Color;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    Color = gl_Color;
}