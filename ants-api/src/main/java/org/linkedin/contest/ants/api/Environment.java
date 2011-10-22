package org.linkedin.contest.ants.api;

public interface Environment {
    // For debugging purposes, this class will be provided as a full map of the entire
    // area until the competition (accessible via debugger etc.).
    // Obviously, this functionality will not be present during competition runs.

    // Gets the square in the indicated direction from the ant
    public Square getSquare(Direction d);
}
