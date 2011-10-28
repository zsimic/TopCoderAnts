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
		waitingForBoardInfo,
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
			if (turn > 2000) {
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
			if (ant.x == Constants.BOARD_SIZE + 1 && ant.y == Constants.BOARD_SIZE) {
				state = ScoutState.communicatingInfo;
			} else {
				Path path = ant.board.bestPath(ant.x, ant.y, Constants.BOARD_SIZE + 1, Constants.BOARD_SIZE);
				assert path != null;
				follower.setPath(path);
				return follower.act();
			}
		}
		if (state == ScoutState.communicatingInfo) {
			if (opTransmit.messageBody == null) opTransmit.setBoardInfo(ant.north.dir);
			if (opTransmit.isActive()) return opTransmit.act();
			opTransmit.clear();
			if (ant.receivedBoardInfos > 0) {
				state = ScoutState.done;
			} else {
				state = ScoutState.waitingForBoardInfo;
				Scent s = new Scent();
				s.setAwaitingBoardInfo();
				return new Write(s.getValue());
			}
		}
		if (state == ScoutState.waitingForBoardInfo) {
			if (ant.receivedBoardInfos > 0) {
				state = ScoutState.done;
				if (ant.here.scent.isAwaitingBoardInfo()) return new Write(null);
			}
		}
		if (state == ScoutState.done) {
			assert ant.here.isNest() || ant.isNextToNest();
			if (!ant.here.isNest()) return new Move(ant.north.dir);
			ant.setRole(new Guard(ant));
			return new Pass();
		}
		return null;		// confused
	}

}
