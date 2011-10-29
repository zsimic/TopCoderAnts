/**
 * Zoran's ant implementation
 */
package org.linkedin.contest.ants.zoran;

//import org.linkedin.contest.ants.api.*;

/**
 * @author Zoran Simic
 *
 */
public class ZoranAnt extends CommonAnt {

	@Override
	public void initializeState() {
//		testScent();
		if (id<=Constants.totalSlices) setRole(new ScoutSection(this, id - 1));	// 32 scouts, they become gatherers after they're done finding the game board limits
		else if (id<=Constants.totalSlices + 8) setRole(new Guard(this));		// 8 guards, they stay on nest and do nothing (just gather food immediately next to nest)
		else setRole (new Soldier(this));										// the rest are soldiers
	}

	private static void testScent(int turn, int x0, int y0, int x1, int y1) {
		Scent s = new Scent();
		s.setScan(turn, x0, y0, x1, y1);
		Long v1 = s.getValue();
//		System.out.printf("v=%x\n", v);
		s = new Scent();
		s.update(v1);
		Long v2 = s.getValue();
		if (v1.longValue() != v2.longValue()) System.out.printf("Mismatch v1=%x v2=%x\n", v1.longValue(), v2.longValue());
		if (x0 != s.a || y0 != s.b || x1 != s.c || y1 != s.d) System.out.printf("Mismatch x=[%d-%d] y=[%d-%d] vs %s\n", x0, y0, x1, y1, s.toString());
		if (s.nature != 5) System.out.printf("Mismatch nature: %d %d\n", 5, s.nature);
	}

	@SuppressWarnings(value = { "unused" })
	private static void testScent() {
		testScent(0, 0, 0, 0, 0);
		testScent(15, 1, 1, 1, 1);
		testScent(28, 104, 680, 500, 700);
		testScent(40000, 1020, 1020, 1020, 1020);
	}

}
