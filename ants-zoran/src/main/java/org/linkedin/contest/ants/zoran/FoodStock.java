package org.linkedin.contest.ants.zoran;

import java.util.*;

public class FoodStock {

	protected ArrayList<FoodCoordinates> coordinates;	// Coordinates for food seen

	public FoodStock() {
		coordinates = new ArrayList<FoodCoordinates>();
	}

	// Number of items in stock
	public int size() {
		return coordinates.size();
	}

	// Is this food stock empty?
	public boolean isEmpty() {
		return coordinates.isEmpty();
	}

	// Pop last food coordinate
	public FoodCoordinates pop() {
		if (coordinates.isEmpty()) return null;
		FoodCoordinates c = coordinates.remove(coordinates.size()-1);
		return c;
	}

	// Amount of food at coordinates x,y
	public int foodAmount(int x, int y) {
		for (FoodCoordinates coord : coordinates) {
			if (coord.x == x && coord.y == y) return coord.amount;
		}
		return 0;
	}

	// Add found food coordinates in scent
	public void add(Scent s) {
		assert s.isFoodCoordinates();
		add(s.a, s.b, s.c);
	}

	// Add found food coordinates
	public void add(int x, int y, int amount) {
		for (FoodCoordinates c : coordinates) {
			if (x == c.x && y == c.y) {
				if (amount != c.amount) c.amount = amount;
				return;
			}
		}
		coordinates.add(new FoodCoordinates(x, y, amount));
	}

	// Sort coordinates in this stock per amount first, then per distance to travel from nest
	public void sort() {
		Collections.sort(coordinates, new Comparator<FoodCoordinates>() {
			public int compare(FoodCoordinates c1, FoodCoordinates c2) {
				if (c1.amount == c2.amount) {
					return (int)(CommonAnt.normalDistance(c1.x, c1.y) - CommonAnt.normalDistance(c2.x, c2.y));
				}
				return c1.amount - c2.amount;
			}
		});
	}

}
