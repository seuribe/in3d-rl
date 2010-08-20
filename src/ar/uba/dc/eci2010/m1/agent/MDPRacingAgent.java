package ar.uba.dc.eci2010.m1.agent;

import java.util.Random;

import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.RLGlue;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

public class MDPRacingAgent implements AgentInterface {

	/** tabla de rewards R(s, a, s')  */
	private double[][][] R;
	/** Tabla con probabilidades de transicion S, A, S' */
	private int[][][] TS;
	private int[][] T;
	private double P[][][];
	/** Funcion Q(s, a) */
	private double[][] Q;
	
	private double V[];
	private int policy[];
	
	private int numStates;
	private int numActions;
	private double maxReward;

    private Random random = new Random();
    private Action lastAction;
    private Observation lastObservation;

    private static final double DISCOUNT_RATE = 0.95;
    
    private boolean learn = true;

	private double exploreProb = 1;
	private final double MIN_EXPLORE = 0.5;
	private final double DISCOUNT_EXPLORE = 0.99;
	
	private final double ITERATE_EPSILON = 0.001;
	
	private int numExplore = 0;
	private int numExploit = 0;
	
    private final MDP mdp;
    
    public MDPRacingAgent(MDP mdp) {
    	this.mdp = mdp;
    }
    
	@Override
	public void agent_init(String taskSpecification) {
		System.out.println("ModelBasedRacingAgent.agent_init()");
		TaskSpecVRLGLUE3 spec = new TaskSpecVRLGLUE3(taskSpecification);
		maxReward = spec.getRewardRange().getMax();
		
        numStates = spec.getDiscreteObservationRange(0).getMax() + 1;
        numActions = spec.getDiscreteActionRange(0).getMax() + 1;

        exploreProb = 1;
		initializeTables();
	}

	private void initializeTables() {
		Q = new double[numStates][numActions];
		R = new double[numStates][numActions][numStates];
		TS = new int[numStates][numActions][numStates];
		P = new double[numStates][numActions][numStates];
		T = new int[numStates][numActions];
		V = new double[numStates];
		policy = new int[numStates];
	}

	@Override
	public String agent_message(String message) {
		if (message.startsWith("freeze learning")) {
			learn = false;
			return "Ok!";
		} else if (message.startsWith("unfreeze learning")) {
			learn = true;
			return "Ok!";
		} else if (message.startsWith("reset-explore-exploit")) {
			System.out.println("final exploreProb: " + exploreProb);
			System.out.println("exploit/explore: " + numExploit + "/" + numExplore);
			System.out.println("MaxV: " + maxV);
			exploreProb = 1;
			numExploit = 0;
			numExplore = 0;
			return "Ok!";
		}
		return "What?";
	}

	@Override
	public Action agent_start(Observation observation) {
//		System.out.println("ModelBasedRacingAgent.agent_start()");
		
		int state = observation.getInt(0);

		int action = egreedy(state);
		
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

        TS[s1][a][s2]++;
        T[s1][a]++;
        P[s1][a][s2] = (double)TS[s1][a][s2]/(double)T[s1][a];
        
        R[s1][a][s2] = reward;
		int newAction = egreedy(s2);
		double change = 1;
		while (change > ITERATE_EPSILON) {
			change = valueIterate();
			policyIterate();
		}
/*
        timesBeen[s2]++;

        int newActionInt;
        if (learn) {
	        newActionInt = policy(s2);
            // Update world state
			T[s1][a][s2]++;
			R[s1][a][s2] = R[s1][a][s2] + (reward - R[s1][a][s2]) / T[s1][a][s2];
			
	        // Update this Q
			updateQ(s1, a, s2);
			for (int s = 0 ; s < numStates ; s++) {
				updateQ(s, a, s2);
			}
        } else {
        	newActionInt = exploit(s2);
        }
*/
/*		
        Action returnAction = new Action();
        returnAction.intArray = new int[]{newActionInt};
*/

		lastObservation = observation.duplicate();
		lastAction = makeAction(newAction);

		return lastAction;
	}

	private int egreedy(int s2) {
		exploreProb *= DISCOUNT_EXPLORE;
//		if (exploreProb < MIN_EXPLORE) {
//			exploreProb = MIN_EXPLORE;
//		}
		if (learn && random.nextDouble() < MIN_EXPLORE) {
			numExplore++;
			return random.nextInt(numActions);
		}
		numExploit++;
		return policy[s2];
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
		System.out.println(exploreProb);
		System.out.println("exploit/explore: " + numExploit + "/" + numExplore);
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

	
	private double maxV = 0;
	/**
	 * Hace una pasada de value-iteration y policy-iteration
	 * @return Devuelve el maximo cambio que haya detectado
	 */
	private double valueIterate() {
//		System.out.println("MDPRacingAgent.valueIterate()");
//		System.out.println(exploreProb);
//		System.out.println(lastObservation.getInt(0));
//		System.out.println("explore/exploit: " + numExplore + "/" + numExploit);
		double maxChange = 0;
		for (int s = 0 ; s < numStates ; s++) {
			double prevValue = V[s];
			double maxR = 0;
			for (int a = 0 ; a < numActions ; a++) {
				double r = 0;
				for (int s2 = 0 ; s2 < numStates ; s2++) {
//					double p = (double)TS[s][a][s2] / T[s][a];
//					if (Double.isNaN(p)) {
//						p = 0;
//					}
//					System.out.println(p);
					r += mdp.getP(s, a, s2) /*T[s][a][s2]*/ * (R[s][a][s2] + DISCOUNT_RATE * V[s2]);
				}
				if (r > maxR) {
					maxR = r;
				}
			}
			V[s] = maxR;
			if (V[s] > maxV) {
				maxV = V[s];
			}
			double change = Math.abs(prevValue - V[s]);
			if (change > maxChange) {
				maxChange = change;
			}
		}
		return maxChange;
	}
	
	private void policyIterate() {
		for (int s = 0 ; s < numStates ; s++) {
			int maxA = 0;
			double maxR = 0;
			for (int a = 0 ; a < numActions ; a++) {
				double r = 0;
				for (int s2 = 0 ; s2 < numStates ; s2++) {
					r += mdp.getP(s, a, s2) * (R[s][a][s2] + (DISCOUNT_RATE * V[s2]));
				}
				if (r > maxR) {
					maxR = r;
					maxA = a;
				}
			}
			policy[s] = maxA;
		}
	}
	
}
