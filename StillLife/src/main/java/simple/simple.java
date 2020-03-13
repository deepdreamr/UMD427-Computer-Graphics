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
public class simple
{	
	static RenderPanel renderPanel;
	static RenderContext renderContext;
	static Shader normalShader;
	static Shader diffuseShader;
	static ArrayList<Material> material = new ArrayList<Material>();
	static SimpleSceneManager sceneManager;
	static ArrayList<Shape> shape = new ArrayList<Shape>();
	
	
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
			l.position = new Vector3f( 5.f, 2.f, 0.f);
			l.attenuation = new Vector3f(1, 1, 1);
			l.diffuse = new Vector3f( 20.f, 20.f, 20.f);
			sceneManager.addLight(l);
			
			/*
			 * try { shape.add(new Shape(ObjReader.read("../obj/teapot.obj", 5.0f,
			 * renderContext))); } catch (IOException e1) { return; }
			 */
			{
				PiecewiseBezierCurve piecewis = new PiecewiseBezierCurve(new Point3f(1,2,0),
						new Point3f(2, 2.7818f, 0), new Point3f(3, 2.9749f, 0), new Point3f(4, 2.434f, 0),
						new Point3f(5, 1.566f, 0), new Point3f(6, 1.0251f, 0), new Point3f(7, 1.218f, 0));
				SurfaceConstruction s = new SurfaceConstruction(piecewis, 5, 22, renderContext);
				shape.add(new Shape(s.getVertexData()));
				shape.get(0).getTransformation().rotZ((float)-Math.PI/2);

				PiecewiseBezierCurve piecewis2 = new PiecewiseBezierCurve(new Point3f(1, .3758f,0),
						new Point3f(2, .39143f, 0), new Point3f(3, .3923f, 0), new Point3f(4, .31483f, 0),
						new Point3f(4.5f, 1.108f, 0), new Point3f(5.1f, 1.265f, 0), new Point3f(6.5f, 1.125082f, 0));
				SurfaceConstruction s2 = new SurfaceConstruction(piecewis2, 5, 22, renderContext);
				shape.add(new Shape(s2.getVertexData()));
			
				shape.get(1).getTransformation().rotZ((float)-Math.PI/2);
				shape.get(1).getTransformation().setScale(.6f);
				shape.get(1).getTransformation().setTranslation(new Vector3f(-6.5f, -2.5f, 0f));				

				PiecewiseBezierCurve piecewis3 = new PiecewiseBezierCurve(new Point3f(-.3f, .95394f, 0),
						new Point3f(-.2f, .9798f, 0), new Point3f(-.1f, .99499f, 0), new Point3f(0, 1, 0),
						new Point3f(.1f, .99499f, 0), new Point3f(.2f, .9798f, 0), new Point3f(.3f, .95394f, 0));
				SurfaceConstruction s3 = new SurfaceConstruction(piecewis3, 7, 22, renderContext);
				shape.add(new Shape(s3.getVertexData()));
			
				shape.get(2).getTransformation().rotZ((float)-Math.PI/2);
				shape.get(2).getTransformation().setTranslation(new Vector3f(3.5f, -4.5f, 3.f));
				
			}
			sceneManager.addShape(shape.get(0));
			sceneManager.addShape(shape.get(1));
			sceneManager.addShape(shape.get(2));
			
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
			Material mat1 = new Material();
			mat1.shader = diffuseShader;
			mat1.diffuseMap = renderContext.makeTexture();
			try {
				mat1.diffuseMap.load("../textures/vase.jpg");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}
			material.add(mat1);
			shape.get(0).setMaterial(mat1);
			
			Material mat2 = new Material();
			mat2.shader = diffuseShader;
			mat2.diffuseMap = renderContext.makeTexture();
			try {
				mat2.diffuseMap.load("../textures/glass.jpg");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}
			material.add(mat2);
			shape.get(1).setMaterial(mat2);
		
			Material mat3 = new Material();
			mat3.shader = diffuseShader;
			mat3.diffuseMap = renderContext.makeTexture();
			try {
				mat3.diffuseMap.load("../textures/wood.jpg");
			} catch(Exception e) {				
				System.out.print("Could not load texture.\n");
				System.out.print(e.getMessage());
			}
			material.add(mat3);
			shape.get(2).setMaterial(mat3);
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
				case 'n': {
					// Remove material from shape, and set "normal" shader
					for(Shape s : shape) {
						s.setMaterial(null);			
					}
					renderContext.useShader(normalShader);
					break;
				}
				case 'd': {
					// Remove material from shape, and set "default" shader
					for(Shape s : shape) {
						s.setMaterial(null);			
					}
					renderContext.useDefaultShader();
					break;
				}
				case 'm': {
					// Set a material for more complex shading of the shape
					for(int i = 0; i < shape.size(); i++) {
						if(shape.get(i).getMaterial() == null) {
							shape.get(i).setMaterial(material.get(i));
						} else
						{
							shape.get(i).setMaterial(null);
							renderContext.useDefaultShader();
						}
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
		renderPanel.getCanvas().addKeyListener(new SimpleKeyListener());
		renderPanel.getCanvas().setFocusable(true);   	    	    
	    
	    jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    jframe.setVisible(true); // show window
	}
}
