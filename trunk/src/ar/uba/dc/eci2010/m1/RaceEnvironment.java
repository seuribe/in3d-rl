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

import ar.uba.dc.eci2010.m1.agent.MDP;

public class RaceEnvironment implements EnvironmentInterface {

	/** La recompensa por cada paso que realiza sin llegar a la meta. Incentiva que llegue rapido. */
	private static final int STEP_REWARD = -1;
	/** La recompensa por llegar a la meta */
	private static final int TERMINATION_REWARD = 100;
	public static int TRACK_WIDTH;
	private int TRACK_HEIGHT;
	
	public static final int MAX_REWARD = TERMINATION_REWARD;
	
	private Position start;
	
	private Position agent;
	
	private CellType[][] track;
	private boolean outputMoves = false;

	/**
	 * Modela la posicion del agente en el entorno 
	 *
	 */
	private class Position {
		public int x, y;
		
		public Position(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public Position(Position pos) {
			this.x = pos.x;
			this.y = pos.y;
		}

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
		
		public String toString() {
			return "(" + x + "," + y + ")";
		}
	}
	
	/**
	 * Modela la direccion en que puede decidir avanzar el agente. Dependiendo del terreno - y su funcion de transicion - 
	 * puede ser que avance en esa direccion o en otra.
	 * 
	 */
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
		
		public String toString() {
			return name();
		}
	}
	
	/**
	 * Cada tipo de celda que hay en el terreno. Contiene su identificacion y la funcion de transicion para ese
	 * tipo de terreno. 
	 *
	 */
	private enum CellType {
		Track('T', new Transition(1.0f, 0f, 0f, 0f)), // En el track avanza siempre bien
		Sand('S', new Transition(0.75f, 0f, 0f, 0.1f)), // peque;a probabilidad de tener que volver atras (si se queda trabado)
		Ice('I', new Transition(0.5f, 0.25f, 0.25f, 0f)), // En el hielo puede patinar e irse para los costados
		Offtrack('.', new Transition(0, 0, 0, 0)), // Irrelevante, porque no deberia poder entrar nunca. pero si entra, que se quede clavado
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
		
		public String toString() {
			return name();
		}
	}

	public RaceEnvironment(String trackFile) {
		readMap(trackFile);
	}
	
	@Override
	public void env_cleanup() {
		// no hace falta cleanup ? 
	}

