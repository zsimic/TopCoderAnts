package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class ScoutBorder extends Role {

	ScoutBorder(CommonAnt ant, int section) {
		super(ant);
		this.section = section;
	}

	private enum ScoutState {
		scanning,
		returning,
		communicatingInfo,
		done
	}

	protected int section;
	private FollowPath follower = new FollowPath(this);
	private TransmitMessage opTransmit = new TransmitMessage(this);
	private ScoutState state = ScoutState.scanning;

	@Override
	public String toString() {
		String mode;
		if (follower.isActive()) mode = "following path";
		else if (opTransmit.isActive()) mode = "transmitting";
		else mode = "";
		return String.format("Scout section %d %s %s", section, state.toString(), mode);
	}

	@Override
	Action effectiveAct() {
		if (follower.isActive()) return follower.act();
		if (state == ScoutState.scanning) {
			if (turn > 2000 || Math.max(ant.board.sizeX(), ant.board.sizeY()) > Constants.BOARD_SIZE * 0.9) {
				state = ScoutState.returning;
			} else {
				Path path = ant.board.pathToClosestUnexplored(ant.x, ant.y, section);
				if (path == null) {
					state = ScoutState.returning;
					Logger.error(ant, "no more cells to explore");
					Logger.dumpBoard(ant);
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
