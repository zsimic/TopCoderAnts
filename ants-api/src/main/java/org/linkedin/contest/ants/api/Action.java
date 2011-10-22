package org.linkedin.contest.ants.api;

/**
 * A marker interface for different types of actions. Valid implementations are as follows:
 *
 * Move - Attempts to move one square in a given direction
 * Write - Writes a given long to the square the ant is currently on
 * Get Food - Takes one unit of food from an adjacent square. The ant will now be considered to be carrying the food
 * Drop Food - Drops the unit of food that the ant is carrying, if any, in the given direction
 * Say - Broadcasts the given string to some adjacent ants. Limited to 255 characters
 * Pass - The ant will take no action this turn
 */

public interface Action {
}
