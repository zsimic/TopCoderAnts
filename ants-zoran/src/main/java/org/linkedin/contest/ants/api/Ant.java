package org.linkedin.contest.ants.api;

import java.util.List;

/**
 * In addition to the below methods, please include a no-argument constructor to allow instantiation of your ants
 */

public interface Ant {

    /**
     * init() will be called on each ant at creation time.
     * This will be before the first move of the match
     *
     */

    public void init();

    /**
     * On each turn, act will be called to allow your ant to take some action in the world.
     * @param environment a description of the 9 squares around this ant (one in each direction + the one the ant is currently on)
     * @param events A list of Strings describing actions that affected this ant in the last turn.
     * In particular, if any ants said anything or fought with this ant, they will show up here
     * @return an implementation of the Action class indicating what this ant should do
     */

    public Action act(Environment environment, List<WorldEvent> events);

    /**
     * A special action that will be called on death, to allow your ant to do something other than disappear
     * The only valid actions here are Write, Say, or Pass, and no environment information is given.
     * Only the specific cause of death is given, or null if the death was self-inflicted (for example, a failed attack)
     * This method will be called regardless of the cause of death, unless the death is for a rules violation
     * @param cause The WorldEvent (attack) that caused the ant to die.
     * @return the action to perform
     */

    public Action onDeath(WorldEvent cause);
}
