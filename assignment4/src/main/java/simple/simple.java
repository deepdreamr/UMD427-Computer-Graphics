package simple;

import jrtr.*;
import jrtr.glrenderer.*;
import jrtr.swrenderer.*;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.lang.reflect.Array;

import javax.vecmath.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements a simple 3D rendering application using the 3D rendering API 
 * provided by the package {@link jrtr}. Opens a 3D rendering window and 
 * shows a rotating cube. 
 */
public class simple
{	
	static RenderPanel renderPanel;
	static RenderContext renderContext;
	static Shader normalShader;
	static Shader diffuseShader;
	static Material material;
	static GraphSceneManager sceneManager;
	
	static TransformGroup robot;
	static TransformGroup world;
	static TransformGroup bodyhead;
	static TransformGroup bodygroup;
	static TransformGroup headgroup;
	static TransformGroup limbs;
	static TransformGroup leftlegrightarm;
	static TransformGroup rightlegleftarm;
	static TransformGroup cube;
	
	
	
	//static ArrayList<Shape> shape = new ArrayList<Shape>();
	static float current = 0;
	static float currentstep, basicstep, accum = 0;
	static int px = -1, py = -1, x = 0, y = 0;
	static Quat4f ball_rot = new Quat4f(0, 0, 0, 1);
	static Quat4f holding_rot = new Quat4f(0, 0, 0, 1);
	static Object lock = new Object();
	
	
	/**
	 * An extension of {@link GLRenderPanel} or {@link SWRenderPanel} to 
	 * provide a call-back function for initialization. 
	 */ 
	public final static class SimpleRenderPanel extends GLRenderPanel
	{
		/**
		 * Initialization call-back. We initialize our renderer and scene here.
		 * We construct a simple 3D scene consisting of a cube and start a timer 
		 * task to generate an animation.
		 * 
		 * @param r	the render context that is associated with this render panel
		 */
		
		private void transform(Matrix4f trans) 
		{
			Matrix4f temp = new Matrix4f();
			temp.setIdentity();
			trans.setScale(.3f);
			temp.setRow(1, new Vector4f(0, 1.2f, 0, 0));
			trans.mul(temp);
		}
		
		private ShapeNode copy(Shape s) 
		{
			Shape newShape = new Shape(s.getVertexData());
			return new ShapeNode(newShape);
		}
		
