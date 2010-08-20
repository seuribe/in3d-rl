package ar.uba.dc.eci2010.m1.agent;

public interface MDP {

	public double getReward(int state);
	public double getP(int from, int action, int to);
	
}
