package org.linkedin.contest.ants.zoran;

import java.util.*;

public class Path {

	protected int x;							// Last x coordinate on this path (use it to get back to nest)
	protected int y;
	protected boolean isCorrupt;				// Is this path corrupt (can't be used to go back to nest)
	protected int size;							// Size of this path
	private Integer lastKey;					// Key corresponding to x, y
	private Hashtable<Integer, Integer> hash;	// Hash used to determine quickly whether a coordinate is already part of the path
	private ArrayList<Integer> list;			// List of coordinates in this path (encoded like lastKey)

	Path() {
		x = 0;
		y = 0;
		isCorrupt = false;
		size = 0;
		lastKey = 0;
		hash = new Hashtable<Integer, Integer>();
		list = new ArrayList<Integer>();
	}

	@Override
	public String toString() {
		if (isCorrupt) return "corrupt";
		return String.format("%d points", size);
	}

	// Number of coordinates in path
	public int size() {
		return list.size();
	}

	// Clear this path
	public void clear() {
		x = 0;
		y = 0;
		isCorrupt = false;
		size = 0;
		lastKey = 0;
		hash.clear();
		list.clear();
	}

	// Previous square on this path relative to given square (which must match the current x,y coordinate, or be a neighbor)
	public ZSquare prev(CommonAnt ant, ZSquare square) {
		if (size == 0) {
			return null;
		}
		int px, py;
		if (size == 1) {
			px = 0;
			py = 0;
		} else if (x == square.x && y == square.y) {
			Integer k = list.get(size - 2);
			px = (k & Constants.xPointMask) - Constants.BOARD_SIZE;
			py = ((k & Constants.yPointMask) >>> Constants.pointBitOffset) - Constants.BOARD_SIZE;
		} else {
			px = square.x;
			py = square.y;
		}
		if (Math.abs(x - px) > 1 || Math.abs(y - py) > 1) {
			return null;
		}
		return ant.square(px - x, py - y);
	}

	// Add coordinates in given 'square' to this path
	public void add(ZSquare square) {
		if (isCorrupt) return;
		if (Math.abs(x - square.x) > 1 || Math.abs(y - square.y) > 1) {
			clear();
			isCorrupt = true;
			return;
		}
		x = square.x;
		y = square.y;
		lastKey = ((y + Constants.BOARD_SIZE) << Constants.pointBitOffset) | (x + Constants.BOARD_SIZE);
		if (hash.containsKey(lastKey)) {
			int n = hash.get(lastKey) + 1;
			while (size > n) {
				hash.remove(list.remove(--size));
			}
		} else {
			hash.put(lastKey, size++);
			list.add(lastKey);
		}
	}

	// Remove 'square' from this path (because it was marked as obstacle)
	public void remove(ZSquare square) {
		if (size == 0 || isCorrupt) {
			return;
		}
		if (x == square.x && y == square.y) {
			lastKey = list.remove(--size);
			hash.remove(lastKey);
			x = (lastKey & Constants.xPointMask) - Constants.BOARD_SIZE;
			y = ((lastKey & Constants.yPointMask) >>> Constants.pointBitOffset) - Constants.BOARD_SIZE;
		} else {
			clear();
			isCorrupt = true;
		}
	}

}
