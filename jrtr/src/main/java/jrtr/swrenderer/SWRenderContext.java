package jrtr.swrenderer;

import jrtr.RenderContext;
import jrtr.RenderItem;
import jrtr.SceneManagerInterface;
import jrtr.SceneManagerIterator;
import jrtr.Shader;
import jrtr.Texture;
import jrtr.VertexData;
import jrtr.Material;

import java.awt.image.*;
import javax.vecmath.*;
import java.util.LinkedList;
import java.util.ListIterator;


/**
 * A skeleton for a software renderer. It works in combination with
 * {@link SWRenderPanel}, which displays the output image. In project 2 
 * you will implement your own rasterizer in this class.
 * <p>
 * To use the software renderer, you will simply replace {@link GLRenderPanel} 
 * with {@link SWRenderPanel} in the user application.
 */
public class SWRenderContext implements RenderContext {

	private SceneManagerInterface sceneManager;
	private BufferedImage colorBuffer;
	private double[][] zBuffer;
	private int[] zeroColorBuffer;
	private int width, height;
	
	// Rendering pipeline state variables
	private Matrix4f viewportMatrix;
	private Matrix4f projectionMatrix;
	private float[] vertexColor;
	private float[] vertexNormal;
	private float[] vertexTexCoords;
	
	public SWRenderContext()
	{
		// Initialize rendering pipeline state variables to default values
		projectionMatrix = new Matrix4f();
		viewportMatrix = new Matrix4f();
		zBuffer = new double[width][height];
		vertexColor = new float[3];
		vertexColor[0] = 1.f;
		vertexColor[1] = 1.f;
		vertexColor[2] = 1.f;
		vertexNormal = new float[3];
		vertexTexCoords = new float[2];
	}
	
	public void setSceneManager(SceneManagerInterface sceneManager)
	{
		this.sceneManager = sceneManager;
	}
	
	/**
	 * This is called by the SWRenderPanel to render the scene to the 
	 * software frame buffer.
	 */
	public void display()
	{
		if(sceneManager == null) return;
		
		beginFrame();
	
		SceneManagerIterator iterator = sceneManager.iterator();	
		while(iterator.hasNext())
		{
			draw(iterator.next());
		}		
		
		endFrame();
	}

	/**
	 * This is called by the {@link SWJPanel} to obtain the color buffer that
	 * will be displayed.
	 */
	public BufferedImage getColorBuffer()
	{
		return colorBuffer;
	}
	
	/**
	 * Set a new viewport size. The render context will also need to store
	 * a viewport matrix, which you need to reset here. 
	 */
	public void setViewportSize(int width, int height)
	{
		this.width = width;
		this.height = height;
		
		// Set viewport matrix, note that the y coordinate 
		// is multiplied by -1, because the java BufferedImage
		// has its origin at the top right
		viewportMatrix = new Matrix4f();
		viewportMatrix.setIdentity();
		viewportMatrix.setElement(0,0,(float)width/2.f);
		viewportMatrix.setElement(0,3,(float)width/2.f);
		viewportMatrix.setElement(1,1,-(float)height/2.f);
		viewportMatrix.setElement(1,3,(float)height/2.f);
		viewportMatrix.setElement(2,2,.5f);
		viewportMatrix.setElement(2,3,.5f);
		
		// Allocate framebuffer
		colorBuffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		zeroColorBuffer = new int[width*height];
	}
		
	/**
	 * Clear the framebuffer here.
	 */
	private void beginFrame()
	{
		projectionMatrix = sceneManager.getFrustum().getProjectionMatrix();
		
		// Clear framebuffer
		colorBuffer.setRGB(0, 0, width, height, zeroColorBuffer, 0, width);
		// Clear zBuffer
		zBuffer = new double[width][height];
	}
	
	private void endFrame()
	{		
	}
	
