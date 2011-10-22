package org.linkedin.contest.ants.impl;

public interface DefaultAntConfig
{

	// event responses
	public static final Double AVENGE = 100000d;
	public static final Double RETALIATE = 10000d;
	public static final Double ATTACK = 1000d;
	public static final Double RESPOND_NOTHING = 500d;
	
	// environment responses
	public static final Double DEPOSIT_FOOD = Double.MAX_VALUE;
	public static final Double PICKUP_FOOD = 5000d;
	public static final Double STACK_OR_ATTACK = 250d;
	public static final Double SPEAK_TO_UNKNOWN_ANTS = 100d;
	public static final Double SPEAK_TO_UNKNOWN_ANTS_CHANCE = 0.4d;
	public static final Double MOVE = 70d;
	public static final Double OVERWRITE_WRITING = 50d;
	
	
	// ant generated messages
	public static final String AVENGE_ME = "Avenge meeeeeeee!!";
	public static final String CAN_YOU_HELP_ME = "Can you help me?";
	public static final String NO_I_CANT = "No I can't";
	
	// ant generated writing
	public static final Long GARBAGE = 12345567890L;
}
