package ar.uba.dc.eci2010.m1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

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
	private static final int TRACK_WIDTH  = 44;
	private static final int TRACK_HEIGHT = 20;
	
	private Position start;
	
	private Position agent;
	
	private CellType[][] track;

	private class Position {
		public int x, y;
		
		public Position() {
			this.x = 0;
			this.y = 0;
		}
		
		public Position(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public Position(Position pos) {
			this.x = pos.x;
			this.y = pos.y;
		}
/*
		public boolean equals(Position p) {
			return x == p.x && y == p.y;
		}
*/		
		public boolean equals(int x, int y) {
			return this.x == x && this.y == y;
		}
		public Position moveNew(int moveX, int moveY) {
			return new Position(this.x + moveX, this.y + moveY);
		}
		public boolean inBounds(int minx, int miny, int maxx, int maxy) {
			return x >= minx && y >= miny && x < maxx && y < maxy;
		}

		public Position moveNew(Direction dir) {
			return moveNew(dir.moveX, dir.moveY);
		}

		public void move(Direction dir) {
			x += dir.moveX;
			y += dir.moveY;
		}
	}
	
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
			return this == End;
		}
		
		public boolean isValid() {
			return this != Offtrack;
		}
	}
	
	@Override
	public void env_cleanup() {
	}

	private void readMap() {
		try {
			track = new CellType[TRACK_HEIGHT][TRACK_WIDTH];
			File file = new File("data/track.txt");
			BufferedReader reader = new BufferedReader(new FileReader(file));
			for (int y = 0 ; y < TRACK_HEIGHT ; y++) {
				String line = reader.readLine();
				for (int x = 0 ; x < TRACK_WIDTH ; x++) {
					track[y][x] = CellType.get(line.charAt(x));
					if (track[y][x] == CellType.Start) {
						start = new Position(x, y);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (start == null) {
			System.out.println("Error! No starting position on track!");
		}
	}
	
	@Override
	public String env_init() {

		if (track == null) {
			readMap();
		}

		reset();

		TaskSpecVRLGLUE3 spec = new TaskSpecVRLGLUE3();
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
		if (message.startsWith("print-state")) {
			dumpState();
			return "Understood";
		}
		if (message.startsWith("reset")) {
			reset();
			return "Understood";
		}
		return "Message not understood";
	}

	private void reset() {
		agent = new Position(start);
	}

	private void dumpState() {
		System.out.println();
		for (int y = 0 ; y < track.length ; y++) {
			for (int x = 0 ; x < track[y].length ; x++) {
				if (agent.equals(x, y)) {
					System.out.print('*');
				} else {
					System.out.print(track[y][x].id);
				}
			}
			System.out.println();
		}
	}

	@Override
	public Observation env_start() {
		reset();
		Observation obs = new Observation(1, 0, 0);
		obs.setInt(0, getWorldState());
		return obs;
	}

	private int steps = 0;
	@Override
	public Reward_observation_terminal env_step(Action action) {
		
		steps++;
//		if (steps % 10 == 0) {
//			dumpState();
//		}
		
		int nAction = action.getInt(0);
		Direction dir = Direction.get(nAction);
		
		if (canMove(dir)) {
			agent.move(dir);
		}
		
		Reward_observation_terminal rot = new Reward_observation_terminal();
		
		Observation obs = new Observation(1, 0, 0);
		obs.setInt(0, getWorldState());
		
		if (track[agent.y][agent.x].isTerminal()) {
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

	private boolean canMove(Direction dir) {
		CellType ct = track[agent.y][agent.x];
		dir = ct.T.attempt(dir);
		Position newPos = agent.moveNew(dir);
		return inBounds(newPos) && inTrack(newPos);
	}

	private boolean inTrack(Position pos) {
		return track[pos.y][pos.x].isValid();
	}

	private boolean inBounds(Position pos) {
		return pos.inBounds(0, 0, TRACK_WIDTH, TRACK_HEIGHT);
	}

	private Position getNewPosition(Direction dir) {
		return agent.moveNew(dir.moveX, dir.moveY);
	}

	private int getWorldState() {
		return agent.y * TRACK_WIDTH + agent.x;
	}

	private int getNumStates() {
		return TRACK_HEIGHT * TRACK_WIDTH;
	}

}
