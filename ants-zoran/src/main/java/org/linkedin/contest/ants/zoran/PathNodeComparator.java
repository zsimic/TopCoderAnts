package org.linkedin.contest.ants.zoran;

import java.util.Comparator;

/**
 * This is the comparator for the priority queue used in the A* algorithm implementations
 */
public class PathNodeComparator implements Comparator<PathNode> {

	public int compare(PathNode first, PathNode second) {
		if (first.getF() < second.getF()) return -1;
        if (first.getF() > second.getF()) return 1;
        return 0;
	}

}
