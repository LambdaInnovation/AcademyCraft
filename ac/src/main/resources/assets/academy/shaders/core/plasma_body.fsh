#version 150

uniform int ballCount;
// 16 balls packed as 4 mat4s (one vec4 per column) — the Forge 1.20.1
// ShaderInstance JSON loader only recognizes int/float/matrix uniforms
// (getTypeFromString returns -1 for "vec4"), and GLSL array elements are
// unreliable for glGetUniformLocation across drivers. mat4 columns give each
// ball a plain vec4 via column indexing.
uniform mat4 balls0;
uniform mat4 balls1;
uniform mat4 balls2;
uniform mat4 balls3;
uniform float alpha;
uniform vec4 ColorModulator;

in vec3 camspace;

out vec4 fragColor;

vec4 ball(int i) {
    if (i == 0) return balls0[0];
    if (i == 1) return balls0[1];
    if (i == 2) return balls0[2];
    if (i == 3) return balls0[3];
    if (i == 4) return balls1[0];
    if (i == 5) return balls1[1];
    if (i == 6) return balls1[2];
    if (i == 7) return balls1[3];
    if (i == 8) return balls2[0];
    if (i == 9) return balls2[1];
    if (i == 10) return balls2[2];
    if (i == 11) return balls2[3];
    if (i == 12) return balls3[0];
    if (i == 13) return balls3[1];
    if (i == 14) return balls3[2];
    return balls3[3];
}

float field_density(vec3 position) {
    float ret = 0.0;
    for (int i = 0; i < ballCount; ++i) {
        vec4 b = ball(i);
        float dist = max(0.1, length(position - b.xyz));
        ret += alpha * b.w / (dist * dist);
    }
    return clamp(ret, 0.0, 2.0);
}

vec4 ray_march(vec3 begin, vec3 dir) {
    dir *= 0.15;
    vec3 pos = begin;
    vec4 accum = vec4(0.0);
    for (int i = 0; i < 20 && accum.a < 1.0; ++i) {
        float density = field_density(pos);
        float a = 0.075 * density;
        vec3 c = mix(vec3(0.43, 0.74, 1.0), vec3(0.98, 0.51, 0.92), 1.0 - density / 2.0);
        accum.rgb = mix(accum.rgb, c, a / max(0.001, accum.a + a));
        accum.a += a;
        pos += dir;
    }

    if (accum.a < 0.2) {
        accum.a = 2.0 * accum.a - 0.2;
    }
    return accum;
}

void main() {
    vec3 cam = vec3(camspace.x, camspace.y, -camspace.z);
    vec3 dir = normalize(cam);
    vec4 rc = ray_march(cam - dir * 3.0, dir);
    rc.a = clamp(rc.a, 0.0, 1.0) * (0.5 + alpha * 0.5);
    fragColor = rc * ColorModulator;
}
