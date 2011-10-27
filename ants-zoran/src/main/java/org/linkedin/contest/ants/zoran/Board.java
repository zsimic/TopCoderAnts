package org.linkedin.contest.ants.zoran;

import java.util.*;
import org.linkedin.contest.ants.api.*;

public class Board {

	private CommonAnt ant;
	private ArrayList<BitSet> knownRows;
	private ArrayList<BitSet> obstacleRows;
	private int minX, minY, maxX, maxY;
	private int actualMinX, actualMaxX;

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
	public Path pathToClosestUnexplored(int xStart, int yStart, Direction direction) {
		assert get(xStart, yStart) == Constants.STATE_PASSABLE;
		HashMap<Integer, PathNode> opened = new HashMap<Integer, PathNode>();
		HashMap<Integer, PathNode> closed = new HashMap<Integer, PathNode>();
		PriorityQueue<PathNode> pQueue = new PriorityQueue<PathNode>(20, new PathNodeComparator());
		PathNode start = new PathNode(xStart, yStart, 0, 0, null);
		opened.put(Constants.encodedXY(xStart, yStart), start);
		pQueue.add(start);
		PathNode goal = null;
		boolean cont = true;
		while (cont) {
			PathNode current = pQueue.poll();
			if (get(current.x, current.y) == Constants.STATE_UNKNOWN) {
				goal = current;					// We found the target
				assert goal.parent != null;		// Otherwise, we're asking the ant to go where it already is!
				cont = false;
				break;
			}
			opened.remove(current.id);
			closed.put(current.id, current);
			for (Direction neighbor : neighbors) {
				int nx = current.x + neighbor.deltaX;		// Coordinates of neighbor
				int ny = current.y + neighbor.deltaY;
				byte state = get(nx, ny);
				if (state == Constants.STATE_OBSTACLE) {
					// Pass
				} else if (state == Constants.STATE_UNKNOWN) {
					if (isPointInSection(nx, ny, direction)) {
						goal = current;
						cont = false;
						break;
					}
				} else {
					Integer key = Constants.encodedXY(nx, ny);
					if (!closed.containsKey(key)) {
						double g = current.g + 1;									// current g + distance from current to neighbor
						PathNode node = opened.get(key);
						if (node == null) {
							// Not in the open set yet
							node = new PathNode(nx, ny, g, 0, current);
							opened.put(key, node);
							pQueue.add(node);
						} else if (g < node.g) {
							// Have a better route to the current node, change its parent
							node.parent = current;
							node.g = g;
						}
					}
				}
			}
			if (opened.isEmpty()) cont = false;
		}
		return pathFromNode(goal);
	}

	private boolean isPointInSection(int x, int y, Direction direction) {
		return true;
	}

