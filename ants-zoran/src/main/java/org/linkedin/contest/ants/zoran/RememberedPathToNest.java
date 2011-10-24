package org.linkedin.contest.ants.zoran;

import java.util.*;

public class RememberedPathToNest {

	protected int x;							// Last x coordinate on this path (use it to get back to nest)
	protected int y;
	protected boolean isCorrupt;				// Is this path corrupt (can't be used to go back to nest)
	protected int size;							// Size of this path
	private Integer lastKey;					// Key corresponding to x, y
	private Hashtable<Integer, Integer> hash;	// Hash used to determine quickly whether a coordinate is already part of the path
	private ArrayList<Integer> list;			// List of coordinates in this path (encoded like lastKey)

	RememberedPathToNest() {
		hash = new Hashtable<Integer, Integer>();
		list = new ArrayList<Integer>();
		clear();
	}

	@Override
	public String toString() {
		if (isCorrupt) return "corrupt";
		return String.format("%d points", size);
	}

	public boolean has(int x, int y) {
		return hash.containsKey(Constants.encodedXY(x, y));
	}

	// Number of coordinates in path
	public int size() {
		return list.size();
	}

	// Clear this path
	public void clear() {
		x = Constants.BOARD_SIZE;
		y = Constants.BOARD_SIZE;
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
			px = Constants.BOARD_SIZE;
			py = Constants.BOARD_SIZE;
		} else if (x == square.x && y == square.y) {
			Integer k = list.get(size - 2);
			px = Constants.decodedX(k);
			py = Constants.decodedY(k);
		} else {
			px = square.x;
			py = square.y;
		}
		if (Math.abs(x - px) > 1 || Math.abs(y - py) > 1) {
			assert false;
			return null;		// We're more than one square away from 'square', a tracking error occured somewhere
		}
		return ant.squareDelta(px - x, py - y);
	}

	// Add coordinates in given 'square' to this path
	public void add(ZSquare square) {
		if (isCorrupt) return;
		if (Math.abs(x - square.x) > 1 || Math.abs(y - square.y) > 1) {
			assert false;			// A tracking error occured somewhere
			clear();
			isCorrupt = true;
			return;
		}
		x = square.x;
		y = square.y;
		lastKey = Constants.encodedXY(x, y);
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
			x = Constants.decodedX(lastKey);
			y = Constants.decodedY(lastKey);
		} else {
			clear();
			isCorrupt = true;
		}
	}

}
