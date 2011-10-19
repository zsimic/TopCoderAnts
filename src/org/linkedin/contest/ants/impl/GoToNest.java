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
		return String.format("%s %d path size", ZoranAnt.className(this), ant.path.points.size());
	}

	@Override
	Action effectiveAct() {
		if (opGoTo.isActive()) {
			return opGoTo.act();
		} else if (ant.here.isNest()) {
			return null;		// We're done
		} else {
			Point p = ant.path.pop();
			if (p != null && p.x == ant.here.x && p.y == ant.here.y) {
				p = ant.path.pop();
			}
			if (p == null) {
				if (Math.abs(ant.x) <= 1 && Math.abs(ant.y) <= 1) {
					ZSquare s = ant.square(-ant.x, -ant.y);
					return new Move(s.dir);
				} else {
					opGoTo.activate(0, 0);
					return opGoTo.act();
				}
			}
			ZSquare s = ant.square(p.x - ant.here.x, p.y - ant.here.y);
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
