package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class DumpFoodStockInfo extends Operation {

	DumpFoodStockInfo(Role role) {
		super(role);
	}

	@Override
	public String toString() {
		return String.format("Dump food info (%d left)", ant.foodStock.size());
	}

	// Effective next turn implementation for this operation
	// return null when operation is complete (will be removed from the stack)
	// return new Pass() if operation is not yet done but shouldn't be removed from the stack
	@Override
	Action effectiveAct() {
		assert ant.here.isNest();
		FoodCoordinates c = ant.foodStock.pop();
		if (c == null) return null;		// We're done
		if (ant.here.scent.isEmpty()) {
			Scent s = new Scent();
			s.setFoodCoordinates(c);
			ant.dump(String.format("food: %s", c.toString()));
			return new Write(s.getValue());
		}
		return new Pass();	// Wait until there is no more writing on nest (manager clears writings as it consumes them)
	}

}
