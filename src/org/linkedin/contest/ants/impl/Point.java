package org.linkedin.contest.ants.impl;

public class Point {

	protected int x;
	protected int y;

	Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public String toString() {
		return String.format("x=%d y=%d", x, y);
	}

}
