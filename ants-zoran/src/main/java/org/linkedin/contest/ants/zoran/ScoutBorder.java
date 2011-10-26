package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class ScoutBorder extends Role implements Ruler {

	ScoutBorder(CommonAnt ant, ZSquare dir) {
		super(ant);
		this.direction = dir;
		targetX = Constants.BOARD_SIZE + direction.deltaX * Constants.BOARD_SIZE * 1000;
		targetY = Constants.BOARD_SIZE + direction.deltaY * Constants.BOARD_SIZE * 1000;
	}

	private enum ScoutState {
		scanning,
		returning,
		borderInfo,
		boardInfo,
		done
	}

	protected int x0, y0, x1, y1;
	private int targetX, targetY;
	private ZSquare direction;
	private FollowPath follower = new FollowPath(this);
	private DumpFoodStockInfo opDump = new DumpFoodStockInfo(this);
	private ScoutState state = ScoutState.scanning;

	// Distance to target
	public double distance(int x, int y) {
		return Constants.normalDistance(x - targetX * Math.abs(direction.deltaX), y - targetY * Math.abs(direction.deltaY));
	}

	@Override
	Action effectiveAct() {
		if (follower.isActive()) {
			return follower.act();
		}
		if (state == ScoutState.scanning) {
			if (Math.max(ant.board.sizeX(), ant.board.sizeY()) > Constants.BOARD_SIZE * 0.9) {
				state = ScoutState.returning;
				System.out.print(String.format("%s %s\n", ant.toString(), direction.toString()));
				System.out.print(ant.board.representation());
			} else {
				Path path = ant.board.bestPath(ant.x, ant.y, this);
				assert path != null;
				follower.setPath(path);
				return follower.act();
			}
		}
		if (state == ScoutState.returning) {
			if (ant.isNextToNest()) {
				if (opDump.isActive()) return opDump.act();
				state = ScoutState.borderInfo;
			}
			Path path = ant.board.bestPath(ant.x, ant.y, new PointRuler(Constants.BOARD_SIZE + direction.deltaX, Constants.BOARD_SIZE + direction.deltaY));
			assert path != null;
			follower.setPath(path);
			return follower.act();
		}
		if (state == ScoutState.borderInfo) {
			state = ScoutState.boardInfo;
			return new Write(0L);
		}
		if (state == ScoutState.boardInfo) {
		}
		if (state == ScoutState.done) {
			ant.setRole(new Guard(ant));
			return new Pass();
		}
		return null;		// confused
	}

}
