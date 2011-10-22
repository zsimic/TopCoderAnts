package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Manager extends Role {

	Manager(CommonAnt ant) {
		super(ant);
	}

	@Override
	Action effectiveAct() {
		assert ant.here.isNest();
		if (ant.here.scent.isFoodCoordinates()) {
			ant.foodStock.add(ant.here.scent);
			return new Write(null);
		}
		if (ant.here.scent.isEmpty() && !ant.foodStock.isEmpty() && (ant.knowsBoundaries() || ant.turn > 3000)) {
			ant.foodStock.sort();
			FoodCoordinates c = ant.foodStock.pop();
			if (c != null) {
				Scent s = new Scent();
				s.setFetchFood(c);
				return new Write(s.getValue());
			}
		}
		return new Pass();
	}

}
