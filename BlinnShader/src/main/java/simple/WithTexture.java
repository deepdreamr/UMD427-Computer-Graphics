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
import java.util.Timer;
import java.util.TimerTask;

/**
 * Implements a simple 3D rendering application using the 3D rendering API 
 * provided by the package {@link jrtr}. Opens a 3D rendering window and 
 * shows a rotating cube. 
 */
public class WithTexture
{	
	static RenderPanel renderPanel;
	static RenderContext renderContext;
	static Shader normalShader;
	static Shader diffuseShader;
	static Material material;
	static Material material2;
	static SimpleSceneManager sceneManager;
	static ArrayList<Shape> shape = new ArrayList<Shape>();
	static int current = 0;
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
		private void createCar() 
		{
			shape.add(new Shape(makeGround()));
			shape.add(new Shape(makeCube()));
			shape.add(new Shape(makeCylinder(20)));
			
			sceneManager.addShape(shape.get(0));
			sceneManager.addShape(shape.get(1));
			sceneManager.addShape(shape.get(2));
			
		}
		
		public void setShape(int ind, Shader s)
		{
			material2 = new Material();
			material2.shader = s;
			material2.diffuseMap = renderContext.makeTexture();
			try {
				material2.diffuseMap.load("../textures/plant.jpg");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}
			shape.get(ind).setMaterial(material2);
		}
		
		public void init(RenderContext r)
		{
			renderContext = r;
										
			// Make a scene manager and add the object
			sceneManager = new SimpleSceneManager();

			Light l = new Light();
			l.type = Light.Type.POINT;
			l.position = new Vector3f( 5.f, 25.f, 0.f);
			//l.attenuation = new Vector3f(10, 10, 10);
			l.diffuse = new Vector3f( 5.f, 5.f, 5.f);
			l.specular = new Vector3f( 13.f, 13.f, 13.f);
			l.ambient = new Vector3f( .0001f, .0f, .0f);
			
			
			sceneManager.addLight(l);
			Light l2 = new Light();
			l2.position = new Vector3f( -7.f, -5.f, 2.f);
			l2.diffuse = new Vector3f(5f, 5f, 5f);
			l2.specular = new Vector3f( 14.f, 1.f, 1.f);
			l2.ambient = new Vector3f( .0001f, .0f, .0f);
			sceneManager.addLight(l2);


			//shape.add(new Shape(makeGround()));
			try {
				shape.add(new Shape(ObjReader.read("../obj/teapot.obj",
						5.0f,renderContext))); 
			} catch (IOException e1) { return; }

			shape.add(new Shape(makeCube()));
			
			shape.get(1).getTransformation().setTranslation(new Vector3f(0, 3, -1));			
			shape.get(0).getTransformation().setTranslation(new Vector3f(0, -1, 0));

			sceneManager.addShape(shape.get(1));
			sceneManager.addShape(shape.get(0));
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
		    	diffuseShader.load("../jrtr/shaders/blinnshader2.vert", "../jrtr/shaders/blinnshader2.frag");
		    } catch(Exception e) {
		    	System.out.print("Problem with shader:\n");
		    	System.out.print(e.getMessage());
		    }
			
		    // Make a material that can be used for shading
			material = new Material();
			material.shader = diffuseShader;
			material.diffuseMap = renderContext.makeTexture();
			try {
				material.diffuseMap.load("../textures/wood.jpg");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}

			material2 = new Material();
			material2.shader = diffuseShader;
			material2.diffuseMap = renderContext.makeTexture();
			try {
				material2.diffuseMap.load("../textures/plant.jpg");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}
			/*
			 * renderContext.useDefaultShader(); setShape(1, diffuseShader);
			 */
			renderContext.useShader(diffuseShader);
			
			// Register a timer task
		    Timer timer = new Timer();
		    basicstep = 0.01f;
		    currentstep = basicstep;
		    
		    //timer.scheduleAtFixedRate(new AnimationTask(), 0, 10);
		    timer.scheduleAtFixedRate(new MouseRotationTask(), 0, 10);
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
			float c[] = {1,0,0, 1,1,0, 1,0,0, 1,0,0,
					     0,1,0, 0,1,0, 0,1,0, 0,1,0,
						 1,1,0, 1,1,0, 1,1,0, 1,1,0,
						 0,1,1, 0,1,1, 0,1,1, 0,1,1,
						 0,0,1, 0,0,1, 0,0,1, 0,0,1,
						 1,0,1, 1,0,1, 1,0,1, 1,0,1};

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
			
			uvs.add(0.f);
			uvs.add(0.f);
			
			uvs.add(0.f);
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
			
			// Construct a data structure that stores the vertices, their
			// attributes, and the triangle mesh connectivity
			VertexData vertexData = renderContext.makeVertexData(v.length / 3);
			vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
			vertexData.addElement(n, VertexData.Semantic.NORMAL, 3);
			vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
			
			
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
			float[] v = {-10,-1.5f,10, 10,-1.5f,10, -10,-1.5f,-10, 10,-1.5f,-10};
			float[] n = {0,0,1, 0,0,1, 0,0,1, 0,0,1};
			float[] c = {0,1,0, 1,1,1, 0,1,1, 0,0,1};
			
