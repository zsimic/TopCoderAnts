package org.linkedin.contest.ants.zoran;

import java.util.ArrayList;
import java.util.List;

import org.linkedin.contest.ants.api.*;

abstract class CommonAnt implements Ant {

	protected int id;								// Ant's id (decides ant's initial role)
	protected int turn;								// Turn being played, acts as an internal timer
	protected int x;								// Current X coordinate, nest is on Constants.BOARD_SIZE by convention, to make sure all coordinates are > 0
	protected int y;								// Current Y coordinate, relative to nest
	protected boolean hasFood;						// Is ant currently carrying food?
	protected int x0, y0, x1, y1;					// Boundaries of the board
	protected ZSquare here,northeast,east,southeast,south,southwest,west,northwest,north;
	protected List<ZSquare> neighbors;				// All neighboring cells (all except 'here')
	protected List<ZSquare> cells;					// All cells (including 'here')
	protected Board board;							// Board as discovered so far
	protected Trail trail;							// Trail of where the ant has previously been on (used to avoid going back to cells recently visited)
	protected FoodStock foodStock;					// Coordinates of points where food was seen
	protected Path path;							// Path back to nest
	protected Role role;							// Scout, Guard, Gatherer, Soldier
	protected int needsBoundaries;					// Do we need boundaries coordinates?

	@Override
	public String toString() {
		String rs = role == null ? "" : role.toString();
		return String.format("%d %d p=%d f=%d x=%d y=%d %s", turn, id, path.size(), foodStock.size(), x, y, rs);
	}

    /**
     * init() will be called on each ant at creation time.
     * This will be before the first move of the match
     *
     */
	public void init() {
		id = turn = x0 = y0 = x1 = y1 = 0;
		x = y = Constants.BOARD_SIZE;
		hasFood = false;
		needsBoundaries = 4;
		board = new Board(this);
		trail = new Trail();
		path = new Path();
		foodStock = new FoodStock();
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
	}

    /**
     * On each turn, act will be called to allow your ant to take some action in the world.
     * @param environment a description of the 9 squares around this ant (one in each direction + the one the ant is currently on)
     * @param events A list of Strings describing actions that affected this ant in the last turn.
     * In particular, if any ants said anything or fought with this ant, they will show up here
     * @return an implementation of the Action class indicating what this ant should do
     */
	public Action act(Environment environment, List<WorldEvent> events) {
		long elapsedTimeMillis = System.currentTimeMillis();
		Action act = null;
		turn++;
		here.update(environment);
		if (role==null) {
			assert id==0;
			assert turn==1;
			assert here.isNest();
			id = intValueOnNest() + 1;
			initializeState();
			assert role != null;
			return new Write(new Long(id));
		}
		if (id==1) {
			if (turn==1000) {
				System.out.print(board.representation());
			}
		}
		northeast.update(environment);
		east.update(environment);
		southeast.update(environment);
		south.update(environment);
		southwest.update(environment);
		west.update(environment);
		northwest.update(environment);
		north.update(environment);
		board.updateCell(northeast);
		board.updateCell(east);
		board.updateCell(southeast);
		board.updateCell(south);
		board.updateCell(southwest);
		board.updateCell(west);
		board.updateCell(northwest);
		board.updateCell(north);
		if (here.isNest()) {
			path.clear();
			if (needsBoundaries > 0) {
				if (x0 == 0 && north.scent.isBoundary()) {
					x0 = north.scent.b;
					needsBoundaries--;
				}
				if (x1 == 0 && south.scent.isBoundary()) {
					x1 = south.scent.b;
					needsBoundaries--;
				}
				if (y0 == 0 && west.scent.isBoundary()) {
					y0 = west.scent.b;
					needsBoundaries--;
				}
				if (y1 == 0 && east.scent.isBoundary()) {
					y1 = east.scent.b;
					needsBoundaries--;
				}
				if (id==40 && needsBoundaries == 0) {
					dump(String.format("boundaries found: NE=%d %d SW=%d %d]", x0, y0, x1, y1));
				}
			}
		}
		if (events.size()>0) assert false;
		for (WorldEvent event : events) {
			Direction dir = event.getDirection();
			assert false;
			String eventString = event.getEvent();
			ZEvent ev = new ZEvent(eventString);
			square(dir).setEvent(ev);
			dump(String.format("event %s %s", event.getDirection(), eventString));
		}
		if (here.scent.stinky) {
			// Erase opponent's writing
			act = new Write(null);
		} else if (isOnDeadEndSquare()) {
			// Mark cell as non-passable, because it's a dead-end
			here.scent.setObstacle(turn);
			path.remove(here);
			act = new Write(here.scent.getValue());
		}
		if (act == null) act = role.act();
		if (act == null) act = new Pass();
		if (act instanceof Move) {
			ZSquare square = square(((Move)act).getDirection());
			assert square.isPassable();
			x += square.deltaX;
			y += square.deltaY;
			trail.add(square);
			path.add(square);
			progressDump("move " + square.dir.name());
		} else if (act instanceof GetFood) {
			assert !hasFood;
			hasFood = true;
			progressDump("takes food");
		} else if (act instanceof DropFood) {
			assert hasFood;
			hasFood = false;
			progressDump("drops food");
		} else if (act instanceof Write) {
			Scent s = new Scent(((Write)act).getWriting());
			progressDump(String.format("writing value: %s", s.toString()));
		} else if (act instanceof Pass) {
			// Do nothing
		} else {
			progressDump(className(act));
		}
		elapsedTimeMillis = System.currentTimeMillis() - elapsedTimeMillis;
		if (elapsedTimeMillis > 10) {
			System.out.print(String.format("Check time: %d\n", elapsedTimeMillis));
//			assert false;		// We took too long to compute!
		}
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
		return sayInAllDirections(ZEvent.MAN_DOWN + " " + role.toString());
	}