		private void createRobot() 
		{
			world = new TransformGroup();
			// world -> robot
			robot = new TransformGroup();
			// robot -> bodyhead -> limbs
			bodyhead = new TransformGroup();
			bodygroup = new TransformGroup();
			headgroup = new TransformGroup();
			limbs = new TransformGroup();
			leftlegrightarm = new TransformGroup();
			rightlegleftarm = new TransformGroup();
			
			world.name = "world";
			robot.name = "robot";
			bodyhead.name = "body+head";
			bodygroup.name = "body";
			headgroup.name = "head";
			limbs.name = "limbs";
			
			bodygroup.transformation.setIdentity();
			headgroup.transformation.setIdentity();
			
			// limbs -> legs -> arms
			TransformGroup rightleg = new TransformGroup();
			TransformGroup leftleg = new TransformGroup();
			TransformGroup rightarm = new TransformGroup();
			TransformGroup leftarm = new TransformGroup();
			
			leftleg.name = "left leg";
			rightleg.name = "right leg";
			leftarm.name = "left arm";
			rightarm.name = "right arm";
			
			Sphere spher = new Sphere(50, 1.f, new float[]{1.f, 1.f, 1.f}, new float[]{1.f, 1.f, 1.f});
			spher.createVertexData(renderContext);
			Shape cylinder = new Shape(makeCylinder(25));
			Shape cube = new Shape(makeCube());
			Shape sphere = new Shape(spher.getVertexData());
			// limbs
			{	
				ArrayList<ShapeNode> armiture = new ArrayList<ShapeNode>();
				ShapeNode c =						copy(cylinder);
				ShapeNode c2 = 						copy(cylinder);
				ShapeNode s = 						  copy(sphere);
				ShapeNode s2 = 						  copy(sphere);
				ShapeNode s3 = 						  copy(sphere);
				ShapeNode cb = 							copy(cube);
				
				//Matrix4f trans = s.getShape().getTransformation(); trans.setTranslation(new Vector3f(0, 2.5f, 0.f));
				
				Matrix4f trans = c.getShape().getTransformation(); trans.setTranslation( new Vector3f(0, -1.5f, 0));
				
				trans = s2.getShape().getTransformation(); trans.setTranslation( new Vector3f(0, -3f, 0.f));
				
				trans = c2.getShape().getTransformation(); trans.setTranslation( new Vector3f(0, -4.5f, 0.f));
				
				trans = s3.getShape().getTransformation(); trans.setTranslation( new Vector3f(0, -5.5f, 0.f));
				
				trans = cb.getShape().getTransformation(); trans.setTranslation( new Vector3f(0, -6.f, 0f));
				
				armiture.add(c); armiture.add(c2); armiture.add(s); armiture.add(s2); armiture.add(s3);
				armiture.add(cb);
				
				//simplicity just make arms and legs the same
				for(ShapeNode node : armiture) 
				{
					leftarm.children.add(node);
					leftleg.children.add(node);				
					rightarm.children.add(node);
					rightleg.children.add(node);
					
				}
				trans = leftarm.getTransformation();
				trans.rotY((float)-Math.PI/6);
				transform(trans);
				trans.setTranslation(new Vector3f(-1.0f, 1.8f, -4.f));
				
				trans = rightarm.getTransformation();
				trans.rotY((float)-Math.PI/6);
				transform(trans);
				trans.setTranslation(new Vector3f(1.0f, 1.8f, -4.f));
				
				trans = leftleg.getTransformation();
				trans.rotY((float)-Math.PI/6);
				transform(trans);
				trans.setTranslation(new Vector3f(-.5f, -0.2f, -4.f));
				
				trans = rightleg.getTransformation();
				trans.rotY((float)-Math.PI/6);
				transform(trans);
				trans.setTranslation(new Vector3f(.5f, -0.2f, -4.f));
			}
			
			rightlegleftarm.children.add(leftarm);
			leftlegrightarm.children.add(leftleg);
			leftlegrightarm.children.add(rightarm);
			rightlegleftarm.children.add(rightleg);
			
			limbs.children.add(rightlegleftarm);
			limbs.children.add(leftlegrightarm);
			
			// body 
			{
				ShapeNode c =						copy(cylinder);
				Matrix4f trans = c.getShape().getTransformation();
				trans.rotY((float)-Math.PI/6);
				trans.setTranslation(new Vector3f(0, 1, -4.f));
				bodygroup.children.add(c);
			}
			
			// head
			{

				ShapeNode s =						copy(sphere);
				Matrix4f trans = s.getShape().getTransformation();
				trans.rotY((float)-Math.PI/6);
				trans.setTranslation(new Vector3f(0, 2.5f, -4.f));
				trans.setScale(.7f);
				headgroup.children.add(s);
			}

			bodyhead.children.add(bodygroup);
			bodyhead.children.add(headgroup);
			
			//transform/scale/rotate these.
			robot.children.add(limbs);
			robot.children.add(bodyhead);
			
			//transform this.
			world.children.add(robot);
			world.children.add(new ShapeNode(new Shape(makeGround())));
		}
		
		private void genTeapots(int num) 
		{
			world = new TransformGroup();
			Shape s;
			try {
				s = new Shape(ObjReader.read("../obj/teapot.obj", 3.0f, renderContext));
			}
			catch (IOException e1) { return; }
			// big allocations 
			for(int j = 0; j < num; j ++) 
			{
				for(int i = 0; i < num; i ++)
				{
					TransformGroup t = new TransformGroup();
					t.getTransformation().setTranslation(new Vector3f(i*4, -4.f, -j*4));
					t.children.add(new ShapeNode(s));
					world.children.add(t);
				}
			}
		}	
		private void makeCubeO() 
		{
			world = new TransformGroup();
			
			// big allocations 
			Shape s = new Shape(makeCube());
			s.getTransformation().setTranslation(new Vector3f(0, -4.f, 0));
			cube = new TransformGroup();
			cube.children.add(new ShapeNode(s));
			world.children.add(cube);
			
		}
		
