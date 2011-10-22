package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Guard extends Role {

	Guard(CommonAnt ant) {
		super(ant);
	}

	// Effectively act
	@Override
	Action effectiveAct() {
		if (ant.hasFood) {
			return new DropFood(ant.here.dir);
		}
		ZSquare s = ant.squareWithFood(ant.here);
		if (s != null) {
			return new GetFood(s.dir);
		}
		return null;
	}

}
