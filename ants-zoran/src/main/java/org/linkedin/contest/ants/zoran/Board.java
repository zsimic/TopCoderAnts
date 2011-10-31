package org.linkedin.contest.ants.zoran;

import java.util.*;
import org.linkedin.contest.ants.api.*;

public class Board {

	private CommonAnt ant;
	private ArrayList<BitSet> knownRows;
	private ArrayList<BitSet> obstacleRows;
	private int minX, minY, maxX, maxY;
	private int actualMinX, actualMaxX;
	protected int knownCells;

	Board(CommonAnt ant) {
		this.ant = ant;
		knownRows = new ArrayList<BitSet>();
		obstacleRows = new ArrayList<BitSet>();
		knownRows.add(new BitSet(Constants.BOARD_SIZE));		// One row initially corresponding to the row where the nest is
		obstacleRows.add(new BitSet(Constants.BOARD_SIZE));
		minX = Constants.BOARD_SIZE / 2;
		maxX = minX + Constants.BOARD_SIZE - 1;
		minY = maxY = Constants.BOARD_SIZE;
		actualMinX = actualMaxX = Constants.BOARD_SIZE;
		knownCells = 0;
	}

	@Override
	public String toString() {
		return String.format("x=%d %d y=%d %d", minX, maxX, minY, maxY);
	}

	public int sizeX() {
		return actualMaxX - actualMinX;
	}

	public int sizeY() {
		return maxY - minY;
	}

	private final static Direction[] neighbors = new Direction[]{
		Direction.northeast, Direction.east, Direction.southeast, Direction.south,
		Direction.southwest, Direction.west, Direction.northwest, Direction.north
	};

	// Path to closest unexplored cell from xStart,yStart, following 'section' (one of 8 major directions from nest)
	public Path pathToClosestUnexplored(RotationCoordinates slice, int avoidX, int avoidY, int maxMillis) {
		assert slice != null;
		assert get(ant.x, ant.y) == Constants.STATE_PASSABLE;
		HashMap<Integer, PathNode> opened = new HashMap<Integer, PathNode>();
		HashMap<Integer, PathNode> closed = new HashMap<Integer, PathNode>();
		PriorityQueue<PathNode> pQueue = new PriorityQueue<PathNode>(20, new PathNodeComparator());
		PathNode start = new PathNode(ant.x, ant.y, 0, 0, null);
		opened.put(Constants.encodedXY(ant.x, ant.y), start);
		pQueue.add(start);
		PathNode goal = null;
		PathNode closest = null;
		int consider = 2;		// Number of unknowns to consider before picking one
		long timeLimit = System.currentTimeMillis() + maxMillis;
		while (true) {
			PathNode current = pQueue.poll();
			if (get(current.x, current.y) == Constants.STATE_UNKNOWN) {
				if (goal == null || current.getF() < goal.getF()) goal = current;
				consider--;
				if (consider < 0) {
					assert goal.parent != null;		// Otherwise, we're asking the ant to go where it already is!
					break;
				}
			}
			opened.remove(current.id);
			closed.put(current.id, current);
			if (current.parent != null && (closest == null || closest.h > current.h)) closest = current;
			for (Direction neighbor : neighbors) {
				int nx = current.x + neighbor.deltaX;		// Coordinates of neighbor
				int ny = current.y + neighbor.deltaY;
				byte state = get(nx, ny);
				if (state != Constants.STATE_OBSTACLE && !(current.x == avoidX && current.y == avoidY)) {
					Integer key = Constants.encodedXY(nx, ny);
					if (!closed.containsKey(key)) {
						double g = current.g + 1;							// current g + distance from current to neighbor
						double h = distanceFromSlice(nx - Constants.BOARD_SIZE, ny - Constants.BOARD_SIZE, slice);	// Heuristic when exploring
						PathNode node = opened.get(key);
						if (node == null) {
							// Not in the open set yet
							node = new PathNode(nx, ny, g, h, current);
							opened.put(key, node);
							pQueue.add(node);
						} else if (g < node.g) {
							// Have a better route to the current node, change its parent
							node.parent = current;
							node.g = g;
							node.h = h;
						}
					}
				}
			}
			if (opened.isEmpty() || timeLimit < System.currentTimeMillis()) {
				break;
			}
		}
		if (goal == null) return pathFromNode(closest);
		return pathFromNode(goal);
	}

	private static double distanceFromSlice(int x, int y, RotationCoordinates slice) {
		double px = slice.projectedX(x, y);
		if (px < 0) return 10 * Constants.BOARD_SIZE;
		double py = slice.projectedY(x, y);
		double distance = Math.abs(py) + (Constants.BOARD_SIZE - px);// + Constants.normalDistance(x, y) / Constants.BOARD_MAX_DISTANCE;
		return distance;
	}

