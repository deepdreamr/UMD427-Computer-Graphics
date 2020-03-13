#version 150
// GLSL version 1.50
// Fragment shader for diffuse shading in combination with a texture map

// Uniform variables passed in from host program
uniform sampler2D myTexture;

// Variables passed in from the vertex shader
//in float ndotl[2];

in vec3 frag_cl[8];
in vec3 frag_L[8];
in vec2 frag_texcoord;
in vec4 mViewNorm;
// Output variable, will be written to framebuffer automatically
out vec4 frag_shaded;

void main()
{		
	// The built-in GLSL function "texture" performs the texture lookup
	frag_shaded = vec4(0,0,0,0);
	for(int i = 0; i < 8; i++) {
		float L = max(dot(mViewNorm, vec4(frag_L[i], 0)),0);
		frag_shaded = vec4(frag_cl[i],0) * (texture(myTexture, frag_texcoord) * L) + frag_shaded;
	}
}

