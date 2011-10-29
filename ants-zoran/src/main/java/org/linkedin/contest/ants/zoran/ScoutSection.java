package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class ScoutSection extends Role {

	ScoutSection(CommonAnt ant, int slice, int totalSlices) {
		super(ant);
		assert slice >= 0 && slice < totalSlices;
		this.slice = slice;
		this.slice1 = Constants.rotationCoordinates(slice, totalSlices);
		this.slice2 = Constants.rotationCoordinates(slice + 1, totalSlices);
//		maxScout = Constants.BOARD_SIZE * Constants.BOARD_SIZE * 3 / totalSlices;	// Stop scouting when this number of cells have been discovered by the scout
		maxScout = 35000;	// Stop scouting when this number of cells have been discovered by the scout
	}

	protected int slice;							// The slice we want to explore (from 0 to totalSlices)
	private RotationCoordinates slice1, slice2;		// The ant will be encouraged to stay within these 2 slices of the board
	private int avoidX = 0, avoidY = 0;				// Square to avoid because of possible enemy ants
	private boolean isHauling = false;
	private int skipSteps = 0;
	private int maxScout;
	private FollowPath follower = new FollowPath(this);

	@Override
	public String toString() {
		String mode;
		if (isHauling) mode = (skipSteps > 0) ? "hauling+skip" : "hauling";
		else if (follower.isActive()) mode = "following path";
		else mode = "";
		return String.format("Scout slice %d %s", slice, mode);
	}

	private void ensureHasPathToNest() {
		if (follower.isActive()) return;
		Path path = ant.board.bestPathToNest();
		follower.setPath(path);
		assert follower.isActive();
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

	private Action nextFollowerMove(boolean requirePassable) {
		assert follower.isActive();
		Action act = follower.act();
		if (requirePassable) assert ant.square(act).isPassable();
		return act;
	}

	@Override
	Action effectiveAct() {
		ZSquare sfood;
		if (ant.turn % 50000 == 0) Logger.dumpBoard(ant, Integer.toString(ant.turn));
		if (isHauling) {
			if (ant.here.isNest() || ant.isNextToNest()) {
				if (ant.hasFood) return new DropFood(ant.nest().dir);
				sfood = ant.squareWithFood(ant.here);
				if (sfood != null) return new GetFood(sfood.dir);
				stopHauling();
			} else if (skipSteps > 0) {
				skipSteps--;
				return nextFollowerMove(true);
			} else {
				ensureHasPathToNest();
				ZSquare dropZone = ant.squareTo(follower.peek());
				assert dropZone != null;
				if (ant.hasFood) return new DropFood(dropZone.dir);
				sfood = ant.squareWithFood(dropZone);
				if (sfood != null) return new GetFood(sfood.dir);
				if (dropZone.hasFood()) {
					skipSteps = 1;
					return nextFollowerMove(true);		// Will move to 'dropZone'
				}
				stopHauling();
			}
		}
		sfood = ant.squareWithFood(null);
		if (sfood != null) {
			startHauling();
			return new GetFood(sfood.dir);
		}
		if (ant.board.knownCells > maxScout) {
			// Stop scouting for unexplored cells, we get close to hitting the VM heap space limit
			Logger.dumpBoard(ant, "done");
			ant.setRole(new Guard(ant));
			return new Pass();
		}
		if (!follower.isActive()) {
			findNewPathToExplore();
		}
		Action act = nextFollowerMove(false);
		if (act == null) {
			Logger.error(ant, "no next move, check why");
			return new Pass();
		}
		ZSquare s = ant.square(act);
		if (!s.isPassable()) {		// Happens when we explore
			follower.setPath(null);
			return new Pass();
		}
		if (s != ant.here && !s.isAroundNest() && s.getNumberOfAnts() > 1) {
			Logger.inform(ant, String.format("avoiding %d ants on %d,%d", s.getNumberOfAnts(), s.x, s.y));
			avoidX = s.x;
			avoidY = s.y;
			follower.setPath(null);
			s = ant.bestSquareToAvoidConflict(s);
			if (s != null) return new Move(s.dir);
			return new Pass();
		}
		return act;
	}

	private void findNewPathToExplore() {
		Path path = ant.board.pathToClosestUnexplored(slice1, slice2, avoidX, avoidY);
		avoidX = 0;
		avoidY = 0;
		if (path == null) {
			Logger.error(ant, "can't find path to explore");
		}
		follower.setPath(path);
	}

}
