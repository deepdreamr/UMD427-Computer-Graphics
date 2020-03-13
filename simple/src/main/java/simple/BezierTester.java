package simple;

import java.awt.Graphics2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Point3f;

import jrtr.BezierCurve;
import jrtr.BezierCurve.BernsteinInfo;
import jrtr.PiecewiseBezierCurve;
import jrtr.RenderPanel;



public class BezierTester {
	static JFrame frame = new JFrame();
	static JPanel renderPanel = new JPanel();
	static Graphics2D graph;
	
	public static void makeWindow()
	{
		frame.setSize(500, 500);
		frame.setLocationRelativeTo(null); // center of screen
		frame.getContentPane().add(renderPanel);// put the canvas into a JFrame window

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true); // show window

		graph = (Graphics2D) renderPanel.getGraphics();
		
	}
	
	public static void run()
	{
		
	}
	
	public static void main(String[] args) 
	{

		PiecewiseBezierCurve piecewis = new PiecewiseBezierCurve(new Point3f(0,0,0),
				new Point3f(2, 2, 0), new Point3f(3, 1, 0), new Point3f(5, 4, 0),
				new Point3f(7, 6, 0), new Point3f(8, 3, 0), new Point3f(10, 2, 0));
		BernsteinInfo[] a = piecewis.BernsteinPoly(12); 
		for(BernsteinInfo b : a) 
		{
			System.out.print(b.pnt + " ");
			System.out.println(b.vec);
		}
		
	}

}
