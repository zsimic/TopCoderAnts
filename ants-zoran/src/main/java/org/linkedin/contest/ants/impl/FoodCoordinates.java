package org.linkedin.contest.ants.impl;

public class FoodCoordinates {

	protected int x;		// X coordinate where food was seen
	protected int y;		// Y coordinate
	protected int amount;	// Amount of food seen

	public FoodCoordinates(int x, int y, int amount) {
		this.x = x;
		this.y = y;
		this.amount = amount;
	}

	@Override
	public String toString() {
		return String.format("x=%d y=%d amt=%d", x, y, amount);
	}

}
