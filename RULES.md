Ants
====

Administrator: Jacob Kessler

Important Files:

- **ants-api.jar**: The API you need to implement to participate, contains the basic interface you will be implementing along with various utility classes, the source will also be provided.
- **ants-server.jar**: The game server, for testing purposes, contains the actual game implementation. You should not need to refer to any of its classes, although some will be accessible if the server is started in Debug mode.
- **ants-impl.jar**: Sample implementations to serve as API reference, contains two sample implementations of an AI interface. The source will be provided.
Please note the sample implementations are deliberately stupid, and are provided purely as a reference for the APIs.

The Ants challenge centers around coordinating a significant number of independent agents with fairly narrow
views of a game world to accomplish a task.

Two classes of ants (yours and an opponents) will each be instantiated 50 times and will compete to gather food
in a 2d game world. Thus, you will (initially) have 50 independent agents to work with.
The object of the game is for your ants to collect more food than your opponent.
Food is scattered randomly around the game world. To collect it, your ants must find it,
carry it back to their nest, and drop it there.
Each ant acts independently. The only means of communication between ants are writing messages on a particular
square or sending (short) strings to nearby ants. Ants may have no shared memory state, but are free to maintain per-ant state.
The game will end either after each ant has taken 100K turns or one side has gathered more than 50% of the food on the map.

## The Game World
The game world consists of a 512x512 board with impassable edges

Each square may be impassable or open. Open squares may contain ants, food, nests, or some combination of those three.

The world is static apart from the actions of ants - there are no random world events.

### Objects and Actions in the World

#### Nests
Each type of ant has a nest, which is otherwise identical to a normal open square.
Each piece of food present on the nest square is worth one point.
Note that all ants (even opposing ants) may freely enter nest squares, as well as adding or removing food from them.

All ants start on their nest.
Nests will be placed randomly, but will be at least one third of the world (171 squares) away from each other.
The area around each nest is guaranteed to be clear of impassable squares.

#### Food
The goal of the game, Food is found randomly scattered around the world, and each piece present at the nest at the end of the game is worth one point.
Ants may carry at most one unit of food at a time, but a square may contain any number of units of food.

#### Ants
Ants are the agents by which you must find and gather food. Any number of Ants of the same type (IE instantiations of the same class) may occupy the same square.
If an ant attempts to enter a square occupied by another type of ant, they will fight.
The chance of victory is heavily influenced by the the number of ants in the defending square and the number of surrounding squares
occupied by the attacking ant's side. Ants also receive bonuses near their nests Any attempt to enter a square occupied by a hostile
ant will result in either the death of the attacking ant or the death of an ant in the defending square. If the attack kills the last
hostile ant on the defending square, the attacking ant will move onto the defending square.

Ants have two means of communicating:

* Ants may write a Long onto the square that they currently occupy, which will remain on that square until changed by another ant. The Long will be visible to all ants on or adjacent to that square.
* Ants may also send a String of up to 255 characters to any adjacent square(s) that they want to. It is up to the receiving ants to process and respond to such messages, they are not stored in the world.

#### Impassable squares
Some squares are impassable, and cannot be moved through. These both prevent ants from moving off the edge of the board, and provide obstacles to make the process of searching more interesting. They are placed randomly at world generation time and remain static.

#### Actions
Each turn, each ant instance is given a chance to act in the game world. Six actions are possible:

* Move - Attempts to move one square in a given direction
* Write - Writes a given long to the square the ant is currently on
* Get Food - Takes one unit of food from an adjacent square. The ant will now be considered to be carrying the food
* Drop Food - Drops the unit of food that the ant is carrying, if any, in the given direction
* Say - Broadcasts the given string to some adjacent ants. Limited to 255 characters
* Pass - The ant will take no action this turn

Actions that cannot be completed (dropping food while not carrying any, for example) will be treated as passes by the server

#### The End of an Ants Game
The game will end once one side has collected more than 50% of the food on the map, or when each surviving ant has taken 100,000 turns.

#### Tallying Scores
Once the game has ended, the player who has collected the most food wins.
If both players have collected the same amount of food, the player with the most remaining ants wins.
If both players also have the same number of ants, the game will be considered a tie.

---------------

The basic API you will be implementing is in ants-api as the interface `org.linkedin.contest.ants.api.Ant` :

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
		* @return the action to perform
		*/

		public Action onDeath(WorldEvent cause);
		}


The contest will be run on a typical LinkedIn developer machine with AIs paired off to go head-to-head.
Unless there are radical hardware changes in the next month, this will be an 8-core Xeon E5620 with 64 GB of RAM.

Each match will be held as a series of 10 games, with the individual games being played duplicate style.
i.e. each player will play one game starting at each of the two nest locations.
Collected food will be summed to determine the winner. Wins will be worth 3 points, losses 0, and ties 1.

The contestants need to provide their code to in a build-able form and the code should be able to run on a JVM.
(Any JVM based language is fine). The competition machine is currently running Java 1.6.0_21, although that is subject to upgrade.

