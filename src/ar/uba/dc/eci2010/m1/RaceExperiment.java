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
		env = new RaceEnvironment("data/track.txt");
	}

	@Test
	public void runModelBasedTest() {
		agent = new ModelBasedRacingAgent(10);
		glue = new LocalGlue(env, agent);
		RLGlue.setGlue(glue);

		RLGlue.RL_init();
		
		// Learn model
		System.out.println("learning model...");
		roamWithNoLearning(10000);

		// Learn V
		System.out.println("running experiments...");
		runEpisodes(100, 1000);

		// Evaluate
		System.out.println("Evaluating...");
		evaluate();
	}

	private void runEpisodes(int n, int steps) {
		for (int i = 0 ; i < n ; i++) {
			RLGlue.RL_episode(steps);
			System.out.println(RLGlue.RL_num_steps());
		}
	}

	private void roamWithNoLearning(int steps) {
		RLGlue.RL_agent_message("freeze learning");
		RLGlue.RL_episode(steps);
		RLGlue.RL_agent_message("unfreeze learning");
	}
	
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
		evaluate();
	}
	
	public void evaluate() {
		RLGlue.RL_agent_message("freeze learning");
		double sum = 0;
		double sum_squares = 0;
		final int N_EPISODES = 100;
		for (int i = 0; i < N_EPISODES; i++) {
            RLGlue.RL_episode(1000);
            double ret = RLGlue.RL_num_steps();
            sum += ret;
            sum_squares += ret * ret;
        }

		double mean = sum / (double)N_EPISODES;
		double variance = (sum_squares - (double)N_EPISODES * mean * mean) / ((double)N_EPISODES - 1.0f);
		System.out.println("mean: " + mean + ", variance: " + variance);

		RLGlue.RL_agent_message("unfreeze learning");
	}
}
