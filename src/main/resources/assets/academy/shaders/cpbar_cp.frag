#version 120

varying vec2 UV;
varying vec4 Color;

uniform sampler2D samplerTex;
uniform sampler2D samplerIcon;

const vec2 origSize = vec2(964, 147);
const vec2 iconOffset = vec2(857, 43);
const vec2 iconMul = vec2(1.0 / 65, 1.0 / 65);

void main() {
    vec2 temp = ((UV * origSize) - iconOffset) * iconMul;
    
    float maskColor;
    if(temp.s < 0 || temp.s > 1 || temp.t < 0 || temp.t > 1) {
        maskColor = 1;
    } else {
        maskColor = 1 - texture2D(samplerIcon, temp.st).a;
    }
    
    vec4 texColor = texture2D(samplerTex, UV.st);
    gl_FragColor = Color * vec4(texColor.rgb, texColor.a * maskColor);
}