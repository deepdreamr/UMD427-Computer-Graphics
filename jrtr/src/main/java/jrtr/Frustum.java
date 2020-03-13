package jrtr;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

/**
 * Stores the specification of a viewing frustum, or a viewing
 * volume. The viewing frustum is represented by a 4x4 projection
 * matrix. You will extend this class to construct the projection 
 * matrix from intuitive parameters.
 * <p>
 * A scene manager (see {@link SceneManagerInterface}, {@link SimpleSceneManager}) 
 * stores a frustum.
 */
public class Frustum {

	private Matrix4f projectionMatrix;
	private Camera camera;
	private Vector3f[] normals;
	
	/**
	 * Construct a default viewing frustum. The frustum is given by a 
	 * default 4x4 projection matrix.
	 */
	public Frustum(Camera camera)
	{
		projectionMatrix = new Matrix4f();
		/*float f[] = {2.f, 0.f, 0.f, 0.f, 
					 0.f, 2.f, 0.f, 0.f,
				     0.f, 0.f, -1.02f, -2.02f,
				     0.f, 0.f, -1.f, 0.f};*/
		
		float f[] = {1.f, 0.f, 0.f, 0.f,
			0.f, 1.f, 0.f, 0.f,
			0.f, 0.f, -1.02f, -2.02f,
			0.f, 0.f, -1.f, 0.f};
		this.camera = camera;

		normals = new Vector3f[] {
				new Vector3f(1/((float)Math.sqrt(2)), 0, 1/((float)Math.sqrt(2))),
				new Vector3f(-1/((float)Math.sqrt(2)), 0, 1/((float)Math.sqrt(2))),
				new Vector3f(0, 1/((float)Math.sqrt(2)), 1/((float)Math.sqrt(2))),
				new Vector3f(0, -1/((float)Math.sqrt(2)), 1/((float)Math.sqrt(2))) };
		projectionMatrix.set(f);
	}
	
	public boolean intersection(ShapeNode shape, Matrix4f transformation)
	{	
		Point3f cent = shape.getBoundingObjCent();
		float radius = shape.getBoundingRadius();
		
		Matrix4f modelview = new Matrix4f(camera
			.getCameraMatrix());
		modelview.mul(transformation);
		Point3f centToCamera = new Point3f(cent);
		modelview.transform(centToCamera);
		Vector3f v = new Vector3f(centToCamera);
		//System.out.println(centToCamera.x + " "+ centToCamera.y + " "+centToCamera.z);
		//		centToCamera
		for(Vector3f n : normals) 
		{
			
			float inter = n.dot(v);
			//System.out.println(inter);
			if(inter > radius)
				return true;
		}
		//render it if not outside
		return false;
	}
	
	/**
	 * Return the 4x4 projection matrix, which is used for example by 
	 * the renderer.
	 * 
	 * @return the 4x4 projection matrix
	 */
	public Matrix4f getProjectionMatrix()
	{
		return projectionMatrix;
	}
	
	public void setProjectionMatrix(Matrix4f m)
	{
		this.projectionMatrix = m;
	}
}