		public void init(RenderContext r)
		{
			renderContext = r;
										
			// Make a scene manager and add the object
			//createRobot();
			//makeCubeO();
			genTeapots(100);
			sceneManager = new GraphSceneManager(world);
			/*
			 * try { shape.add(new Shape(ObjReader.read("../obj/teapot.obj", 5.0f,
			 * renderContext))); } catch (IOException e1) { return; }
			 */
			
			// Add the scene to the renderer
			renderContext.setSceneManager(sceneManager);
			
			// Load some more shaders
		    normalShader = renderContext.makeShader();
		    try {
		    	normalShader.load("../jrtr/shaders/normal.vert", "../jrtr/shaders/normal.frag");
		    } catch(Exception e) {
		    	System.out.print("Problem with shader:\n");
		    	System.out.print(e.getMessage());
		    }
	
		    diffuseShader = renderContext.makeShader();
		    try {
		    	diffuseShader.load("../jrtr/shaders/diffuse.vert", "../jrtr/shaders/diffuse.frag");
		    } catch(Exception e) {
		    	System.out.print("Problem with shader:\n");
		    	System.out.print(e.getMessage());
		    }

		    // Make a material that can be used for shading
			material = new Material();
			material.shader = diffuseShader;
			material.diffuseMap = renderContext.makeTexture();
			try {
				material.diffuseMap.load("../textures/plant.jpg");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}

			// Register a timer task
		    Timer timer = new Timer();
		    basicstep = 0.01f;
		    currentstep = basicstep;
		    //timer.scheduleAtFixedRate(new AnimationTask(), 0, 10);
		    //timer.scheduleAtFixedRate(new RoboWalkAnimation(), 0, 10);
		}

		/**
		 * Make a mesh for a cube.
		 * 
		 * @return vertexData the data representing the cube mesh
		 */
		private VertexData makeCube()
		{
			// Make a simple geometric object: a cube
		
			// The vertex positions of the cube
			float v[] = {-1,-1,1, 1,-1,1, 1,1,1, -1,1,1,		// front face
				         -1,-1,-1, -1,-1,1, -1,1,1, -1,1,-1,	// left face
					  	 1,-1,-1,-1,-1,-1, -1,1,-1, 1,1,-1,		// back face
						 1,-1,1, 1,-1,-1, 1,1,-1, 1,1,1,		// right face
						 1,1,1, 1,1,-1, -1,1,-1, -1,1,1,		// top face
						-1,-1,1, -1,-1,-1, 1,-1,-1, 1,-1,1};	// bottom face

			// The vertex normals 
			float n[] = {0,0,1, 0,0,1, 0,0,1, 0,0,1,			// front face
				         -1,0,0, -1,0,0, -1,0,0, -1,0,0,		// left face
					  	 0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1,		// back face
						 1,0,0, 1,0,0, 1,0,0, 1,0,0,			// right face
						 0,1,0, 0,1,0, 0,1,0, 0,1,0,			// top face
						 0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0};		// bottom face

			// The vertex colors
			float c[] = {1,1,1, 1,1,1, 1,1,1, 1,1,1,
					     1,1,1, 1,1,1, 1,1,1, 1,1,1,
						 1,1,1, 1,1,1, 1,1,1, 1,1,1,
						 1,1,1, 1,1,1, 1,1,1, 1,1,1,
						 1,1,1, 1,1,1, 1,1,1, 1,1,1,
						 1,1,1, 1,1,1, 1,1,1, 1,1,1};

			// Texture coordinates 
			float uv[] = {0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1};

			// Construct a data structure that stores the vertices, their
			// attributes, and the triangle mesh connectivity
			VertexData vertexData = renderContext.makeVertexData(24);
			vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
			vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
			vertexData.addElement(n, VertexData.Semantic.NORMAL, 3);
			vertexData.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			
			// The triangles (three vertex indices for each triangle)
			int indices[] = {0,2,3, 0,1,2,			// front face
							 4,6,7, 4,5,6,			// left face
							 8,10,11, 8,9,10,		// back face
							 12,14,15, 12,13,14,	// right face
							 16,18,19, 16,17,18,	// top face
							 20,22,23, 20,21,22};	// bottom face

			vertexData.addIndices(indices);
			
			
			return vertexData;
		}
		
