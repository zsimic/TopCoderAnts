package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Soldier extends Role {

	Soldier(CommonAnt ant) {
		super(ant);
	}

	@Override
	Action effectiveAct() {
		return new Pass();
	}

}