package jrtr;

import java.util.ArrayList;
import java.util.Random;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import jrtr.VertexData.Semantic;

public class DiamondSquareFractalGen {
	private final float[][] heightField;
	private final int size;
	private Random rand;

	// range
	private int random(int min, int max) {
		return rand.nextInt((max - min) + 1) + min;
	}

	public DiamondSquareFractalGen(int n, int seed) {
		size = (int) (Math.pow(2, n) + 1);
		heightField = new float[size][size];
		rand = new Random(seed);
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				heightField[i][j] = -1;
			}
		}
	}

	private void diamond(int x, int y, int sz, int half) {
		// DEFINITLY SKIP CORNERS;
		if (heightField[x][y] != -1)
			return;
		heightField[x][y] = 0;

		// change this to have greater peeks;
		int random = random(0, half / 2);
		// float random = rand.nextFloat();
		if (x == 0) {
			heightField[x][y] = heightField[x][y - half] + heightField[x][y + half] + heightField[x + half][y];
			heightField[x][y] /= 3;
			heightField[x][y] += random;
		} else if (y == 0) {
			heightField[x][y] = heightField[x - half][y] + heightField[x + half][y] + heightField[x][y + half];
			heightField[x][y] /= 3;
			heightField[x][y] += random;
		} else if (x == size - 1) {
			heightField[x][y] = heightField[x][y - half] + heightField[x][y + half] + heightField[x - half][y];
			heightField[x][y] /= 3;
			heightField[x][y] += random;
		} else if (y == size - 1) {
			heightField[x][y] = heightField[x - half][y] + heightField[x + half][y] + heightField[x][y - half];
			heightField[x][y] /= 3;
			heightField[x][y] += random;
		} else {
			heightField[x][y] = heightField[x - half][y] + heightField[x + half][y] + heightField[x][y - half]
					+ heightField[x][y + half];
			heightField[x][y] /= 4;
			heightField[x][y] += random;
		}
	}

	private void square(int x, int y, int sz, int half) {
		heightField[x + half][y + half] = 0;

		// change this to have greater peeks;
		int random = random(0, half / 2);
		// float random = rand.nextFloat();
		/*
		 * System.out.println( heightField[x][y]+ " "+ heightField[x+sz-1][y] +
		 * " "+heightField[x][y+sz-1] + " " + heightField[x+sz-1][y+sz-1]);
		 */
		heightField[x + half][y + half] = heightField[x][y] + heightField[x + sz - 1][y] + heightField[x][y + sz - 1]
				+ heightField[x + sz - 1][y + sz - 1];
		heightField[x + half][y + half] /= 4;
		heightField[x + half][y + half] += random;

	}

	/**
	 * size is always indexed greater than 0
	 */
	private void diamondSquare(int x, int y, int sz) {
		int half = (int) Math.floor(sz / 2);
		if (half == 0 || heightField[x + half][y + half] != -1)
			return;
		// square step.
		for (int i = x; i < size - 1; i += sz - 1) {
			for (int j = y; j < size - 1; j += sz - 1)
				square(i, j, sz, half);
		}

		for (int i = x; i < size; i += half) {
			for (int j = y; j < size; j += half) {
				diamond(i, j, sz, half);
			}
		}

		diamondSquare(x, y, half + 1);
	}

	private void diamondSquare() {
		// set corners don't want negative corners
		heightField[0][0] = rand.nextInt(size / 2);
		heightField[size - 1][0] = rand.nextInt(size / 2);
		heightField[0][size - 1] = rand.nextInt(size / 2);
		heightField[size - 1][size - 1] = rand.nextInt(size / 2);

		diamondSquare(0, 0, size);
	}

	private class Face {
		Point3f a, b, c;
		Vector3f norm;

		public boolean isNeighbor(Point3f x) {
			return a.equals(x) || b.equals(x) || c.equals(x);
		}
	}

	public VertexData terrainGen(RenderContext re) {
		diamondSquare();

		float[] vertices = new float[size * size * 3];
		float[] normals = new float[size * size * 3];
		float[] colors = new float[size * size * 3];
		int[] indices = new int[(size - 1) * (size - 1) * 6];
		float[] color1 = { 1, 1, 1 };
		float[] color2 = { 0f, .6f, 0f };

		ArrayList<Face> faces = new ArrayList<Face>();
		// borrowed from sphere class..
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				vertices[3 * (i * size + j) + 0] = i;
				vertices[3 * (i * size + j) + 1] = heightField[i][j];
				vertices[3 * (i * size + j) + 2] = j;
				if (i + 1 < size && j + 1 < size) {
					int idx1 = i * size + j;
					int idx2 = i * size + j + 1;
					;
					int idx3 = (i + 1) * size + j;
					int idx4 = (i + 1) * size + j + 1;

					indices[6 * (i * (size - 1) + j) + 0] = idx1;
					indices[6 * (i * (size - 1) + j) + 1] = idx2;
					indices[6 * (i * (size - 1) + j) + 2] = idx3;

					indices[6 * (i * (size - 1) + j) + 3] = idx2;
					indices[6 * (i * (size - 1) + j) + 4] = idx3;
					indices[6 * (i * (size - 1) + j) + 5] = idx4;
				}

				if (heightField[i][j] > size / 2 - 2) {
					colors[3 * (i * size + j) + 0] = color1[0];
					colors[3 * (i * size + j) + 1] = color1[1];
					colors[3 * (i * size + j) + 2] = color1[2];
				} else {
					colors[3 * (i * size + j) + 0] = color2[0];
					colors[3 * (i * size + j) + 1] = color2[1];
					colors[3 * (i * size + j) + 2] = color2[2];
				}
			}
		}
		
		// setup triangles
		for (int idx = 0; idx < indices.length; idx += 3) {
			int i1 = indices[idx];
			int i2 = indices[idx + 1];
			int i3 = indices[idx + 2];

			Point3f a = new Point3f(vertices[i1*3], vertices[i1*3+1], vertices[i1*3+2]);
			Point3f b = new Point3f(vertices[i2*3], vertices[i2*3+1], vertices[i2*3+2]);
			Point3f c = new Point3f(vertices[i3*3], vertices[i3*3+1], vertices[i3*3+2]);

			Face face = new Face();
			face.a = a;
			face.b = b;
			face.c = c;
			Vector3f vec1 = new Vector3f(), vec2 = new Vector3f();
			vec1.sub(a, b); vec2.sub(a, c);			
			Vector3f vec3 = new Vector3f(); vec3.cross(vec1, vec2); vec3.normalize();
			face.norm = vec3;
			faces.add(face);
		}
		
		// normals are a summation of all neighboring triangles.
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				Point3f v = new Point3f(
						vertices[3*(i*size+j)+0],
						vertices[3*(i*size+j)+1],
						vertices[3*(i*size+j)+2]
				);
				Vector3f a = new Vector3f(0, 0, 0);
				for(Face f : faces)
				{
					if(f.isNeighbor(v))
					{
						a.add(f.norm);
					}
				}
				a.normalize();
				normals[3*(i*size+j)+0] = a.x;
				normals[3*(i*size+j)+1] = a.y;
				normals[3*(i*size+j)+2] = a.z;
			}
		}

		VertexData ve = re.makeVertexData(size * size);
		ve.addElement(vertices, Semantic.POSITION, 3);
		ve.addElement(normals, Semantic.NORMAL, 3);
		ve.addElement(colors, Semantic.COLOR, 3);

		ve.addIndices(indices);

		return ve;
	}

	public Vector3f getCenter() {
		return new Vector3f(-size / 2, -size / 2, -size / 2);
	}
}
