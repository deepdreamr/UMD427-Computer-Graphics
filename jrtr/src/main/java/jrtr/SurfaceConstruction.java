package jrtr;

import java.util.ArrayList;

import javax.vecmath.Vector3f;

import jrtr.BezierCurve.BernsteinInfo;
import jrtr.VertexData.Semantic;

public class SurfaceConstruction {
	
	private VertexData vertexData;

	/**
	 * @param curve - the piecewise curve function defined by ... control points,
	 * @param n - number of line segments defined for circle to revolve around,
	 * @param k - number of line segments defined for curve function,
	 * @param re - the render context.
	 */
	public SurfaceConstruction(PiecewiseBezierCurve curve, int n, int k, RenderContext re)
	{

		float[] vertices = new float[3*k*n];
		//int[] indices = new int[6*k*n];

		ArrayList<Integer> indices = new ArrayList<Integer>();
		float[] normals = new float[3*k*n];
		float[] colors = new float[3*k*n];
		float[] texcoords = new float[2*k*n];

		BernsteinInfo[] info = curve.BernsteinPoly(k);

		for(int j = 0; j < k; j++)
		{
			float u = j /(float)(k-1);
			float x = info[j].pnt.x;
			float y = info[j].pnt.y;
			
			for(int i = 0; i < n; i++)
			{
				float v = 2*(float)Math.PI*i /(n);

				vertices[3*(j*n+i)+0] = x;
				vertices[3*(j*n+i)+1] = y*(float)Math.cos(v);
				vertices[3*(j*n+i)+2] = y*(float)Math.sin(v);

				//would be easier with triangle strips.
				if(j+1 < k) {
					if(i+1 < n) {
						indices.add(j*n 			+ i);
						indices.add(j*n 			+ (i+1));
						indices.add((j+1)*n			+ (i));
						indices.add(j*n 			+ (i+1));
						indices.add((j+1)*n 		+ (i));
						indices.add((j+1)*n 		+ (i+1));
					} else {
						indices.add(j*n 			+ i);
						indices.add((j-1)*n 		+(i+1));
						indices.add((j+1)*n			+ (i));
						indices.add((j-1)*n 		+ (i+1));
						indices.add((j+1)*n 		+ (i));
						indices.add((j)*n 			+ (i+1));
					}
				}
				
				// tangent vectors
				
				Vector3f temp = new Vector3f(-info[j].vec.y, 
						info[j].vec.x*(float)Math.cos(v), info[j].vec.x*(float)Math.sin(v));
				temp.normalize();
				normals[3*(j*n+i)+0] = temp.x;
				normals[3*(j*n+i)+1] = temp.y;
				normals[3*(j*n+i)+2] = temp.z;//cross.z;
				colors[3*(j*n+i)+0] = 1.f;
				colors[3*(j*n+i)+1] = 1.f;
				colors[3*(j*n+i)+2] = 1.f;
				
				texcoords[2*(j*n+i)+0] = u;
				texcoords[2*(j*n+i)+1] = v*(float)n /(float)(Math.PI*2*n-1);
			}
		}
		
		// precision * circle length
		vertexData = re.makeVertexData(vertices.length/3);

		vertexData.addElement(vertices, Semantic.POSITION, 3);
		vertexData.addElement(normals, Semantic.NORMAL, 3);
		vertexData.addElement(texcoords, Semantic.TEXCOORD, 2);
		vertexData.addElement(colors, Semantic.COLOR, 3);
		
		int[] indexArr = new int[indices.size()];
		for(int i=0;i<indexArr.length;i++) {
			indexArr[i] = indices.get(i); 
		}
		vertexData.addIndices(indexArr);
	}
	
	public VertexData getVertexData()
	{
		return vertexData;
	}
	
}
