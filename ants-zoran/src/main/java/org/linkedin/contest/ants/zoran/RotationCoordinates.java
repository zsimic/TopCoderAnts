package org.linkedin.contest.ants.zoran;

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
