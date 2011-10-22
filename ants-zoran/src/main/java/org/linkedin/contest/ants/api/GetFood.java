package org.linkedin.contest.ants.api;

/*
 * Attempts to pick up a unit of food from the indicated direction
 * If the ant is already carrying food, or there is no food in that direction
 * The ant will do nothing
 */

public class GetFood implements Action {
    private Direction dir;

    public GetFood(Direction d) {
        dir = d;
    }

    public Direction getDirection() {
        return dir;
    }

    public void setDirection(Direction dir) {
        this.dir = dir;
    }
}
