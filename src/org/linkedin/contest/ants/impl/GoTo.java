package org.linkedin.contest.ants.impl;

import org.linkedin.contest.ants.api.*;

public class GoTo extends Operation {

	GoTo(Role role) {
		super(role);
	}

	private int targetX, targetY;			// Target coordinates
	private double minDistance;				// Minimum distance we've reached from target
	private int minReached;					// How many times we reached the minimum distance

	public void activate(int x, int y) {
		this.targetX = x;
		this.targetY = y;
		this.minDistance = -1;
		this.minReached = 0;
		activate(true);
	}

	// Effective next turn implementation for this operation
	// return null when operation is complete (will be removed from the stack)
	// return new Pass() if operation is not yet done but shouldn't be removed from the stack
	@Override
	Action effectiveAct() {
		ZSquare s = ant.bestSquareForTarget(targetX, targetY);
		double d = CommonAnt.normalDistance(targetX - ant.x, targetY - ant.y);
		if (d<0.5) {
			// We've reached the target
			return null;
		}
		if (minDistance<0 || d < minDistance) {
			minDistance = d;
			minReached = 0;
		} else if (Math.abs(minDistance - d) < 0.001) {
			minReached++;
			if (minReached>=5) {
				// It's the 5th time we reach the same minimum distance from target
				// Assume target isn't reachable and decide this is as close as it gets
				return null;
			}
		}
		return new Move(s.dir);
	}

}
