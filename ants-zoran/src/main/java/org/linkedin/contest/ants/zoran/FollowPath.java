package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class FollowPath extends Operation {

	protected Path path;

	FollowPath(Role role) {
		super(role);
	}

	public Integer peek() {
		if (path == null || path.isEmpty()) return null;
		return path.peek();
	}

	@Override
	public String toString() {
		if (!isActive()) return "inactive";
		if (path != null) return path.toString();
		return "empty";
	}

	@Override
	public Action effectiveAct() {
		assert path != null;
		int n = path.size();
		if (n > 0) {
			Integer key = path.pop();
			if (n == 1) activate(false);
			ZSquare sq = ant.squareTo(Constants.decodedX(key), Constants.decodedY(key));
			return new Move(sq.dir);
		}
		return null;
	}

	public void setPath(Path path) {
		this.path = path;
		activate(path != null && path.size() > 0);
	}

	public void setFromTrail(Trail trail) {
		Path path = new Path(trail);
		setPath(path);
	}

}
