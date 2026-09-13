#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord;
out vec4 vertexColor;

void main() {
    vec4 clip = ProjMat * ModelViewMat * vec4(Position, 1.0);
    // The layer byte (Color.g: 64 → plate, 96 → ring) selects one of the two
    // EXACT constants the other depth shaders use (gui_plate_depth_26: -0.5,
    // gui_ring_color_depth_26: -0.25). The icons test EQUAL and the
    // connection lines NOT_EQUAL against those constants, so a byte-derived
    // value like (g*2-1) — which never equals them in floating point — would
    // reject every icon and mis-cut every line.
    clip.z = (Color.g > 0.3 ? -0.25 : -0.5) * clip.w;
    gl_Position = clip;
    texCoord = UV0;
    vertexColor = Color;
}
