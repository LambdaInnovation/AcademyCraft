#version 120

varying vec4 Color;
varying vec2 UV, MaskUV;

uniform sampler2D texture, mask;

void main() {
    vec4 texcrl = texture2D(texture, UV.st);
    //gl_FragColor = Color * texcrl;
    gl_FragColor = Color * vec4(texcrl.rgb, texcrl.a * texture2D(mask, MaskUV.st).a);
}