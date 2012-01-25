package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

/**
 * 'long' ants can leave on board as scent,
 *  organized in a specific way so it can carry various types of info 
 *  (with a checksum to verify it was left by an ant from our team)
 */
public class Scent {

	public Long rawValue;		// Value as found on the cell (could be null or written by an enemy ant)
	public boolean stinky;		// Was the value read invalid (probably written by an enemy ant)
	public int a;				// Optional 1st argument to 'nature'
	public int b;				// Optional 2nd argument to 'nature'
	public int c;				// Optional 3rd argument to 'nature'

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
		return String.format("%d %d %d %x", a, b, c, rawValue);
	}

	// Does this scent contain no meaningful info?
	public boolean isEmpty() {
		return stinky || rawValue == null;
	}

	// Update this scent with 'value' found on board
	public void update(Long value) {
		rawValue = value;
		if (isValidScent(value)) {
			long v = value.longValue();
			stinky = false;
			a = (int)(v & Constants.A_MASK);
			b = (int)((v & Constants.B_MASK) >>> Constants.bBitOffset);
			c = (int)((v & Constants.C_MASK) >>> Constants.cBitOffset);
			assert a >= 0 && b >= 0 && c >= 0; 
		} else {
			stinky = value != null;
			a = 0;
			b = 0;
			c = 0;
		}
	}

	// Encoded scent value
	public Long getValue() {
		assert a >= 0 && b >= 0 && c >= 0; 
		long v = ((long)c << Constants.cBitOffset)
				| ((long)b << Constants.bBitOffset)
				| a;
		long n = Long.bitCount(v);
		v |= (n << Constants.checksumBitOffset);
		return v;
	}

	// Is scent with given 'value' a valid scent (produced by our own ants)?
	private static boolean isValidScent(Long value) {
		if (value == null || value.longValue() == 0) return false;
		long chk = (value.longValue() & Constants.CHECKSUM_MASK) >>> Constants.checksumBitOffset;
		int n = Long.bitCount(value.longValue() & Constants.VALUE_MASK);
		n = n & 0xff;
		return n == chk;
	}

}
