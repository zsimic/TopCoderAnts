package org.linkedin.contest.ants.api;


/*
 * Attempts to move one square in a given direction
 * * Any number of the same class of ants may occupy one square
 * * If an ant attempts to move onto a square occupied by another type of ant, they will fight
 *   * A fight will end either when the moving ant dies, or when a hostile ant on the destination square dies
 *   * If an ant fights a single opposing ant and wins, it will successfully move to the destination square
 *   * Attacking ants close to their nest, or stacks of ants, both reduce the chance of a successful attack
 */

public class Move implements Action {
    private Direction dir;

    public Move(Direction d) {
        dir = d;
    }

    public Direction getDirection() {
        return dir;
    }

    public void setDirection(Direction dir) {
        this.dir = dir;
    }
}