	/**
	 * Lee el mapa desde archivo. No deberia hacer falta mas de una vez dado que no se altera.
	 * @param fileName TODO
	 */
	private void readMap(String fileName) {
		try {
			File file = new File(fileName);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			String wLine = reader.readLine();
			TRACK_WIDTH = Integer.parseInt(wLine);
			String hLine = reader.readLine();
			TRACK_HEIGHT = Integer.parseInt(hLine);

			track = new CellType[TRACK_HEIGHT][TRACK_WIDTH];
			
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
			readMap("data/minitrack.txt");
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
		if (message.startsWith("show moves")) {
			outputMoves = true;
			return "Understood";
		}
		if (message.startsWith("hide moves")) {
			outputMoves = false;
			return "Understood";
		}
		if (message.startsWith("reset")) {
			reset();
			return "Understood";
		}
		return "Message not understood";
	}

	/**
	 * Solo necesita reinicializar la posicion del jugador a la inicial
	 */
	private void reset() {
		agent = new Position(start);
	}

	/**
	 * Imprime por stdout la pista, incluyendo la posicion del jugador
	 */
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

	@Override
	public Reward_observation_terminal env_step(Action action) {

//		System.out.println(agent);
		
		if (outputMoves) {
			dumpState();
		}
		
		int nAction = action.getInt(0);
		Direction dir = Direction.get(nAction);

		CellType ct = track[agent.y][agent.x];
		dir = ct.T.attempt(dir);
		Position newPos = agent.moveNew(dir);
		if (inBounds(newPos) && inTrack(newPos)) {
			agent.move(dir);
		}

		Reward_observation_terminal rot = new Reward_observation_terminal();
		
		Observation obs = new Observation(1, 0, 0);
		obs.setInt(0, getWorldState());

		rot.setReward(stateReward(track[agent.y][agent.x]));
		rot.setTerminal(track[agent.y][agent.x].isTerminal());
		rot.setObservation(obs);

		return rot;
	}

	private double stateReward(CellType cellType) {
		return cellType.isTerminal() ? TERMINATION_REWARD : STEP_REWARD;
	}

	private boolean inTrack(Position pos) {
		return track[pos.y][pos.x].isValid();
	}

	private boolean inBounds(Position pos) {
		return pos.inBounds(0, 0, TRACK_WIDTH, TRACK_HEIGHT);
	}

	private int getWorldState() {
		return getState(agent);
	}

	private int getState(Position pos) {
		return pos.y * TRACK_WIDTH + pos.x;
	}
	
	private Position getPosition(int state) {
		return new Position(state%TRACK_WIDTH, state/TRACK_WIDTH);
	}

	private CellType getCellType(int state) {
		return track[state/TRACK_WIDTH][state%TRACK_WIDTH];
	}
	
	private int getNumStates() {
		return TRACK_HEIGHT * TRACK_WIDTH;
	}
	
	private int getStateInDirection(int fromState, Direction dir) {
		Position pos = getPosition(fromState);
		pos.move(dir);
		if (!inBounds(pos) || !inTrack(pos)) {
			return fromState;
		}
		return getState(pos);
	}

	
	public MDP getMDP() {
		return new RaceMDP(TRACK_WIDTH * TRACK_HEIGHT, 4);
	}

	public class RaceMDP implements MDP {
		private double P[][][];
		private final int numStates;
		private final int numActions;
		
		public RaceMDP(int numStates, int numActions) {
			this.numStates = numStates;
			this.numActions = numActions;

			P = new double[numStates][numActions][numStates];
			for (int s = 0 ; s < numStates ; s++) {
				CellType ct = getCellType(s);
				if (!ct.isValid()) {
					continue;
				}
				double sum = 0;
				for (int a = 0 ; a < numActions ; a++) {
					
					Direction dir = Direction.get(a);
					int newState = getStateInDirection(s, dir);
					P[s][a][newState] += ct.T.getPDir(0);
					sum += P[s][a][newState];
					
					Direction right = dir.getRight();
					newState = getStateInDirection(s, right);
					P[s][a][newState] += ct.T.getPDir(1);
					sum += P[s][a][newState];
					
					Direction back = dir.getBack();
					newState = getStateInDirection(s, back);
					P[s][a][newState] += ct.T.getPDir(2);
					sum += P[s][a][newState];
					
					Direction left = dir.getLeft();
					newState = getStateInDirection(s, left);
					P[s][a][newState] += ct.T.getPDir(3);
					sum += P[s][a][newState];
					
/*
					for (int s2 = 0 ; s2 < numStates ; s2++) {
						P[s][a][s2] = 0;
					}
*/
				}
			}
		}
		
		@Override
		public double getP(int fromState, int action, int toState) {
//			if (P[fromState][action][toState] == -1) {
//				double p = 0; // Si no es el estado en ninguna de las direcciones... la P es 0
//				CellType fromType = getCellType(fromState);
//				Direction dir = Direction.get(action);
//				for (int i = 0 ; i < numActions ; i++) { // Busca en cada direccion si es posible llegar y cual es la prob. en la T
//					int newState = getStateInDirection(fromState, dir);
//					if (newState == toState) {
//						p = fromType.T.getPDir(dir);
//					}
//					dir = dir.getRight();
//				}
//				P[fromState][action][toState] = p;
//			}
			return P[fromState][action][toState];
		}

		@Override
		public double getReward(int state) {
			CellType ct = getCellType(state);
			return stateReward(ct);
		} 
	}

}
