package org.linkedin.contest.ants.impl;

import java.util.HashMap;
import java.util.Map;

import org.linkedin.contest.ants.api.Direction;

public class DirectionHelper {

	private static Map<Direction,Direction> oppositeDirections = null;
	
	static
	{
		oppositeDirections = new HashMap<Direction,Direction>();
		oppositeDirections.put(Direction.here, Direction.here);
		setOppositeDirections(Direction.east, Direction.west,oppositeDirections);
		setOppositeDirections(Direction.north, Direction.south,oppositeDirections);
		setOppositeDirections(Direction.northeast, Direction.southwest,oppositeDirections);
		setOppositeDirections(Direction.southeast, Direction.northwest,oppositeDirections);
	}
	
	private static void setOppositeDirections(Direction d1, Direction d2, Map<Direction,Direction> map)
	{
		map.put(d1,d2);
		map.put(d2,d1);
	}
	
	protected static Direction getOppositeDirection(Direction d)
	{
		return oppositeDirections.get(d);
	}
}
