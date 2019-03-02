Properties {
    Uniform {
        screenSize = vec2(1, 1);
        tex = sampler2D;
    }
    VertexLayout {
        vertexPos = POSITION;
        uv = UV1;
    }
    Instance {
        offset = vec2(0, 0);
        size = 1.0;
        alpha = 1.0;
    }
}

Settings {
    DepthTest Always;
    DepthMask Off;
    Blend On;
    BlendFunc SrcAlpha OneMinusSrcAlpha;
}

Vertex {
#version 330

uniform vec2 screenSize;

// per-vertex
layout (location=0) in vec2 vertexPos;
layout (location=1) in vec2 uv;

// per-instance
layout (location=2) in vec2 offset;
layout (location=3) in float size;
layout (location=4) in float alpha;

out float v_alpha;
out vec2 v_uv;

void main() {
    vec2 pos = (vertexPos * size) + offset;

    gl_Position = vec4(pos / screenSize - vec2(0.5), 0, 1);

    v_alpha = alpha;
    v_uv = uv;
}
}

Fragment {
#version 330 

uniform sampler2D tex;

in float v_alpha;
in vec2 v_uv;

out vec4 fragColor;

void main() {
	fragColor = texture(tex, v_uv) * v_alpha;
}
}