		private VertexData makeCylinder(int segments)
		{
			//given number of segments create a triangle
			ArrayList<Float> vertices = new ArrayList<Float>();
			ArrayList<Float> colors = new ArrayList<Float>();
			ArrayList<Float> norms = new ArrayList<Float>();
			ArrayList<Float> uvs = new ArrayList<Float>();
			
			ArrayList<Integer> indices = new ArrayList<Integer>();
			vertices.add(0.f);
			vertices.add(1.f);
			vertices.add(0.f);
			
			vertices.add(0.f);
			vertices.add(-1.f);
			vertices.add(0.f);
			
			Vector3f temp = new Vector3f(0.f, 1.f, 0.f);
			temp.normalize();
			norms.add(temp.x);
			norms.add(temp.y);
			norms.add(temp.z);		

			temp = new Vector3f(0.f, -1.f, 0.f);
			temp.normalize();
			norms.add(temp.x);
			norms.add(temp.y);
			norms.add(temp.z);	

			uvs.add(0.5f);
			uvs.add(1.f);
			uvs.add(0.5f);
			uvs.add(0.f);
			
			colors.add(1.f);
			colors.add(1.f);
			colors.add(1.f);
		
			colors.add(1.f);
			colors.add(1.f);
			colors.add(1.f);
			
			
			for(int ind = 0; ind < segments; ind ++)
			{
				float angle = ((float)ind / (float)segments) * (float)(2*Math.PI);
				float x = (float)Math.cos(angle);
				float z = (float)Math.sin(angle);
				vertices.add(x);
				vertices.add(1.f);
				vertices.add(z);
				
				vertices.add(x);
				vertices.add(-1.f);
				vertices.add(z);

				temp = new Vector3f(x, 1.f, z);
				temp.normalize();
				norms.add(temp.x);
				norms.add(temp.y);
				norms.add(temp.z);		

				temp = new Vector3f(x, -1.f, z);
				temp.normalize();
				norms.add(temp.x);
				norms.add(temp.y);
				norms.add(temp.z);		

				float u =(float) (angle/(2*Math.PI));
		
				uvs.add(u);
				uvs.add(1.f);
				uvs.add(u);
				uvs.add(0.f);
				
				colors.add(1.f);
				//colors.add(1.f);
				colors.add(1.f);
				colors.add(1.f);
				
				colors.add(1.f);
				colors.add(1.f);
				colors.add(1.f);
				
				if (ind > 0) {
					//Top Circle
					indices.add(2 * (ind + 1) - 4 + 2);
					indices.add(0);
					indices.add(2 * (ind + 1) - 2 + 2);
					//Bottom Circle
					indices.add(2 * (ind + 1) - 3 + 2);
					indices.add(1);
					indices.add(2 * (ind + 1) - 1 + 2);
					//Side Cylinder
					indices.add(2 * (ind + 1) - 4 + 2);
					indices.add(2 * (ind + 1) - 3 + 2);
					indices.add(2 * (ind + 1) - 2 + 2);

					indices.add(2 * (ind + 1) - 2 + 2);
					indices.add(2 * (ind + 1) - 3 + 2);
					indices.add(2 * (ind + 1) - 1 + 2);
				}
			}
			
			indices.add(2 * (segments+1) - 4 + 2);
			indices.add(0);
			indices.add(2);
			indices.add(2 * (segments+1) - 3 + 2);
			indices.add(1);
			indices.add(3);
			

			indices.add(2 * (segments + 1) - 4 + 2);
			indices.add(3);
			indices.add(2);

			indices.add(2 * (segments + 1) - 4 + 2);
			indices.add(2 * (segments + 1) - 3 + 2);
			indices.add(3);
			
			float[] v = new float[vertices.size()];
			for(int i = 0; i < v.length; i ++)
				v[i] = vertices.get(i);
			
			float[] c = new float[colors.size()];
			for(int i = 0; i < c.length; i ++)
				c[i] = colors.get(i);
			
			float[] n = new float[norms.size()];
			for(int i = 0; i < n.length; i ++)
				n[i] = norms.get(i);

			float[] uv = new float[uvs.size()];
			for(int i = 0; i < uv.length; i ++)
				uv[i] = uvs.get(i);
			
			// Construct a data structure that stores the vertices, their
			// attributes, and the triangle mesh connectivity
			VertexData vertexData = renderContext.makeVertexData(v.length / 3);
			vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
			vertexData.addElement(n, VertexData.Semantic.NORMAL, 3);
			vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
			vertexData.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			
			
			int[] indexArr = new int[indices.size()];
			for(int i = 0; i < indexArr.length; i ++)
				indexArr[i] = indices.get(i);
			
			vertexData.addIndices(indexArr);
			
			return vertexData;
		}
		
		
		private VertexData makeTorus(int m, int n)
		{
			//given number of segments create a triangle
			ArrayList<Float> vertices = new ArrayList<Float>();
			ArrayList<Float> colors = new ArrayList<Float>();
			ArrayList<Integer> indices = new ArrayList<Integer>();
			/*
			 * vertices.add(0.f); vertices.add(1.f); vertices.add(0.f);
			 * 
			 * vertices.add(0.f); vertices.add(-1.f); vertices.add(0.f);
			 * 
			 * 
			 * colors.add(0.f); colors.add(0.f); colors.add(0.f);
			 * 
			 * colors.add(1.f); colors.add(1.f); colors.add(1.f);
			 */
			
			
			for(int ind = 0; ind < m; ind ++)
			{
				float theta = ((float)ind / ((float)m-1)) * (float)(2*Math.PI);
				//Change 6 dependent on torus rings segment number
				int i = ind*(m-1);
				int i2 = (ind+1)*(m-1);
				for(int circ = 0; circ < n; circ++)
				{
					
					float angle = (float)(circ/((float)n-1))*(float)(2 * Math.PI);
					float x = (1.5f + ((float)Math.cos(angle))) * (float)Math.cos(theta);
					float y = (1.5f + ((float)Math.cos(angle))) * (float)Math.sin(theta);
					float z = (float)Math.sin(angle);
					
					vertices.add(x);
					vertices.add(y);
					vertices.add(z);
					if(circ % 2 == 0) {
						colors.add(1.f);
						colors.add(1.f);
						colors.add(1.f);
					} else {
						colors.add(0.f);
						colors.add(0.f);
						colors.add(0.f);		
					}
					//
					if(m-1 != ind) {
						indices.add(i + circ);
						indices.add(i2 + circ + 1);
						indices.add(i2 +circ);
						indices.add(i + circ);
						indices.add(i + circ + 1);
						indices.add(i2 +circ + 1);
					} else {
						indices.add(i + circ);
						indices.add(i2 + circ + 1);
						indices.add(i2 +circ);
						indices.add(i + circ);
						indices.add(i + circ + 1);
						indices.add(i2 +circ + 1);	
					}
				}
			}
			indices.remove(indices.size()-1);
			indices.remove(indices.size()-1);
			indices.remove(indices.size()-1);
			float[] v = new float[vertices.size()];
			for(int i = 0; i < v.length; i ++)
				v[i] = vertices.get(i);
			
			float[] c = new float[colors.size()];
			for(int i = 0; i < c.length; i ++)
				c[i] = colors.get(i);
			
			// Construct a data structure that stores the vertices, their
			// attributes, and the triangle mesh connectivity
			VertexData vertexData = renderContext.makeVertexData(v.length / 3);
			vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
			vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
			
			
			int[] indexArr = new int[indices.size()];
			for(int i = 0; i < indexArr.length; i ++)
				indexArr[i] = indices.get(i);
			
			vertexData.addIndices(indexArr);
			
			return vertexData;
		}
	
