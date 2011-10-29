package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Guard extends Role {

	Guard(CommonAnt ant) {
		super(ant);
	}

	private FollowPath follower = null;

	// Effectively act
	@Override
	Action effectiveAct() {
		if (ant.here.isNest()) {
			if (follower != null) follower = null;
			if (ant.hasFood) {
				return new DropFood(ant.here.dir);
			}
			for (ZSquare s : ant.neighbors) {
				if (s.hasFood()) return new GetFood(s.dir);
			}
			return new Pass();
		}
		if (follower == null) follower = new FollowPath(this);
		if (!follower.isActive()) follower.setPath(ant.board.bestPathToNest());
		return follower.act();
	}

}
