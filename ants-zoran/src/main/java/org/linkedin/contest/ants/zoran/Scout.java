package org.linkedin.contest.ants.zoran;

import org.linkedin.contest.ants.api.*;

public class Scout extends Role {

	protected int border;
	protected int metBorder;
	private int targetX, targetY;
	private ZSquare direction;
	private GoToNest opNest;
	private DumpFoodStockInfo opDump;

	Scout(CommonAnt ant, ZSquare square) {
		super(ant);
		direction = square;
		targetX = Constants.BOARD_SIZE + direction.deltaX * Constants.BOARD_SIZE * 10;
		targetY = Constants.BOARD_SIZE + direction.deltaY * Constants.BOARD_SIZE * 10;
		border = 0;
		metBorder = 0;
		opNest = new GoToNest(this);
		opDump = new DumpFoodStockInfo(this);
	}

	// Extra state info for this specific role, redefine to get some extra relevant output in toString()
	@Override
	protected String stateInfo() {
		String s;
		if (opDump.isActive()) s = "dumping food info";
		else if (opNest.isActive()) s = "returning to nest";
		else if (!hasBorder()) s ="looking for border";
		else if (ant.here.isNest()) s = "back on nest";
		else if (ant.isNextToNest()) s = "next to nest";
		else if (hasBorder()) s ="";
		else s = "??";
		return String.format("%s %s", direction.dir.name(), s);
	}

	private boolean hasBorder() {
		return metBorder>10;
	}

	@Override
	Action effectiveAct() {
		ant.sniffFood();
		if (ant.id == 1) {
			if (turn == 1) return ant.sayInAllDirections("turn 1");
			else if (turn == 3) return ant.sayInAllDirections("turn 3");
			else if (turn == 5) return ant.sayInAllDirections("turn 5");
			else if (turn == 7) return ant.sayInAllDirections("turn 7");
		}
		if (opDump.isActive()) {
			// Dumping food info
			Action act =opDump.act();
			if (act == null) {
				ant.setRole(new Guard(ant));
				return new Pass();
			}
			return act;
		} else if (opNest.isActive()) {
			// Returning to nest
			if ((ant.x == direction.deltaX && ant.y == direction.deltaY) || ant.here.isNest()) {
				opNest.activate(false);
				// We have arrived back right next to the nest where we started from: write down the boundary found
				if (ant.here.scent.isBoundary()) {
					if (!ant.here.isNest()) return new Move(ant.square(-direction.deltaX, -direction.deltaY).dir);
					opDump.activate(true);
					return opDump.act();
				}
				Scent s = new Scent();
				s.setBoundary(this);
				ant.dump(String.format("arrived next to nest, scent: %s", s.toString()));
				return new Write(s.getValue());
			}
			Action act =opNest.act();
			assert act != null;
			return act;
		} else if (!hasBorder()) {
			if (ant.here.isNest()) {
				// Starting
				return new Move(direction.dir);
			}
			// Scanning for border
			int b, bx, by;
			if (direction.deltaX == 0) {
				b = ant.y;
				bx = targetX + (int)((ant.x - targetX) * 0.8);
				by = targetY;
			} else {
				b = ant.x;
				bx = targetX;
				by = targetY + (int)((ant.y - targetY) * 0.8);
			}
			if (border < b) {
				border = b;
				metBorder = 0;
			} else if (border == b) {
				metBorder++;
			}
			if (hasBorder() || turn >= 1500) {
				// We found the border: return next to nest and write it down for the other ants to see
				ant.dump(String.format("found border: %d", border));
				// Return to square next to the nest where we started scouting
				opNest.activate(true);
				metBorder = 100;
				return opNest.act();
			}
			ZSquare s = ant.bestSquareForTarget(bx, by);
			return new Move(s.dir);
		} else if (ant.here.isNest()) {
			// All done
			ant.setRole(new Guard(ant));
			return new Pass();
		} else if (ant.isNextToNest()) {
			return new Move(ant.square(-ant.here.x, -ant.here.y).dir);
		}
		return null;		// Confused
	}

}