		private VertexData makeGround()
		{
			float[] v = {-10,-2.5f,10, 10,-2.5f,10, -10,-2.5f,-10, 10,-2.5f,-10};
			float[] c = {0,1,0, 1,1,1, 0,1,1, 0,0,1};
			VertexData vertexData = renderContext.makeVertexData(v.length/3);
			vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
			vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
			int[] indices = {0,1,2, 2,3,1};
			vertexData.addIndices(indices);
			return vertexData;
		}
		
	}

	/**
	 * A timer task that generates an animation. This task triggers
	 * the redrawing of the 3D scene every time it is executed.
	 */
	public static class AnimationTask extends TimerTask
	{
		public void run()
		{
			// Update transformation by rotating with angle "currentstep"
		
			TransformGroup tran = cube;
				
			Matrix4f t = tran.getTransformation();
			t.setTranslation(new Vector3f(current*10, 0, 0));
    		current += currentstep;
			/*
			 * Matrix4f rotX = new Matrix4f(); rotX.rotX(currentstep); Matrix4f rotY = new
			 * Matrix4f(); rotY.rotY(currentstep); t.mul(rotX); t.mul(rotY);
			 * 
			 */			//redundant hopefully
			//shape.get(current).setTransformation(t);
			
    		// Trigger redrawing of the render window
    		renderPanel.getCanvas().repaint();
		}
	}
	
	
	public static class RoboWalkAnimation extends TimerTask
	{
		public void run()
		{
			// Update transformation by rotating with angle "currentstep"
    		Matrix4f transform = new Matrix4f(); transform.setIdentity();
    		
    		Matrix4f left =  leftlegrightarm.getTransformation();
    		Matrix4f right = rightlegleftarm.getTransformation();

    		Matrix4f body = bodyhead.getTransformation();
    		//Matrix4f head = headgroup.getTransformation();
    		
    		Matrix4f rotY = new Matrix4f(); rotY.setIdentity();
    		
    		rotY.rotY(accum);
    		transform.mul(rotY);
    		
    		float sign = current;
    		left.set(transform);
    		right.set(transform);
    		
    		for(SceneNode l : leftlegrightarm.children)
    		{    
    			if(l instanceof TransformGroup) {
    				Matrix4f t =  ((TransformGroup)l).transformation;
    				Matrix4f transformation = new Matrix4f();
    				Matrix4f rot = new Matrix4f();
    				
    				transformation.setIdentity();
    				
    				float s = t.getScale();
    				Vector3f trans = new Vector3f();
    				t.get(trans);
    				sign = (float)(Math.PI/4 *Math.cos(current));
    				rot.rotX(sign);
    				
    				transformation.setTranslation(trans);
    				transformation.mul(rot);
    				transformation.setScale(s);
    				t.set(transformation);
    			}
    		}
    		for(SceneNode l : rightlegleftarm.children)
    		{    		
    			if(l instanceof TransformGroup) {
    				Matrix4f t =  ((TransformGroup)l).transformation;
    				Matrix4f transformation = new Matrix4f();
    				Matrix4f rot = new Matrix4f();
    				
    				transformation.setIdentity();
    				
    				float s = t.getScale();
    				Vector3f trans = new Vector3f();
    				t.get(trans);
    				sign = -(float)(Math.PI/4 *Math.cos(current));
    				rot.rotX(sign);
    				
    				transformation.setTranslation(trans);
    				transformation.mul(rot);
    				transformation.setScale(s);
    				t.set(transformation);
    			}
    		}
    		/*m.rotY(accum);
    		
    		m2.rotY(-accum);

    		left.rotY(accum);
    		
    		right.rotY(accum);
    		leftlegrightarm.transformation.set(left);
    		rightlegleftarm.transformation.set(right);
    		*/
    		
    		body.set(transform);
    		
    		// Trigger redrawing of the render window
    		renderPanel.getCanvas().repaint(); 
    		accum = currentstep;
    		current += currentstep;
		}
	}
	
