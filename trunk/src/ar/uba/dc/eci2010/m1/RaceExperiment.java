package ar.uba.dc.eci2010.m1;

import org.rlcommunity.rlglue.codec.LocalGlue;
import org.rlcommunity.rlglue.codec.RLGlue;

import ar.uba.dc.eci2010.m1.agent.MDPRacingAgent;
import ar.uba.dc.eci2010.m1.agent.ModelBasedRacingAgent;
import ar.uba.dc.eci2010.m1.agent.RMaxRacingAgent;

/**
 * 
 *
 */
public class RaceExperiment {
	public static void main(String[] args) {
		RaceEnvironment env = new RaceEnvironment("data/minitrack.txt");
//		SampleSarsaAgent agent = new SampleSarsaAgent();
		ModelBasedRacingAgent agent = new ModelBasedRacingAgent(20);
//		MDPRacingAgent agent = new MDPRacingAgent(env.getMDP());
//		RMaxRacingAgent agent = new RMaxRacingAgent(RaceEnvironment.MAX_REWARD, 5, 12);
		
		LocalGlue localGlueImplementation=new LocalGlue(env, agent);
		RLGlue.setGlue(localGlueImplementation);
		
		
		RLGlue.RL_init();
		System.out.println("running experiments");
		for (int i = 0 ; i < 100 ; i++) {
			RLGlue.RL_episode(100000);
			RLGlue.RL_agent_message("reset-explore-exploit");
			System.out.println(RLGlue.RL_num_steps());
		}
		System.out.println();
		System.out.println("Evaluating...");
		RLGlue.RL_agent_message("freeze learning");
		double sum = 0;
		double sum_squares = 0;
		final int N_EPISODES = 100;
		for (int i = 0; i < N_EPISODES; i++) {
            RLGlue.RL_episode(1000);
            double ret = /*RLGlue.RL_return()*/RLGlue.RL_num_steps();
            sum += ret;
            sum_squares += ret * ret;
        }

		double mean = sum / (double)N_EPISODES;
		double variance = (sum_squares - (double)N_EPISODES * mean * mean) / ((double)N_EPISODES - 1.0f);
		System.out.println("mean: " + mean + ", variance: " + variance);

//		RLGlue.RL_env_message("show moves");
//		RLGlue.RL_episode(500);

		RLGlue.RL_agent_message("unfreeze learning");
	}
}
