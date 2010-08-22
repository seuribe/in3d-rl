package ar.uba.dc.eci2010.m1.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import ar.uba.dc.eci2010.m1.RaceEnvironment;

public class ModelBasedRacingAgent implements AgentInterface {
//	private int numStates;
	private int numActions;
	private double maxReward;
	private int minKnown;

//    private Random random = new Random();
    private Action lastAction;
    private Observation lastObservation;

    private static final double DISCOUNT_RATE = 0.95;
    
    private AgentState runningState = AgentState.NormalRun;
    
    private Map<Integer, State> states = new HashMap<Integer, State>();
    
    public enum AgentState {
    	LearnModel, NormalRun, Evaluate;
    }
    
    public ModelBasedRacingAgent(int minKnown) {
    	this.minKnown = minKnown;
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
			return visited > minKnown;
		}
		
		/**
		 * La probabilidad de que tomando la accion action termine en el estado state
		 * @param action
		 * @param state
		 * @return
		 */
		public double getP(int action, State state) {
			Transition t = getTransitionTo(action, state);
			return actionTaken[action] / t.timesTaken;
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

		private List<Transition> getAllTransitions() {
			List<Transition> alltrans = new ArrayList<Transition>();
			for (Map<State, Transition> values : trans.values()) {
				alltrans.addAll(values.values());
			}
			return alltrans;
		}

    	public String toString() {
    		return "[id: " + id + ", coords = " + (id%RaceEnvironment.TRACK_WIDTH) + "," + (id/RaceEnvironment.TRACK_WIDTH) + "]";// TRAMPA! pero es para debugging...
    	}

		public int timesVisited() {
			return visited;
		}
		
		/**
		 * Determina la mejor accion desde el estado con el fin de explorar lo mas posible
		 * @param state
		 * @return
		 */
		private int getBestExploreAction() {
			int minTaken = 0;
			for (int a = 0 ; a < numActions ; a++) {
				if (actionTaken[a] < actionTaken[minTaken]) {
					minTaken = a;
				}
			}
			return minTaken;
		}

		public void resetKnown() {
			visited = 0;
		}
		
    }
    
	private double valueIterate() {
		double maxChange = 0;
		for (State st : states.values()) {
			double prevValue = st.V;
			double maxR = 0;
			int maxA = 0;
			for (int a = 0 ; a < numActions ; a++) {

				double r = 0;
				// Si jamas tomo esta accion desde aca, asumo que es optima
				if (st.getTimesTaken(a) == 0) {
					r = DISCOUNT_RATE * (maxReward / (1 - DISCOUNT_RATE));
				} else {
					for (State to : st.getVisitableStates(a)) {
						// Si no es conocida, uso los valores Vmax y p = 1
//						if (!to.isKnown()) {
//							r += (st.reward + DISCOUNT_RATE * (maxReward / (1 - DISCOUNT_RATE)));
//						} else {
							// Si es conocida, uso el p observado y el valor de V[s2] que corresponde
							r += st.getP(a, to) * (to.reward + DISCOUNT_RATE * to.V);
//						}
					}
				}

				if (r > maxR) {
					maxA = a;
					maxR = r;
				}
			}
			st.V = maxR;
			st.policy = maxA;
			maxChange = Math.max(maxChange, Math.abs(prevValue - st.V));
		}
		return maxChange;
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
		System.out.println("ModelBasedRacingAgent.agent_init()");
		TaskSpecVRLGLUE3 spec = new TaskSpecVRLGLUE3(taskSpecification);
		maxReward = spec.getRewardRange().getMax();
		
//        numStates = spec.getDiscreteObservationRange(0).getMax() + 1;
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
		if (message.startsWith("learn-model")) {
			runningState = AgentState.LearnModel;
			return "Ok!";
		} else if (message.startsWith("normal")) {
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
			case LearnModel:
				newAction = to.getBestExploreAction();
				break;
			case NormalRun:
				if (to.timesVisited() == 1) {
					valueIterate();
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
		//
	}
	
}