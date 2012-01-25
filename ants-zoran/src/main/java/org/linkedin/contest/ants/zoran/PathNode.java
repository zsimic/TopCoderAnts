package org.linkedin.contest.ants.zoran;

/**
 * Needed for the A* path algorithm
 */
public class PathNode {

	protected int x, y;			// x,y coordinates of associated point
	protected Integer id;		// x and y coordinates put in one integer (12 bits each)
	protected PathNode parent;	// Parent path, used to reconstruct the path after search is done
	protected double g;			// Distance from start point on path
	protected double h;			// Heuristic estimate of distance to target

	PathNode(int x, int y, double g, double h, PathNode parent) {
		update(x, y, g, h, parent);
	}

	public void update(int x, int y, double g, double h, PathNode parent) {
		this.x = x;
		this.y = y;
		id = Constants.encodedXY(x, y);
		this.g = g;
		this.h = h;
		this.parent = parent;
	}

	public double getF() {
		return g + h;
	}

	public Integer getId() {
		return id;
	}

	@Override
	public String toString() {
		return String.format("xy=%d %d f=%g", x, y, getF());
	}

}
