package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class FollowPath extends Operation {

	private Path path;

	FollowPath(Role role) {
		super(role);
	}

	@Override
	public Action effectiveAct() {
		assert path != null;
		int n = path.size();
		if (n > 0) {
			Integer key = path.pop();
			if (n == 1) activate(false);
			ZSquare sq = ant.squareTo(Constants.decodedX(key), Constants.decodedY(key));
			if (sq.isPassable()) return new Move(sq.dir);		// Squares can be marked as unpassable dynamically by other ants (closing)
			activate(false);
		}
		return null;
	}

	public void setPath(Path path) {
		this.path = path;
		activate(path != null && path.size() > 0);
	}
	
}
