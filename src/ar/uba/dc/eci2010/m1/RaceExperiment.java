package ar.uba.dc.eci2010.m1;

import org.junit.Before;
import org.junit.Test;
import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.LocalGlue;
import org.rlcommunity.rlglue.codec.RLGlue;

import ar.uba.dc.eci2010.m1.agent.modelbased.ModelBasedRacingAgent;
import ar.uba.dc.eci2010.m1.agent.modelfree.QAgent;

/**
 * Clase de prueba para los agentes
 *
 */
public class RaceExperiment {

	private AgentInterface agent;
	private EnvironmentInterface env;
	private LocalGlue glue;

	@Before
	public void setupEnv() {
		env = new RaceEnvironment("data/bigtrack.txt");
	}

	/**
	 * Prueba con distintos parametros el agente basado en modelo
	 */
	@Test
	public void runMultipleModelBased() {
		for (int e = 10 ; e < 70 ; e+=5) {
			for (int s = 10 ; s < 70 ; s+=5) {
				agent = new ModelBasedRacingAgent(e, s);
				glue = new LocalGlue(env, agent);
				RLGlue.setGlue(glue);
				RLGlue.RL_init();
				RLGlue.RL_agent_message("normal");
				runEpisodes(200, 100, false, true);
				RLGlue.RL_agent_message("evaluate");
				System.out.print("Running e: " + e + ", s: " + s + " ");
				evaluate(100, 1000);
			}
		}
	}

	/**
	 * Prueba el agente model-based
	 */
	@Test
	public void runModelBasedTest() {
		int minKnownState = (int) Math.sqrt(RaceEnvironment.TRACK_WIDTH * RaceEnvironment.TRACK_HEIGHT);
		int minKnownTransition = minKnownState;
		agent = new ModelBasedRacingAgent(minKnownState, minKnownTransition);
		glue = new LocalGlue(env, agent);
		RLGlue.setGlue(glue);

		RLGlue.RL_init();

		int totalCells = RaceEnvironment.TRACK_WIDTH * RaceEnvironment.TRACK_HEIGHT;
		
		// Learn V
		System.out.println("running experiments...");
		RLGlue.RL_agent_message("normal");
//		RLGlue.RL_agent_message("reset-known");
//		RLGlue.RL_env_message("show moves");
		runEpisodes(20, totalCells * 10, true, true);

		// Evaluate
		System.out.println("Evaluating...");
		RLGlue.RL_agent_message("evaluate");
		evaluate(100, totalCells);
	}

	private void runEpisodes(int nEpisodes, int steps, boolean printSteps, boolean endQuick) {
		int lastSteps = 0;
		int minSteps = Integer.MAX_VALUE;
		for (int i = 0 ; i < nEpisodes ; i++) {
			RLGlue.RL_episode(steps);
//			RLGlue.RL_env_message("print-state");
			int thisSteps = RLGlue.RL_num_steps();
			if (printSteps) {
				System.out.println(thisSteps);
			}
			if (thisSteps < minSteps) {
				minSteps = thisSteps;
			}
			if (endQuick && thisSteps == lastSteps && thisSteps == minSteps) {
				break;
			}
			lastSteps = thisSteps;
		}
	}

	@Test
	public void runModelFreeTest() {
		agent = new QAgent();
		glue = new LocalGlue(env, agent);
		RLGlue.setGlue(glue);
		
		RLGlue.RL_init();
		
		// Learn V
		System.out.println("running experiments...");
		runEpisodes(200, 1000, true, false);

		// Evaluate
		System.out.println("Evaluating...");
		RLGlue.RL_agent_message("freeze-learning");
		evaluate(100, 500);
	}
	
	/**
	 * Prueba multiples episodios y devuelve el promedio de pasos que necesito
	 * Imprime el promedio y el desvio estandar
	 * @param nEpisodes
	 * @param steps
	 * @return
	 */
	public double evaluate(int nEpisodes, int steps) {
		double sum = 0;
		double sum_squares = 0;
		for (int i = 0; i < nEpisodes; i++) {
            RLGlue.RL_episode(steps);
            double ret = RLGlue.RL_num_steps();
            sum += ret;
            sum_squares += ret * ret;
        }

		double mean = sum / (double)nEpisodes;
		double variance = (sum_squares - (double)nEpisodes * mean * mean) / ((double)nEpisodes - 1.0f);
		System.out.println("mean: " + mean + ", variance: " + variance);

		return mean;
	}
}
