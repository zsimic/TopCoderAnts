Introduction:
-------------
This is my entry for the "Ants" TopCoder LinkedIn competition.

How to run a game:
------------------
Easiest way is to use the `run.pl` script:

		mkdir logs
		mkdir results
		perl run.pl --compile --debug --run 1

Use `perl run.pl -h` to see help on its usage.
The script invocation above will basically compile the project and run 1 game.
You can delete the 'logs' and 'results' folder anytime.
When you run with --debug, the Java program generates log information in the 'logs' folder.
The perl script looks at the 'logs' folder (as well as the servers output) and generates some overview in the 'results' folder.

You can also run the game directly from Eclipse of course.

Design:
-------

Initially, I had in mind a more complex design with more "roles" and "operations".
The idea was that ants could have various roles, and that they could switch roles in game as needed.
That turned out to be not possible in the time frame of the competition (and the fact that I joined late).

- **CommonAnt**: generic ant behavior/management + some utility and logging functions
 - **ZoranAnt**: effective implementation submitted, it just assigns the initial ant roles based on their "internal id"
- **Role**: allows to isolate behavior from the overall turn management, ants can change roles in time
 - **Guard**: simply stays put on the nest (in case opponents come to steal gathered food) and take food from neighboring cells (to help gatherers save a turn or two)
 - **Scout**: explores the board looking for food, brings food home as soon as it finds any
- **Operation**: helps abstract a set of simple actions into one 'Operation' (for example: go back to nest, which may be composed of hundreds of 'Move' actions)
 - **FollowPath**: follow a given 'Path' (sequence of 'Move' actions)
- **Board**: board representation allowing to quickly store/retrieve whether a cell was explored or not, and provides 2 flavors of the A* algorithm to help ants navigate through the game board
- **Path**: sequence of adjacent board coordinates, easily reversible
- **Trail**: stores the path from the nest to ant's current position, removing unneeded loops
- Helpers
 - **Constants**:
 - **Logger**:
 - **PathNode**:
 - **Scent**:
 - **ZSquare**:
