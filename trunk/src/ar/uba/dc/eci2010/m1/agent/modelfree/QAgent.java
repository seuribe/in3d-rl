package ar.uba.dc.eci2010.m1.agent.modelfree;

import java.util.Random;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

/**
 * This is a very simple Q agent for discrete-action, discrete-state
 * environments. It uses e-greedy/softmax exploration.
 * 
 * @author Ignacio Luciani
 */
public class QAgent implements AgentInterface {

	private static final ActionSelection actualActionSelection = ActionSelection.EGREEDY;
	private Random randGenerator = new Random();
	private Action lastAction;
	private Observation lastObservation;
	private double[][] valueFunction = null;
	private double gamma;
	private double alpha = 1;
	private double epsilon = 0.1;
	private boolean policyFrozen = false;
	private boolean exploringFrozen = false;
	private int numActions;
	private int numStates;

	private enum ActionSelection {
		EGREEDY, SOFTMAX
	}

	/**
	 * Parse the task spec, make sure it is only 1 integer observation and
	 * action, and then allocate the valueFunction.
	 * 
	 * @param taskSpecification
	 */
	public void agent_init(String taskSpecification) {
		TaskSpec theTaskSpec = new TaskSpec(taskSpecification);

		/* Lots of assertions to make sure that we can handle this problem. */
		assert (theTaskSpec.getNumDiscreteObsDims() == 1);
		assert (theTaskSpec.getNumContinuousObsDims() == 0);
		assert (!theTaskSpec.getDiscreteObservationRange(0)
				.hasSpecialMinStatus());
		assert (!theTaskSpec.getDiscreteObservationRange(0)
				.hasSpecialMaxStatus());
		numStates = theTaskSpec.getDiscreteObservationRange(0).getMax() + 1;

		assert (theTaskSpec.getNumDiscreteActionDims() == 1);
		assert (theTaskSpec.getNumContinuousActionDims() == 0);
		assert (!theTaskSpec.getDiscreteActionRange(0).hasSpecialMinStatus());
		assert (!theTaskSpec.getDiscreteActionRange(0).hasSpecialMaxStatus());
		numActions = theTaskSpec.getDiscreteActionRange(0).getMax() + 1;

		gamma = theTaskSpec.getDiscountFactor();

		valueFunction = new double[numActions][numStates];
	}

	/**
	 * Choose an action e-greedily/softmaxly from the value function and store
	 * the action and observation.
	 * 
	 * @param observation
	 * @return the action
	 */
	public Action agent_start(Observation observation) {
		int newActionInt = actionSelection(actualActionSelection,
				observation.getInt(0));

		/**
		 * Create a structure to hold 1 integer action and set the value
		 */
		Action returnAction = new Action(1, 0, 0);
		returnAction.intArray[0] = newActionInt;

		lastAction = returnAction.duplicate();
		lastObservation = observation.duplicate();

		return returnAction;
	}

	/**
	 * Choose an action e-greedily/softmaxly from the value function and store
	 * the action and observation. Update the valueFunction entry for the last
	 * state, action pair.
	 * 
	 * @param reward
	 * @param observation
	 * @return the action
	 */
	public Action agent_step(double reward, Observation observation) {
		int newStateInt = observation.getInt(0);
		int lastStateInt = lastObservation.getInt(0);
		int lastActionInt = lastAction.getInt(0);
		int newActionInt = actionSelection(actualActionSelection, newStateInt);

		double Qsa = valueFunction[lastActionInt][lastStateInt];
		double maxQsa = getMaxQ(newStateInt);

		/* Only update the value function if the policy is not frozen */
		if (!policyFrozen) {
			double newQsa = Qsa + alpha * (reward + gamma * maxQsa - Qsa);
			valueFunction[lastActionInt][lastStateInt] = newQsa;
		}

		/* Creating the action a different way to showcase variety */
		Action returnAction = new Action();
		returnAction.intArray = new int[] { newActionInt };

		lastAction = returnAction.duplicate();
		lastObservation = observation.duplicate();

		return returnAction;
	}

	/**
	 * The episode is over, learn from the last reward that was received.
	 * 
	 * @param reward
	 */
	public void agent_end(double reward) {
		int lastStateInt = lastObservation.getInt(0);
		int lastActionInt = lastAction.getInt(0);

		double Qsa = valueFunction[lastActionInt][lastStateInt];

		/* Only update the value function if the policy is not frozen */
		if (!policyFrozen) {
			double newQsa = Qsa + alpha * (reward - Qsa);
			valueFunction[lastActionInt][lastStateInt] = newQsa;
		}

		/* Cleanup */
		lastObservation = null;
		lastAction = null;
	}

	/**
	 * Release memory that is no longer required/used.
	 */
	public void agent_cleanup() {
		lastAction = null;
		lastObservation = null;
		valueFunction = null;
	}

	/**
	 * This agent responds to some simple messages for freezing learning.
	 * 
	 * @param message
	 * @return Actual message to display.
	 */
	public String agent_message(String message) {

		if (message.equals("freeze learning")) {
			policyFrozen = true;
			return "message understood, policy frozen";
		}
		if (message.equals("unfreeze learning")) {
			policyFrozen = false;
			return "message understood, policy unfrozen";
		}

		return "QAgent does not understand your message.";
	}

	/**
	 * Selects a random action with probability epsilon, or the action with the
	 * highest value otherwise. This is a quick'n'dirty implementation, it does
	 * not do tie-breaking.
	 * 
	 * @param state
	 *            actual state
	 * @return Next action to take.
	 */
	private int eGreedy(int state) {
		if (!exploringFrozen) {
			if (randGenerator.nextDouble() <= epsilon) {
				return randGenerator.nextInt(numActions);
			}
		}

		/* otherwise choose the greedy action */
		int maxIndex = 0;
		for (int action = 1; action < numActions; action++) {
			if (valueFunction[action][state] > valueFunction[maxIndex][state]) {
				maxIndex = action;
			}
		}
		return maxIndex;
	}

	/**
	 * Selects the action probabilistically.
	 * 
	 * @param state
	 *            actual state
	 * @return Next action to take.
	 */
	private int softmax(int state) {
		int selectedAction = 0;
		double prob[] = new double[numActions];
		double sumProb = 0, beta = 1, offset = 0;
		double rndValue = Math.random();

		for (int action = 0; action < numActions; action++) {
			prob[action] = Math.exp(valueFunction[action][state] / beta);
			sumProb += prob[action];
		}

		for (int action = 0; action < numActions; action++) {
			prob[action] = prob[action] / sumProb;
		}

		for (int action = 0; action < numActions; action++) {
			if (rndValue > offset && rndValue < offset + prob[action])
				selectedAction = action;
			offset += prob[action];
		}

		return selectedAction;
	}

	private int actionSelection(ActionSelection actionSelection, int state) {

		switch (actionSelection) {
			case EGREEDY:
				return eGreedy(state);
			case SOFTMAX:
				return softmax(state);
			default:
				return eGreedy(state);
			}
	}

	/**
	 * Gets the max Q from the value function table.
	 * 
	 * @param newStateInt
	 *            actual state
	 * 
	 * @return max Q value
	 */
	private double getMaxQ(int newStateInt) {
		double maxQ = -Double.MAX_VALUE;

		for (int action = 0; action < numActions; action++) {
			if (valueFunction[action][newStateInt] > maxQ) {
				maxQ = valueFunction[action][newStateInt];
			}
		}
		return maxQ;
	}

}
