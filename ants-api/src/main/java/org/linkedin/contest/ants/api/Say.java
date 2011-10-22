package org.linkedin.contest.ants.api;

import java.util.*;

/*
 * Broadcasts a message in the indicated directions. All ants in the specified directions
 * will have a message inserted into their list of events for their next turn
 * Limited to 255 characters
 */

public class Say implements Action {
    private String message;
    private Set<Direction> directions;

    public Say(String m, Direction... dirs) {
        message = m;
        directions = EnumSet.noneOf(Direction.class);
        directions.addAll(Arrays.asList(dirs));
    }

    public Set<Direction> getDirections()
    {
        return Collections.unmodifiableSet(directions);
    }

    public void setDirections(Direction... dirs)
    {
        directions = EnumSet.noneOf(Direction.class);
        directions.addAll(Arrays.asList(dirs));
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String m) {
        message = m;
    }
}
