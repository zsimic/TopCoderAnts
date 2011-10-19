package org.linkedin.contest.ants.impl;

import java.util.*;

public class Path {

	protected int x;			// Last x coordinate on this path (use it to get back to nest)
	protected int y;
//	private Hashtable hash;
	protected ArrayList<Point> points;
	
	Path() {
		points = new ArrayList<Point>();
	}

	@Override
	public String toString() {
		return String.format("%d points", points.size());
	}
	
	protected Point last() {
		int n = points.size() - 1;
		if (n < 0) return null;
		return points.get(n);
	}
	
	protected Point pop() {
		int n = points.size() - 1;
		if (n < 0) return null;
		return points.remove(n);
	}
	
	protected void add(ZSquare square) {
		int n = points.size() - 1;
		Point p = null;
		while (p == null && n > 0) {
			Point pp = points.get(n);
			if (pp.x == square.x && pp.y == square.y) {
				p = pp;
			} else {
				n--;
			}
		}
		if (p != null) {
			int ni = points.size() - 1;
			while (ni > n) {
				points.remove(ni--);
			}
		} else {
			p = new Point(square.x, square.y);
			points.add(p);
		}
	}

	protected void clear() {
		points.clear();
	}

}
