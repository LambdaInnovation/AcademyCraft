#version 120

varying vec4 Color;
varying vec2 UV, MaskUV;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    Color = gl_Color;
    UV = gl_MultiTexCoord0.xy;
    MaskUV = gl_MultiTexCoord4.xy;
}