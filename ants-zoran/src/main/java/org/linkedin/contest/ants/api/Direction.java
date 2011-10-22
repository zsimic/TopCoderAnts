package org.linkedin.contest.ants.api;


public enum Direction {
    north(-1,0),
    northeast(-1,1),
    east(0, 1),
    southeast(1,1),
    south(1,0),
    southwest(1,-1),
    west(0,-1),
    northwest(-1,-1),
    here(0,0),
    ;

    public final int deltaX;
    public final int deltaY;

    Direction(int xOffset, int yOffset)
    {
        deltaX = xOffset;
        deltaY = yOffset;
    }
}
