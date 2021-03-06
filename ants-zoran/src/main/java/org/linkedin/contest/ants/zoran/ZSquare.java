package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

/**
 * Same as Square, but with a few additions such as coordinates
 */
public class ZSquare {

	protected Square square;		// Associated square from environment
	protected Scent scent;			// Scent found on square
	protected CommonAnt ant;		// Associated ant
	protected Direction dir;		// Direction of this square (relative to 'ant')
	protected int deltaX, deltaY;	// Relative coordinates of this square (relative to 'here')
	protected int x, y;				// Coordinates of this square (relative to nest)

	ZSquare(CommonAnt ant, Direction dir) {
		this.ant = ant;
		this.dir = dir;
		this.deltaX = dir.deltaX;
		this.deltaY = dir.deltaY;
		this.scent = new Scent();
	}

	@Override
	public String toString() {
		return String.format("%s%c [%d,%d]", dir.name(), isPassable() ? ' ' : '*', x, y);
	}

	// Normalized deltaX
	public double normalX() {
		switch (dir.ordinal()) {
		case 1:
		case 3:
		case 5:
		case 7:
			return deltaX*0.707106781;
		default: return deltaX;
		}
	}

	public double normalY() {
		switch (dir.ordinal()) {
		case 1:
		case 3:
		case 5:
		case 7:
			return deltaY*0.707106781;
		default: return deltaY;
		}
	}

	// Update square state from environment
	public void update (Environment environment) {
		square = environment.getSquare(dir);
		scent.update(square.getWriting());
		x = ant.x + deltaX;
		y = ant.y + deltaY;
		ant.board.updateCell(this);
		if (square.getNumberOfAnts() >= 5 && Math.abs(x - Constants.BOARD_SIZE) > 170 && Math.abs(y - Constants.BOARD_SIZE) > 170) {
			if (ant.board.get(x, y) == Constants.STATE_PASSABLE) {
				// This looks suspiciously like enemy's nest, mark it as non passable.
				ant.board.setObstacle(x, y);
//L				Logger.inform(ant, String.format("avoiding %d,%d probably enemy nest", x, y));
			}
		}
	}

	// Is this square a nest, meaning that food here is counted toward the final score?
	public boolean isNest() {
		return square.isNest();
	}

	// Is this square currently relatively close to the nest?
	protected boolean isAroundNest() {
		return Math.abs(x - Constants.BOARD_SIZE) <= 60 && Math.abs(y - Constants.BOARD_SIZE) <= 60;
	}

	protected boolean isNextToNest() {
		return !isNest() && Math.abs(x - Constants.BOARD_SIZE) <= 1 && Math.abs(y - Constants.BOARD_SIZE) <= 1;
	}

	// Is this square passable, meaning that it is possible to move onto it?
	public boolean isPassable() {
		return square.isPassable();
	}

	// Does the square have at least one unit of food on it?
	public boolean hasFood() {
		return square.hasFood();
	}

	// Does the square have at least one ant on it?
	public boolean hasAnts() {
		return square.hasAnts();
	}

	// How many ants are on this square?
	public int getNumberOfAnts() {
		return square.getNumberOfAnts();
	}

	// How much food is on this square?
	public int getAmountOfFood() {
		return square.getAmountOfFood();
	}

}