	/**
	 * The main rendering method. This collects all information necessary to render each triangle
	 * and calls @drawTriangle to perform the rasterization.
	 */
	private void draw(RenderItem renderItem)
	{
		VertexData vertexData = renderItem.getShape().getVertexData();
		LinkedList<VertexData.VertexElement> vertexElements = vertexData.getElements();
		int indices[] = vertexData.getIndices();

		// Don't draw if there are no indices
		if(indices == null) return;
		
		// Vertex attributes for a triangle
		float[][] colors = new float[3][3];
		float[][] positions = new float[3][4];
		float[][] normals = new float[3][3];
		float[][] texCoords = new float[3][2];

		// Construct full transformation matrix
		Matrix4f t = new Matrix4f(viewportMatrix);
		t.mul(projectionMatrix);
		t.mul(sceneManager.getCamera().getCameraMatrix());
		t.mul(renderItem.getT());
	     
        // Draw geometry
		int k = 0;	// index of triangle vertex
		for(int j=0; j<indices.length; j++)
		{
			int i = indices[j];
			
			// Iterate over vertex elements, i.e., position, color, normal, texture, etc.
			ListIterator<VertexData.VertexElement> itr = vertexElements.listIterator(0);
			while(itr.hasNext())
			{
				VertexData.VertexElement e = itr.next();
				if(e.getSemantic() == VertexData.Semantic.POSITION)
				{
					Vector4f p = new Vector4f(e.getData()[i*3],e.getData()[i*3+1],e.getData()[i*3+2],1);
					t.transform(p);
					positions[k][0] = p.x;
					positions[k][1] = p.y;
					positions[k][2] = p.z;
					positions[k][3] = p.w;
	
					// Assign the other "state variables" to the current vertex
					colors[k][0] = vertexColor[0];
					colors[k][1] = vertexColor[1];
					colors[k][2] = vertexColor[2];
					
					normals[k][0] = vertexNormal[0];
					normals[k][1] = vertexNormal[1];
					normals[k][2] = vertexNormal[2];
			
					texCoords[k][0] = vertexTexCoords[0];
					texCoords[k][1] = vertexTexCoords[1];
					
					k++;
				}
				// Read the "state variables" for color, normals, textures, if they are available
				if(e.getSemantic() == VertexData.Semantic.COLOR)
				{
					vertexColor[0] = e.getData()[i*3];
					vertexColor[1] = e.getData()[i*3+1];
					vertexColor[2] = e.getData()[i*3+2];
				}
				if(e.getSemantic() == VertexData.Semantic.NORMAL)
				{
					vertexNormal[0] = e.getData()[i*3];
					vertexNormal[1] = e.getData()[i*3+1];
					vertexNormal[2] = e.getData()[i*3+2];
				}
				if(e.getSemantic() == VertexData.Semantic.TEXCOORD)
				{
					vertexTexCoords[0] = e.getData()[i*2];
					vertexTexCoords[1] = e.getData()[i*2+1];
				}
			}
			
			if(k == 3)
			{
				drawTriangle(positions, colors, normals, texCoords, renderItem.getShape().getMaterial());
				k = 0;
			}
		}
	}
	
	private void rasterize(int xStart, int yStart, int xEnd, int yEnd, Matrix3d coef, Matrix3d coef2, float[][] colors) 
	{
		Vector3d iden = new Vector3d(1, 1, 1);
		
		for(int j = yStart; j < yEnd; j++) 
		{
			for(int i = xStart; i < xEnd; i++) 
			{
				// (x/w, y/w, 1) -> (x, y, w)
				Vector3d temp = new Vector3d(i+.5d, j+.5d, 1);
				Vector3d res = new Vector3d();
				
				coef.transform(temp, res);
				// alpha, beta, gamma > 0 point is in our triangle
				if(res.x > 0 && res.y > 0 && res.z > 0) {
					// handle z buffer
					Vector3d res2 = new Vector3d();
					coef2.transform(iden, res2);
					double over_w = temp.dot(res2);
					if(over_w > zBuffer[i][j])
					{
						Vector3d red = new Vector3d(colors[0][0], colors[1][0], colors[2][0]);
						Vector3d green = new Vector3d(colors[0][1], colors[1][1], colors[2][1]);
						Vector3d blue = new Vector3d(colors[0][2], colors[1][2], colors[2][2]);
						
						coef2.transform(red);
						coef2.transform(green);
						coef2.transform(blue);
						double rd = red.dot(temp) / over_w;
						double gd = green.dot(temp) / over_w;
						double bd = blue.dot(temp) / over_w;
						
						colorBuffer.setRGB(i, j, ((int)(255.f*rd) << 16) | ((int)(255.f*gd) << 8) | ((int)(255.f*bd)));

						zBuffer[i][j] = over_w;
					}
				}
			}
		}
	}
	
