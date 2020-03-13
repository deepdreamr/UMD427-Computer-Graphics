package jrtr;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import jrtr.BezierCurve.BernsteinInfo;

public class PiecewiseBezierCurve {
	private final int segments;
	private final Matrix4f[] point_matrices;
	
	public PiecewiseBezierCurve(Point3f... p) 
	{
		assert((p.length-1) % 3 == 0) : "not a integer value of (n-1)(mod 3) ";
		
		segments = (p.length-1)/3;
		point_matrices = new Matrix4f[segments];
		
		for(int i = 0; i < segments; i ++) 
		{
			point_matrices[i] = new Matrix4f();
			for(int j = 0; j < 4; j ++) 
			{
				Point3f pt = p[j + i*3];
				point_matrices[i].setColumn(j, pt.x, pt.y, pt.z, 0);
			}
		}
	}

	public BernsteinInfo[] BernsteinPoly(int nPoints)
	{
		Matrix4f[] c = new Matrix4f[segments];
		for(int i = 0; i < segments; i++)
		{
			c[i] = new Matrix4f(point_matrices[i]);
			c[i].mul(BezierCurve.bezier);
		}
		
		Vector4f t = new Vector4f();
		Vector4f tangent = new Vector4f();
		BernsteinInfo[] arr = new BernsteinInfo[nPoints];
		
		for(int i = 0; i < nPoints; i++)
		{
			float val = (float)i /((float) (nPoints-1));
			// transforms
			t.set(val*val*val, val*val, val, 1.f);
			tangent.set(3 * val * val, 2 * val, 1, 0);
			
			// calculate segment based off of val
			int idx = (int) (val * segments);
			// make sure idx isn't greater than array bounds
			idx = idx >= c.length ? c.length - 1 : idx;
			
			c[idx].transform(t);
			c[idx].transform(tangent);
			
			arr[i] = BezierCurve.gen();
			arr[i].pnt = new Point3f(t.getX(),t.getY(), t.getZ());
			arr[i].vec = new Vector3f(tangent.getX(),tangent.getY(), tangent.getZ());
		}
		
		return arr;
	}

}
