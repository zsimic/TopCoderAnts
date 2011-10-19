package org.linkedin.contest.ants.impl;

import java.util.*;

public class Path {

	protected int x;			// Last x coordinate on this path (use it to get back to nest)
	protected int y;
	protected boolean isEmpty;
	private Integer lastKey;
	private Hashtable<Integer, Integer> hash;
	private ArrayList<Integer> list;
	
	Path() {
		hash = new Hashtable<Integer, Integer>();
		list = new ArrayList<Integer>();
		isEmpty = true;
	}

	@Override
	public String toString() {
		return String.format("%d points", list.size());
	}

	public int size() {
		return list.size();
	}

	private void update(Integer key) {
		x = key & xMask;
		y = (key & yMask) >>> bitOffset;
		lastKey = key;
	}

	protected void pop() {
		int n = list.size() - 1;
		Integer v = list.remove(n);
		hash.remove(v);
		if (n<=0) {
			x = 0;
			y = 0;
			lastKey = 0;
			isEmpty = true;
			return;
		} else {
			update(list.get(n-1));
		}
	}

	protected void add(ZSquare square) {
		x = square.x;
		y = square.y;
		isEmpty = false;
		lastKey = (y << bitOffset) | x;
		if (hash.containsKey(lastKey)) {
			int n = hash.get(lastKey);
			int nm = list.size() - 1;
			while (nm > n) {
				list.remove(nm--);
			}
		} else {
			hash.put(lastKey, list.size());
			list.add(lastKey);
		}
	}

	protected void clear() {
		hash.clear();
		list.clear();
		isEmpty = true;
	}

	private final static int xMask = 0x000fff;
	private final static int yMask = 0xfff000;
	private final static int bitOffset = Integer.bitCount(xMask);

}
