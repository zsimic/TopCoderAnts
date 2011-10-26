package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Gatherer extends Role {

	Gatherer(CommonAnt ant) {
		super(ant);
	}

	// Effectively act
	@Override
	Action effectiveAct() {
		return new Pass();
	}

}
