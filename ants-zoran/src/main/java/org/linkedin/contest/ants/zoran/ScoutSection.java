package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class ScoutSection extends Role {

	ScoutSection(CommonAnt ant, int slice) {
		super(ant);
		assert slice >= 0 && slice < Constants.totalSlices;
		this.slice = slice;
		this.slice1 = Constants.rotationCoordinates(slice);
		this.slice2 = Constants.rotationCoordinates(slice + 1);
	}

	protected int slice;							// The slice we want to explore (from 0 to totalSlices)
	private RotationCoordinates slice1, slice2;		// The ant will be encouraged to stay within these 2 slices of the board
	private int avoidX = 0, avoidY = 0;
	private FollowPath follower = new FollowPath(this);

	@Override
	public String toString() {
		String mode;
		if (follower.isActive()) mode = "following path";
		else mode = "";
		return String.format("Scout slice %d %s", slice, mode);
	}

	@Override
	Action effectiveAct() {
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
		if (turn > 10000) {
			Logger.dumpBoard(ant, "done");
			ant.setRole(new Gatherer(ant));
			return new Pass();
		}
		Path path = ant.board.pathToClosestUnexplored(ant.x, ant.y, slice1, slice2, avoidX, avoidY);
		avoidX = 0;
		avoidY = 0;
		if (path == null) {
			Logger.error(ant, "can't find path to explore");
			return new Pass();
		}
		follower.setPath(path);
		return follower.act();
	}

}
