#version 120

varying vec4 Color;

uniform sampler2D gSampler = 0;
uniform vec4 Color = vec4(1, 1, 1, 1);

void main() {
    gl_Color = Color * texture2D(gSampler, gl_MultiTexCoord0.st);
}