	// 'Say' action in all directions
	public Say sayInAllDirections(String what) {
		return new Say(String.format("%d %s", id, what), Direction.northeast,Direction.east,Direction.southeast,
				Direction.south,Direction.southwest,Direction.west,Direction.northwest,Direction.north,Direction.here);
	}
	
//--  Properties, queries
//-----------------------

	// Do we know the boundaries of the board yet?
	protected boolean knowsBoundaries() {
		return needsBoundaries > 0;
	}

	// Neighboring square with food on it, if any
	protected ZSquare squareWithFood(ZSquare excluded) {
		for (ZSquare s : neighbors) {
			if (s != excluded && s.hasFood()) return s;
		}
		return null;
	}

	protected boolean isNextToNest() {
		return !here.isNest() && Math.abs(x - Constants.BOARD_SIZE) <= 1 && Math.abs(y - Constants.BOARD_SIZE) <= 1;
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

	// Square with given dx, dy
	protected ZSquare square(int dx, int dy) {
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

	// Distance for (0,0) to (x,y)
	protected static double normalDistance(int x, int y) {
		return Math.sqrt(x*x + y*y);
	}

	// Best square to move to in order to get to (x,y), knowing that we're going in direction 'dir'
	protected ZSquare bestSquareForTarget(int x, int y) {
		ZSquare best = null;
		double bestDistance = 0;
		for (ZSquare s : neighbors) {
			if (s.isPassable()) {
				double penalty = trail.penalty(s);
				double d = normalDistance(s.x - x, s.y - y);
				if (penalty > 0 && !trail.isLast(s)) {
					progressDump(String.format("penalty %s: %g d=%g", s.dir.name(), penalty, d));
				}
				d += penalty;
				if (best == null || d < bestDistance) {
					bestDistance = d;
					best = s;
				}
			}
		}
		assert best != null;
		return best;
	}

//--  Basic operations
//--------------------

	// Set 'role'
	protected void setRole(Role role) {
		assert role != null;
		this.role = role;
		dump("changed role");
	}

	// Sniff for nearby food (for squares excluding nest and immediate nest neighbors)
	protected void sniffFood() {
		for (ZSquare s : cells) {
			if (s.hasFood() && !isNextToNest()) {
				foodStock.add(s.x, s.y, s.getAmountOfFood());
			}
		}
	}

//--  Ant implementation
//----------------------

	// Is 'here' a dead-end square (leads to nowhere)
	protected boolean isOnDeadEndSquare() {
		if (here.getAmountOfFood() == 0) return false;
		int obstacles = 0;
		int consecutiveObstacles = 0;
		int consecutiveFree = 0;
		int co = 0;
		int cf = 0;
		ZSquare prev = null;
		for (ZSquare s : neighbors) {
			if (!s.isPassable()) obstacles++;
			if (prev == null || prev.isPassable() == s.isPassable()) {
				if (s.isPassable()) cf++;
				else co++;
			} else {
				if (consecutiveFree < cf) consecutiveFree = cf;
				if (consecutiveObstacles < co) consecutiveObstacles = co;
				if (s.isPassable()) {
					cf = 1;
					co = 0;
				} else {
					cf = 0;
					co = 1;
				}
			}
			prev = s;
		}
		if (consecutiveFree < cf) consecutiveFree = cf;
		if (consecutiveObstacles < co) consecutiveObstacles = co;
		assert obstacles <= 7;
		switch (obstacles) {
		case 7:
			return true;
		case 6:
			return consecutiveFree == 2 || consecutiveObstacles == 6;
		case 5:
			return consecutiveFree == 3 || consecutiveObstacles == 5;
		case 4:
			return consecutiveFree == 4 || consecutiveObstacles == 4;
		default:
			return false;
		}
	}

	// We store a raw integer value on nest initially to assign roles to ants on startup
	private int intValueOnNest() {
		assert here.isNest();
		Long w = here.square.getWriting();
		if (w==null) return 0;
		if (w < Integer.MIN_VALUE || w > Integer.MAX_VALUE) {
			return 0;
		}
		return w.intValue();
	}

	protected void dump(String s) {
		System.out.printf("--> " + toString() + " " + s + "\n");
	}

	protected void progressDump(String s) {
		if (id!=1) return;
		System.out.printf(toString() + " " + s + "\n");
	}

	public final static String className(Object obj) {
		String s = obj.getClass().getName();
		int i = s.lastIndexOf('.');
		if (i>0) return s.substring(i+1);
		return s;
	}

}
