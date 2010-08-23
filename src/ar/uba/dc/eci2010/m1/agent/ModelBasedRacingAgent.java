package ar.uba.dc.eci2010.m1.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import ar.uba.dc.eci2010.m1.RaceEnvironment;

public class ModelBasedRacingAgent implements AgentInterface {
//	private int numStates;
	private int numActions;
	private double maxReward;
	private int minKnownState;
	private int minKnownTransition;

    private Random random = new Random();
    private Action lastAction;
    private Observation lastObservation;

    private static final double DISCOUNT_RATE = 0.95;
    private static final double ITERATE_ERROR = 0.01;
    
    private AgentState runningState = AgentState.NormalRun;
    
    private Map<Integer, State> states = new HashMap<Integer, State>();
	private double Vmax;
	private int iterations;
    
    public enum AgentState {
    	NormalRun, Evaluate;
    }
    
    /**
     * 
     * @param minKnownState Cantidad de veces que debe visitar un estado para considerarlo conocido
     * @param minKnownTransition Cantidad de veces que debe realizar una transicion para considerarla conocida
     */
    public ModelBasedRacingAgent(int minKnownState, int minKnownTransition) {
    	this.minKnownState = minKnownState;
    	this.minKnownTransition = minKnownTransition;
    }
    
	private class Transition {
		public final State from;
		public final State to;
		public final int action;
		public int timesTaken;

		public Transition(State from, int action, State to) {
			this.from = from;
			this.action = action;
			this.to = to;
		}
		
		public void taken() {
			timesTaken++;
		}
		
		public boolean isKnown() {
			return timesTaken < minKnownTransition;
		}
		
	}
    
    private class State {
		public final int id;
    	private Map<Integer, Map<State, Transition>> trans;
    	private int[] actionTaken;
    	private int totalTransTaken;
		private int visited;
		public double V;
		private double reward;
		public int policy;

    	public State(int id) {
    		this.id = id;
    		this.trans = new HashMap<Integer, Map<State, Transition>>();
    		this.actionTaken = new int[numActions];
    	}

    	public void takeTransition(int action, State to) {
    		totalTransTaken++;
    		actionTaken[action]++;
    		Transition t = getTransitionTo(action, to);
			t.taken();
//			System.out.println(actionTaken[0] + " " + actionTaken[1] + " " + actionTaken[2] + " " + actionTaken[3]);

//			System.out.println("Transition taken from " + this + " to " + to);
    	}

    	/** Cantidad de veces que tomo esta accion desde ahi */
    	public int getTimesTaken(int action) {
    		return actionTaken[action];
    	}
    	
		private Transition getTransitionTo(int action, State to) {
			Map<State, Transition> tmap = trans.get(action);
			if (tmap == null) {
				tmap = new HashMap<State, Transition>();
				trans.put(action, tmap);
			}
			Transition t = tmap.get(to);
			if (t == null) {
				t = new Transition(this, action, to);
				tmap.put(to, t);
			}
			return t;
		}

		public void visit(double reward) {
			this.visited++;
			this.reward = reward;
		}
		
		public boolean isKnown() {
			for (int a = 0 ; a < numActions ; a++) {
				if (actionTaken[a] < minKnownTransition) {
					return false;
				}
			}
//			return visited > minKnownState;
			return true;
		}
		
		/**
		 * La probabilidad de que tomando la accion action termine en el estado state
		 * @param action
		 * @param state
		 * @return
		 */
		public double getP(int action, State state) {
			Transition t = getTransitionTo(action, state);
			double p = t.timesTaken / actionTaken[action];
			assert(p <= 1);
			return p;
		}

		public List<State> getVisitableStates(int action) {
			List<State> ret = new ArrayList<State>();
			Map<State, Transition> tt = trans.get(action);
			if (tt == null) {
				return ret;
			}
			for (State st : tt.keySet()) {
				ret.add(st);
			}
			return ret;
		}
/*
		private List<Transition> getAllTransitions() {
			List<Transition> alltrans = new ArrayList<Transition>();
			for (Map<State, Transition> values : trans.values()) {
				alltrans.addAll(values.values());
			}
			return alltrans;
		}
*/
    	public String toString() {
    		return "[id: " + id + ", coords = " + (id%RaceEnvironment.TRACK_WIDTH) + "," + (id/RaceEnvironment.TRACK_WIDTH) + "]";// TRAMPA! pero es para debugging...
    	}

		public void resetKnown() {
			visited = 0;
		}
		
    }
    
