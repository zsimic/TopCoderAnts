package org.linkedin.contest.ants.zoran;

import java.util.*;

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

	public String representation() {
		String s = String.format("Board x=%d-%d y=%d-%d\n", actualMinX, actualMaxX, minY, maxY);
		String line;
		int jNest = Constants.BOARD_SIZE - minX;
		int iNest = Constants.BOARD_SIZE - minY;
		int jStart = actualMinX - minX;
		int jEnd = actualMaxX - minX;
		int px, py;
//		s += String.format("%4d ", actualMinX);
		s += "     ";
		for (int j = jStart; j < jEnd; j++) {
			px = j + minX;
			if (px % 10 == 0) s += '|';
			else if (px % 5 == 0) s += '5';
			else s += ' ';
		}
		s += '\n';
		for (int i = knownRows.size() - 1; i >= 0; i--) {
			py = i + minY;
			line = new String();
			line += String.format("%4d ", py);
			BitSet known = knownRows.get(i);
			BitSet obs = obstacleRows.get(i);
			for (int j = jStart; j < jEnd; j++) {
				px = j + minX;
				if (!known.get(j)) {
					line += ' ';
				} else if (obs.get(j)) {
					assert !ant.path.has(px, py);
					line += '#';
				} else if (i == iNest && j == jNest) {
					line += 'N';
				} else if (ant.path.has(px, py)) {
					line += 'x';
				} else {
					int food = ant.foodStock.foodAmount(px, py);
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
	public int get(int x, int y) {
		assert validCoordinates(x,y);
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
		}
	}

}
