package org.linkedin.contest.ants.zoran;

import java.util.*;

public class FoodStock {

	protected ArrayList<FoodCoordinates> coordinates;	// Coordinates for food seen
	protected int totalFood;

	public FoodStock() {
		coordinates = new ArrayList<FoodCoordinates>();
		totalFood = 0;
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

	public String representation() {
		String s = "";
		for (FoodCoordinates c : coordinates) {
			s += String.format("%d %d %d\n", c.x, c.y, c.amount);
		}
		return s;
	}

	// Add food coordinates from received 'lines'
	public void setFromLines(List<String> lines) {
		for (String line : lines) {
			if (line.length() < 5) return;
			int i = line.indexOf(' ');		// x
			if (i < 0) return;
			String sn = line.substring(0, i);
			if (!Constants.isNumber(sn)) return;
			int x = Integer.parseInt(sn);
			line = line.substring(i + 1);
			i = line.indexOf(' ');			// y
			if (i < 0) return;
			sn = line.substring(0, i);
			if (!Constants.isNumber(sn)) return;
			int y = Integer.parseInt(sn);
			line = line.substring(i + 1);	// amount
			if (!Constants.isNumber(line)) return;
			int amount = Integer.parseInt(line);
			add(x, y, amount);
		}
	}

	// Add found food coordinates
	public void add(int x, int y, int amount) {
		for (FoodCoordinates c : coordinates) {
			if (x == c.x && y == c.y) {
				if (amount != c.amount) {
					totalFood -= c.amount;
					c.amount = amount;
					totalFood += amount;
				}
				return;
			}
		}
		totalFood += amount;
		coordinates.add(new FoodCoordinates(x, y, amount));
	}

	// Sort coordinates in this stock per amount first, then per distance to travel from nest
	public void sort() {
		Collections.sort(coordinates, new Comparator<FoodCoordinates>() {
			public int compare(FoodCoordinates c1, FoodCoordinates c2) {
				if (c1.amount == c2.amount) {
					return (int)(Constants.normalDistance(c1.x - Constants.BOARD_SIZE, c1.y - Constants.BOARD_SIZE) - Constants.normalDistance(c2.x - Constants.BOARD_SIZE, c2.y - Constants.BOARD_SIZE));
				}
				return c1.amount - c2.amount;
			}
		});
	}

}
