/**
 * Zoran's ant implementation
 */
package org.linkedin.contest.ants.impl;

//import org.linkedin.contest.ants.api.*;

/**
 * @author Zoran Simic
 *
 */
public class ZoranAnt extends CommonAnt {

	public void initializeState() {
//		testScent();
		if (id<=4) setRole (new Scout(this, square (2 * (id % 4))));	// 4 scouts, one in each direction, they become guards after they're done finding the game board limits
		else if (id<=10) setRole (new Guard(this));						// 6 + 4 guards
		else if (id==11) setRole (new Manager(this));					// 1 manager
		else if (id<=20) setRole (new Gatherer(this));					// 9 gatherers
		else setRole (new Soldier(this));								// 30 soldiers
		progressDump("created");
	}

	private void testScent(int x, int y, int amt) {
		Scent s = new Scent();
		s.setFoodCoordinates(new FoodCoordinates(x, y, amt));
		Long v1 = s.getValue();
//		System.out.printf("v=%x\n", v);
		s = new Scent();
		s.update(v1);
		Long v2 = s.getValue();
		if (v1.longValue() != v2.longValue()) System.out.printf("Mismatch v1=%x v2=%x\n", v1.longValue(), v2.longValue());
		if (x != s.xa() || y != s.xb() || amt != s.c) System.out.printf("Mismatch xya=[%d %d %d] vs [%d %d %d]\n", x, y, amt, s.xa(), s.xb(), s.c);
		if (s.nature != 5) System.out.printf("Mismatch nature: %d %d\n", 5, s.nature);
	}

	@SuppressWarnings(value = { "unused" })
	private void testScent() {
		testScent(10,-25, 6);
		testScent(-57,510, 1);
		testScent(0, 0, 0);
	}

}
