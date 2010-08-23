package ar.uba.dc.eci2010.m1;


/**
 * Modela una posicion en el entorno 
 *
 */
class Position {
	public int x, y;
	
	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public Position(Position pos) {
		this.x = pos.x;
		this.y = pos.y;
	}

	public boolean equals(int x, int y) {
		return this.x == x && this.y == y;
	}
	public Position moveNew(int moveX, int moveY) {
		return new Position(this.x + moveX, this.y + moveY);
	}
	public boolean inBounds(int minx, int miny, int maxx, int maxy) {
		return x >= minx && y >= miny && x < maxx && y < maxy;
	}

	public Position moveNew(Direction dir) {
		return moveNew(dir.moveX, dir.moveY);
	}

	public void move(Direction dir) {
		x += dir.moveX;
		y += dir.moveY;
	}
	
	public String toString() {
		return "(" + x + "," + y + ")";
	}
}