package org.linkedin.contest.ants.impl;

import org.linkedin.contest.ants.api.*;

public class Soldier extends Role {

	Soldier(ZoranAnt ant) {
		super(ant);
	}

	@Override
	Action effectiveAct() {
		return null;
	}

}
