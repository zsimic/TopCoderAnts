package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class ScoutBorder extends Role {

	ScoutBorder(CommonAnt ant) {
		super(ant);
	}

	protected int x0, y0, x1, y1;

	@Override
	Action effectiveAct() {
		return null;
	}

}
