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
			ant.path.pop();
			if (ant.path.x == ant.here.x && ant.path.y == ant.here.y) {
				ant.path.pop();
			}
			if (ant.path.isEmpty) {
				if (Math.abs(ant.x) <= 1 && Math.abs(ant.y) <= 1) {
					ZSquare s = ant.square(-ant.x, -ant.y);
					return new Move(s.dir);
				} else {
					opGoTo.activate(0, 0);
					return opGoTo.act();
				}
			}
			ZSquare s = ant.square(ant.path.x - ant.here.x, ant.path.y - ant.here.y);
			if (s != null) {
				return new Move(s.dir);
			} else {
				ant.path.clear();
				opGoTo.activate(0, 0);
				return opGoTo.act();
			}
		}
	}

}
