package ar.uba.dc.eci2010.m1;

/**
 * Modela la direccion en que puede decidir avanzar el agente. Dependiendo del terreno - y su funcion de transicion - 
 * puede ser que avance en esa direccion o en otra.
 * 
 */
public enum Direction {
	NORTH(0, 1, 2, 3, 0, -1), EAST(1, 2, 3, 0, 1, 0), SOUTH(2, 3, 0, 1, 0, 1), WEST(3, 0, 1, 2, -1, 0), NONE(4, 4, 4, 4, 0, 0);

	private int index;
	private final int right;
	private final int back;
	private final int left;
	final int moveX;
	final int moveY;

	private Direction(int index, int right, int back, int left, int moveX, int moveY) {
		this.index = index;
		this.right = right;
		this.back = back;
		this.left = left;
		this.moveX = moveX;
		this.moveY = moveY;
	}
	
	public Direction getRight() {
		return values()[right];
	}
	public Direction getBack() {
		return values()[back];
	}
	public Direction getLeft() {
		return values()[left];
	}

	public static Direction get(int index) {
		for (Direction dir : values()) {
			if (dir.index == index) {
				return dir;
			}
		}
		return null;
	}
	
	public String toString() {
		return name();
	}
}