Introduction
------------
This is my entry for the "Ants" TopCoder LinkedIn competition.

How to run the game
-------------------
Easiest way is to use the `run.pl` script:

	git clone git://github.com/zsimic/TopCoderAnts.git
	perl run.pl --compile --debug --run 1

Use `perl run.pl -h` to see help on its usage.
The script invocation above will basically compile the project and run 1 game.
You can delete the 'logs' and 'results' folder anytime.
When you run with --debug, the Java program generates log information in the 'logs' folder.
The perl script looks at the 'logs' folder (as well as the servers output) and generates some overview in the 'results' folder.

You can also run the game directly from Eclipse of course.

See the [rules of the game](TopCoderAnts/blob/master/RULES.md).

There is also a [viewer](TopCoderAnts/tree/master/viewer) available now.

Description
-----------

Initially, I had in mind a more complex design with more "roles" and "operations".
The idea was that ants could have various roles, and that they could switch roles in game as needed.
I had in mind 5 roles:

- Manager: 1 ant staying next to the nest and assigning roles to all the other ants (that would do their job then come back to the nest for the next job)
- Scout: would go and just scout an area, then come back and communicate the board layout they saw to the manager
- Gatherer: would go and fetch food spotted by scouts
- Guard: would just guard the nest
- Soldier: would go in packs looking for other ants and attack them

That turned out to be not possible in the time frame of the competition (and the fact that I joined late).
Main limitations were:

- Not enough time
- No good visibility on the state of things (How well are my ants doing? Are they doing what they're supposed to do?)
- Near impossible to debug, random maps, lots of turns, lots of ants, large text files to deal with
- Runtime constraint of <500 ms for all 50 ants to finish their execution became quickly hard to respect :)

So, I quickly decided to keep as simple as possible:

- No fancy roles, just concentrate on getting food with gatherers (and a few guards just in case)
- 'assert' statements strategically placed to help find bugs as soon as they happen
- No time wasted on visualization tools
- Have just one tool that was capable of measuring roughly how the ants were performing by looking at how many turns it took them to amass half of the food on the board (the lower the number of turns needed, the better the ants are performing)
- Use 2 specialized versions of the A* algorithm to:
 - Be able to find the shortest path from current position to the nest (considering unexplored cells as unpassable)
 - Be able to find the closest unexplored cell from current position (in "discovery mode")
- The A* algorithm also had to allow for a "time cap": no more than 7 ms to find its shortest path, if it can't do it within that time frame just drop it or return the best solution found so far
- Try and keep runtime down as much as possible:
 - Avoid creating objects as much as possible
 - Come up with a few quick specialized containers that would help with doing basic things fast (the **Board**, **Path** and **Trail** classes)
 - Have a "production" mode with no logging whatsoever

What helped get things in a working state are mainly the 'assert' statements I think.
They really helped weed out the bugs early and prevented them from quickly accumulating.

The **Board** class was also very useful as it allowed for a very rudimentary visualization tool,
helped understand how well the scouts were covering their turf.
It turned out that it was easy to have its get/set routines down to a complexity of O(1), making it inexpensive to use at runtime.

The **Trail** class proved to be an excellent fallback for the times where the A* algorithm started taking too much time.
It was easy to make its storing complexity O(1), helping keep the overall runtime relatively unimpacted by its use.

