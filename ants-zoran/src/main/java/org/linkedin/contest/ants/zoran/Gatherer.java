package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Gatherer extends Role {

	Gatherer(CommonAnt ant) {
		super(ant);
	}

	private FollowPath follower = new FollowPath(this);
	private boolean isHauling = false;
	private int avoidX = 0, avoidY = 0;

	// Effectively act
	@Override
	Action effectiveAct() {
		if (isHauling) {
		}
		if (follower.isActive()) {
			Action act = follower.act();
			if (act != null) {
				ZSquare s = ant.square(act);
				if (s != ant.here && !s.isAroundNest() && s.getNumberOfAnts() > 1) {
					avoidX = s.x;
					avoidY = s.y;
					follower.setPath(null);
					s = ant.bestSquareToAvoidConflict(s);
					if (s != null) return new Move(s.dir);
					return new Pass();
				}
				return act;
			}
		}
		return new Pass();
	}

}
