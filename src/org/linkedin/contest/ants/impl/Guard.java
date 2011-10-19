package org.linkedin.contest.ants.impl;

import org.linkedin.contest.ants.api.*;

public class Guard extends Role {

	Guard(ZoranAnt ant) {
		super(ant);
	}

	// Effectively act
	Action effectiveAct() {
		if (ant.hasFood) {
			return new DropFood(ant.here.dir);
		} else {
			ZSquare s = ant.squareWithFood(ant.here);
			if (s != null) {
				return new GetFood(s.dir);
			} else {
				return null;
			}
		}
	}

}
