#version 150

uniform mat4 projection;
uniform mat4 modelview;
uniform vec4 lightPositions[8]; 
// Color component of diffuse lighting.
uniform vec3 cli[8];
uniform int nLights;

in vec3 normal;
in vec4 position;
in vec2 texcoord;

//out float ndotl[2];
out vec3 frag_cl[8];
out vec3 frag_L[8];
out vec2 frag_texcoord;
out vec4 mViewNorm;
void main()
{	
	// no more than 8 lights
	for(int i = 0; i < 8; i++) 
	{
		frag_cl[i] = 10* cli[i] / pow(1+length(lightPositions[i] - (modelview * position)), 2);
		frag_L[i] = normalize(lightPositions[i] - (modelview * position)).xyz;
	}

	//ndotl[0] = max(dot(modelview * vec4(normal,0), L),0);
	frag_texcoord = texcoord;
	mViewNorm = normalize(modelview * vec4(normal,0));
	gl_Position = projection * modelview * position;
}
