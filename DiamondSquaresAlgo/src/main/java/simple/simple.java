package simple;

import jrtr.*;
import jrtr.VertexData.Semantic;
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
public class simple
{	
	static RenderPanel renderPanel;
	static RenderContext renderContext;
	static Shader normalShader;
	static Shader diffuseShader;
	static Material material;
	static GraphSceneManager sceneManager;
	//static ArrayList<Shape> shape = new ArrayList<Shape>();
	static int current = 0;
	static TransformGroup world = new TransformGroup();
	static TransformGroup land;
	static TransformGroup camera = new TransformGroup();
	static TransformGroup airplane = new TransformGroup();
	static Vector3f posi;
	static float accel = 1.f;
	//static float currentstep, basicstep, accum = 0;
	static int px = -1, py = -1, x = -1, y = -1;
	static Object lock = new Object();
	
	public static ShapeNode getShape(SceneNode n) 
	{
		ShapeNode temp = null;
		if(n instanceof TransformGroup) {
			TransformGroup t = (TransformGroup) n;
			for(SceneNode na : t.children) {
				temp = getShape(na);
				if(temp != null) {
					return temp;
				}
			}	
		}else if (n instanceof ShapeNode) {
			return (ShapeNode) n;
		}
		return null;
	}
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
		/**
		 * ironic since its procedural generated terrain.
		 */
		private void flatland()
		{
			land = new TransformGroup();

			DiamondSquareFractalGen g = new DiamondSquareFractalGen(6, 100);
			ShapeNode terrain = new ShapeNode(new Shape(g.terrainGen(renderContext)));
			land.children.add(terrain);
			
			Shape plane = null;
			plane = getShape(airplane).getShape();
			
			Matrix4f trans = terrain.getShape().getTransformation();
			//trans.setScale(.7f);
			Vector3f center = g.getCenter();center.z*=3;
			trans.setTranslation(center);
			world.children.add(land);
			if(plane == null) return;
			
			float[] colors = new float[plane.getVertexData().getNumberOfVertices()*3];
			for(int i = 0; i < colors.length; i++)
			{
				if((i+1)%3 == 0)
					colors[i] = 1.f;
				else colors[i] = 0.f;
			}
			plane.getVertexData().addElement(colors, Semantic.COLOR, 3);
			plane.getTransformation().rotY((float)-Math.PI/2);
		}
		
		
		public void init(RenderContext r)
		{
			renderContext = r;
										
			// Make a scene manager and add the object
			sceneManager = new GraphSceneManager(world);

			Light l = new Light();
			l.type = Light.Type.POINT;
			l.position = new Vector3f( 0.f, 10.f, 0.f);
			l.attenuation = new Vector3f(300, 300, 300);
			l.diffuse = new Vector3f( 10.f, 100.f, 10.f);
			sceneManager.addLight(l);
			posi = new Vector3f();
			
			try { airplane.children.add(
					new ShapeNode(
							new Shape(ObjReader.read("../obj/airplane.obj", 5.0f,renderContext))
							)
					);
			} catch (IOException e1) { return; }
			
			camera.getTransformation().set(sceneManager.getCamera().getCameraMatrix());
			airplane.children.add(camera);
			world.children.add(airplane);
			flatland();
			
			Vector3f look = sceneManager.getCamera().getLookAtPoint();
			airplane.getTransformation().setTranslation(look);
			
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
		    	diffuseShader.load("../jrtr/shaders/multidiffuse.vert", "../jrtr/shaders/multidiffuse.frag");
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
		    
		    //timer.scheduleAtFixedRate(new AnimationTask(), 0, 10);
		    timer.scheduleAtFixedRate(new MouseRotationTask(), 0, 10);
		}

	}
	
	public static void updateCamera() {
		Matrix4f invert = new Matrix4f(airplane.transformation);
		invert.invert();
		/*
		 * Matrix3f rot = new Matrix3f(); Vector3f pos = new Vector3f(); Vector3f behind
		 * = new Vector3f(0, -0.5f, -5); airplane.transformation.getRotationScale(rot);
		 * invert.get(pos); rot.transform(behind); pos.add(behind);
		 */
		//invert.setTranslation(pos);
		
		
		sceneManager.getCamera().setCameraMatrix(invert);
	}
	
	/**
	 * A timer task that generates an animation. This task triggers
	 * the redrawing of the 3D scene every time it is executed.
	 */
	public static class MouseRotationTask extends TimerTask
	{
		
		public void run()
		{
			Matrix4f t = airplane.getTransformation();
    		
			if(px != x && py != y)
			{
				double diff1 = 0, diff2 = 0;
				
				synchronized(lock) {
					Matrix4f rotX = new Matrix4f();
					rotX.setIdentity();
					Matrix4f rotY = new Matrix4f();
					rotY.setIdentity();
					if(px < x - 10) {
						float width = renderPanel.getCanvas().getWidth();
						diff1 = px - x;
						float angle = (float)(diff1/width);
						if(angle < 0) 
							rotY.rotY(angle);
					}
					else if(px > x + 10) {
						float width = renderPanel.getCanvas().getWidth();
						diff1 = px - x;
						float angle = (float)(diff1/width);
						if(angle > 0 ) 
							rotY.rotY(angle);
						diff1 *= -1;
					}
					if(py < y - 10) {
						float height = renderPanel.getCanvas().getHeight();
						diff2 = py - y;
						float angle = (float)(diff2/height);
						if(angle < 0)
							rotX.rotX(angle);
					} else if(py > y + 10) {
						float height = renderPanel.getCanvas().getHeight();
						diff2 = py - y;
						float angle = (float)(diff2/height);
						if(angle > 0)
							rotX.rotX(angle);
						diff2 *= -1;
					}
					Matrix4f trans = new Matrix4f(); trans.setIdentity();
					if(diff1 > diff2) {
						trans.mul(rotX);
					}
					else {
						trans.mul(rotY);
					} 
					t.mul(trans);
					if(t.equals(new Matrix4f())) {
						t.setIdentity();
					}
				}
			}
    		airplane.getTransformation().set(t);
    		updateCamera();
			 
    		renderPanel.getCanvas().repaint(); 
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
			ShapeNode n = null;
			if(land != null)
				n = getShape(land);
			posi = new Vector3f();
			switch(e.getKeyChar())
			{
				case 'w': {
					posi.z -= accel;
					break;
				}
				case 'a': {
					posi.x -= accel;
					break;
				}
		
				case 's': {
					posi.z += accel;
					break;
				}
				case 'd': {
					posi.x += accel;
					break;
				}
				case '+': {
					accel *= 2.f;
					// Accelerate movement
					break;
				}
				case '-': {
					accel *= 1/2f;
					// Slow down movement
					break;
				}
				case 'n': {
					// Remove material from shape, and set "normal" shader
					if(n != null)
					n.getShape().setMaterial(null);
					renderContext.useShader(normalShader);
					break;
				}
				case 'f': {
					// Remove material from shape, and set "default" shader
					if(n != null)
						n.getShape().setMaterial(null);
					renderContext.useDefaultShader();
					break;
				}
				case 'm': {
					// Set a material for more complex shading of the shape
					if(n != null)
					{
						if(n.getShape().getMaterial() == null)
							n.getShape().setMaterial(material);
						else
						{
							n.getShape().setMaterial(null);
							renderContext.useDefaultShader();
						}
					}
					break;
				}
			}
			Matrix3f rotation = new Matrix3f();
			airplane.getTransformation().getRotationScale(rotation);
			rotation.transform(posi);
			Vector3f temp = new Vector3f();
			airplane.getTransformation().get(temp);
			temp.add(posi);
			airplane.getTransformation().setTranslation(temp);
			updateCamera();
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
			synchronized(lock) 
			{
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
				px = py = x = y = renderPanel.getCanvas().getWidth()/2;
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