	// Best path from xStart,yStart to xEnd,yEnd (excluding the start coordinates)
	public Path bestPath(int xStart, int yStart, int xEnd, int yEnd) {
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
		boolean cont = true;
		while (cont) {
			PathNode current = pQueue.poll();
			opened.remove(current.id);
			if (current.x == xEnd && current.y == yEnd) {
				goal = current;					// We found the target
				assert goal.parent != null;		// Otherwise, we're asking the ant to go where it already is!
				cont = false;
				break;
			}
			closed.put(current.id, current);
			for (Direction neighbor : neighbors) {
				int nx = current.x + neighbor.deltaX;		// Coordinates of neighbor
				int ny = current.y + neighbor.deltaY;
				byte state = get(nx, ny);
				if (state != Constants.STATE_OBSTACLE) {
					Integer key = Constants.encodedXY(nx, ny);
					if (!closed.containsKey(key)) {
						double h = Constants.normalDistance(nx - xEnd, ny - yEnd);	// distance to target used as heuristic
						double g = current.g + 1;									// current g + distance from current to neighbor
						PathNode node = opened.get(key);
						if (node == null) {
							// Not in the open set yet
							node = new PathNode(nx, ny, g, h, current);
							if (state == Constants.STATE_UNKNOWN) {
								// We haven't explored 'node' yet, put it in 'closed' immediately (its heuristic won't change)
								closed.put(key, node);
							} else {
								opened.put(key, node);
								pQueue.add(node);
							}
						} else if (g < node.g) {
							// Have a better route to the current node, change its parent
							node.parent = current;
							node.g = g;
							node.h = h;
						}
						if (state == Constants.STATE_UNKNOWN) {
							if (goal == null || goal.getF() > node.getF()) {
								// Mark best not-yet-explored node as the goal (unless we reach the real goal)
								goal = node;
							}
						} else if (closest == null || closest.h > node.getF()) {
							closest = node;
						}
					}
				}
			}
			if (opened.isEmpty()) cont = false;
		}
		if (goal == null) goal = closest;
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
		String s = "";
		if (decorate) s+= String.format("Board x=%d-%d y=%d-%d\n", actualMinX, actualMaxX, minY, maxY);
		else s+= String.format("%d %d\n", actualMinX, minY);
		String line;
		int jNest = Constants.BOARD_SIZE - minX;
		int iNest = Constants.BOARD_SIZE - minY;
		int jStart = actualMinX - minX;
		int jEnd = actualMaxX - minX;
		int px, py;
		if (decorate) {
			s += "     ";
			for (int j = jStart; j < jEnd; j++) {
				px = j + minX;
				if (px % 10 == 0) s += '|';
				else if (px % 5 == 0) s += '5';
				else s += ' ';
			}
			s += '\n';
		}
		for (int i = knownRows.size() - 1; i >= 0; i--) {
			py = i + minY;
			line = new String();
			if (decorate) line += String.format("%4d ", py);
			BitSet known = knownRows.get(i);
			BitSet obs = obstacleRows.get(i);
			for (int j = jStart; j < jEnd; j++) {
				px = j + minX;
				if (!known.get(j)) {
					line += ' ';
				} else if (obs.get(j)) {
					line += '#';
				} else if (i == iNest && j == jNest) {
					line += 'N';
				} else {
					int food = decorate ? ant.foodStock.foodAmount(px, py) : 0;
					if (food == 0) line += '.';
					else if (food > 10) line += '^';
					else line += '%';
				}
			}
			s += line + '\n';
		}
		return s;
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

	// Set board state from received 'lines'
	public void setFromLines(List<String> lines) {
		if (lines.size() < 4) return;
		String firstLine = lines.remove(0);
		if (firstLine.isEmpty()) return;
		int i = firstLine.indexOf(' ');			// xStart
		if (i < 0) return;
		String sn = firstLine.substring(0, i);
		if (!Constants.isNumber(sn)) return;
		int xStart = Integer.parseInt(sn);
		sn = firstLine.substring(i + 1);		// yStart
		if (!Constants.isNumber(sn)) return;
		int yEnd = Integer.parseInt(sn) + lines.size() - 1;
		for (i = maxY + 1; i < yEnd; i++) {
			ensureCellExists(Constants.BOARD_SIZE, i);
		}
		if (xStart + lines.get(0).length() - 1 > maxX + 1) {
			for (i = maxX + 1; i < xStart + lines.get(0).length() - 1; i++) {
				ensureCellExists(i, Constants.BOARD_SIZE);
			}
		}
		for (String line : lines) {
			for (i = line.length() - 1; i >= 0; i--) {
				char c = line.charAt(i);
				if (c == '.') setPassable(xStart + i, yEnd);
				else if (c == '#') setObstacle(xStart + i, yEnd);
				else if (c == 'N') assert xStart + i == Constants.BOARD_SIZE && yEnd == Constants.BOARD_SIZE;
				else assert c == ' ';
			}
			yEnd--;
		}
		if (ant.id == 10) {
			System.out.print(representation(true));
		}
	}

	// Set state of point (x,y) to 'value' (must be one of the non-unknown Constants.STATE_* values)
	private void setCell(int x, int y, byte state) {
		assert state == Constants.STATE_PASSABLE || state == Constants.STATE_OBSTACLE;
		assert validCoordinates(x, y);
		int px = x - minX;
		int py = y - minY;
//		assert !obstacleRows.get(py).get(px) || state == Constants.STATE_OBSTACLE;
		if (obstacleRows.get(py).get(px) && state == Constants.STATE_PASSABLE) {
			ant.dump(String.format("check x=%d y=%d s=%d", x, y, state));
		}
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
