package org.linkedin.contest.ants.zoran;

public class Constants {

	public static final int ANTS_COUNT = 50;
	public static final int BOARD_SIZE = 512;									// Board size
	public static final double BOARD_MAX_DISTANCE = Math.sqrt(2)*BOARD_SIZE;	// Max distance on board from nest

	public final static RotationCoordinates rotationCoordinates(int slice, int totalSlices) {
		int i = slice % totalSlices;
		double cosf = Math.cos(2.0*i/totalSlices*Math.PI);
		double sinf = Math.sin(2.0*i/totalSlices*Math.PI);
		RotationCoordinates rc = new RotationCoordinates(cosf, sinf);
		return rc;
	}

	// Encoding/decoding cell coordinates in one Integer
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

	// Describing a cell (3 possible states, encoded on 2 bits in class Board)
	public static final byte STATE_UNKNOWN = 0;
	public static final byte STATE_PASSABLE = 1;
	public static final byte STATE_OBSTACLE = 2;

	// Masks, bit offsets and constants for the Scent class
	public final static long A_MASK        = 0x000000000000ffffL;				// 16 bits holding 1st argument
	public final static long B_MASK        = 0x00000000ffff0000L;				// 16 bits holding 2nd argument
	public final static long C_MASK        = 0x0000ffff00000000L;				// 16 bits holding 3rd argument
	public final static long CHECKSUM_MASK = 0xff00000000000000L;				// 8 highest bits holding the checksum to validate current scent
	public final static long VALUE_MASK    = A_MASK | B_MASK | C_MASK;			// non-checksum bits

	public final static int argBitCount = Long.bitCount(A_MASK);				// Number of bits in an argument
	public final static int checksumBitCount = Long.bitCount(CHECKSUM_MASK);	// Number of bits in the 'checksum' part of stored scent
	public final static int valueBitCount = Long.bitCount(VALUE_MASK);			// Number of bits in the non-checksum part of a stored value
	public final static int aBitOffset = 0;										// Offset of argument 'a' in the scent
	public final static int bBitOffset = aBitOffset + argBitCount;				// Offset of argument 'b' in the scent
	public final static int cBitOffset = bBitOffset + argBitCount;				// Offset of argument 'c' in the scent
	public final static int checksumBitOffset = 64 - checksumBitCount;

	public static boolean isNumber(String s) {
		for (int i = s.length() - 1; i >= 0; i--) {
			if (!Character.isDigit(s.charAt(i))) return false;
		}
		return s.length() > 0;
	}

	public final static String className(Object obj) {
		String s = obj.getClass().getName();
		int i = s.lastIndexOf('.');
		if (i>0) return s.substring(i+1);
		return s;
	}

}
