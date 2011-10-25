package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class GoToNest extends Operation {

	GoToNest(Role role) {
		super(role);
		opGoTo = new GoTo(role);
	}

	private GoTo opGoTo;

	@Override
	public String toString() {
		return "";
//		return String.format("GoToNest %d path size", ant.path.size());
	}

	@Override
	Action effectiveAct() {
		if (opGoTo.isActive()) {
			return opGoTo.act();
		} else if (ant.here.isNest()) {
			return null;		// We're done
		}
//		ZSquare prev = ant.path.prev(ant, ant.here);
//		if (prev == null) {
//			opGoTo.activate(0, 0);
//			return opGoTo.act();
//		}
//		return new Move(prev.dir);
		return null;
	}

}