	// Best path back to nest
	public Path bestPathToNest(int maxMillis) {
		assert !ant.here.isNest();
		Path path = bestPath(ant.x, ant.y, Constants.BOARD_SIZE, Constants.BOARD_SIZE, false, maxMillis);
		if (path == null) {
			path = new Path(ant.trail);
		}
		return path;
	}

	// Best path from xStart,yStart to xEnd,yEnd (excluding the start coordinates)
	public Path bestPath(int xStart, int yStart, int xEnd, int yEnd, boolean useClosest, int maxMillis) {
		assert xStart != xEnd || yStart != yEnd;
		assert get(xStart, yStart) == Constants.STATE_PASSABLE;
		assert get(xEnd, yEnd) == Constants.STATE_PASSABLE;
		HashMap<Integer, PathNode> opened = new HashMap<Integer, PathNode>();
		HashMap<Integer, PathNode> closed = new HashMap<Integer, PathNode>();
		PriorityQueue<PathNode> pQueue = new PriorityQueue<PathNode>(20, new PathNodeComparator());
		PathNode start = new PathNode(xStart, yStart, 0, Constants.normalDistance(xStart - xEnd, yStart - yEnd), null);
		opened.put(Constants.encodedXY(xStart, yStart), start);
		pQueue.add(start);
		PathNode goal = null;
		PathNode closest = null;
		long timeLimit = System.currentTimeMillis() + maxMillis;
		while (true) {
			PathNode current = pQueue.poll();
			opened.remove(current.id);
			if (current.x == xEnd && current.y == yEnd) {
				goal = current;					// We found the target
				assert goal.parent != null;		// Otherwise, we're asking the ant to go where it already is!
				break;
			}
			if (useClosest && current.parent != null && (closest == null || closest.getF() > current.getF())) closest = current;
			closed.put(current.id, current);
			for (Direction neighbor : neighbors) {
				int nx = current.x + neighbor.deltaX;		// Coordinates of neighbor
				int ny = current.y + neighbor.deltaY;
				byte state = get(nx, ny);
				if (state == Constants.STATE_PASSABLE) {
					Integer key = Constants.encodedXY(nx, ny);
					if (!closed.containsKey(key)) {
						double h = Constants.normalDistance(nx - xEnd, ny - yEnd);	// distance to target used as heuristic
						double g = current.g + 1;									// current g + distance from current to neighbor
						PathNode node = opened.get(key);
						if (node == null) {
							// Not in the open set yet
							node = new PathNode(nx, ny, g, h, current);
							opened.put(key, node);
							pQueue.add(node);
						} else if (g < node.g) {
							// Have a better route to the current node, change its parent
							node.parent = current;
							node.g = g;
							node.h = h;
						}
					}
				}
			}
			if (opened.isEmpty() || timeLimit < System.currentTimeMillis()) {
				break;
			}
		}
		if (goal == null) return pathFromNode(closest);
		return pathFromNode(goal);
	}

	private static Path pathFromNode(PathNode goal) {
		if (goal == null) return null;
		assert goal.parent != null;		// Otherwise, we're asking the ant to go where it already is!
		Path p = new Path();
		int prevX = goal.x;
		int prevY = goal.y;
		PathNode node = goal;
		while (node != null) {
			assert (Math.abs(prevX - node.x) <= 1 && Math.abs(prevY - node.y) <= 1);
			if (node.parent != null) p.add(node.id);
			prevX = node.x;
			prevY = node.y;
			node = node.parent;
		}
		return p;
	}

	public String representation() {
		return representation(true);
	}

