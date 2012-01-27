package org.linkedin.contest.ants.zoran;

/**
 * This class allows to calculate the 'cost' for the exploratory version of the A* algorithm
 * We project the ant's coordinates to the 'slice' that it is supposed to follow
 * The cost is then simply the absolute value of project 'y' coordinate, with a special penalty for negative 'x' coordinates
 * Doing it this ways allows to calculate rotated coordinates quickly (2 multiplications and 1 addition) instead of a more onerous cartesian projection...
 */
public class RotationCoordinates {

	public int slice;
	public int totalSlices;
	public double cosf;
	public double sinf;

	RotationCoordinates(int slice, int totalSlices) {
		this.slice = slice % totalSlices;
		this.totalSlices = totalSlices;
		cosf = Math.cos((2.0 * Math.PI * slice) / totalSlices);
		sinf = Math.sin((2.0 * Math.PI * slice) / totalSlices);
	}

	@Override
	public String toString() {
		return String.format("Slice %d/%d", slice, totalSlices);
	}

	public double projectedX(int x, int y) {
		return cosf * x + sinf * y;
	}

	public double projectedY(int x, int y) {
		return cosf * y - sinf * x;
	}

}
