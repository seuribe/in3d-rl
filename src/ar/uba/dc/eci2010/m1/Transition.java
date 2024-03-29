/**
 * 
 */
package ar.uba.dc.eci2010.m1;

import java.util.Random;


/**
 * Modela la funcion de transicion para distintos tipos de celdas. Dado que los posibles movimientos son
 * Norte, Sur, Este, Oeste, lo que almacena es la probabilidad de que, dada una direccion concreta, vaya
 * en esa direccion o en la que esta respectivamente a izquierda, derecha o atras.
 * Si el total de las probabilidades es menor de 1, significa que puede quedar en su lugar el agente.
 *
 */
public class Transition {
	private float fwd;
	private float left;
	private float right;
	private float back;

	private float[] p = new float[4];
	
	private static final Random random = new Random();
	
	public Transition(float fwd, float left, float right, float back) {
		this.p[0] = fwd;
		this.p[1] = right;
		this.p[2] = back;
		this.p[3] = left;
		this.fwd = fwd;
		this.right = right + fwd;
		this.back = back + right + fwd;
		this.left = left + back + right + fwd;
	}
	
	public float getAdvanceP() {
		return fwd;
	}

	public float getPDir(int action) {
		return p[action];
	}
	
	public float getPDir(Direction dir) {
		return p[dir.ordinal()];
	}
	
	public Direction attempt(Direction dir) {
		float rnd = random.nextFloat();
		if (rnd <= fwd) {
			return dir;
		} else if (rnd <= right) {
			return dir.getRight();
		} else if (rnd <= back) {
			return dir.getBack();
		} else if (rnd <= left) {
			return dir.getLeft();
		}
		return Direction.NONE;
	}

	public String toString() {
		return "[f:" + fwd + " r:" + right + " b:" + back + " l:" + left + "]";
	}
	
}