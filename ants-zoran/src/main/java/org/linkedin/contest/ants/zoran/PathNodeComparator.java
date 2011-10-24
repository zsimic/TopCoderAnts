package org.linkedin.contest.ants.zoran;

import java.util.Comparator;

public class PathNodeComparator implements Comparator<PathNode> {

	public int compare(PathNode first, PathNode second) {
		if (first.getF() < second.getF()) return -1;
        if (first.getF() > second.getF()) return 1;
        return 0;
	}

}
