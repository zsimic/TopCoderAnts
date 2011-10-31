package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.Direction;

public class RotationCoordinates {

	public int slice;
	public int totalSlices;
	public boolean invert;
	public Direction bestFirstDirection;
	public double cosf;
	public double sinf;

	RotationCoordinates(int slice, int totalSlices) {
		this.slice = slice % totalSlices;
		this.totalSlices = totalSlices;
		invert = slice > (totalSlices / 2);
		cosf = Math.cos(2.0 * this.slice / totalSlices * Math.PI);
		sinf = Math.sin(2.0 * this.slice / totalSlices * Math.PI);
		double r = (double)slice / totalSlices;
		if (r > 0.875) bestFirstDirection = Direction.southwest;		// 7/8
		else if (r > 0.75) bestFirstDirection = Direction.west;			// 6/8
		else if (r > 0.625) bestFirstDirection = Direction.northwest;	// 5/8
		else if (r > 0.5) bestFirstDirection = Direction.north;			// 4/8
		else if (r > 0.375) bestFirstDirection = Direction.northeast;	// 3/8
		else if (r > 0.25) bestFirstDirection = Direction.east;			// 2/8
		else if (r > 0.125) bestFirstDirection = Direction.southeast;	// 1/8
		else bestFirstDirection = Direction.south;						// 0/8
	}

	@Override
	public String toString() {
		return String.format("Slice %d/%d", slice, totalSlices);
	}

	public double projectedX(int x, int y) {
		return cosf * x - sinf * y;
	}

	public double projectedY(int x, int y) {
		return cosf * y + sinf * x;
	}

	public Direction nextDirection(Direction dir) {
		if (invert) {
			if (dir == Direction.south) return Direction.southwest;
			else if (dir == Direction.southwest) return Direction.west;
			else if (dir == Direction.west) return Direction.northwest;
			else if (dir == Direction.northwest) return Direction.north;
			else if (dir == Direction.north) return Direction.northeast;
			else if (dir == Direction.northeast) return Direction.east;
			else if (dir == Direction.east) return Direction.southeast;
			else {
				assert dir == Direction.southeast;
				return Direction.south;
			}
		}
		if (dir == Direction.south) return Direction.southeast;
		else if (dir == Direction.southeast) return Direction.east;
		else if (dir == Direction.east) return Direction.northeast;
		else if (dir == Direction.northeast) return Direction.north;
		else if (dir == Direction.north) return Direction.northwest;
		else if (dir == Direction.northwest) return Direction.west;
		else if (dir == Direction.west) return Direction.southwest;
		else {
			assert dir == Direction.southwest;
			return Direction.south;
		}
	}
	
}
