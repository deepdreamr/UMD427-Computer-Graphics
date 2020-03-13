package jrtr;

import java.util.ArrayList;

import javax.vecmath.Point3f;

import jrtr.VertexData.Semantic;
import jrtr.VertexData.VertexElement;

public class ShapeNode implements SceneNode {
	
	private Shape shape;
	private Point3f cent;
	private float radius;
	
	public ShapeNode(Shape shape)
	{
		this.shape = shape;
		
		//Start gen. bounding sphere code
		VertexElement e = null;
		for(VertexElement vd : shape.getVertexData().getElements())
		{
			if(vd.getSemantic() == Semantic.POSITION) {
				e = vd;	break;
			}
		}
		
		if(e != null) {
			e.getNumberOfComponents();
			float x=0,y=0,z=0;
			int size = e.getData().length / e.getNumberOfComponents();
			int idx = 0;
			float[] vert = e.getData();
			ArrayList<Point3f> list = new ArrayList<Point3f>();
			while(size > idx)
			{
				x += vert[idx]; y += vert[idx+1]; z += vert[idx+2];
				list.add(new Point3f(vert[idx], vert[idx+1], vert[idx+2]));
				idx++;
			}
			
			cent = new Point3f(x/size,y/size,z/size);
			Point3f max = null;
			for(Point3f ve : list)
			{
				if(max == null || ve.distance(cent) > max.distance(cent))
					max = ve;
			}
			radius = max.distance(cent);
		}
	}
	
	public Shape getShape()
	{
		return shape;
	}
	
	public Point3f getBoundingObjCent() 
	{
		return cent;
	}
	
	public float getBoundingRadius()
	{
		return radius;
	}
}