package org.linkedin.contest.ants.impl;

import org.linkedin.contest.ants.api.*;

public class Scout extends Role {

	protected int border;
	protected int metBorder;
	private int targetX, targetY;
	private ZSquare direction;
	private GoToNest opNest;
	private DumpFoodStockInfo opDump;

	Scout(ZoranAnt ant, ZSquare square) {
		super(ant);
		direction = square;
		targetX = direction.deltaX * Constants.BOARD_SIZE * 10;
		targetY = direction.deltaY * Constants.BOARD_SIZE * 10;
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
		else s = "??";
		return String.format("%s %s", direction.dir.name(), s);
	}

	private boolean hasBorder() {
		return metBorder>10 || turn > 1500;
	}

	@Override
	Action effectiveAct() {
		ant.sniffFood();
		if (opDump.isActive()) {
			// Dumping food info
			return opDump.act();
		} else if (opNest.isActive()) {
			// Returning to nest
			if ((ant.x == direction.deltaX && ant.y == direction.deltaY) || ant.here.isNest()) {
				opNest.activate(false);
				// We have arrived back right next to the nest where we started from: write down the boundary found
				if (ant.here.scent.isBoundary()) {
					if (!ant.here.isNest()) return new Move(ant.square(-direction.deltaX, -direction.deltaY).dir);
					opDump.activate(true);
					return opDump.act();
				} else {
					Scent s = new Scent();
					s.setBoundary(this);
					ant.dump(String.format("arrived next to nest, scent: %x", s.getValue()));
					return new Write(s.getValue());
				}
			}
			return opNest.act();
		} else if (!hasBorder()) {
			if (ant.here.isNest()) {
				// Starting
				return new Move(direction.dir);
			}
			// Scanning for border
			int b, bx, by;
			if (direction.deltaX == 0) {
				b = Math.abs(ant.y);
				bx = (int)(ant.x * 0.8);
				by = targetY;
			} else {
				b = Math.abs(ant.x);
				bx = targetX;
				by = (int)(ant.y * 0.8);
			}
			if (border < b) {
				border = b;
				metBorder = 0;
			} else if (border == b) {
				metBorder++;
			}
			if (hasBorder()) {
				// We found the border: return next to nest and write it down for the other ants to see
				ant.dump(String.format("found border: %d", border));
				// Return to square next to the nest where we started scouting
				opNest.activate(true);
				return opNest.act();
			} else {
				ZSquare s = ant.bestSquareForTarget(bx, by);
				return new Move(s.dir);
			}
		} else if (ant.here.isNest()) {
			// All done
			ant.setRole(new Guard(ant));
			return new Pass();
		}
		return null;		// Confused
	}

}
