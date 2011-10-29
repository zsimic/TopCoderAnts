package org.linkedin.contest.ants.zoran;

public class RotationCoordinates {

	public double cosf;
	public double sinf;

	RotationCoordinates(double cosf, double sinf) {
		this.cosf = cosf;
		this.sinf = sinf;
	}

	public double projectedX(int x, int y) {
		return cosf * x - sinf * y;
	}

	public double projectedY(int x, int y) {
		return cosf * y + sinf * x;
	}

}
