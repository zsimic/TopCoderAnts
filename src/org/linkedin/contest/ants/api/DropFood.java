package org.linkedin.contest.ants.api;

/*
 * Attempts to drop a unit of food in the indicated direction
 * If the ant is not carrying food, the ant will do nothing
 */

public class DropFood implements Action {
    private Direction dir;

    public DropFood(Direction d) {
        dir = d;
    }

    public Direction getDirection() {
        return dir;
    }

    public void setDirection(Direction dir) {
        this.dir = dir;
    }
}
