package jrtr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Stack;

import javax.vecmath.*;

public class GraphSceneManager implements SceneManagerInterface {

	private SceneNode root;
	private LinkedList<Light> lights;
	private Camera camera;
	private Frustum frustum;
	
	/**
	 * Implement the iterative graph traversal here. 
	 */
	private class GraphSceneManagerItr implements SceneManagerIterator {
		ListIterator<RenderItem> itr;
		public GraphSceneManagerItr(GraphSceneManager sceneManager)
		{
			ArrayList<RenderItem> list = new ArrayList<RenderItem>();
			// Empty
			if(sceneManager.root == null) {
				itr = list.listIterator();
				return;
			}
			
			Stack<SceneNode> sceneInfo = new Stack<SceneNode>();
			
			// for storing previous transformations
			Stack<Matrix4f> transforms = new Stack<Matrix4f>();
			
			sceneInfo.push(root);
			int skipped = 0;
			while(!sceneInfo.isEmpty()) 
			{
				SceneNode scene = sceneInfo.peek();
				if(scene instanceof TransformGroup) 
				{
					TransformGroup transform = (TransformGroup) scene;
					Matrix4f prev =  transforms.isEmpty() ? null : transforms.peek();
					Matrix4f cur = transform.getTransformation();
					
					if(prev != null) {
						// M0 * M1
						cur.mul(prev, cur);
						transforms.pop();
					}
					// pop off current scene 
					sceneInfo.pop();
					for(SceneNode s : transform.children)
					{
						// Store the multiplication for children shapes.
						//if(s instanceof ShapeNode)
						transforms.push(new Matrix4f(cur));
						sceneInfo.push(s);
					}
				}
				else if(scene instanceof ShapeNode)
				{
					ShapeNode shape = (ShapeNode) scene;
					
					Matrix4f t = new Matrix4f();
					t.setIdentity();
					if(!transforms.isEmpty()) {
						t = transforms.peek();
					}
					
					t.mul( shape.getShape().getTransformation());
					
					//skip this shape if its not in the frustum.
					//comment here to erase
					if(frustum.intersection(shape, t))
					{
						transforms.pop();
						sceneInfo.pop();
						skipped ++;
						continue;
					}
					
					list.add(new RenderItem(shape.getShape(), t));
					// pop off last transformation matrix
					transforms.pop();
					// pop off last info information
					sceneInfo.pop();
				}
			}
			//System.out.println(skipped);
			itr = list.listIterator();
		}
		
		public boolean hasNext()
		{
			return itr.hasNext();
		}

		public RenderItem next()
		{
			return itr.next();
		}
	}
	
	public GraphSceneManager(SceneNode root)
	{
		this.root = root;
		camera = new Camera();
		frustum = new Frustum(camera);
		lights = new LinkedList<Light>();
	}
	
	public Camera getCamera()
	{
		return camera;
	}
	
	public Frustum getFrustum()
	{
		return frustum;
	}

	public SceneManagerIterator iterator() {
		return new GraphSceneManagerItr(this);
	}
	
	public void addLight(Light light)
	{
		lights.add(light);
	}
	
	public Iterator<Light> lightIterator()
	{
		return lights.iterator();
	}
}