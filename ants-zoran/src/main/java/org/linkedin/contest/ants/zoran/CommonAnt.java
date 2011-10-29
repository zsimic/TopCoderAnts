package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;
import java.util.*;

abstract class CommonAnt implements Ant {

	protected int id;								// Ant's id (decides ant's initial role)
	protected int turn;								// Turn being played, acts as an internal timer
	protected int x;								// Current X coordinate, nest is on Constants.BOARD_SIZE by convention, to make sure all coordinates are > 0
	protected int y;								// Current Y coordinate, relative to nest
	protected int receivedBoardInfos;				// Number of times this ant received board info
	protected boolean receivalInProgress;			// Are we currently receiving a multi-part message
	protected boolean hasFood;						// Is ant currently carrying food?
	protected ZSquare here,northeast,east,southeast,south,southwest,west,northwest,north;
	protected List<ZSquare> neighbors;				// All neighboring cells (all except 'here')
	protected List<ZSquare> cells;					// All cells (including 'here')
	protected Board board;							// Board as discovered so far
	protected Role role;							// Scout, Guard, Gatherer, Soldier
	private HashMap<Integer, String> pendingTransmissions;		// Pending transmissions from other ants

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
		receivedBoardInfos = 0;
		receivalInProgress = false;
		hasFood = false;
		board = new Board(this);
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
		pendingTransmissions = new HashMap<Integer, String>();
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
			board.updateCell(here);
			id = intValueOnNest() + 1;
			initializeState();
			assert role != null;
			return new Write(new Long(id));
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
		for (WorldEvent event : events) {
			receiveEvent(event.getEvent());
		}
		if (here.scent.stinky) act = new Write(null);		// Erase opponent's writing
		if (act == null) act = role.act();
		if (act == null) act = new Pass();
		if (act instanceof Move) {
			ZSquare square = square(act);
			assert square.isPassable();
			x += square.deltaX;
			y += square.deltaY;
			Logger.trace(this, "move " + square.dir.name());
		} else if (act instanceof GetFood) {
			assert !hasFood;
			hasFood = true;
			Logger.trace(this, "takes food");
		} else if (act instanceof DropFood) {
			assert hasFood;
			hasFood = false;
			Logger.trace(this, "drops food");
		} else if (act instanceof Write) {
			Scent s = new Scent(act);
			Logger.trace(this, String.format("writing value: %s", s.toString()));
		} else if (act instanceof Say) {
			Logger.trace(this, String.format("Say: '%s'", act.toString()));
		} else if (act instanceof Pass) {
			// Do nothing
		} else {
			Logger.trace(this, className(act));
		}
		elapsedTimeMillis = System.currentTimeMillis() - elapsedTimeMillis;
		Logger.logRunTime(this, elapsedTimeMillis);
		return act;
	}

	// Receive event sent by another ant, we communicate board info only...
	private void receiveEvent(String event) {
		String message = event;
		assert message.length() > 15;
		if (message.startsWith(Constants.AN_ANT_SAYS)) {
			Logger.trace(this, "received evt: " + message);
			message = message.substring(Constants.AN_ANT_SAYS.length());
			if (message.length() < 10) return;
			char messageType = message.charAt(0);
			int antId = Constants.decodedCharInt(message.charAt(1));
			int page = Constants.decodedCharInt(message.charAt(2));
			int totalPages = Constants.decodedCharInt(message.charAt(3));
			if (antId <= 0 || page <= 0 || totalPages <= 0 || antId > 50 || page > totalPages) return;		// Not sent by us
			if (messageType == Constants.messageBoard) {
				if (!here.isNest() && !isNextToNest()) return;
				message = message.substring(4);
				String prev = pendingTransmissions.get(antId);
				if (prev != null) {
					message = message + prev;
				} else if (page != totalPages) {
					receivalInProgress = false;
					return;			// We missed the first part of the message
				} else {
					receivalInProgress = true;
				}
				if (page == 1) {
					receivalInProgress = false;
					if (prev != null) pendingTransmissions.remove(antId);
					interpretReceivedBoardInfo(message);
				} else {
					pendingTransmissions.put(antId, message);
				}
			}
		}
	}

	// Interpret received string (containing board findings)
	private void interpretReceivedBoardInfo(String received) {
		ArrayList<String> list = new ArrayList<String>();
		String message = TransmitMessage.uncompressed(this, received);
		String[] lines = message.split("\n");
		int i = 0;
		for (; i < lines.length; i++) {
			String line = lines[i];
			list.add(line);
		}
		board.setFromLines(list);
		receivedBoardInfos++;
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
//		return sayInAllDirections("Man down " + role.toString());
		if (cause==null) Logger.error(this, String.format("died after trying to attack"));
		else Logger.error(this, String.format("died %s",cause.toString()));
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
			if (!s.isNest() && !s.isAroundNest() && s != excluded && s.hasFood()) return s;
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
			if (s.getNumberOfAnts() == 0) {
				double d = Constants.normalDistance(target.x - s.x, target.y - s.y);
				if (d > dist) best = s;
			}
		}
		return best;
	}

	// Square target by given action
	protected ZSquare square(Action act) {
		if (act instanceof Move) {
			return square(((Move)act).getDirection());
		}
		return here;
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
		return squareTo(Constants.decodedX(key), Constants.decodedY(key));
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

//--  Basic operations
//--------------------

	// Set 'role'
	protected void setRole(Role role) {
		assert role != null;
		this.role = role;
		Logger.trace(this, "changed role");
	}

//--  Ant implementation
//----------------------

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

	public final static String className(Object obj) {
		String s = obj.getClass().getName();
		int i = s.lastIndexOf('.');
		if (i>0) return s.substring(i+1);
		return s;
	}

}
