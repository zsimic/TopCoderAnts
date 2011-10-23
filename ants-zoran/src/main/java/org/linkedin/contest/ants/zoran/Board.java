package org.linkedin.contest.ants.zoran;

import java.util.ArrayList;

public class Board {

	private ArrayList<int[]> rows;
	private int minX, minY, maxX, maxY;

	Board() {
		rows = new ArrayList<int[]>();
		minX = Constants.BOARD_SIZE / 2;
		maxX = minX + Constants.BOARD_SIZE - 1;
		minY = maxY = Constants.BOARD_SIZE;
		maxY--;
	}

	// Get state of point at (x,y), see Constants.STATE_* for possible values
	public int get(int x, int y) {
		assert validCoordinates(x,y);
		int rx = x - minX;
		int row[] = rows.get(y-minY);
		int v = row[rx >>> POINTS_PER_CELL_BITS];
		int ioffset = rx & POINTS_PER_CELL_MASK;
		v &= STATE_MASK << ioffset;
		v >>>= ioffset;
		assert v==Constants.STATE_UNKNOWN || v==Constants.STATE_PASSABLE || v==Constants.STATE_OBSTACLE;
		return v;
	}

	// Set state of point (x,y) to 'value' (must be one of the non-unknown Constants.STATE_* values)
	private void setCell(int x, int y, byte value) {
		assert value==Constants.STATE_PASSABLE || value == Constants.STATE_OBSTACLE;
		assert validCoordinates(x,y);
		int rx = x - minX;
		int icell = rx >>> POINTS_PER_CELL_BITS;
		int row[] = rows.get(y-minY);
		int v = row[icell];
		int ioffset = rx & POINTS_PER_CELL_MASK;
		v = v ^ (STATE_MASK << ioffset);
		row[icell] = v | (value << ioffset);
	}

	// Mark cell with coordinates (x,y) as passable
	public void setPassable(ZSquare square) {
		if (square.isPassable()) setPassable(square.x, square.y);
		else setObstacle(square.x, square.y);
	}

	// Mark cell with coordinates (x,y) as passable
	public void setPassable(int x, int y) {
		ensureCellExists(x,y);
		assert get(x,y)==Constants.STATE_UNKNOWN;
		setCell(x,y,Constants.STATE_PASSABLE);
	}

	// Mark cell with coordinates (x,y) as being an obstacle
	public void setObstacle(int x, int y) {
		ensureCellExists(x,y);
		assert get(x,y)==Constants.STATE_UNKNOWN || get(x,y)==Constants.STATE_PASSABLE;
		setCell(x,y,Constants.STATE_OBSTACLE);
	}

	// Does this board hold cell (x,y) right now?
	private boolean validCoordinates(int x, int y) {
		return x>=minX && x<=maxX && y>=minY && y<=maxY;
	}

	// Ensure that we are covering cell with coordinates (x,y)
	private void ensureCellExists(int x, int y) {
		if (x<minX) {
			// We support only gradually shifting cells, so the new x needs to be right next to previous minX
			assert x==minX-1;
			for (int row[] : rows) {
				// Shift all rows to the right
				for (int i = row.length-1; i > 0; i--) {
					row[i] = row[i-1];
				}
			}
			minX -= POINTS_PER_CELL;
			minY -= POINTS_PER_CELL;
		} else if (x > maxY) {
			assert x==maxX+1;
			for (int row[] : rows) {
				// Shift all rows to the left
				for (int i = 0; i < row.length-1; i++) {
					row[i] = row[i+1];
				}
			}
			minX += POINTS_PER_CELL;
			minY += POINTS_PER_CELL;
		}
		if (y<minY) {
			assert y==minY-1;
			int row[] = new int[ROW_SIZE];
			// Add row in front
			rows.add(0, row);
			minY--;
		} else if (y>maxY) {
			assert y==maxY+1;
			int row[] = new int[ROW_SIZE];
			// Add row at back
			rows.add(row);
			maxY++;
		}
	}

//	private static final int STATE_BITS = 2;					// Number of bits representing a state value (see Constants.STATE_*)
	private static final int STATE_MASK = 0x3;					// Mask for a state value (first STATE_BITS turned on)
	private static final int POINTS_PER_CELL = 16;				// Number of states we can store in an int (32 divided by STATE_BITS)
	private static final int POINTS_PER_CELL_BITS = 4;			// Number of bits to shift an x coordinate to find the 'cell' holding its state value
	private static final int POINTS_PER_CELL_MASK = 0xf;		// Mask to apply to an x coordinate to find its value within a 'cell' holding its state value
	private static final int ROW_SIZE = Constants.BOARD_SIZE / POINTS_PER_CELL;	// Number of 'cells' (ie ints) to allocate per row

}
