package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;
import java.util.*;

/**
 * Generic ant management + some utility and logging functions
 * Ant's actual behavior is determined by ant's 'role' object, not this object
 */
abstract class CommonAnt implements Ant {

	protected int id;								// Ant's id (decides ant's initial role)
	protected int turn;								// Turn being played, acts as an internal timer
	protected int x;								// Current X coordinate, nest is on Constants.BOARD_SIZE by convention, to make sure all coordinates are > 0
	protected int y;								// Current Y coordinate, relative to nest
	protected boolean hasFood;						// Is ant currently carrying food?
	protected ZSquare here,northeast,east,southeast,south,southwest,west,northwest,north;
	protected List<ZSquare> neighbors;				// All neighboring cells (all except 'here')
	protected List<ZSquare> cells;					// All cells (including 'here')
	protected Board board;							// Board as discovered so far
	protected Role role;							// Scout, Guard, Gatherer, Soldier
	protected Trail trail;							// Trail leading back to nest

	@Override
	public String toString() {
		String rs = role == null ? "-no role-" : role.toString();
		return String.format("%d %d xy=[%d,%d] bs=[%d,%d,%d] %s", turn, id, x, y, board.sizeX(), board.sizeY(), board.knownCells, rs);
	}

    /**
     * init() will be called on each ant at creation time.
     * This will be before the first move of the match
     *
     */
	public void init() {
		id = turn = 0;
		x = y = Constants.BOARD_SIZE;
		hasFood = false;
		board = new Board(this);
		trail = new Trail();
		north = new ZSquare(this, Direction.north);
		northeast = new ZSquare(this, Direction.northeast);
		east = new ZSquare(this, Direction.east);
		southeast = new ZSquare(this, Direction.southeast);
		south = new ZSquare(this, Direction.south);
		southwest = new ZSquare(this, Direction.southwest);
		west = new ZSquare(this, Direction.west);
		northwest = new ZSquare(this, Direction.northwest);
		here = new ZSquare(this, Direction.here);
		neighbors = new ArrayList<ZSquare>();
		cells = new ArrayList<ZSquare>();
		neighbors.add(north);
		neighbors.add(northeast);
		neighbors.add(east);
		neighbors.add(southeast);
		neighbors.add(south);
		neighbors.add(southwest);
		neighbors.add(west);
		neighbors.add(northwest);
		cells.add(north);
		cells.add(northeast);
		cells.add(east);
		cells.add(southeast);
		cells.add(south);
		cells.add(southwest);
		cells.add(west);
		cells.add(northwest);
		cells.add(here);
		trail.add(this);
	}

    /**
     * On each turn, act will be called to allow your ant to take some action in the world.
     * @param environment a description of the 9 squares around this ant (one in each direction + the one the ant is currently on)
     * @param events A list of Strings describing actions that affected this ant in the last turn.
     * In particular, if any ants said anything or fought with this ant, they will show up here
     * @return an implementation of the Action class indicating what this ant should do
     */
	public Action act(Environment environment, List<WorldEvent> events) {
//L		long elapsedTimeMillis = System.currentTimeMillis();		// Logger.
		Action act = null;
		turn++;
		here.update(environment);
		if (role==null) {
			assert id==0;
			assert turn==1;
			assert here.isNest();
			id = here.scent.a + 1;
			Scent s = new Scent();
			s.a = id;
			initializeState();
			assert role != null;
			return new Write(s.getValue());
		}
		northeast.update(environment);
		east.update(environment);
		southeast.update(environment);
		south.update(environment);
		southwest.update(environment);
		west.update(environment);
		northwest.update(environment);
		north.update(environment);
		if (here.scent.stinky) act = new Write(null);		// Erase opponent's writing
		if (act == null) act = role.act();
		if (act == null) act = new Pass();
		if (act instanceof Move) {
			ZSquare s = square(act);
			assert s.isPassable();
			x += s.deltaX;
			y += s.deltaY;
			trail.add(this);
//L			Logger.trace(this, "move " + s.dir.name());
		} else if (act instanceof GetFood) {
			assert !hasFood;
			ZSquare s = square(act);
			assert s.isPassable() && s.hasFood();
			hasFood = true;
//L			Logger.trace(this, String.format("takes food %s %d,%d", s.dir.name(), s.x, s.y));
		} else if (act instanceof DropFood) {
			assert hasFood;
			ZSquare s = square(act);
			assert s.isPassable();
			hasFood = false;
//L			Logger.trace(this, String.format("drops food %s %d,%d", s.dir.name(), s.x, s.y));
//L		} else if (act instanceof Write) {	// Logger.
//L			Logger.trace(this, String.format("writes: %s", (new Scent(act)).toString()));
//L		} else if (act instanceof Say) {	// Logger.
//L			Logger.trace(this, String.format("Says: '%s'", act.toString()));
//L		} else if (act instanceof Pass) {	// Logger.
			// Do nothing
//L		} else {							// Logger.
//L			Logger.trace(this, "check action " + Constants.className(act));
//L			assert false;					// Logger.
		}
//L		elapsedTimeMillis = System.currentTimeMillis() - elapsedTimeMillis;		// Logger.
//L		Logger.logRunTime(this, elapsedTimeMillis);
//L		if (turn % 1000 == 0) Logger.dumpBoard(this);
		return act;
	}

	// Initialize ant's state (called on first turn, and should assign a role here, based on id)
	abstract void initializeState();

