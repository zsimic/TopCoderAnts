package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class ScoutSection extends Role {

	ScoutSection(CommonAnt ant, int sliceNumber, int totalSlices) {
		super(ant);
		assert sliceNumber >= 0 && sliceNumber < totalSlices;
		this.sliceNumber = sliceNumber;
		this.slice = Constants.rotationCoordinates(sliceNumber, totalSlices);
		maxScout = 100000;		// Stop scouting when this number of cells have been discovered by the scout
	}

	protected int sliceNumber;					// The slice we want to explore (from 0 to totalSlices)
	private RotationCoordinates slice;			// The ant will be encouraged to stay in the direction of its given slice to explore
	private int avoidX = 0, avoidY = 0;			// Square to avoid because of possible enemy ants
	private int resumeX = 0, resumeY = 0;		// Where to resume exploring after hauling food back to nest
	private boolean isHauling = false;
	private int skipSteps = 0;
	private int maxScout;
	private String mode = "starting";
	private FollowPath follower = new FollowPath(this);

	@Override
	public String toString() {
		String s = mode;
		if (skipSteps > 0) s += "+skip";
		return String.format("Scout slice %d %s", sliceNumber, s);
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
		if (resumeX != 0 && resumeY != 0 && resumeX != ant.x && resumeY != ant.y) {
			mode = String.format("returning to %d,%d", resumeX, resumeY);
			Path path = ant.board.bestPath(ant.x, ant.y, resumeX, resumeY);
			follower.setPath(path);
			resumeX = 0;
			resumeY = 0;
		} else {
			mode = "exploring";
		}
	}

	private void startHauling() {
		isHauling = true;
		skipSteps = 0;
		follower.setPath(null);
		ensureHasPathToNest();
		mode = "hauling";
		if (!ant.here.isNest() && !ant.isNextToNest()) {
			resumeX = ant.x;
			resumeY = ant.y;
		}
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
				if (ant.here.hasFood()) return nextFollowerMove(true);
				stopHauling();
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
			mode = "switching to Guard";
			ant.setRole(new Guard(ant));
			return new Pass();
		}
		if (!follower.isActive()) {
			findNewPathToExplore();
		}
		Action act = nextFollowerMove(false);
		if (act == null) {
			Logger.error(ant, "no next move, check why");
			mode = "confused";
			return new Pass();
		}
		ZSquare s = ant.square(act);
		if (!s.isPassable()) {		// Happens when we explore
			follower.setPath(null);
			mode = "hit wall";
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
		if (avoidX != 0 && avoidY != 0) {
			mode += String.format(", avoiding %d,%d", avoidX, avoidY);
		} else {
			mode = "exploring";
		}
		Path path = ant.board.pathToClosestUnexplored(slice, avoidX, avoidY);
		avoidX = 0;
		avoidY = 0;
		if (path == null) {
			Logger.error(ant, "can't find path to explore");
		}
		follower.setPath(path);
	}

}
