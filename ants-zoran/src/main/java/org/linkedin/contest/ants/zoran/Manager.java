package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Manager extends Role {

	private TransmitMessage opTransmit = new TransmitMessage(this);

	Manager(CommonAnt ant) {
		super(ant);
	}

	@Override
	Action effectiveAct() {
		assert ant.here.isNest();		// Manager stays on nest the whole time
		if (opTransmit.isActive()) return opTransmit.act();
		if (!ant.receivalInProgress && ant.south.scent.isAwaitingBoardInfo()) {
			opTransmit.setBoardInfo(ant.south.dir);
			return opTransmit.act();
		}
		if (ant.here.scent.isEmpty() && !ant.foodStock.isEmpty()) {
			ant.foodStock.sort();
			FoodCoordinates c = ant.foodStock.pop();
			if (c != null) {
				Scent s = new Scent();
				s.setFetchFood(c);
				return new Write(s.getValue());
			}
		}
		return new Pass();
	}

}