	/**
	 * A mouse listener for the main window. This can be
	 * used to process mouse events.
	 */
	public static class SimpleMouseListener implements MouseListener
	{
    	public void mousePressed(MouseEvent e) {}
    	public void mouseReleased(MouseEvent e) {}
    	public void mouseEntered(MouseEvent e) {}
    	public void mouseExited(MouseEvent e) {}
    	public void mouseClicked(MouseEvent e) 
    	{
    		currentstep = currentstep == 0 ? basicstep : 0;
    	}
	}
	
	/**
	 * A key listener for the main window. Use this to process key events.
	 * Currently this provides the following controls:
	 * 's': stop animation
	 * 'p': play animation
	 * '+': accelerate rotation
	 * '-': slow down rotation
	 * 'd': default shader
	 * 'n': shader using surface normals
	 * 'm': use a material for shading
	 */
	public static class SimpleKeyListener implements KeyListener
	{
		
		private Material material(SceneNode n)
		{
			if(n == null)
				return null;
			Stack<SceneNode> info = new Stack<SceneNode>();
			info.push(n);
			while(!info.isEmpty())
			{
				SceneNode node = info.pop();
				if(node instanceof ShapeNode) {
					return ((ShapeNode) node).getShape().getMaterial();
				}
				else if (node instanceof TransformGroup) 
				{
					TransformGroup tg = (TransformGroup) node;
					for(SceneNode child : tg.children) 
					{
						info.push(child);
					}
				}
			}
			return null;
		}
		
