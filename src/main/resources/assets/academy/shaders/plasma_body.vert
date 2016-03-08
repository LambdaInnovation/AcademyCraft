#version 120

varying vec3 camspace;

vec3 pd(vec4 inp) {
    return inp.xyz / inp.w;
}

void main() {
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	camspace = pd(gl_ModelViewMatrix * gl_Vertex);
}
