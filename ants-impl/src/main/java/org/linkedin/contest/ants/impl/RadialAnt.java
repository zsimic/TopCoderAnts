package org.linkedin.contest.ants.impl;

import java.util.*;

import org.linkedin.contest.ants.api.*;

public class RadialAnt implements Ant {

    private int turn = 0;
    private boolean needsDirection = true;
    private int nestDeltaX = 0;
    private int nestDeltaY = 0;
    private int baseDeltaX = 0;
    private int baseDeltaY = 0;
    private double idealTangent;
    private double idealAngle;
    private int myID = 0;
    private int cycles = 0;
    private state currentState;
    private Stack<Direction> path = new Stack<Direction>();
    private Direction lastDirection;

    private enum state {
        searching,
        gathering,
        following,
        returning,
    }

	private boolean hasFood = false;

	public RadialAnt()
	{
	}

	public void init() {
	    currentState = state.searching;
    }

	public Action act(Environment environment,List<WorldEvent> events) {
        Action ret;
        turn += 1;
        if (myID == 0)
            myID = 49 - environment.getSquare(Direction.here).getNumberOfAnts();
        if (needsDirection) {
            // choose a new direction
            cycles += 1;
            idealAngle = 2 * Math.PI * (myID / 50.0) * cycles;
            idealTangent = getIdealTangent(idealAngle);
            needsDirection = false;

        }

        if (currentState == state.searching)
        {
            ret = doSearch(environment);
        }

        else if (currentState == state.gathering)
        {
            ret = doGather(environment);
        }

        else if (currentState == state.following)
        {
            ret = doFollow(environment);
        }
        else if (currentState == state.returning)
        {
            ret = doReturn(environment);
        } else {
            // confused!
            currentState = state.searching;
            ret = doSearch(environment);
        }
        return ret;
	}

    public Action doReturn(Environment e)
    {
        // We are returning to the nest
        if (hasFood) {
            // see if we need to write 'food' here
            Long msg = e.getSquare(Direction.here).getWriting();
            if (msg == null || !isFood(msg)) {
                return new Write(encode(msgState.food));
            }
        }
        // we don't have food, or we've already written here
        if (!path.empty()) {
            Direction d = path.pop();
            updateDeltas(d);
            return new Move(d);
        } else {
            // we have returned... we think. Try to follow to food, or search otherwise
            needsDirection = true;
            return doFollow(e);
        }

    }

    public Action doFollow(Environment e)
    {
        // we are following another ant's scent trail to food
        int earliestTurn = Integer.MAX_VALUE;
        Direction dir = Direction.here;
        for (Direction d : Direction.values()) {
            if (e.getSquare(d).hasWriting())
            {
                Long msg = e.getSquare(d).getWriting();
                if (isFood(msg)) {
                    int turn = getTurn(msg);
                    if (turn < earliestTurn) {
                        earliestTurn = turn;
                        dir = d;
                    }
                }

            }
        }
        if (dir == Direction.here) {
            // could not find a better place to go
            if (earliestTurn == Integer.MAX_VALUE) {
                // apparently, no writing. Switch to searching
                currentState = state.searching;
                return doSearch(e);
            }
            else {
                // we're here! switch to gather mode
                needsDirection = true;
                currentState = state.gathering;
                baseDeltaX = 0;
                baseDeltaY = 0;
                path.clear();
                return doGather(e);
            }
        } else {
            updateDeltas(dir);
            return new Move(dir);
        }

    }

    private Action doGather(Environment e)
    {
        // step one: Do we have food?
        if (hasFood)
        {
            // If we have food, we should be returning to the nest.
            int latestTurn = 0;
            Direction dir = Direction.here;
            for (Direction d : Direction.values()) {
                if (e.getSquare(d).hasWriting())
                {
                    Long msg = e.getSquare(d).getWriting();
                    if (isFood(msg)) {
                        int turn = getTurn(msg);
                        if (turn > latestTurn) {
                            latestTurn = turn;
                            dir = d;
                        }
                    }

                }
            }
            if (dir == Direction.here) {
                // could not find a better place to go
                if (latestTurn == Integer.MAX_VALUE) {
                    // apparently, no writing. Try to move back to the 'base'
                    Direction d = path.pop();
                    updateDeltas(d);
                    return new Move(d);
                }
                else {
                    // we're here! Try to drop food on the nest
                    for (Direction d : Direction.values()) {
                        if (e.getSquare(d).isNest())
                        {
                            currentState = state.following;
                            baseDeltaX = 0;
                            baseDeltaY = 0;
                            path.clear();
                            hasFood = false;
                            return new DropFood(d);

                        }
                    }
                    // Confused!
                    Direction d = findBestDirection(nestDeltaX, nestDeltaY, e);
                    updateDeltas(d);
                    return new Move(d);
                }
            } else {
                updateDeltas(dir);
                return new Move(dir);
            }

        } else {
            // don't have food yet, go looking for it
            for (Direction d : Direction.values()) {
                if (e.getSquare(d).hasFood() && !e.getSquare(d).isNest()) {
                    hasFood = true;
                    return new GetFood(d);
                }
            }

            if (distanceFromBase() > 5) {
                currentState = state.searching;
            }
            Direction d = findBestDirection(baseDeltaX, baseDeltaY, e, idealTangent);
            updateDeltas(d);
            path.push(DirectionHelper.getOppositeDirection(d));
            return new Move(d);
        }

    }

