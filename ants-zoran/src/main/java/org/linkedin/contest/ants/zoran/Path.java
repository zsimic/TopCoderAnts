package org.linkedin.contest.ants.zoran;

import java.util.*;

/**
 * Stores a path from start to target, in reverse order so we can simply use pop()
 */
public class Path {

	private Stack<Integer> points;		// Points in the path (encoded by Constants.encodeXY)

	Path() {
		points = new Stack<Integer>();
	}

	Path(Trail trail) {
		points = new Stack<Integer>();
		for (Integer key : trail.list) {
			add(key);
		}
	}

	public int size() {
		return points.size();
	}

	public boolean isEmpty() {
		return points.isEmpty();
	}

	public int targetX() {
		if (points.isEmpty()) return 0;
		return Constants.decodedX(points.get(0));
	}

	public int targetY() {
		if (points.isEmpty()) return 0;
		return Constants.decodedY(points.get(0));
	}

//L	@Override																					// Logger.
//L	public String toString() {																	// Logger.
//L		String s = "";																			// Logger.
//L		for (Integer key : points) {															// Logger.
//L			if (s.length() > 0) s += ',';														// Logger.
//L			s += String.format("[%d,%d]", Constants.decodedX(key), Constants.decodedY(key));	// Logger.
//L		}																						// Logger.
//L		return s;																				// Logger.
//L	}																							// Logger.

	public Integer peek() {
		return points.peek();
	}
	
	// Pop first point out of this path
	public Integer pop() {
		assert !isEmpty();
		return points.pop();
	}

	// Reversed path
	public Path reverse(int x, int y) {
		if (points.isEmpty()) return null;
		Path path = new Path();
		Integer key = points.peek();
		int px = Constants.decodedX(key);
		int py = Constants.decodedY(key);
		if (px != x && py != y) {
			assert Math.abs(px - x) <= 1 && Math.abs(py - y) <= 1;
			path.add(Constants.encodedXY(x, y));
		}
		for (int i = points.size() - 1; i >= 0; i--) {
			path.add(points.get(i));
		}
		return path;
	}

	public void add(Integer key) {
		assert !points.contains(key);
		points.add(key);
	}

	// This is to be used for 'resume' paths, where we had a reverse path back to a certain target from the nest
	// Typically a gatherer won't reach the nest back exactly, so a 'truncateTo' would allow it to resume back to target from where it is currently
	public void truncateTo(int x, int y) {
		int targetKey = Constants.encodedXY(x, y);
		while (points.size() > 0) {
			Integer k = points.pop();
			if (k.intValue() == targetKey) break;
		}
	}

}
