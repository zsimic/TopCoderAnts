package org.linkedin.contest.ants.zoran;

import java.util.*;

// Stores a path from start to target, in reverse order so we can simply use pop()
public class Path {

	private Stack<Integer> points;		// Points in the path (encoded by Constants.encodeXY)

	Path() {
		points = new Stack<Integer>();
	}

	public int size() {
		return points.size();
	}

	public boolean isEmpty() {
		return points.isEmpty();
	}

	@Override
	public String toString() {
		String s = "";
		for (Integer key : points) {
			if (s.length() > 0) s += ',';
			s += String.format("x=%d y=%d", Constants.decodedX(key), Constants.decodedY(key));
		}
		return s;
	}

	// Pop first point out of this path
	public Integer pop() {
		assert !isEmpty();
		return points.pop();
	}

	public void add(Integer key) {
		assert !points.contains(key);
		points.add(key);
	}

}
