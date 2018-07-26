#version 120

varying Vec3d camspace;

Vec3d pd(vec4 inp) {
    return inp.xyz / inp.w;
}

void main() {
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	camspace = pd(gl_ModelViewMatrix * gl_Vertex);
}
