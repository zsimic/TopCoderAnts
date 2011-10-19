package org.linkedin.contest.ants.impl;

public final class Trail {

	protected int trailSize = 0;			// Number of points in the trail
	protected int xArray[];
	protected int yArray[];
	protected int index;
	protected int count;

	Trail() {
		trailSize = 160;
		xArray = new int[trailSize];
		yArray = new int[trailSize];
		index = 0;
		count = 0;
	}

	// Clear the trail
	public void clear() {
		index = 0;
		count = 0;
	}

	// Penalty for going to 'square': affected to square already recently visited
	public double penalty(ZSquare square) {
		if (count==0) return 0;
		double penalty = 0;
//		int seen = 0;
		int i = index - 1;
		if (i<0) i = trailSize-1;
		int n = count;
		while (n>0) {
			int xx = xArray[i];
			int yy = yArray[i--];
			if (xx==square.x && yy==square.y) {
				penalty += n*1000;
//				seen++;
//				if (seen>3) {
//					return 500000000;
//				}
			}
			if (i<0) i = trailSize-1;
			n--;
		}
		return penalty;
	}

	// Was 'square' the last one added to the trail?
	public boolean isLast(ZSquare square) {
		if (count==0) return false;
		int i = index - 1;
		if (i<0) i = trailSize-1;
		int xx = xArray[i];// + square.deltaX;
		int yy = yArray[i];// + square.deltaY;
		return (xx==square.x && yy==square.y);
	}

	// Add 'square' to this trail
	public void add(ZSquare square) {
		xArray[index] = square.x;
		yArray[index++] = square.y;
		if (index >= trailSize) index = 0;
		if (count < trailSize) count++;
	}

}