    /**
     * A special action that will be called on death, to allow your ant to do something other than disappear
     * The only valid actions here are Write, Say, or Pass, and no environment information is given.
     * Only the specific cause of death is given, or null if the death was self-inflicted (for example, a failed attack)
     * This method will be called regardless of the cause of death, unless the death is for a rules violation
     * @param cause The WorldEvent (attack) that caused the ant to die.
     * @return the action to perform
     */
	public Action onDeath(WorldEvent cause) {
//L		if (cause==null) Logger.error(this, String.format("died after trying to attack"));
//L		else Logger.error(this, String.format("died %s",cause.toString()));
//		return sayInAllDirections("Man down " + role.toString());
		return null;
	}

	// 'Say' action in all directions
	public Say sayInAllDirections(String what) {
		assert what != null;
		return new Say(String.format("From id %d, x=%d y=%d '%s'", id, x, y, what),
				Direction.northeast,Direction.east,Direction.southeast,Direction.south,
				Direction.southwest,Direction.west,Direction.northwest,Direction.north,Direction.here);
	}
	
//--  Properties, queries
//-----------------------

	protected ZSquare nest() {
		assert here.isNest() || isNextToNest();
		if (here.isNest()) return here;
		return squareTo(Constants.BOARD_SIZE, Constants.BOARD_SIZE);
	}

	// Neighboring square with food on it, if any (but only "far enough" from the nest)
	protected ZSquare squareWithFood(ZSquare excluded) {
		for (ZSquare s : cells) {
			if (!s.isNest() && !s.isNextToNest() && s != excluded && s.hasFood()) return s;
		}
		return null;
	}

	// Neighboring square with food on it, if any (but only "far enough" from the nest)
	protected ZSquare squareWithFood(ZSquare excluded1, ZSquare excluded2) {
		for (ZSquare s : cells) {
			if (!s.isNest() && !s.isNextToNest() && s != excluded1 && s != excluded2 && s.hasFood()) return s;
		}
		return null;
	}

	// Are we relatively close to the nest?
	protected boolean isAroundNest() {
		return here.isAroundNest();
	}

	protected boolean isNextToNest() {
		return here.isNextToNest();
	}

	protected ZSquare bestSquareToAvoidConflict(ZSquare target) {
		if (isAroundNest()) return null;			// Don't avoid conflict when nearby own nest
		double dist = 0;
		ZSquare best = null;
		for (ZSquare s : neighbors) {
			if (s.isPassable() && s.getNumberOfAnts() == 0) {
				double d = Constants.normalDistance(target.x - s.x, target.y - s.y);
				if (d > dist) best = s;
			}
		}
		if (best != null) assert best.isPassable();
		return best;
	}

	// Square target by given action
	protected ZSquare square(Action act) {
		Direction dir = Direction.here;
		if (act instanceof Move) dir = ((Move)act).getDirection();
		else if (act instanceof GetFood) dir = ((GetFood)act).getDirection();
		else if (act instanceof DropFood) dir = ((DropFood)act).getDirection();
		return square(dir);
	}

	// Square in given direction number (0: north, 1: northeast, ...)
	protected ZSquare square(int dir) {
		int i = dir % 9;
		switch (i) {
		case 0: return north;
		case 1: return northeast;
		case 2: return east;
		case 3: return southeast;
		case 4: return south;
		case 5: return southwest;
		case 6: return west;
		case 7: return northwest;
		default:
			return here;
		}
	}

	protected ZSquare squareTo(Integer key) {
		assert key != null;
		ZSquare s = squareTo(Constants.decodedX(key), Constants.decodedY(key));
		assert s != null;
		return s;
	}

	// Square on given x,y (which must a neighbor of current ant position)
	protected ZSquare squareTo(int nx, int ny) {
		assert Math.abs(nx - x) <= 1 && Math.abs(ny - y) <= 1;
		return squareDelta(nx - x, ny - y);
	}

	// Square with given dx, dy
	protected ZSquare squareDelta(int dx, int dy) {
		if (dx == -1) {
			if (dy == -1) return northwest;
			else if (dy == 0) return north;
			else if (dy == 1) return northeast;
		} else if (dx == 0) {
			if (dy == -1) return west;
			else if (dy == 0) return here;
			else if (dy == 1) return east;
		} else if (dx == 1) {
			if (dy == -1) return southwest;
			else if (dy == 0) return south;
			else if (dy == 1) return southeast;
		}
		return null;
	}

	// Square in given direction
	protected ZSquare square(Direction dir) {
		if (dir.deltaX<0) {
			if (dir.deltaY<0) return northwest;
			else if (dir.deltaY>0) return northeast;
			else return north;
		} else if (dir.deltaX>0) {
			if (dir.deltaY<0) return southwest;
			else if (dir.deltaY>0) return southeast;
			else return south;
		} else {
			if (dir.deltaY<0) return west;
			else if (dir.deltaY>0) return east;
			else return here;
		}
	}

	// Number of ants in all 8 neighboring squares (excluding 'here')
	protected int neighboringAnts(ZSquare excluded) {
		int n = 0;
		for (ZSquare s : neighbors) {
			if (s != excluded) n += s.getNumberOfAnts();
		}
		return n;
	}

	// Set 'role'
	protected void setRole(Role role) {
		assert role != null;
		this.role = role;
//L		Logger.trace(this, "changed role");
	}

}
