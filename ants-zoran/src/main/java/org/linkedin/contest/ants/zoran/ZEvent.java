package org.linkedin.contest.ants.zoran;

public class ZEvent {

	public ZEventType eventType;	// Event type
	public ZSquare square;			// Square from which the event was received
	public String role;

	public static final String MAN_DOWN = "for the win!!";

	ZEvent(String s) {
		int i = 0;
		if (s.startsWith(MAN_DOWN)) {
			eventType = ZEventType.manDown;
			i = MAN_DOWN.length() + 1;
		} else {
			eventType = ZEventType.giberish;
		}
		if (i>0) {
			role = s.substring(i);
		} else {
			role = null;
		}
	}

	ZEvent(ZEventType evt) {
		this.eventType = evt;
	}

	@Override
	public String toString() {
		if (eventType==ZEventType.manDown) return MAN_DOWN;
		return "";
	}

}