		private void traverse(TransformGroup group, Material m) 
		{
			if(group == null) 
				return;
			Stack<SceneNode> info = new Stack<SceneNode>();
			info.push(group);
			while(!info.isEmpty())
			{
				SceneNode n = info.pop();
				if(n instanceof ShapeNode) {
					ShapeNode temp = (ShapeNode) n; 
					temp.getShape().setMaterial(m);
				}
				else if(n instanceof TransformGroup)
				{
					TransformGroup tg = (TransformGroup) n;
					for(SceneNode child : tg.children) 
					{
						info.push(child);
					}
				}
			}
		}
		
		public void keyPressed(KeyEvent e)
		{
			switch(e.getKeyChar())
			{
				case 's': {
					// Stop animation
					currentstep = 0;
					break;
				}
				case 'p': {
					// Resume animation
					currentstep = basicstep;
					break;
				}
				case '+': {
					// Accelerate roation
					currentstep += basicstep;
					break;
				}
				case '-': {
					// Slow down rotation
					currentstep -= basicstep;
					break;
				}
				case 'n': {
					// Remove material from shape, and set "normal" shader
					traverse(robot, null);
					renderContext.useShader(normalShader);
					break;
				}
				case 'd': {
					// Remove material from shape, and set "default" shader
					traverse(robot, null);
					renderContext.useDefaultShader();
					break;
				}
				case 't': {
					break;
				}
				case 'c': {
					break;
				}
				case 'm': {
					// Set a material for more complex shading of the shape
					if(material(robot) == null) {
						traverse(robot, null);
						
					} else
					{
						traverse(robot, null);
						renderContext.useDefaultShader();
					}
					break;
				}
			}
			
			// Trigger redrawing
			renderPanel.getCanvas().repaint();
		}
		
		public void keyReleased(KeyEvent e)
		{
		}

		public void keyTyped(KeyEvent e)
        {
        }

	}
	
	public static class TrackCameraMouseListener implements MouseMotionListener, MouseListener
	{
		public void mouseDragged(MouseEvent e)
		{
			synchronized(lock) {
				x = e.getX();
				y = e.getY();
			}
		}
		
		public void mouseMoved(MouseEvent e) {}

		public void mouseClicked(MouseEvent e) {}

		public void mousePressed(MouseEvent e) 
		{
			if(e.getButton() == MouseEvent.BUTTON1)
			{
				synchronized(lock) {
					px = e.getX();
					py = e.getY();
					x = px;
					y = py;
				}
			}
		}

		public void mouseReleased(MouseEvent e) 
		{
			synchronized(lock) {
				ball_rot.mul(holding_rot, ball_rot);
				holding_rot = new Quat4f(0, 0, 0, 1);
				px = py = x = y = -1;
			}
		}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		
	}
	
	/**
	 * The main function opens a window 3D rendering window, implemented by the class
	 * {@link SimpleRenderPanel}. {@link SimpleRenderPanel#init} is then called backed 
	 * for initialization automatically by the Java event dispatching thread (EDT), see
	 * <a href="https://stackoverflow.com/questions/7217013/java-event-dispatching-thread-explanation" target="_blank">
	 * this discussion on stackoverflow</a> and <a href="https://en.wikipedia.org/wiki/Event_dispatching_thread" target="_blank">
	 * this explanation on wikipedia</a>. Additional event listeners are added to handle mouse
	 * and keyboard events from the EDT. {@link SimpleRenderPanel#init}
	 * constructs a simple 3D scene, and starts a timer task to generate an animation.
	 */
	public static void main(String[] args)
	{		
		// Make a render panel. The init function of the renderPanel
		// (see above) will be called back for initialization.
		renderPanel = new SimpleRenderPanel();
		
		// Make the main window of this application and add the renderer to it
		JFrame jframe = new JFrame("simple");
		jframe.setSize(500, 500);
		jframe.setLocationRelativeTo(null); // center of screen
		jframe.getContentPane().add(renderPanel.getCanvas());// put the canvas into a JFrame window

		// Add a mouse and key listener
	    //renderPanel.getCanvas().addMouseListener(new SimpleMouseListener());
		TrackCameraMouseListener mouselisten = new TrackCameraMouseListener();
		renderPanel.getCanvas().addMouseListener(mouselisten);
	    renderPanel.getCanvas().addMouseMotionListener(mouselisten);
	    renderPanel.getCanvas().addKeyListener(new SimpleKeyListener());
		renderPanel.getCanvas().setFocusable(true);   	    	    
	    
	    jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    jframe.setVisible(true); // show window
	}
}
