package org.linkedin.contest.ants.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.linkedin.contest.ants.api.Action;
import org.linkedin.contest.ants.api.Ant;
import org.linkedin.contest.ants.api.Direction;
import org.linkedin.contest.ants.api.DropFood;
import org.linkedin.contest.ants.api.Environment;
import org.linkedin.contest.ants.api.GetFood;
import org.linkedin.contest.ants.api.Move;
import org.linkedin.contest.ants.api.Say;
import org.linkedin.contest.ants.api.Square;
import org.linkedin.contest.ants.api.WorldEvent;
import org.linkedin.contest.ants.api.Write;

public class AntImpl implements Ant {

	// event messages
	private final String ATTACK_MESSAGE = "attack";
	private final String AN_ANT_SAYS = "An ant says ";
	

	private final MessageCodec codec = new MessageCodec();

	private boolean hasFood = false;
	
	public AntImpl()
	{
	}

	public void init() {
		//
	}

	public Action act(Environment environment,List<WorldEvent> events) {
		
		SortedMap<Double,Action> actionMap = new TreeMap<Double,Action>(); // map will result in equivalently weighted actions overwriting one another, but meh...
		
		// handle events
		handleEvents(events, actionMap);
		
		// check environment
		checkEnvironment(environment, actionMap);
		
		Action desiredAction = actionMap.get(actionMap.lastKey());
		assert(desiredAction != null) : "No action is being taken!!";

		return desiredAction;
	}

	
	private void checkEnvironment(Environment environment, Map<Double, Action> actionMap)
	{

		// check what is going on in every direction
		for ( Direction d : Direction.values() )
		{
			Square square = environment.getSquare(d);
			if ( square.isNest() ) // if the square is a nest
			{
				if ( hasFood ) // if we have food
				{
					actionMap.put(DefaultAntConfig.DEPOSIT_FOOD, new DropFood(d));
				}
			}
			else if ( square.hasFood() ) // if the square has food
			{
				if ( !hasFood ) // if we don't have food
				{
					actionMap.put(DefaultAntConfig.PICKUP_FOOD, new GetFood(d));
				}
			}
			else 
			{
				// was thinking of randomly weighting all of these actions, but the ant got lazy...
				Random random = new Random(new Date().getTime());
				
				if ( !Direction.here.equals(d) )
				{
					if ( square.hasAnts() )
					{
						Double randomD = random.nextDouble();
						if ( DefaultAntConfig.SPEAK_TO_UNKNOWN_ANTS_CHANCE > randomD )
						{
							// stack or attack!
							actionMap.put(DefaultAntConfig.STACK_OR_ATTACK, new Move(d));
						}
						else
						{
							// say something
							actionMap.put(DefaultAntConfig.SPEAK_TO_UNKNOWN_ANTS, new Say(codec.encrypt(DefaultAntConfig.CAN_YOU_HELP_ME),d));
						}
					}
					else if ( square.isPassable() )
					{
						Double randomD = random.nextDouble();
						actionMap.put(randomD*DefaultAntConfig.MOVE, new Move(d));
					}
				}
				
				if ( square.hasWriting() )
				{
					Long writing = square.getWriting();
					if ( !DefaultAntConfig.GARBAGE.equals(writing) )
					{
						actionMap.put(DefaultAntConfig.OVERWRITE_WRITING, new Write(DefaultAntConfig.GARBAGE));
					}
				}
			}
		}
	}
	
	private void handleEvents(List<WorldEvent> events, Map<Double, Action> actionMap)
	{
		// check our events
		for ( WorldEvent event : events )
		{
			// get the event type
			String eventString = event.getEvent();
			if ( eventString != null )
			{
				if ( ATTACK_MESSAGE.equals(eventString) ) // if we are being attacked
				{
					// attack back! with a certain weight
					actionMap.put(DefaultAntConfig.RETALIATE, new Move(DirectionHelper.getOppositeDirection(event.getDirection())));
				}
				else if ( eventString.startsWith(AN_ANT_SAYS) ) // if another ant is talking to us
				{
					// get what the ant is saying
					String antSaysWhat = eventString.substring(AN_ANT_SAYS.length());
					// decode the message
					String decodedMessage = codec.decrypt(antSaysWhat);
					if ( DefaultAntConfig.AVENGE_ME.equals(decodedMessage) ) // if our fellow ant has died
					{
						// avenge! with a certain weight
						Direction d = DirectionHelper.getOppositeDirection(event.getDirection());
						if ( !d.equals(Direction.here) ) // if we know the direction the ant died on (and it isn't our square), try and attack there (won't always contain an enemy ant, but hey, this ant is dumb...)
						{
							actionMap.put(DefaultAntConfig.AVENGE, new Move(d));
						}
					}
//					else if ( <some other message>.equals(decodedMessage) )
//					{
//						// do something
//					}
					else if ( DefaultAntConfig.NO_I_CANT.equals(antSaysWhat) )
					{
						// oh...
					}
					else if ( DefaultAntConfig.CAN_YOU_HELP_ME.equals(antSaysWhat) )
					{
						// I'd love to, however...
						actionMap.put(DefaultAntConfig.RESPOND_NOTHING, new Say(DefaultAntConfig.NO_I_CANT, DirectionHelper.getOppositeDirection(event.getDirection())));
					}
					else // I don't understand what they are saying
					{
						// attack! with a certain weight
						actionMap.put(DefaultAntConfig.ATTACK, new Move(DirectionHelper.getOppositeDirection(event.getDirection())));
					}
				}
//				else if ( <some other type>.equals(eventString) )
//				{
//					// do something
//				}
			}
		}		
	}

	public Action onDeath(WorldEvent cause) {
		
		Say sayAction = new Say(codec.encrypt(DefaultAntConfig.AVENGE_ME),
							    Direction.north,
							    Direction.northeast,
							    Direction.east,
							    Direction.southeast,
							    Direction.south,
							    Direction.southwest,
							    Direction.west,
							    Direction.northwest,
							    Direction.here);
		return sayAction;
	}



}
