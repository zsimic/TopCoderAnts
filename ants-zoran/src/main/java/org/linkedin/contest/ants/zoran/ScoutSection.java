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
	private int avoidX = 0, avoidY = 0;				// Square to avoid because of possible enemy ants
	private boolean isHauling = false;
	private int skipSteps = 0;
	private FollowPath follower = new FollowPath(this);

	@Override
	public String toString() {
		String mode;
		if (follower.isActive()) mode = "following path";
		else mode = "";
		return String.format("Scout slice %d %s", slice, mode);
	}

	private void ensureHasPathToNest() {
		if (follower.isActive()) return;
		Path path = ant.board.bestPathToNest();
		follower.setPath(path);
	}

	private void stopHauling() {
		isHauling = false;
		skipSteps = 0;
		follower.setPath(null);
	}

	private void startHauling() {
		isHauling = true;
		skipSteps = 0;
		follower.setPath(null);
		ensureHasPathToNest();
	}

	@Override
	Action effectiveAct() {
		ZSquare sfood;
		if (isHauling) {
			if (ant.here.isNest() || ant.isNextToNest()) {
				if (ant.hasFood) return new DropFood(ant.nest().dir);
				sfood = ant.squareWithFood(ant.here);
				if (sfood != null) return new GetFood(sfood.dir);
				stopHauling();
			} else if (skipSteps > 0) {
				assert follower.isActive();
				skipSteps--;
				return follower.act();
			} else {
				ensureHasPathToNest();
				ZSquare dropZone = ant.squareTo(follower.peek());
				if (dropZone != null) {
					if (ant.hasFood) return new DropFood(dropZone.dir);
					sfood = ant.squareWithFood(dropZone);
					if (sfood != null) return new GetFood(sfood.dir);
					if (dropZone.hasFood()) {
						skipSteps = 1;
						return new Move(dropZone.dir);
					}
				}
				stopHauling();
			}
		}
		sfood = ant.squareWithFood(null);
		if (sfood != null) {
			startHauling();
			return new GetFood(sfood.dir);
		}
		if (ant.board.knownCells > Constants.BOARD_MAX_SCOUT) {
			// Stop scouting for unexplored cells, we get close to hitting the VM heap space limit
			Logger.dumpBoard(ant, "done");
			ant.setRole(new Guard(ant));
			return new Pass();
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
		Path path = ant.board.pathToClosestUnexplored(slice1, slice2, avoidX, avoidY);
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
