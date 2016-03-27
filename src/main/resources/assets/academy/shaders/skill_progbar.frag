#version 120

uniform sampler2D texCircle;
uniform sampler2D texGradient;
uniform float progress;

varying vec2 uv;

void main() {
    float threshold = texture2D(texGradient, uv).r;

	gl_FragColor = progress > threshold ? texture2D(texCircle, uv) : vec4(0, 0, 0, 0);
}
