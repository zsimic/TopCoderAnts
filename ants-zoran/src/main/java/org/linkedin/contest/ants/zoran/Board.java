package org.linkedin.contest.ants.zoran;

import java.util.ArrayList;
import java.util.BitSet;

public class Board {

	private ArrayList<BitSet> knownRows;
	private ArrayList<BitSet> obstacleRows;
	private int minX, minY, maxX, maxY;

	Board() {
		knownRows = new ArrayList<BitSet>();
		obstacleRows = new ArrayList<BitSet>();
		minX = Constants.BOARD_SIZE / 2;
		maxX = minX + Constants.BOARD_SIZE - 1;
		minY = maxY = Constants.BOARD_SIZE;
		maxY--;
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
		assert !obstacleRows.get(py).get(px) || state == Constants.STATE_OBSTACLE; 
		knownRows.get(py).set(px, true);
		obstacleRows.get(py).set(px, state==Constants.STATE_OBSTACLE);
	}

	// Mark cell with coordinates (x,y) as passable
	public void setPassable(ZSquare square) {
		if (square.isPassable()) setPassable(square.x, square.y);
		else setObstacle(square.x, square.y);
	}

	// Mark cell with coordinates (x,y) as passable
	public void setPassable(int x, int y) {
		ensureCellExists(x, y);
		assert !isKnown(x, y) || get(x, y) == Constants.STATE_PASSABLE;
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
			minY--;
		} else if (x > maxY) {
			assert x==maxX+1;
			slide(knownRows, 1);
			slide(obstacleRows, 1);
			minX++;
			minY++;
		}
		if (y<minY) {
			assert y==minY-1;
			// Add row in front
			knownRows.add(0, new BitSet(Constants.BOARD_SIZE));
			obstacleRows.add(0, new BitSet(Constants.BOARD_SIZE));
			minY--;
		} else if (y>maxY) {
			assert y==maxY+1;
			// Add row at back
			knownRows.add(new BitSet(Constants.BOARD_SIZE));
			obstacleRows.add(new BitSet(Constants.BOARD_SIZE));
			maxY++;
		}
	}

	// Shift rows by one in given direction
	private static void slide(ArrayList<BitSet> rows, int dir) {
		int start = dir > 0 ? Constants.BOARD_SIZE-1 : 1;
		int end = dir > 0 ? 0 : Constants.BOARD_SIZE-2;
		int i;
		for (BitSet row : rows) {
			i = start;
			while (i<=start && i>=end) {
				row.set(i, row.get(i+dir));
				i += dir;
			}
		}
	}

}
