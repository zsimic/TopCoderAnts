package org.linkedin.contest.ants.zoran;

public class RotationCoordinates {

	public int slice;
	public int totalSlices;
	public int bestFirstDirection;
	public double cosf;
	public double sinf;

	RotationCoordinates(int slice, int totalSlices) {
		this.slice = slice % totalSlices;
		this.totalSlices = totalSlices;
		cosf = Math.cos(2.0 * this.slice / totalSlices * Math.PI);
		sinf = Math.sin(2.0 * this.slice / totalSlices * Math.PI);
		double r = (double)slice / totalSlices;
		if (r > 0.875) bestFirstDirection = 5;
		else if (r > 0.75) bestFirstDirection = 6;
		else if (r > 0.625) bestFirstDirection = 7;
		else if (r > 0.5) bestFirstDirection = 0;
		else if (r > 0.375) bestFirstDirection = 1;
		else if (r > 0.25) bestFirstDirection = 2;
		else if (r > 0.125) bestFirstDirection = 3;
		else bestFirstDirection = 4;
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

}
