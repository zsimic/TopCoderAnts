package org.linkedin.contest.ants.zoran;

//import org.linkedin.contest.ants.api.*;

public class Scent {

	public Long rawValue;		// Value as found on the cell (could be null or written by an enemy ant)
	public boolean stinky;		// Was the value read invalid (probably written by an enemy ant)
	public int turn;			// Turn when value was written
	public int nature;			// Nature of this scent
	public int a;				// Optional 1st argument to 'nature'
	public int b;				// Optional 2nd argument to 'nature'
	public int c;				// Optional 3rd argument to 'nature'
	public int d;				// Optional 4th argument to 'nature'

	public Scent() {
		update(null);
	}

	public Scent(Long val) {
		update(val);
	}

//--  Properties, queries
//-----------------------

	@Override
	public String toString() {
		if (stinky) return String.format("stinky %x", rawValue);
		else if (nature == Constants.NATURE_BOUNDARY) return String.format("boundary id=%d border=%d %x", a, b, rawValue);
		else if (nature == Constants.NATURE_OBSTACLE) return String.format("obstacle %x", rawValue);
		else if (nature == Constants.NATURE_FOOD_COORDINATES) return String.format("foodxy x=%d y=%d a=%d %x", a, b, c, rawValue);
		return String.format("nature %d: %d %d %d %d %x", nature, a, b, c, d, rawValue);
	}

	// Does this scent contain no meaningful info?
	public boolean isEmpty() {
		return stinky || rawValue == null;
	}

	// Is this a scent indicating boundary of the board?
	public boolean isBoundary() {
		return nature == Constants.NATURE_BOUNDARY;
	}

	// Does this scent contain an order to scan a region for food?
	public boolean isScan() {
		return nature == Constants.NATURE_SCAN;
	}

	// Does this scent contain an order to go to a certain cell?
	public boolean isGoTo() {
		return nature == Constants.NATURE_GOTO;
	}

	// Does this scent indicate that the cell where it's on should be considered as a non-passable obstacle?
	public boolean isObstacle() {
		return nature == Constants.NATURE_OBSTACLE;
	}

	// Scent providing food coordinates that a scout found
	public boolean isFoodCoordinates() {
		return nature == Constants.NATURE_FOOD_COORDINATES;
	}

	// Scent giving an order to a gatherer to go fetch food at coordinates 'xa', 'xb'
	public boolean isFetchFood() {
		return nature == Constants.NATURE_FETCH_FOOD;
	}

//--  Basic operations
//--------------------

	// Store a scent indicating where the boundary of the board is (info found by scouts)
	public void setBoundary(Scout scout) {
		this.nature = Constants.NATURE_BOUNDARY;
		this.turn = scout.ant.turn;
		this.a = scout.ant.id;
		this.b = scout.border;
	}

	// Store a scent to order a gatherer ant to scan a region
	public void setScan(int turn, int x0, int x1, int y0, int y1) {
		this.nature = Constants.NATURE_SCAN;
		this.turn = turn;
		this.a = x0;
		this.b = x1;
		this.c = y0;
		this.d = y1;
	}

	// Scent issuing a "go to cell" order (for soldiers)
	public void setGoTo(int turn, int x, int y) {
		this.nature = Constants.NATURE_GOTO;
		this.turn = turn;
		this.a = x;
		this.b = y;
	}

	// Scent indicating that current cell should be considered an non-passable obstacle
	public void setObstacle(int turn) {
		this.nature = Constants.NATURE_OBSTACLE;
		this.turn = turn;
	}

	public void setFoodCoordinates(FoodCoordinates c) {
		this.nature = Constants.NATURE_FOOD_COORDINATES;
		this.turn = 0;
		this.a = c.x;
		this.b = c.y;
		this.c = c.amount;
	}

	public void setFetchFood(FoodCoordinates c) {
		this.nature = Constants.NATURE_FETCH_FOOD;
		this.turn = 0;
		this.a = c.x;
		this.b = c.y;
		this.c = c.amount;
	}

//-- Implementation
//-----------------

	// Update this scent with 'value' found on board
	public void update(Long value) {
		rawValue = value;
		if (isValidScent(value)) {
			stinky = false;
			turn = (int)(value & Constants.TURN_MASK) << (Constants.turnNeededBitCount - Constants.turnBitCount);
			nature = (int)((value & Constants.NATURE_MASK) >>> Constants.natureBitOffset);
			a = (int)((value & Constants.A_MASK) >>> Constants.aBitOffset);
			b = (int)((value & Constants.B_MASK) >>> Constants.bBitOffset);
			c = (int)((value & Constants.C_MASK) >>> Constants.cBitOffset);
			d = (int)((value & Constants.D_MASK) >>> Constants.dBitOffset);
			assert nature > 0 && turn >= 0;
			assert a >= 0 && b >= 0 && c >= 0 && d >= 0; 
		} else {
			stinky = value != null;
			turn = 0;
			nature = 0;
			a = 0;
			b = 0;
			c = 0;
			d = 0;
		}
	}

	// Encoded scent value
	public Long getValue() {
		assert nature > 0 && turn >= 0;
		assert a >= 0 && b >= 0 && c >= 0 && d >= 0; 
		long v = ((long)d << Constants.dBitOffset)
				| ((long)c << Constants.cBitOffset)
				| ((long)b << Constants.bBitOffset)
				| ((long)a << Constants.aBitOffset)
				| ((long)nature << Constants.natureBitOffset)
				| ((long)turn >>> (Constants.turnNeededBitCount - Constants.turnBitCount));
		long n = Long.bitCount(v);
		v |= (n << Constants.checksumBitOffset);
		return v;
	}

	// Is scent with given 'value' a valid scent (produced by our own ants)?
	private static boolean isValidScent(Long value) {
		if (value==null) return false;
		long chk = (value & Constants.CHECKSUM_MASK) >>> Constants.valueBitCount;
		int n = Long.bitCount(value & Constants.VALUE_MASK);
		n = n & 0xf;
		return n == chk;
	}

}
