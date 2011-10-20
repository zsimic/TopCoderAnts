package org.linkedin.contest.ants.impl;

import org.linkedin.contest.ants.api.*;

public class GoToNest extends Operation {

	GoToNest(Role role) {
		super(role);
		opGoTo = new GoTo(role);
	}

	private GoTo opGoTo;

	@Override
	public String toString() {
		return String.format("%s %d path size", CommonAnt.className(this), ant.path.size());
	}

	@Override
	Action effectiveAct() {
		if (opGoTo.isActive()) {
			return opGoTo.act();
		} else if (ant.here.isNest()) {
			return null;		// We're done
		} else {
			ZSquare prev = ant.path.prev(ant, ant.here);
			if (prev == null) {
				opGoTo.activate(0, 0);
				return opGoTo.act();
			} else {
				return new Move(prev.dir);
			}
		}
	}

}
