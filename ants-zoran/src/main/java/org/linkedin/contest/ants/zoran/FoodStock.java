package org.linkedin.contest.ants.zoran;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
	protected boolean isEmpty() {
		return coordinates.isEmpty();
	}

	// Add found food coordinates in scent
	protected void add(Scent s) {
		assert s.isFoodCoordinates();
		add(s.xa(), s.xb(), s.c);
	}

	// Add found food coordinates
	protected void add(int x, int y, int amount) {
		for (FoodCoordinates c : coordinates) {
			if (x == c.x && y == c.y) {
				if (amount != c.amount) c.amount = amount;
				return;
			}
		}
		coordinates.add(new FoodCoordinates(x, y, amount));
	}

	// Pop last food coordinate
	protected FoodCoordinates pop() {
		if (coordinates.isEmpty()) return null;
		FoodCoordinates c = coordinates.remove(coordinates.size()-1);
		return c;
	}

	// Sort coordinates in this stock per amount first, then per distance to travel from nest
	protected void sort() {
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
