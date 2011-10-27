package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class ScoutBorder extends Role {

	ScoutBorder(CommonAnt ant, ZSquare dir) {
		super(ant);
		this.direction = dir;
		targetX = Constants.BOARD_SIZE + direction.deltaX * Constants.BOARD_SIZE;
		targetY = Constants.BOARD_SIZE + direction.deltaY * Constants.BOARD_SIZE;
	}

	private enum ScoutState {
		scanning,
		returning,
		communicatingInfo,
		done
	}

	protected int x0, y0, x1, y1;
	private int targetX, targetY;
	private ZSquare direction;
	private FollowPath follower = new FollowPath(this);
	private TransmitMessage opTransmit = new TransmitMessage(this);
	private ScoutState state = ScoutState.scanning;

	@Override
	Action effectiveAct() {
		if (follower.isActive()) return follower.act();
		if (state == ScoutState.scanning) {
			if (turn > 2000 || Math.max(ant.board.sizeX(), ant.board.sizeY()) > Constants.BOARD_SIZE * 0.9) {
				state = ScoutState.returning;
				System.out.print(String.format("%s %s\n", ant.toString(), direction.toString()));
				System.out.print(ant.board.representation());
			} else {
				Path path = ant.board.pathToClosestUnexplored(ant.x, ant.y, direction.dir);
				if (path == null) {
					state = ScoutState.returning;
					System.out.print(String.format("no more cells to explore %s %s\n", ant.toString(), direction.toString()));
					System.out.print(ant.board.representation());
				} else {
					follower.setPath(path);
					return follower.act();
				}
			}
		}
		if (state == ScoutState.returning) {
			if (ant.here.isNest()) {
				state = ScoutState.communicatingInfo;
			} else {
				Path path = ant.board.bestPath(ant.x, ant.y, Constants.BOARD_SIZE, Constants.BOARD_SIZE);
				assert path != null;
				follower.setPath(path);
				return follower.act();
			}
		}
		if (state == ScoutState.communicatingInfo) {
			if (opTransmit.message == null) opTransmit.setMessage(String.format("%s----\n%s", ant.board.representation(false), ant.foodStock.representation()));
			if (opTransmit.isActive()) return opTransmit.act();
			opTransmit.setMessage(null);
			state = ScoutState.done;
		}
		if (state == ScoutState.done) {
			ant.setRole(new Guard(ant));
			return new Pass();
		}
		return null;		// confused
	}

}
