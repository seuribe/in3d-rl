package ar.uba.dc.eci2010.m1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.rlcommunity.rlglue.codec.EnvironmentInterface;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpec;
import org.rlcommunity.rlglue.codec.taskspec.TaskSpecVRLGLUE3;
import org.rlcommunity.rlglue.codec.taskspec.ranges.DoubleRange;
import org.rlcommunity.rlglue.codec.taskspec.ranges.IntRange;
import org.rlcommunity.rlglue.codec.types.Action;
import org.rlcommunity.rlglue.codec.types.Observation;
import org.rlcommunity.rlglue.codec.types.Reward_observation_terminal;

public class RaceEnvironment implements EnvironmentInterface {

	private static final int STEP_REWARD = -1;
	private static final int TERMINATION_REWARD = 1000;
	private static final int TRACK_WIDTH  = 45;
	private static final int TRACK_HEIGHT = 20;
	
	private int agentX;
	private int agentY;
	
	private CellType[][] track;
	
	public static enum Direction {
		NORTH(0, 1, 2, 3, 0, -1), EAST(1, 2, 3, 0, 1, 0), SOUTH(2, 3, 0, 1, 0, 1), WEST(3, 0, 1, 2, -1, 0), NONE(4, 4, 4, 4, 0, 0);

		private int index;
		private final int right;
		private final int back;
		private final int left;
		private final int moveX;
		private final int moveY;

		private Direction(int index, int right, int back, int left, int moveX, int moveY) {
			this.index = index;
			this.right = right;
			this.back = back;
			this.left = left;
			this.moveX = moveX;
			this.moveY = moveY;
		}
		
		public Direction getRight() {
			return values()[right];
		}
		public Direction getBack() {
			return values()[back];
		}
		public Direction getLeft() {
			return values()[left];
		}

		public static Direction get(int index) {
			for (Direction dir : values()) {
				if (dir.index == index) {
					return dir;
				}
			}
			return null;
		}
	}
	
	private enum CellType {
		Track('T', new Transition(1.0f, 0f, 0f, 0f)),
		Sand('S', new Transition(0.75f, 0f, 0f, 0.1f)),
		Ice('I', new Transition(0.5f, 0.25f, 0.25f, 0f)),
		Offtrack('.', new Transition(0, 0, 0, 0)), // Irrelevante, porque no deberia poder entrar nunca
		Start('0', new Transition(1.0f, 0f, 0f, 0f)), // Igual que el track normal
		End('X', new Transition(1.0f, 0f, 0f, 0f)); // Igual que el track normal

		private static final char TERMINAL_ID = 'X';
		
		private final char id;
		private final Transition T;

		private CellType(char id, Transition T) {
			this.id = id;
			this.T = T;
		}
		
		public static CellType get(char id) {
			for (CellType ct : values()) {
				if (ct.id == id) {
					return ct;
				}
			}
			return null;
		}
		
		public boolean isTerminal() {
			return id == TERMINAL_ID;
		}
		
		public boolean isValid() {
			return this != Offtrack;
		}
	}
	
	@Override
	public void env_cleanup() {
		
	}

	@Override
	public String env_init() {
		try {
			track = new CellType[TRACK_HEIGHT][TRACK_WIDTH];
			File file = new File("data/track.txt");
			BufferedReader reader = new BufferedReader(new FileReader(file));
			for (int y = 0 ; y < TRACK_HEIGHT ; y++) {
				String line = reader.readLine();
				for (int x = 0 ; x < TRACK_WIDTH ; x++) {
					track[y][x] = CellType.get(line.charAt(x)); 
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		TaskSpecVRLGLUE3 spec = new TaskSpecVRLGLUE3("Reach the end of the track");
		spec.setDiscountFactor(0.95f);
		spec.setEpisodic();
		spec.addDiscreteObservation(new IntRange(0, getNumStates() - 1));
		spec.addDiscreteAction(new IntRange(0, 3));
		spec.setRewardRange(new DoubleRange(STEP_REWARD, TERMINATION_REWARD));
		spec.setExtra("RaceEnvironment - ECI 2010 M1");
        String taskSpecString = spec.toTaskSpec();
        TaskSpec.checkTaskSpec(taskSpecString);

		return taskSpecString;
	}


	@Override
	public String env_message(String message) {
		return null;
	}

	@Override
	public Observation env_start() {
		Observation obs = new Observation(1, 0, 0);
		obs.setInt(0, getWorldState());
		return obs;
	}

	@Override
	public Reward_observation_terminal env_step(Action action) {
		
		int nAction = action.getInt(0);
		Direction dir = Direction.get(nAction);
		CellType ct = track[agentY][agentX];
		dir = ct.T.attempt(dir);
		int[] newPos = getNewPosition(dir);
		if (outOufBounds(newPos) || outOfTrack(newPos)) {
			newPos = new int[]{agentX, agentY};
		}
		// TODO: pasar estas variables a una clase Position -- Seu
		agentX = newPos[0];
		agentY = newPos[1];
		
		Reward_observation_terminal rot = new Reward_observation_terminal();
		
		Observation obs = new Observation(1, 0, 0);
		obs.setInt(0, getWorldState());
		
		if (track[agentY][agentX].isTerminal()) {
			rot.setReward(TERMINATION_REWARD);
			rot.setTerminal(true);
			rot.setObservation(obs);
		} else {
			rot.setReward(STEP_REWARD);
			rot.setTerminal(false);
			rot.setObservation(obs);
		}
		
		return rot;
	}

	private boolean outOfTrack(int[] newPos) {
		return track[newPos[1]][newPos[0]].isValid();
	}

	private boolean outOufBounds(int[] pos) {
		return pos[0] < 0 || pos[0] >= TRACK_WIDTH || pos[1] < 0 || pos[1] >= TRACK_HEIGHT;
	}

	private int[] getNewPosition(Direction dir) {
		return new int[] {agentX + dir.moveX, agentY + dir.moveY};
	}

	private int getWorldState() {
		return agentY * TRACK_WIDTH + agentX;
	}

	private int getNumStates() {
		return TRACK_HEIGHT * TRACK_WIDTH;
	}

}
