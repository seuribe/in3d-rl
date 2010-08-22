package ar.uba.dc.eci2010.m1;

import org.junit.Before;
import org.junit.Test;
import org.rlcommunity.rlglue.codec.AgentInterface;
import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.LocalGlue;
import org.rlcommunity.rlglue.codec.RLGlue;
import ar.uba.dc.eci2010.m1.agent.ModelBasedRacingAgent;

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
		env = new RaceEnvironment("data/midtrack.txt");
	}

	@Test
	public void runModelBasedTest() {
		agent = new ModelBasedRacingAgent(10);
		glue = new LocalGlue(env, agent);
		RLGlue.setGlue(glue);

		RLGlue.RL_init();
		
		// Learn model
		System.out.println("learning model...");
		RLGlue.RL_agent_message("learn-model");
		runEpisodes(1, 100000);

		// Learn V
		System.out.println("running experiments...");
		RLGlue.RL_agent_message("normal");
		RLGlue.RL_agent_message("reset-known");
		runEpisodes(100, 10000);

		// Evaluate
		System.out.println("Evaluating...");
		RLGlue.RL_agent_message("evaluate");
		evaluate(100, 1000);
	}

	private void runEpisodes(int nEpisodes, int steps) {
		for (int i = 0 ; i < nEpisodes ; i++) {
			RLGlue.RL_episode(steps);
			System.out.println(RLGlue.RL_num_steps());
		}
	}

	@Test
	public void runModelFreeTest() {
		agent = new SampleSarsaAgent();
		glue = new LocalGlue(env, agent);
		RLGlue.setGlue(glue);
		
		RLGlue.RL_init();
		
		// Learn V
		System.out.println("running experiments...");
		runEpisodes(100, 1000);

		// Evaluate
		System.out.println("Evaluating...");
		RLGlue.RL_agent_message("freeze-learning");
		evaluate(100, 10000);
	}
	
	public void evaluate(int nEpisodes, int steps) {
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

		RLGlue.RL_agent_message("unfreeze learning");
	}
}
