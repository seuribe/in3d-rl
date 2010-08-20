package ar.uba.dc.eci2010.m1.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

import ar.uba.dc.eci2010.m1.RaceEnvironment;

public class ModelBasedRacingAgent implements AgentInterface {
//
//	/** tabla de rewards R(s, a, s')  */
//	private double[][][] R;
//	/** Tabla con probabilidades de transicion S, A, S' */
//	private int[][][] T;
//	/** Funcion Q(s, a) */
//	private double[][] Q;
//	
//	private int[] timesBeen;
//	
	private int numStates;
	private int numActions;
	private double maxReward;
	private int minKnown;

//    private Random random = new Random();
    private Action lastAction;
    private Observation lastObservation;

    private static final double DISCOUNT_RATE = 0.95;
    
    private boolean learn = true;

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

		private List<Transition> getAllTransitions() {
			List<Transition> alltrans = new ArrayList<Transition>();
			for (Map<State, Transition> values : trans.values()) {
				alltrans.addAll(values.values());
			}
			return alltrans;
		}
		
		public void visit(double reward) {
//			boolean prevKnown = isKnown();
			this.visited++;
//			if (!prevKnown && isKnown()) {
//				System.out.println("state " + this + " is now known!");
//			}
			this.reward = reward;
/*			
			if (isKnown()) {
				V = reward + getEstFutureRewards();
			}
*/
		}
		
//		private double getEstFutureRewards() {
//			List<Transition> ts = getAllTransitions();
//			double est = 0;
//			for (Transition t : ts) {
//				double p = t.timesTaken / actionTaken[t.action];
//				est += p * t.to.V;
//			}
//			return est;
//		}

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

		
//		public int getBestAction() {
//			int bestA = 1;
//			double bestR = Double.MAX_VALUE;
//			double[] rewards = new double[numActions];
//			for (int a = 0 ; a < numActions ; a++) {
//				if (actionTaken[a] == 0) {
//					continue;
//				}
//				Map<State, Transition> tmap = trans.get(a);
//				for (Transition t : tmap.values()) {
//					State to = t.to;
//					double p = getP(a, to);
//					rewards[a] += p * DISCOUNT_RATE * to.V;
//				}
//				if (rewards[a] > bestR) {
//					bestA = a;
//					bestR = rewards[a];
//				}
//			}
//			return bestA;
//		}
//
//		public int getExploreAction() {
//			int minAction = 0;
//			// three step random explore
//			for (int a = 0 ; a < actionTaken.length ; a++) {
//				if (actionTaken[a] < actionTaken[minAction]) {
//					minAction = a;
//				}
//			}
//			int nMin = 0;
//			for (int a = 0 ; a < actionTaken.length ; a++) {
//				if (actionTaken[a] == actionTaken[minAction]) {
//					nMin++;
//				}
//			}
//			int i = random.nextInt(nMin);
//			for (int a = 0 ; a < actionTaken.length ; a++) {
//				if (actionTaken[a] == actionTaken[minAction]) {
//					i--;
//					if (i == 0) {
//						return a;
//					}
//				}
//			}
//			
//			return minAction;
//		}
    	public String toString() {
    		return "[id: " + id + ", coords = " + (id%RaceEnvironment.TRACK_WIDTH) + "," + (id/RaceEnvironment.TRACK_WIDTH) + "]";// TRAMPA! pero es para debugging...
    	}
		
    }
    
	private double valueIterate() {
		double maxChange = 0;
		for (State st : states.values()) {
//		for (int s = 0 ; s < numStates ; s++) {
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

    
    private Map<Integer, State> states = new HashMap<Integer, State>();

    private State getState(int id) {
    	State st = states.get(id);
    	if (st == null) {
    		st = new State(id);
//    		System.out.println("Arrived at new state " + st + "!");
    		states.put(id, st);
    	}
    	return st;
    }
    
   
	@Override
	public void agent_init(String taskSpecification) {
		System.out.println("ModelBasedRacingAgent.agent_init()");
		TaskSpecVRLGLUE3 spec = new TaskSpecVRLGLUE3(taskSpecification);
		maxReward = spec.getRewardRange().getMax();
		
        numStates = spec.getDiscreteObservationRange(0).getMax() + 1;
        numActions = spec.getDiscreteActionRange(0).getMax() + 1;

		initializeTables();
	}

	private void initializeTables() {
//		Q = new double[numStates][numActions];
//		R = new double[numStates][numActions][numStates];
//		T = new int[numStates][numActions][numStates];
//		timesBeen = new int[numStates];
	}

	@Override
	public String agent_message(String message) {
		if (message.startsWith("freeze learning")) {
			learn = false;
			return "Ok!";
		} else if (message.startsWith("unfreeze learning")) {
			learn = true;
			return "Ok!";
		}
		return "What?";
	}

	@Override
	public Action agent_start(Observation observation) {
//		System.out.println("ModelBasedRacingAgent.agent_start()");
		
		State st = getState(observation.getInt(0));

		int action = st.policy;
		
//		exploreProb = 0.1;

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
		
//		System.out.println("@ state " + to + ", visitable: " + to.getVisitableStates(0));
//		System.out.println("@ state " + to + ", visitable: " + to.getVisitableStates(1));
//		System.out.println("@ state " + to + ", visitable: " + to.getVisitableStates(2));
//		System.out.println("@ state " + to + ", visitable: " + to.getVisitableStates(3));
		
		if (!to.isKnown()) {
			valueIterate();
		}
		
		int newAction = to.policy;

        lastAction = makeAction(newAction);
        lastObservation = observation.duplicate();

		return lastAction;
	}

//	private void updateQ(int s1, int a, int s2) {
//		int sumT = 0;
//		for (int ia = 0 ; ia < numActions ; ia++) {
//			sumT += T[s1][ia][s2];
//		}
//		if (sumT == 0) {
//			return;
//		}
//		double sumR = 0;
//		double maxQ = getMaxQ(s2);
//		for (int ia = 0 ; ia < numActions ; ia++) {
//			double div = (R[s1][ia][s2] + DISCOUNT_RATE * maxQ);
//			if (div == 0) {
//				sumR += 0;
//			} else {
//				sumR += T[s1][ia][s2]/div;
//			}
//		}
//		sumR /= sumT;
//		if (sumR == Double.NaN || sumR == Float.NaN) {
//			sumR = 0;
//		}
//		Q[s1][a] = sumR;
////		System.out.println(sumR);
//	}

//	private double getMaxQ(int s) {
//		return Q[s][exploit(s)];
//	}

	@Override
	public void agent_cleanup() {
		initializeTables();
	}

	@Override
	public void agent_end(double reward) {
//		System.out.println(exploreProb);
	}

	
	
//	private double exploreProb = 0.05;
//	private int policy(int s) {
//		if (timesBeen[s] <= 5) {
//			return explore(s);
//		}
//
//		if (random.nextDouble() <= exploreProb) {
//			return explore(s);
//		} else {
//			return exploit(s);
//		}
//	}
//
//	private int exploit(int state) {
//		int maxA = 0;
//		for (int a = 0 ; a < numActions ; a++) {
//			if (Q[state][a] >= Q[state][maxA]) {
//				maxA = a;
//			}
//		}
//		return maxA;
//	}
//	
//	private int explore(int state) {
//		int a = random.nextInt(numActions);
//		return a;
//	}
	
}
