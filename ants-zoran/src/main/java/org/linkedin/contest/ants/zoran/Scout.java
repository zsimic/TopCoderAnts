package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Scout extends Role {

	Scout(CommonAnt ant, int sliceNumber, int totalSlices) {
		super(ant);
		this.sliceNumber = sliceNumber % totalSlices;
		this.totalSlices = totalSlices;
		this.slice = Constants.rotationCoordinates(sliceNumber, totalSlices);
	}

	protected int sliceNumber;					// The slice we want to explore (from 0 to totalSlices)
	private int totalSlices;
	private boolean changeSlice = false;		// When we spend too much time on a slice, we switch to the next one
	private int sliceSwitchCount = 0;			// After changing slices a certain number of times, we give up
	private RotationCoordinates slice;			// The ant will be encouraged to stay in the direction of its given slice to explore
	private int avoidX = 0, avoidY = 0;			// Square to avoid because of possible enemy ants
	private Path resume = null;					// Path to where to resume exploring after hauling food back to nest
	private boolean isHauling = false;
	private int skipSteps = 0;
	private String mode = "starting";			// Logger.
	private FollowPath follower = new FollowPath(this);
	
	@Override														// Logger.
	public String toString() {										// Logger.
		String s = mode;											// Logger.
		if (skipSteps > 0) s += "+skip";							// Logger.
		return String.format("Scout slice %d %s", sliceNumber, s);	// Logger.
	}																// Logger.

	private void ensureHasPathToNest() {
		if (follower.isActive() || ant.here.isNest() || ant.isNextToNest()) return;
		follower.setPath(ant.board.bestPathToNest(3));
		assert follower.isActive();
	}

	private void stopHauling() {
		isHauling = false;
		skipSteps = 0;
		follower.setPath(null);
		if (resume != null) {
			mode = String.format("returning to %d,%d", resume.targetX(), resume.targetY());		// Logger.
			resume.truncateTo(ant.x, ant.y);
			if (resume.isEmpty()) resume = null;
			follower.setPath(resume);
			resume = null;
		} else {					// Logger.
			mode = "exploring";		// Logger.
		}
	}

	private void startHauling() {
		isHauling = true;
		skipSteps = 0;
		follower.setPath(null);
		ensureHasPathToNest();
		mode = "hauling";									// Logger.
		if (!ant.here.isNest() && !ant.isNextToNest()) {
			resume = follower.path.reverse(ant.x, ant.y);
		}
	}

	private Action nextFollowerMove(boolean requirePassable) {
//		assert follower.isActive();
		Action act = follower.isActive() ? follower.act() : null;
		if (requirePassable) assert act != null && ant.square(act).isPassable();
		return act;
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
				skipSteps--;
				Action act = nextFollowerMove(true);
				if (act != null || ant.here.hasFood()) return act;
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
					Action act = nextFollowerMove(true);
					if (act != null) return act;		// Will move to 'dropZone'
				}
				stopHauling();
			}
			return new Pass();
		}
		sfood = ant.squareWithFood(null);
		if (sfood != null) {
			startHauling();
			return new GetFood(sfood.dir);
		}
		if (changeSlice) {
			changeSlice = false;
			sliceSwitchCount++;
			if (sliceSwitchCount > totalSlices) {		// Give up, we're hitting a wall in all directions looks like
				Logger.inform(ant, "giving up exploration, turning into a guard");
				ant.setRole(new Guard(ant));
				return new Pass();
			}
			sliceNumber = (sliceNumber + 1) % totalSlices;
			Logger.inform(ant, "switched to new slice");
			slice = Constants.rotationCoordinates(sliceNumber, totalSlices);
			follower.setPath(null);
			if (!ant.here.isNest() && !ant.isNextToNest()) {
				ensureHasPathToNest();
			}
			return new Pass();
		}
		if (!follower.isActive()) {
			findNewPathToExplore();
			return new Pass();
		}
		Action act = nextFollowerMove(false);
		if (act == null) {
			Logger.inform(ant, "can't find path to explore");
			mode = "confused";			// Logger.
			return new Pass();
		}
		ZSquare s = ant.square(act);
		if (!s.isPassable()) {			// Happens when we explore
			follower.setPath(null);
			mode = "hit wall";			// Logger.
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
		if (avoidX != 0 && avoidY != 0) {									// Logger.
			mode += String.format(", avoiding %d,%d", avoidX, avoidY);		// Logger.
		} else {															// Logger.
			mode = "exploring";												// Logger.
		}																	// Logger.
		long elapsedTimeMillis = System.currentTimeMillis();
		Path path = ant.board.pathToClosestUnexplored(slice, avoidX, avoidY, 4);
		elapsedTimeMillis = System.currentTimeMillis() - elapsedTimeMillis;
		if (elapsedTimeMillis > 3) {
			changeSlice = true;
			Logger.inform(ant, String.format("scheduling slice switch, spent %d ms looking for next unexplored cell", elapsedTimeMillis));
		}
		avoidX = 0;
		avoidY = 0;
		follower.setPath(path);
	}

}
