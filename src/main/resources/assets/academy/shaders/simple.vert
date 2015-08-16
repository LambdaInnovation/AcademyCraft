#version 120

varying vec4 Color;
varying vec2 UV;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    Color = gl_Color;
    UV = gl_MultiTexCoord0.xy;
}