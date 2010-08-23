package ar.uba.dc.eci2010.m1;

import org.junit.Before;
import org.junit.Test;
import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.LocalGlue;
import org.rlcommunity.rlglue.codec.RLGlue;
import ar.uba.dc.eci2010.m1.agent.ModelBasedRacingAgent;
import ar.uba.dc.eci2010.m1.agent.modelfree.QAgent;

/**
 * 
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

	@Test
	public void runMultipleModelBased() {
		for (int e = 10 ; e < 70 ; e+=5) {
			for (int s = 10 ; s < 70 ; s+=5) {
				agent = new ModelBasedRacingAgent(e, s);
				glue = new LocalGlue(env, agent);
				RLGlue.setGlue(glue);
				RLGlue.RL_init();
				RLGlue.RL_agent_message("normal");
				runEpisodes(200, 100, false);
				RLGlue.RL_agent_message("evaluate");
				System.out.print("Running e: " + e + ", s: " + s + " ");
				evaluate(100, 1000);
			}
		}
	}
	
	@Test
	public void runModelBasedTest() {
		agent = new ModelBasedRacingAgent(20, 20);
		glue = new LocalGlue(env, agent);
		RLGlue.setGlue(glue);

		RLGlue.RL_init();

		// Learn model
//		System.out.println("learning model...");
//		RLGlue.RL_agent_message("learn-model");
//		runEpisodes(1, 10000);

		// Learn V
		System.out.println("running experiments...");
		RLGlue.RL_agent_message("normal");
//		RLGlue.RL_agent_message("reset-known");
//		RLGlue.RL_env_message("show moves");
		runEpisodes(50, 1000, true);

		// Evaluate
		System.out.println("Evaluating...");
		RLGlue.RL_agent_message("evaluate");
		evaluate(100, 1000);
	}

	private void runEpisodes(int nEpisodes, int steps, boolean printSteps) {
		for (int i = 0 ; i < nEpisodes ; i++) {
			RLGlue.RL_episode(steps);
			RLGlue.RL_env_message("print-state");
			if (printSteps) {
				System.out.println(RLGlue.RL_num_steps());
			}
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
		runEpisodes(400, 1000, false);

		// Evaluate
		System.out.println("Evaluating...");
		RLGlue.RL_agent_message("freeze-learning");
		evaluate(100, 500);
	}
	
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
