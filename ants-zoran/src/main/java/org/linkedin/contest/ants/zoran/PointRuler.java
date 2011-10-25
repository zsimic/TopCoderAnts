package org.linkedin.contest.ants.zoran;

public class PointRuler implements Ruler {

	protected int x, y;
	
	PointRuler(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public double distance(int x, int y) {
		return Constants.normalDistance(x - this.x, y - this.y);
	}

}
