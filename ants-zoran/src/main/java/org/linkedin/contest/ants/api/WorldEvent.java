package org.linkedin.contest.ants.api;

/**
 *
 * An event in the world.
 * If it is a message from another ant, it will be in the form 'An ant says {message}'
 * If it is a fight notification, it will be in the form 'attack'
 * In any case, the Direction will indicate the direction from which the event came.
 */
public class WorldEvent {

    private final Direction dir;
    private final String event;

    public WorldEvent(Direction d, String ev) {
        dir = d;
        event = ev;
    }

    public String getEvent() {
        return event;
    }

    public Direction getDirection() {
        return dir;
    }
}