	private double valueIterate() {
		double maxChange = 0;
		for (State st : states.values()) {

			double prevValue = st.V;
			int maxA = 0;

			double actionValues[] = new double[numActions];
			for (int a = 0 ; a < numActions ; a++) {
				actionValues[a] = getActionValue(st, a);
			}

			maxA = getBestAction(actionValues);
			st.V = actionValues[maxA];
			st.policy = maxA;
			RLGlue.RL_env_message("set-policy "  + st.id + " " + st.policy);
			maxChange = Math.max(maxChange, Math.abs(prevValue - st.V));
		}
		return maxChange;
	}

	private int getBestAction(double[] actionValues) {
		double max = actionValues[0];
		for (int a = 0 ; a < actionValues.length ; a++) {
			if ((double)actionValues[a] > max) {
				max = actionValues[a];
			}
		}
		List<Integer> maxList = new ArrayList<Integer>();
		for (int a = 0 ; a < actionValues.length ; a++) {
			if (actionValues[a] == max) {
				maxList.add(a);
			}
		}
		if (maxList.size() == 0) {
			System.out.println("problem!! " + actionValues);
		}
		return maxList.get(random.nextInt(maxList.size()));
	}


	private double getActionValue(State st, int a) {
		double r = 0;
		// Si no tomo suficientes veces esta accion desde aca, asumo que es optima
		if (st.getTimesTaken(a) < (minKnownState/numActions)) {
			r = st.reward + Vmax;
		} else {
			// Si ya la tomo, 
			for (State to : st.getVisitableStates(a)) {
				Transition t = st.getTransitionTo(a, to);
				// Si no es conocida, uso los valores Vmax y p = 1
				if (!t.isKnown()) {
					r += st.reward + Vmax;
				} else {
					// Si es conocida, uso el p observado y el valor de V que corresponde
					r += st.getP(a, to) * (st.reward + DISCOUNT_RATE * to.V);
				}
			}
		}
		return r;
	}
    

    private State getState(int id) {
    	State st = states.get(id);
    	if (st == null) {
    		st = new State(id);
    		states.put(id, st);
    	}
    	return st;
    }
   
	@Override
	public void agent_init(String taskSpecification) {
//		System.out.println("ModelBasedRacingAgent.agent_init()");
		TaskSpecVRLGLUE3 spec = new TaskSpecVRLGLUE3(taskSpecification);
		maxReward = spec.getRewardRange().getMax();
		Vmax = DISCOUNT_RATE * (maxReward / (1 - DISCOUNT_RATE));
		
        numActions = spec.getDiscreteActionRange(0).getMax() + 1;

		initializeTables();
	}

	private void initializeTables() {
		// TODO: ver si hace falta resetar el grafo de estados -- Seu
	}
	
	private void resetKnownStates() {
		for (State state : states.values()) {
			state.resetKnown();
		}
	}

	@Override
	public String agent_message(String message) {
		if (message.startsWith("normal")) {
			runningState = AgentState.NormalRun;
			return "Ok!";
		} else if (message.startsWith("evaluate")) {
			runningState = AgentState.Evaluate;
			return "Ok!";
		} else if (message.startsWith("reset-known")) {
			resetKnownStates();
			return "Ok!";
		}
		return "What?";
	}

	@Override
	public Action agent_start(Observation observation) {
		State st = getState(observation.getInt(0));

		int action = st.policy;
		
        lastAction = makeAction(action);
        lastObservation = observation.duplicate();
        
        return lastAction;
	}

	private Action makeAction(int actionId) {
		Action r = new Action(1, 0, 0);
		r.intArray[0] = actionId;
		return r;
	}
	
	@Override
	public Action agent_step(double reward, Observation observation) {
        int s2 = observation.getInt(0);
        int s1 = lastObservation.getInt(0);
        int a = lastAction.getInt(0);

        State from = getState(s1);
		State to = getState(s2);
		from.takeTransition(a, to);
		to.visit(reward);

		int newAction = 0;
		switch (runningState) {
			case NormalRun:
				if (!to.isKnown()) {
					while (valueIterate() > ITERATE_ERROR) {
						iterations++;
					}
//					System.out.println(iterations);
				}
			case Evaluate:
				newAction = to.policy;
				break;
		}
		
        lastAction = makeAction(newAction);
        lastObservation = observation.duplicate();

		return lastAction;
	}

	@Override
	public void agent_cleanup() {
		initializeTables();
	}

	@Override
	public void agent_end(double reward) {
		System.out.println(reward + ", " + iterations);
	}

	/**
	 * -1 = unknown
	 * @param state
	 * @return
	 */
	public int getPolicy(int state) {
		if (states.containsKey(state)) {
			return getState(state).policy;
		}
		return -1;
	}
	
}