			VertexData vertexData = renderContext.makeVertexData(v.length/3);
			vertexData.addElement(c, VertexData.Semantic.COLOR, 3);
			vertexData.addElement(v, VertexData.Semantic.POSITION, 3);
			vertexData.addElement(n, VertexData.Semantic.NORMAL, 3);
			
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
    		Matrix4f t = shape.get(current).getTransformation();
    		Matrix4f rotX = new Matrix4f();
    		rotX.rotX(currentstep);
    		Matrix4f rotY = new Matrix4f();
    		rotY.rotY(currentstep);
    		t.mul(rotX);
    		t.mul(rotY);

    		shape.get(current).setTransformation(t);
    		
    		// Trigger redrawing of the render window
    		renderPanel.getCanvas().repaint(); 
		}
	}
	
	public static class MouseRotationTask extends TimerTask
	{
		
		public void run()
		{
			Matrix4f t = shape.get(current).getTransformation();
    		if(t.m00 != t.m00) {
    			t = new Matrix4f();
    			t.setIdentity();
    			shape.get(current).setTransformation(t);
    		}
			if(px != x && py != y)
			{
				double width = renderPanel.getCanvas().getWidth() / 2,
					height = renderPanel.getCanvas().getHeight() / 2;
				double x1, x2, y1, y2, z1, z2;
				synchronized(lock) {
					x1 = px/width - 1.0;
					y1 = 1.0 - py/height;
					x2 = x/width - 1.0;
					y2 = 1.0 - y/height;
				}
				double zTemp = 1 - x1 * x1 - y1 * y1;
				z1 = zTemp > 0 ? Math.sqrt(zTemp) : 0;
				zTemp = 1 - x2 * x2 - y2 * y2;
				z2 = zTemp > 0 ? Math.sqrt(zTemp) : 0;
				Vector3d t1 = new Vector3d(x1, y1, z1);
				Vector3d t2 = new Vector3d(x2, y2, z2);

				t1.normalize();
				t2.normalize();
				
				Vector3d axes = new Vector3d();
				axes.cross(t1, t2);
				axes.normalize();
				
				double theta = t1.angle(t2);
				AxisAngle4d ang = new AxisAngle4d(axes, (float)theta);
				holding_rot.set(ang);
				Quat4f temp = new Quat4f();
				temp.mul(holding_rot, ball_rot);

				t.setRotation(temp);
			}
    		shape.get(current).setTransformation(t);
    		
    		renderPanel.getCanvas().repaint(); 
		}
	}
	
	public static class CarRevolvingAnimation extends TimerTask
	{
		public void run()
		{
			// Update transformation by rotating with angle "currentstep"
    		Matrix4f m = new Matrix4f();
    		Matrix4f m2 = new Matrix4f();
    		m.setIdentity();
    		m2.setIdentity();
    		
    		Matrix4f rot = new Matrix4f();
    		Matrix4f rot2 = new Matrix4f();
    		Matrix4f rot3 = new Matrix4f();
    		
    		Matrix4f trans = new Matrix4f(1.f, 0.f, 0.f, 5.f,
					  0.f, 1.f, 0.f, 0.f,
					  0.f, 0.f, 1.f, 0.f,
					  0.f, 0.f, 0.f, 1.f);
    		Matrix4f trans2 = new Matrix4f(1.f, 0.f, 0.f, 0.f,
					  0.f, 1.f, 0.f, 1.f,
					  0.f, 0.f, 1.f, 0.f,
					  0.f, 0.f, 0.f, 1.f);
	
    		
    		rot.rotY(accum);
    		rot2.rotZ(-(float)Math.PI/2);
    		rot3.rotY(-accum);

    		m.mul(rot);
    		m.mul(trans);
    		m.mul(trans2);
    		
    		
    		m2.mul(rot);
    		m2.mul(trans);
    		m2.mul(rot2);
    		m2.mul(rot3);
   		
    		shape.get(1).setTransformation(m);
    		shape.get(2).setTransformation(m2);
    		
    		// Trigger redrawing of the render window
    		renderPanel.getCanvas().repaint(); 
    		accum += currentstep;
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
					shape.get(0).setMaterial(null);
					renderContext.useShader(normalShader);
					break;
				}
				case 'd': {
					// Remove material from shape, and set "default" shader
					shape.get(current).setMaterial(null);
					renderContext.useDefaultShader();
					break;
				}
				case 't': {
					sceneManager.removeShape(shape.get(1));
					current = 1;
					sceneManager.addShape(shape.get(1));
					break;
				}
				case 'c': {
					sceneManager.removeShape(shape.get(1));
					current = 0;
					sceneManager.addShape(shape.get(1));
					break;
				}
				case 'm': {
					// Set a material for more complex shading of the shape
					if(shape.get(1).getMaterial() == null) {
						shape.get(1).setMaterial(material);
						shape.get(0).setMaterial(material2);
					} else
					{
						shape.get(1).setMaterial(null);
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