    private Action doSearch(Environment e) {
        if (turn % 2048 == 0 && !needsDirection) {
            // abandon search and go back
            currentState = state.returning;
            return doReturn(e);
        }
        Square here = e.getSquare(Direction.here);
        if (!here.hasWriting()) {
            return new Write(encode(msgState.visited));
        }
        for (Direction d : Direction.values()) {
                if (e.getSquare(d).hasFood() && !e.getSquare(d).isNest()) {
                    currentState = state.returning;
                    hasFood = true;
                    return new GetFood(d);
                }
            }

        Direction d = findBestDirection(baseDeltaX, baseDeltaY, e, idealTangent);
            updateDeltas(d);
            path.push(DirectionHelper.getOppositeDirection(d));
            return new Move(d);
    }

    private Direction findBestDirection(double deltaX, double deltaY, Environment e)
    {
        double bestDistance = Double.MAX_VALUE;
        Direction bestDirection = Direction.here;
        Direction bestVisitedDirection = Direction.here;
        int bestVisitedTurn = Integer.MAX_VALUE;

        for (Direction d : Direction.values())
        {
            if (d != Direction.here && d != DirectionHelper.getOppositeDirection(lastDirection)) {
                if (e.getSquare(d).isPassable())
                {

                        double newDistance = distance(deltaX + d.deltaX, deltaY + d.deltaY);
                        if (newDistance < bestDistance) {
                            if (!e.getSquare(d).hasWriting() ||
                                    !isVisited(e.getSquare(d).getWriting()) ||
                                    getTurn(e.getSquare(d).getWriting()) + 500 < turn) {
                                bestDistance = newDistance;
                                bestDirection = d;
                            } else {
                                int newTurn = getTurn(e.getSquare(d).getWriting());
                                if (newTurn < bestVisitedTurn) {
                                    bestVisitedDirection = d;
                                    bestVisitedTurn = newTurn;
                                }
                            }
                        }
                }
            }
        }
        if (bestDirection == Direction.here)
        {
            bestDirection = bestVisitedDirection;
        }

        lastDirection = bestDirection;
        return bestDirection;
    }

    private Direction findBestDirection(double deltaX, double deltaY, Environment e, double tangent) {

        double bestTangent = Double.MAX_VALUE;
        Direction bestDirection = Direction.here;
        Direction bestVisitedDirection = Direction.here;
        int bestVisitedTurn = Integer.MAX_VALUE;

        for (Direction d : Direction.values())
        {
            if (d != Direction.here && d != DirectionHelper.getOppositeDirection(lastDirection))
                if (e.getSquare(d).isPassable())
                {
                    double newtan = (deltaY + d.deltaY) / (deltaX + d.deltaX);
                    if (Math.abs(newtan - tangent) < Math.abs(bestTangent - tangent)) {
                        if ((!e.getSquare(d).hasWriting() ||
                                !isVisited(e.getSquare(d).getWriting())) ||
                                getTurn(e.getSquare(d).getWriting()) + 500 < turn) {
                                bestTangent = newtan;
                                bestDirection = d;
                            } else {
                                int newTurn = getTurn(e.getSquare(d).getWriting());
                                if (newTurn < bestVisitedTurn) {
                                    bestVisitedDirection = d;
                                    bestVisitedTurn = newTurn;
                                }
                            }
                    }
                }

        }
        if (bestDirection == Direction.here)
        {
            bestDirection = bestVisitedDirection;
        }
        lastDirection = bestDirection;
        return bestDirection;
    }

    private void updateDeltas(Direction d) {
        nestDeltaX += d.deltaX;
        nestDeltaY += d.deltaY;
        baseDeltaX += d.deltaX;
        baseDeltaY += d.deltaY;

    }

	public Action onDeath(WorldEvent cause) {

		return new Write(encode(msgState.death));
	}

    private Long encode(msgState msg)
    {
       return new Long(turn | ((long) msg.ordinal()) << 32);
    }

    private boolean isFood(Long msg) {
        return msg != null && msgState.food.ordinal() == (msg >> 32);
    }
    private boolean isVisited(Long msg) {
        return msg != null &&msgState.visited.ordinal() == (msg >> 32);
    }
    private boolean isDeath(Long msg) {
        return msg != null && msgState.death.ordinal() == (msg >> 32);
    }

    private int getTurn(Long msg) {
        if (msg == null)
            return Integer.MAX_VALUE;
        long mask = 0xFFFFFFFF;
        return (int) (msg & mask);
    }

    private double distance(double x, double y)
    {
        return Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    }

    private double distanceFromNest() {
        return distance(nestDeltaX, nestDeltaY);
    }

    private double distanceFromBase() {
        return distance(baseDeltaX, baseDeltaY);
    }

    // angle is in radians
    private double getIdealTangent(double angle) {
        double opposite = Math.sin(angle); // assume hypotenuse to have length 1
        double adjacent = Math.cos(angle);
        return opposite/adjacent;
    }

    private enum msgState {
        food,
        death,
        visited,
    }



}
