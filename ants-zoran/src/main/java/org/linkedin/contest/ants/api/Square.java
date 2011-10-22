package org.linkedin.contest.ants.api;


public interface Square {


    // Is this square a nest, meaning that food here is counted toward the final score?
    public boolean isNest();

    // Is this square passable, meaning that it is possible to move onto it?
    public boolean isPassable();
    // Does the square have at least one unit of food on it?
    public boolean hasFood();
    // Does the square have at least one ant on it?
    public boolean hasAnts();
    // Does the square have writing on it?
    public boolean hasWriting();

    // How many ants are on this square?
    public int getNumberOfAnts();
    // How much food is on this square?
    public int getAmountOfFood();
    // What is written on this square. If nothing is written, this will return null.
    public Long getWriting();
}