	public String representation(boolean decorate) {
		StringBuilder sb = new StringBuilder(100000);
		if (decorate) sb.append(String.format("Board x=%d-%d y=%d-%d known=%d\n", actualMinX, actualMaxX, minY, maxY, knownCells));
		else sb.append(String.format("%d %d\n", actualMinX, minY));
		int jNest = Constants.BOARD_SIZE - minX;
		int iNest = Constants.BOARD_SIZE - minY;
		int jStart = actualMinX - minX;
		int jEnd = actualMaxX - minX;
		int px, py;
		if (decorate) {
			sb.append("     ");
			for (int j = jStart; j < jEnd; j++) {
				px = j + minX;
				if (px % 10 == 0) sb.append('|');
				else if (px % 5 == 0) sb.append('5');
				else sb.append(' ');
			}
			sb.append('\n');
		}
		for (int i = knownRows.size() - 1; i >= 0; i--) {
			py = i + minY;
			if (decorate) sb.append(String.format("%4d ", py));
			BitSet known = knownRows.get(i);
			BitSet obs = obstacleRows.get(i);
			for (int j = jStart; j <= jEnd; j++) {
				px = j + minX;
				if (!known.get(j)) {
					sb.append(' ');
				} else if (obs.get(j)) {
					sb.append('#');
				} else if (i == iNest && j == jNest) {
					sb.append('N');
				} else {
					sb.append('.');
				}
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	// Is the status of point at coordinates x,y known?
	public boolean isKnown(int x, int y) {
		if (!validCoordinates(x, y)) return false;
		return knownRows.get(y - minY).get(x - minX);
	}

	// Get state of point at (x,y), see Constants.STATE_* for possible values
	public byte get(int x, int y) {
		if (!validCoordinates(x,y)) return Constants.STATE_UNKNOWN;
		int px = x - minX;
		int py = y - minY;
		if (!knownRows.get(py).get(px)) return Constants.STATE_UNKNOWN;
		if (obstacleRows.get(py).get(px)) return Constants.STATE_OBSTACLE;
		return Constants.STATE_PASSABLE;
	}

	// Set state of point (x,y) to 'value' (must be one of the non-unknown Constants.STATE_* values)
	private void setCell(int x, int y, byte state) {
		assert state == Constants.STATE_PASSABLE || state == Constants.STATE_OBSTACLE;
		assert validCoordinates(x, y);
		int px = x - minX;
		int py = y - minY;
		assert !obstacleRows.get(py).get(px) || state == Constants.STATE_OBSTACLE;
		if (!knownRows.get(py).get(px)) knownCells++;
		knownRows.get(py).set(px, true);
		obstacleRows.get(py).set(px, state==Constants.STATE_OBSTACLE);
		if (x < actualMinX) actualMinX = x;
		else if (x > actualMaxX) actualMaxX = x;
	}

	// Mark cell with coordinates (x,y) as passable
	public void updateCell(ZSquare square) {
		if (square.isPassable()) setPassable(square.x, square.y);
		else setObstacle(square.x, square.y);
	}

	// Mark cell with coordinates (x,y) as passable
	public void setPassable(int x, int y) {
		ensureCellExists(x, y);
//		assert !isKnown(x, y) || get(x, y) == Constants.STATE_PASSABLE;
		setCell(x, y, Constants.STATE_PASSABLE);
	}

	// Mark cell with coordinates (x,y) as being an obstacle
	public void setObstacle(int x, int y) {
		ensureCellExists(x, y);
		setCell(x, y, Constants.STATE_OBSTACLE);
	}

	// Does this board hold cell (x,y) right now?
	private boolean validCoordinates(int x, int y) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY;
	}

	// Ensure that we are covering cell with coordinates (x,y)
	private void ensureCellExists(int x, int y) {
		if (x < minX) {
			// We support only gradually shifting cells, so the new x needs to be right next to previous minX
			assert x == minX - 1;
			slide(knownRows, -1);
			slide(obstacleRows, -1);
			minX--;
			maxX--;
		} else if (x > maxX) {
			assert x == maxX + 1;
			slide(knownRows, 1);
			slide(obstacleRows, 1);
			minX++;
			maxX++;
		}
		if (y < minY) {
			assert y == minY - 1;
			// Add row in front
			knownRows.add(0, new BitSet(Constants.BOARD_SIZE));
			obstacleRows.add(0, new BitSet(Constants.BOARD_SIZE));
			minY--;
			assert maxY - minY >= 0 && maxY - minY < Constants.BOARD_SIZE; 
		} else if (y > maxY) {
			assert y == maxY + 1;
			// Add row at back
			knownRows.add(new BitSet(Constants.BOARD_SIZE));
			obstacleRows.add(new BitSet(Constants.BOARD_SIZE));
			maxY++;
			assert maxY - minY >= 0 && maxY - minY < Constants.BOARD_SIZE; 
		}
	}

	// Shift rows by one in given direction
	private static void slide(ArrayList<BitSet> rows, int dir) {
		assert dir == 1 || dir == -1;
		int start, end, i0, i;
		if (dir > 0) {
			start = 0;
			end = Constants.BOARD_SIZE - 2;
			i0 = start;
		} else {
			start = 1;
			end = Constants.BOARD_SIZE - 1;
			i0 = end;
		}
		for (BitSet row : rows) {
			i = i0;
			while (i>=start && i<=end) {
				row.set(i, row.get(i+dir));
				i += dir;
			}
			row.set(i, false);
		}
	}

}
