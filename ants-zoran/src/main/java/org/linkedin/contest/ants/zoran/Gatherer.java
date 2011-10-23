package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Gatherer extends Role {

	Gatherer(CommonAnt ant) {
		super(ant);
		opNest = new GoToNest(this);
		opGoTo = new GoTo(this);
	}

	protected boolean hasOrder = false;
	protected int x = 0;
	protected int y = 0;
	protected int amount = 0;

//	private Object opHaul = null;
	private GoToNest opNest = null;
	private GoTo opGoTo = null;

	// Effectively act
	@Override
	Action effectiveAct() {
		if (opNest.isActive()) {
			return opNest.act();
		} else if (opGoTo.isActive()) {
			return opGoTo.act();
//		} else if (opHaul.isActive()) {
//			return opHaul.act();
		} else if (ant.here.isNest() && ant.here.scent.isFetchFood()) {
			x = ant.here.scent.a;
			y = ant.here.scent.b;
			amount = ant.here.scent.c;
			opGoTo.activate(x, y);
			return new Write(null);
		}
		return new Pass();
	}

}