Exploration
-----------
The ants rely on specialized variants of the [A* path finding algorithm](http://theory.stanford.edu/~amitp/GameProgramming/AStarComparison.html)
when possible for 2 operations:

- Finding unexplored cells on the board, see `Board.pathToClosestUnexplored`
- Finding the shortest way back to the nest, see `Board.bestPathToNest`

See [this video](http://www.youtube.com/watch?v=FNRfSQDF7TA) for example for a quick illustration of how the A* algorithm works.

## Finding unexplored cells
Each scout ant gets assigned a 'direction' (called 'slice' in the code) to explore.
That direction is simply given by an angle calculated from the ant's id (number assigned to each ant when the game starts, from 1 to 50).
There are 46 scouts at the start of the game, so each scout will get a "direction" to follow of the form ((N-1)/46)*2*pi radians.
'N' represents the scout's current 'slice number'.

The cost function for the A* algorithm is crafted in such a way that the ant will tend to go in its assigned direction from the nest.
Here's a "heat map" of how the cost distribution looks like on the board (the whiter the cell, the higher the cost):

![Cost illustration](https://github.com/zsimic/TopCoderAnts/raw/master/cost-illustration.png)

Following that direction won't always be possible of course (because of obstacles, or because the end of the board is met).
If no path can be found within 7ms, the ant will basically switch to the 'next slice' and explore that.
It will keep incrementing its slice until it can make progress.
If it can't make progress after 2 full rotations (having tried every slice twice),
it gives up and becomes a guard (goes back to nest and stays put there) to avoid consuming CPU cycles for nothing

I've upload a video illustrating an ant exploring its board using this algorithm [here](http://www.youtube.com/watch?v=GbUTx1at1XY).
The black cells are obstacles, while the gray ones are yet-unexplored. The moving blue cell is the ant, while green cells contain food.

## Going back to the nest
When an ant finds food, its mission becomes to bring it back to the nest immediately.
For that, it will first try and find the shortest path to the nest using the other variant of the A* algorithm.
If no path can be found in less than 7ms, it will fall back to simply following its **Trail** back to the nest
(which is simply its path from the nest that it keeps up to date at every movement)

Food hauling
------------
There are several ways of bringing food back to the nest.
There's the obvious way: take one food item, carry it to the nest, drop it there, come back to the same spot and repeat.
That however has a few drawbacks: the food may not be there anymore by the time the ant comes back, and it is inefficient
(lots of turns needed, the algorithm can become quickly complex especially if one starts leaving scents behind to try and direct other ants
to the food spot).

A better way turns out to be to simply pick and drop all the food items one by one towards the nest.
Then take 2 steps toward the nest and do that again. That method is more efficient for cases where the number of food items to be carried is >= 2.
So the scouts use that method: whenever they see food next to them, they go in a pick-drop-step-step mode, bringing the food closer and closer to the nest.
That method also has a few other benefits:

- several ants can end up collaborating together without any synchronization needed in bringing the food back home faster
- if an ant dies while it's doing that, it still made progress for the team: it brought the food closer to home for another ant to continue the work
- it's very simple: no need for scents or remembering where the food spot was
- once you get the automaton right, any ant can stop whatever they're doing and go in the pick-drop-step-step mode
- it's also very easy to determine when to stop working in that mode (when there are no food items nearby basically, or when the nest is reached)

So the overall 'food hauling' algorith becomes:

	if (ant.neighboringAmountOfFood() == 1) {
		// take the food item and bring it to the nest
	} else if (ant.neighboringAmountOfFood() > 1) {
		// go into the pick-drop-step-step mode
	}

I've uploaded a video illustrating the food-hauling described here to [youtube](http://www.youtube.com/watch?v=k8HUP4V1xvQ).
You can see many cases: ants doing the pick-drop-step-step alone, ants collaborating,
and even a bug where 2 ants get stuck picking and dropping food items without making progress (14 seconds in),
Situation gets resolved when a 3rd ant passes nearby and breaks their cycle.

Quick code overview
-------------------

- **CommonAnt**: Generic ant management + some utility and logging functions
 - **ZoranAnt**: Effective implementation submitted, it just assigns the initial ant roles based on their "internal id"
- **Role**: Allows to isolate behavior from the overall turn management, ants can change roles in time
 - **Guard**: Simply stays put on the nest (to protect gathered food from opponents coming and stealing it) and takes food from neighboring cells (to help gatherers save a turn or two)
 - **Scout**: Explores the board looking for food, brings food home as soon as it finds any
- **Operation**: Helps abstract a set of simple actions into one 'Operation' (for example: go back to nest, which may be composed of hundreds of 'Move' actions)
 - **FollowPath**: Follow a given 'Path' (sequence of 'Move' actions)
- **Board**: Board representation allowing to quickly store/retrieve whether a cell was explored or not, and provides 2 flavors of the A* algorithm to help ants navigate through the game board
- **Path**: Sequence of adjacent board coordinates, easily reversible
- **Trail**: Stores the path from the nest to ant's current position, removing unneeded loops
- Helpers
 - **Constants**: Board size and arithmetic on coordinates allowing to 'pack' x,y coordinates in one int
 - **Logger**: Logging features, optional as it impacts performance heavily, one file per ant + 1 board representation per ant
 - **PathNode**: Vessel for A* algorithm
 - **Scent**: 'long' ants can leave on board as scent, organized in a specific way so it can carry various types of info (with a checksum to verify it was left by an ant from our team)
 - **ZSquare**: Same as Square, but with a few additions such as coordinates
