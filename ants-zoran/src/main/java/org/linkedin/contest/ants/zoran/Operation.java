package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public abstract class Operation {

	protected int turn;			// Turn within this operation
	protected Role role;		// Parent role
	protected CommonAnt ant;	// Parent ant
	private boolean isActive;	// Is this operation currently 'active'?

	Operation(Role role) {
		this.turn = 0;
		this.role = role;
		this.ant = role.ant;
		this.isActive = false;
	}

	// Next action to perform
	public Action act() {
		assert role != null && ant != null;
		assert isActive;
		turn++;
		Action act = effectiveAct();
		if (act == null) activate(false);
		return act;
	}

	// Effective next turn implementation for this operation
	// return null when operation is complete (will be removed from the stack)
	// return new Pass() if operation is not yet done but shouldn't be removed from the stack
	abstract Action effectiveAct();

	// Is this operation currently 'active'?
	public boolean isActive() {
		return isActive;
	}

	// Activate/deactivate this operation
	public void activate(boolean active) {
//		assert isActive != active;
		isActive = active;
	}

	@Override
	public String toString() {
		return Constants.className(this);
	}

}
