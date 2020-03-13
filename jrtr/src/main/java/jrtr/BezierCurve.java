package jrtr;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

public class BezierCurve {
	private final Matrix4f point_matrix;
	
	public BezierCurve(Point3f p0, Point3f p1, Point3f p2, Point3f p3)
	{
		point_matrix = new Matrix4f
				(
					p0.x, p1.x, p2.x, p3.x,
					p0.y, p1.y, p2.y, p3.y,
					p0.z, p1.z, p2.z, p3.z,
					0,	  0,	0,	  0 	//last row is unimportant
				);
	}
	
	static final Matrix4f bezier = new Matrix4f
	(
		-1,  3, -3,  1,
		 3, -6,  3,  0,
		-3,  3,  0,  0,
		 1,  0,  0,  0
	);
	
	// Wrapper Class
	public static class BernsteinInfo {
		public Vector3f vec; 
		public Point3f pnt;
	}
	// lazy implementation. 
	public static BernsteinInfo gen() 
	{
		return new BernsteinInfo();
	}
	
	//Input i, n, B(t)->time
	public BernsteinInfo[] BernsteinPoly(int nPoints)
	{
		Matrix4f c = new Matrix4f(point_matrix);
		c.mul(bezier);
		Vector4f t = new Vector4f();
		Vector4f tangent = new Vector4f();
		BernsteinInfo[] arr = new BernsteinInfo[nPoints];
		
		for(int i = 0; i < nPoints; i ++) {
			float val = (float)i /((float) (nPoints-1));
			// transforms
			t.set(val*val*val, val*val, val, 1.f);
			tangent.set((float)3 * val * val, (float)2 * val, 1.f, 0.f);
			c.transform(t);
			c.transform(tangent);
			
			// w is 0
			arr[i] = gen();
			arr[i].pnt = new Point3f(t.getX(),t.getY(), t.getZ());
			arr[i].vec = new Vector3f(tangent.getX(),tangent.getY(), tangent.getZ());
		}
		return arr;
	}
	
	
}
