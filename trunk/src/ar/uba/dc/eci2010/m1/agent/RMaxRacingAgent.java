package ar.uba.dc.eci2010.m1.agent;


import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;

public class RMaxRacingAgent implements AgentInterface {

	/** tabla de rewards R(s, a, s')  */
	private double[][][] R;
	/** Tabla con cantidad de tansiciones S, A -> S' */
	private int[][][] TS;
	/** Tabla con cantidad de tansiciones desde S, A */
	private int[][] T;
	
	/** Known / unkown */
	private boolean K[];
	
	/** Value function */
	private double V[];
	/** Policy */
	private int policy[];
	
	private int numStates;
	private int numActions;

    private Action lastAction;
    private Observation lastObservation;

    private static final double DISCOUNT_RATE = 0.95;
    
    private boolean learn = true;

	private final double ITERATE_EPSILON = 0.001;
	
	/** Cantidad de veces que hay que visitar un estado para considerarlo conocido */
	private final int minKnown;
	/** Valor del reward maximo que puede llegar a recibir el agente */
	private double maxReward;
	/** Minima cantidad de pasos para llegar a la solucion */
	private final int minSteps;
	
    public RMaxRacingAgent(int maxReward, int minKnown, int minSteps) {
    	this.maxReward = maxReward;
		this.minKnown = minKnown;
		this.minSteps = minSteps;
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
		R = new double[numStates][numActions][numStates];
		for (int s = 0 ; s < numStates ; s++) {
			for (int a = 0 ; a < numActions ; a++) {
				for (int s2 = 0; s2 < numStates ; s2++) {
					R[s][a][s2] = maxReward;
				}
			}
		}
		TS = new int[numStates][numActions][numStates];
		T = new int[numStates][numActions];
		V = new double[numStates];
		K = new boolean[numStates];
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
		}
		return "What?";
	}

	@Override
	public Action agent_start(Observation observation) {
//		System.out.println("ModelBasedRacingAgent.agent_start()");
		
		int state = observation.getInt(0);
		int action = policy[state];
		
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

        if (learn) {
            if (!K[s2]) {
        		double change = 1;
        		while (change > ITERATE_EPSILON) {
        			change = valueIterate();
        		}
            }

            TS[s1][a][s2]++;
            T[s1][a]++;
            
            R[s1][a][s2] = reward;
        }
		int newAction = policy[s2];

//		dumpT();
		
		lastObservation = observation.duplicate();
		lastAction = makeAction(newAction);

		return lastAction;
	}

//	private void dumpT() {
//		for (int s = 0 ; s < numStates ; s++) {
//			for (int a = 0 ; a < numActions ; a++) {
//				System.out.print(T[s][a] + " ");
//			}
//			System.out.println();
//		}
//	}

	@Override
	public void agent_cleanup() {
		initializeTables();
	}

	@Override
	public void agent_end(double reward) {
		System.out.println("Agent ended with reward: " + reward);
	}

	/**
	 * @return Devuelve el maximo cambio que haya detectado
	 */
	private double valueIterate() {
		double maxChange = 0;
		for (int s = 0 ; s < numStates ; s++) {
			double prevValue = V[s];
			double maxR = 0;
			int maxA = 0;
			for (int a = 0 ; a < numActions ; a++) {

				double r = 0;
				for (int s2 = 0 ; s2 < numStates ; s2++) {
					
					// Si no es conocida, uso los valores Vmax y p = 1
					if (TS[s][a][s2] < minKnown) {
						r += (R[s][a][s2] + DISCOUNT_RATE * (maxReward / (1 - DISCOUNT_RATE)));
					} else {
						// Si es conocida, uso el p observado y el valor de V[s2] que corresponde
						double p = (double)TS[s][a][s2] / T[s][a];
						r += p * (R[s][a][s2] + DISCOUNT_RATE * V[s2]);
					}
					
				}

				if (r > maxR) {
					maxA = a;
					maxR = r;
				}
			}
			V[s] = maxR;
			policy[s] = maxA;
			maxChange = Math.max(maxChange, Math.abs(prevValue - V[s]));
		}
		return maxChange;
	}

}