Due to time constraints, ants must make their moves quickly.
Any ant taking more than 500ms to submit a move will be removed from the game,
and any side taking more than 500ms to make moves in a given turn (across all ants) will have one of its ants removed from the game.
Note that those 500ms times include external pauses such as OS scheduling, garbage collection, and so on,
so you are advised to be conservative with any time estimates you may do.

Since a significant part of the challenge is to coordinate the ants with minimal overhead,
ants may not contain any mechanisms to share state across instances.
This includes static variables, threadlocals, file IO, and so on.

## Rules:

- You must provide the source code in build-able form.
- Your code must not share any state between ant instances
- Your client must be able to run on a JVM
- You may work in a team of up to three. Cross-organizational teams are encouraged\!
- All submissions must include source code, and must include an identifying name (either of the team or the individual engineer) in their file name to make running and judging faster.
- Your client should not be taking up cycles when it is not your move. (Please no background threads). Please also be judicious in your usage of memory.
- Entries (and revisions to entries) will be accepted until Monday, October 24th at 10am. Each team will be judged using their last correct submission. Status updates will be released each Friday evening.
- Much research has been done on algorithms to solve similar problems. While you are certainly encouraged to do research and read papers, all implementation work should be your own (though it may be an implementation of a published algorithm). You should not rely on a third-party library to do the heavy lifting for you.
- Your program should run entirely on the provided development machine (so, no calls to external resources like a Hadoop cluster \:) ).
- Submitting an invalid move will be considered a pass

FAQ:
----

- Is local memory allowed? Is it guaranteed to be secure, or I should encrypt it if I am THAT paranoid?
 - We are assuming you will be using local memory.
However even with the burly dev boxes we have it would be nice if you could limit your memory usage to under 8 Gig.
We are not guaranteeing the memory to be secure.
However you should not be trying to access either the server or the other players memory.
We have exposed APIs for everything you should need.
Please do not try to do end runs to get other information.
If you find there is data you think you should have access to please ask me about it.
If I agree I will extend the API appropriately for everyone.
- Can we spin up worker threads during our turn provided we stop them before returning from our implementation of Ant.act(...)?
 - No. Please do not use worker threads in your ant implementations
- How do we run the program/game?
 - There is a sample shell script in source.tar called game.sh.
This shell script shows how to run the basic game.
To run your own game you need to change the class for either (or both) the first/second player.
This will run the sample client against itself.
You can also run 'java org.linkedin.contest.ants.server.AntServer \-h' and it will give you a basic usage message.
- Can I get an idea of how I am doing?
 - We are working on an automated test server to allow contestants to see how their entries are doing. More on that as it develops.
- Is there a provided way of determining if an ant/nest belongs to me or my opponent?
 - Not explicitly. You are free to talk to other ants to see if they are on your side, though. You are also free to build ants that will attempt to impersonate an opponent's ants for fun and/or profit.
- Can ants enter or take food from nests, including their opponent's nest?
 - Yes. Nests with food act essentially like any other pile of food in the world, and can be freely taken from using the GetFood action
- To move food, does an ant need to repeatedly pick up and drop the food?
 - No. Once an ant is carrying a unit of food (by executing the GetFood action), the food will move with the ant until it executes a DropFood action or is killed
- How do actions work? Are all actions simultaneous, or discrete turns? Will my ants act as a group?
 - Each ant acts on a discrete turn, and so sees the world exactly as it will be when its action takes effect. Ants in non-interacting regions may act concurrently, but will always appear (to the ants) to act serially.
 - Order is random and interleaved (so, your opponent may move between two of your ants), but stable (so, if ant A acts before ant B, ant A will always act before ant B for that game)
- Can communication through Write and Say be intercepted by opposing ants?
 - Yes. Things written through Write are visible to all ants, and if the set of directions given in Say includes the direction of an opposing ant, they will receive the message as well.
- How does the combat system work?
 - Right now, it works roughly on distance from nest first, followed by number of ants in the defending square, and in general makes it easier to defend rather than attack (except relatively close to a nest). We are currently conducting some combat tests, though, to see if that gives the kind of behaviour we want to see, and may be tweaking it (particularly in regards to how the number of ants in the attacking/defending squares affect the outcome). However, you can generally expect that ants fighting near their nest will be much more likely to win than ants fighting near their opponent's nest.

Dump File Format
----------------
Ants can dump a game to file, by using the `-r {filename}` command-line option

The beginning of the file contains information on the initial state of the arena.
All open square coordinates are listed, and squares that have food or nests will be noted. For example:

		(64,454)
		(60,392) Nest
		(64,453) Food: 2

Blocked squares are not listed.

After the last row, ant actions begin, one per line.
All actions are in the form `{id}: {Action}`, where each ant is given an ID from 1 to 100.
While they are placed in team-id order, they are randomized once they start.

Examples:

		3: Starts at 60,392
		2: Writes 8589934593
		10: Moves north
		52: Gets food from north
		21: Drops food here
		42: *dies*
