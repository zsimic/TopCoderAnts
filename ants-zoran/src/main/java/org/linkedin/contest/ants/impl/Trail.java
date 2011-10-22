package org.linkedin.contest.ants.zoran;

import java.util.Hashtable;

public final class Trail {

	protected int trailSize;			// Number of points in the trail
	protected int penaltyFactor;
	protected int xArray[];
	protected int yArray[];
	protected int index;
	protected int count;
	private Hashtable<Integer, Integer> hash;

	Trail() {
		trailSize = 1000;
		penaltyFactor = 100000;
		xArray = new int[trailSize];
		yArray = new int[trailSize];
		index = 0;
		count = 0;
		hash = new Hashtable<Integer, Integer>();
	}

	// Clear the trail
	public void clear() {
		index = 0;
		count = 0;
		hash.clear();
	}

	// Penalty for going to 'square': affected to square already recently visited
	public double penalty(ZSquare square) {
		if (count==0) return 0;
		Integer k = ((square.y + Constants.BOARD_SIZE) << Constants.pointBitOffset) | (square.x + Constants.BOARD_SIZE);
		Integer v = hash.get(k);
		if (v != null) return v * penaltyFactor;
		return 0;
	}

	// Was 'square' the last one added to the trail?
	public boolean isLast(ZSquare square) {
		if (count==0) return false;
		int i = index == 0 ? trailSize - 1 : index - 1;
		int xx = xArray[i];
		int yy = yArray[i];
		return (xx == square.x && yy == square.y);
	}

	// Add 'square' to this trail
	public void add(ZSquare square) {
		xArray[index] = square.x;
		yArray[index++] = square.y;
		if (index >= trailSize) index = 0;
		if (count < trailSize) {
			count++;
		} else {
			Integer oldk = ((yArray[index] + Constants.BOARD_SIZE) << Constants.pointBitOffset) | (xArray[index] + Constants.BOARD_SIZE);
			Integer oldv = hash.get(oldk);
			assert oldv != null;
			if (oldv == 1) hash.remove(oldk);
			else hash.put(oldk, oldv - 1);
		}
		Integer k = ((square.y + Constants.BOARD_SIZE) << Constants.pointBitOffset) | (square.x + Constants.BOARD_SIZE);
		Integer v = hash.get(k);
		if (v == null) hash.put(k, 1);
		else hash.put(k, v + 1);
	}

}
