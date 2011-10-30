package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public abstract class Role {

	Role(CommonAnt ant) {
		assert ant !=null;
		this.ant = ant;
	}

	protected int roleTurn = 0;			// Turn counter within this role
	protected CommonAnt ant;			// Associated ant

	// Perform next act for this role, an act can consist of either push operations to the ant's operation stack
	// or setting the ant's next action
	public Action act() {
		assert ant != null;
		roleTurn++;
		Action act = effectiveAct();
		assert act != null;
		return act;
	}

	// Effectively act
	abstract Action effectiveAct();

	@Override
	public String toString() {
		return String.format("%s %d %s", Constants.className(this), ant.id, stateInfo());
	}

	// Extra state info for this specific role, redefine to get some extra relevant output in toString()
	protected String stateInfo() {
		return "";
	}

}
