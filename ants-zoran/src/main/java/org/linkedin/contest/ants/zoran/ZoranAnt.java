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

	private static void testScent(int x, int y, int amt) {
		Scent s = new Scent();
		s.setFetchFood(new FoodCoordinates(x, y, amt));
		Long v1 = s.getValue();
//		System.out.printf("v=%x\n", v);
		s = new Scent();
		s.update(v1);
		Long v2 = s.getValue();
		if (v1.longValue() != v2.longValue()) System.out.printf("Mismatch v1=%x v2=%x\n", v1.longValue(), v2.longValue());
		if (x != s.a || y != s.b || amt != s.c) System.out.printf("Mismatch xya=[%d %d %d] vs [%d %d %d]\n", x, y, amt, s.a, s.b, s.c);
		if (s.nature != 5) System.out.printf("Mismatch nature: %d %d\n", 5, s.nature);
	}

	@SuppressWarnings(value = { "unused" })
	private static void testScent() {
		testScent(0,0, 0);
		testScent(1,0, 0);
		testScent(0,1, 0);
		testScent(0,0, 1);
		testScent(57,510, 6);
		testScent(512, 512, 512);
	}

}
