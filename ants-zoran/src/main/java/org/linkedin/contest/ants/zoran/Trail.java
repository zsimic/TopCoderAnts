package org.linkedin.contest.ants.zoran;

import java.util.*;

/**
 * Stores the path from the nest to ant's current position, removing unneeded loops
 */
public class Trail {

	protected ArrayList<Integer> list;			// List of coordinates in this path (encoded like lastKey)
	private Hashtable<Integer, Integer> hash; 	// Hash used to determine quickly whether a coordinate is already part of the path

	Trail() {
		hash = new Hashtable<Integer, Integer>();
		list = new ArrayList<Integer>();
		clear();
	}

//L	@Override																					// Logger.
//L	public String toString() {																	// Logger.
//L		String s = "";																			// Logger.
//L		for (Integer key : list) {																// Logger.
//L			s += String.format("[%d,%d] ", Constants.decodedX(key), Constants.decodedY(key));	// Logger.
//L		}																						// Logger.
//L		return String.format("%d: %s", list.size(), s);											// Logger.
//L	}																							// Logger.

	// Number of coordinates in path
	public int size() {
		return list.size();
	}

	// Clear this path
	public void clear() {
		hash.clear();
		list.clear();
	}

	// Add coordinates in given 'square' to this path
	public void add(CommonAnt ant) {
		assert addable(ant);
		Integer key = Constants.encodedXY(ant.x, ant.y);
		int size = list.size();
		if (hash.containsKey(key)) {		// We've already been through this point, truncate list back to it
			int n = hash.get(key) + 1;
			while (size > n) {
				hash.remove(list.remove(--size));
			}
		} else {
			hash.put(key, size);
			list.add(key);
		}
	}

	private boolean addable(CommonAnt ant) {
		int n = list.size();
		if (n == 0) return true;
		Integer key = list.get(n-1);
		return Math.abs(Constants.decodedX(key) - ant.x) <= 1 && Math.abs(Constants.decodedY(key) - ant.y) <= 1;
	}

}