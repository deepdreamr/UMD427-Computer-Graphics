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
		public void init(RenderContext r)
		{
			renderContext = r;
										
			// Make a scene manager and add the object
			sceneManager = new SimpleSceneManager();
			Light l = new Light();
			l.type = Light.Type.POINT;
			l.position = new Vector3f( 0.f, 5.f, 0.f);
			l.attenuation = new Vector3f(300, 300, 300);
			l.diffuse = new Vector3f( 100.f, 100.f, 100.f);
			sceneManager.addLight(l);
			
			/*
			 * try { shape.add(new Shape(ObjReader.read("../obj/teapot.obj", 5.0f,
			 * renderContext))); } catch (IOException e1) { return; }
			 */
			PiecewiseBezierCurve piecewis = new PiecewiseBezierCurve(new Point3f(1,2,0),
					new Point3f(2, 2.7818f, 0), new Point3f(3, 2.9749f, 0), new Point3f(4, 2.434f, 0),
					new Point3f(5, 1.566f, 0), new Point3f(6, 1.0251f, 0), new Point3f(7, 1.218f, 0));
			SurfaceConstruction s = new SurfaceConstruction(piecewis, 10, 5, renderContext);
			shape.add(new Shape(s.getVertexData()));
			sceneManager.addShape(shape.get(0));
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
				material.diffuseMap.load("../textures/chess.jpg");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}

			// Register a timer task
		    Timer timer = new Timer();
		    basicstep = 0.01f;
		    currentstep = basicstep;
		    
		    //timer.scheduleAtFixedRate(new AnimationTask(), 0, 10);
		    timer.scheduleAtFixedRate(new MouseRotationTask(), 0, 10);
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
					shape.get(current).setMaterial(null);
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
					sceneManager.removeShape(shape.get(current));
					current = 1;
					sceneManager.addShape(shape.get(current));
					break;
				}
				case 'c': {
					sceneManager.removeShape(shape.get(current));
					current = 0;
					sceneManager.addShape(shape.get(current));
					break;
				}
				case 'm': {
					// Set a material for more complex shading of the shape
					if(shape.get(current).getMaterial() == null) {
						shape.get(current).setMaterial(material);
					} else
					{
						shape.get(current).setMaterial(null);
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
