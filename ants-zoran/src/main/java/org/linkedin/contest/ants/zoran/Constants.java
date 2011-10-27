package org.linkedin.contest.ants.zoran;

public class Constants {

	public static final String AN_ANT_SAYS = "An ant says ";
	public static final int BOARD_SIZE = 512;									// Board size
	public static final double BOARD_MAX_DISTANCE = Math.sqrt(2)*BOARD_SIZE;	// Max distance on board from nest

	public final static String compressionEncoding = "US-ASCII";

	// Encoding cell coordinates
	private final static int xPointMask = 0x000fff;
	private final static int yPointMask = 0xfff000;
	private final static int pointBitOffset = Integer.bitCount(xPointMask);

	public final static int encodedXY(int x, int y) {
		return (y << pointBitOffset) | x;
	}

	public final static int decodedX(int key) {
		return key & xPointMask;
	}

	public final static int decodedY(int key) {
		return (key & yPointMask) >>> pointBitOffset;
	}

	// Distance for (0,0) to (x,y)
	protected static double normalDistance(int x, int y) {
		return Math.sqrt(x*x + y*y);
	}

	// Values in Board class
	public static final byte STATE_UNKNOWN = 0;
	public static final byte STATE_PASSABLE = 1;
	public static final byte STATE_OBSTACLE = 2;

	// Masks, bit offsets and constants for the Scent class
	public final static long TURN_MASK     = 0x00000000000000ffL;				// 16 lowest bits to hold turn number
	public final static long NATURE_MASK   = 0x0000000000000f00L;				// 4 bits holding the nature of this scent
	public final static long A_MASK        = 0x0000000000fff000L;				// 10 bits holding 1st argument
	public final static long B_MASK        = 0x0000000fff000000L;				// 10 bits holding 2nd argument
	public final static long C_MASK        = 0x0000fff000000000L;				// 10 bits holding 3rd argument
	public final static long D_MASK        = 0x0fff000000000000L;				// 10 bits holding 4th argument
	public final static long CHECKSUM_MASK = 0xf000000000000000L;				// 4 highest bits holding the checksum to validate current scent
	public final static long VALUE_MASK    = CHECKSUM_MASK ^ -1L;				// non-checksum bits

	public final static int turnBitCount = Long.bitCount(TURN_MASK);			// Number of bits in the 'turns' part of stored scent
	public final static int turnNeededBitCount = 17;							// Number of bits needed to represent the max 100K possible turns
	public final static int natureBitCount = Long.bitCount(NATURE_MASK);		// Number of bits in the 'nature' part of stored scent
	public final static int argBitCount = Long.bitCount(A_MASK);				// Number of bits in an argument
	public final static int checksumBitCount = Long.bitCount(CHECKSUM_MASK);	// Number of bits in the 'checksum' part of stored scent
	public final static int valueBitCount = 64-checksumBitCount;				// Number of bits in the non-checksum part of a stored value
	public final static int natureBitOffset = turnBitCount;						// Offset of the 'nature' part of stored scent
	public final static int aBitOffset = natureBitOffset + natureBitCount;		// Offset of argument 'a' in the scent
	public final static int bBitOffset = aBitOffset + argBitCount;				// Offset of argument 'b' in the scent
	public final static int cBitOffset = bBitOffset + argBitCount;				// Offset of argument 'c' in the scent
	public final static int dBitOffset = cBitOffset + argBitCount;				// Offset of argument 'd' in the scent
	public final static int checksumBitOffset = valueBitCount;

	public final static int NATURE_SCAN = 1;				// Order to scan a region for food
	public final static int NATURE_FETCH_FOOD = 2;			// Order to go fetch food at given coordinates

	public static boolean isNumber(String s) {
		for (int i = s.length() - 1; i >= 0; i--) {
			if (!Character.isDigit(s.charAt(i))) return false;
		}
		return s.length() > 0;
	}

}