	/**
	 * Draw a triangle. Implement triangle rasterization here. You will need to include a z-buffer to 
	 * resolve visibility.  
	 */
	void drawTriangle(float positions[][], float colors[][], float normals[][], float texCoords[][], Material mat)
	{							
		// Project vertices and draw. This is only for demonstration purposes and needs to be replace
		// by your triangle rasterization code.
		Matrix3d coef = new Matrix3d();
		for(int i = 0; i < 3; i++) {
			// Set row of matrix to x, y, w for entire matrix
			coef.setRow(i, positions[i][0], positions[i][1], positions[i][3]);
			// NaN check
			if(positions[i][0] != positions[i][0] || positions[i][1] != positions[i][1] 
					|| positions[i][3]  != positions[i][3] ) {
				return;
			}
		}
		coef.invert();
		Matrix3d coef2 = new Matrix3d(coef);
		coef.transpose();
		Vector3d trans1 = new Vector3d(positions[0][0], positions[0][1], positions[0][3]);
		Vector3d trans2 = new Vector3d(positions[1][0], positions[1][1], positions[1][3]);
		Vector3d trans3 = new Vector3d(positions[2][0], positions[2][1], positions[2][3]);
		
		Vector3d[] result = {new Vector3d(), new Vector3d(), new Vector3d()};
		
		coef.transform(trans1, result[0]);
		coef.transform(trans2, result[1]);
		coef.transform(trans3, result[2]);
		
		// All W values are positive
		if(trans1.z > 0 && trans2.z > 0 && trans3.z > 0)
		{
			// Dimensions of our bounding box
			int xEnd = 0, yEnd = 0, xStart = width, yStart = height;
			for(int i = 0; i < 3; i++) {
				int vx = (int)(positions[i][0]/positions[i][3]);
				int vy = (int)(positions[i][1]/positions[i][3]);
				// find the smallest bounding box based off vertices of triangle
				if(vx > xEnd) 
					xEnd = vx;
				if(vy > yEnd)
					yEnd = vy;
				if(xStart > vx)
					xStart = vx;
				if(yStart > vy)
					yStart = vy;
			}
			
			// PerPixel evaluation 
			yEnd =  Math.min(yEnd, height);
			yStart =  Math.max(yStart, 0);
			xEnd =  Math.min(xEnd, width);
			xStart =  Math.max(xStart, 0);
			rasterize(xStart, yStart, xEnd, yEnd, coef, coef2, colors);
		}
		//bounding box is entire image
		else 
		{
			rasterize(0, 0, width, height, coef, coef2, colors);
		}
	}
	
	/**
	 * Does nothing. We will not implement shaders for the software renderer.
	 */
	public Shader makeShader()	
	{
		return new SWShader();
	}
	
	/**
	 * Does nothing. We will not implement shaders for the software renderer.
	 */
	public void useShader(Shader s)
	{
	}
	
	/**
	 * Does nothing. We will not implement shaders for the software renderer.
	 */
	public void useDefaultShader()
	{
	}

	/**
	 * Does nothing. We will not implement textures for the software renderer.
	 */
	public Texture makeTexture()
	{
		return new SWTexture();
	}
	
	public VertexData makeVertexData(int n)
	{
		return new SWVertexData(n);		
	}
}
