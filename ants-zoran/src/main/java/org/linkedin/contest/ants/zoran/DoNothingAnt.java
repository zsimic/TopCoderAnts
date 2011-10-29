package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;
import java.util.List;

public class DoNothingAnt implements Ant {

	private int turn = 0;

	@Override
	public void init() {
		// We do nothing
	}

	@Override
	public Action act(Environment environment, List<WorldEvent> events) {
		turn++;
		return null;
	}

	@Override
	public Action onDeath(WorldEvent cause) {
		String s = cause != null ? cause.toString() : "no cause";
		System.err.print(String.format("'DoNothing' ant died on turn %d: %s\n", turn, s));
		return null;
	}

}
