package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

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

	public Scent(Action act) {
		if (act instanceof Write) {
			update(((Write)act).getWriting());
		}
	}

//--  Properties, queries
//-----------------------

	@Override
	public String toString() {
		if (stinky) return String.format("stinky %x", rawValue);
		else if (nature == Constants.NATURE_SCAN) return String.format("scan x=%d-%d y=%d-%d [%x]", a, b, c, d, rawValue);
		return String.format("nature %d: %d %d %d %d %x", nature, a, b, c, d, rawValue);
	}

	// Does this scent contain no meaningful info?
	public boolean isEmpty() {
		return stinky || rawValue == null;
	}

	// Does this scent contain an order to scan a region for food?
	public boolean isScan() {
		return nature == Constants.NATURE_SCAN;
	}

//--  Basic operations
//--------------------

	// Store a scent to order a gatherer ant to scan a region
	public void setScan(int turn, int x0, int x1, int y0, int y1) {
		this.nature = Constants.NATURE_SCAN;
		this.turn = turn;
		this.a = x0;
		this.b = x1;
		this.c = y0;
		this.d = y1;
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
		if (value==null || value == 0) return false;
		long chk = (value & Constants.CHECKSUM_MASK) >>> Constants.valueBitCount;
		int n = Long.bitCount(value & Constants.VALUE_MASK);
		n = n & 0xf;
		return n == chk;
	}

}
