package ar.uba.dc.eci2010.m1;

import org.rlcommunity.rlglue.codec.LocalGlue;
import org.rlcommunity.rlglue.codec.RLGlue;

/**
 * 
 *
 */
public class RaceExperiment {
	public static void main(String[] args) {
		RaceEnvironment env = new RaceEnvironment();
		SampleSarsaAgent agent = new SampleSarsaAgent();
		
		LocalGlue localGlueImplementation=new LocalGlue(env, agent);
		RLGlue.setGlue(localGlueImplementation);
		
		final int N_EPISODES = 1000;
		
		RLGlue.RL_init();
		System.out.println("running experiments");
		for (int i = 0 ; i < 2000 ; i++) {
			RLGlue.RL_episode(10000);
		}
		System.out.println();
		System.out.println("Evaluating...");
		RLGlue.RL_agent_message("freeze learning");
		double sum = 0;
		double sum_squares = 0;
        for (int i = 0; i < N_EPISODES; i++) {
            RLGlue.RL_episode(1000);
            double ret = /*RLGlue.RL_return()*/RLGlue.RL_num_steps();
            sum += ret;
            sum_squares += ret * ret;
        }

		double mean = sum / (double)N_EPISODES;
		double variance = (sum_squares - (double)N_EPISODES * mean * mean) / ((double)N_EPISODES - 1.0f);
		System.out.println("mean: " + mean + ", variance: " + variance);

		RLGlue.RL_env_message("show moves");
		RLGlue.RL_episode(500);

		RLGlue.RL_agent_message("unfreeze learning");
	}
}
