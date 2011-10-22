package org.linkedin.contest.ants.zoran;

import java.util.List;

import org.linkedin.contest.ants.api.Action;
import org.linkedin.contest.ants.api.Ant;
import org.linkedin.contest.ants.api.Environment;
import org.linkedin.contest.ants.api.WorldEvent;

public class DoNothingAnt implements Ant {

	@Override
	public void init() {
		// We do nothing
	}

	@Override
	public Action act(Environment environment, List<WorldEvent> events) {
		return null;
	}

	@Override
	public Action onDeath(WorldEvent cause) {
		return null;
	}

}
