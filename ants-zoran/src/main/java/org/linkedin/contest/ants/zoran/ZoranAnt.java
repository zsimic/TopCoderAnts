package org.linkedin.contest.ants.zoran;

/**
 * ZoranAnt implementation
 * 4 guards, 46 scouts (food gatherers)
 */
public class ZoranAnt extends CommonAnt {

	private static final int guards = 4;

	@Override
	public void initializeState() {
		if (id <= guards) setRole(new Guard(this));										// Guards, they stay on nest and do nothing (just gather food immediately next to nest)
		else setRole(new Scout(this, id - guards - 1, Constants.ANTS_COUNT - guards));	// The rest are scouts, they go explore and bring food back to the nest
	}

}
