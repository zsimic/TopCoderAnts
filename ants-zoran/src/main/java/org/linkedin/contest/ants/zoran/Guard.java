package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

/**
 * Simply stays put on the nest to protect gathered food from opponents coming and stealing it
 * and takes food from neighboring cells (to help gatherers save a turn or two)
 */
public class Guard extends Role {

	Guard(CommonAnt ant) {
		super(ant);
	}

	private FollowPath follower = null;

	// Effectively act
	@Override
	Action effectiveAct() {
		if (ant.here.isNest()) {
			// When on nest, just stay there (to make it difficult for enemy to pillage food),
			// and gather food deposited by other ants right next to nest
			if (follower != null) follower = null;
			if (ant.hasFood) {
				return new DropFood(ant.here.dir);
			}
			for (ZSquare s : ant.neighbors) {
				if (s.hasFood()) {
					return new GetFood(s.dir);
				}
			}
			return new Pass();
		}
		// Return back to nest otherwise
		if (follower == null) follower = new FollowPath(this);
		if (!follower.isActive()) {
			follower.setPath(ant.board.bestPathToNest(8));
		}
		return follower.act();
	}

}
