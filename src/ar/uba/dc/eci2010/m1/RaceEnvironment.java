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

/**
 * El entorno en que corren los agentes. Modela una pista discreta con celdas con distintos tipos de transiciones
 * 
 * @author suribe
 *
 */

public class RaceEnvironment implements EnvironmentInterface {

	/** La recompensa por cada paso que realiza sin llegar a la meta. Incentiva que llegue rapido. */
	private static final int STEP_REWARD = -1;
	/** La recompensa por llegar a la meta */
	private static final int TERMINATION_REWARD = 1000;
	public static int TRACK_WIDTH;
	public static int TRACK_HEIGHT;
	
	public static final int MAX_REWARD = TERMINATION_REWARD;
	
	private Position start;
	
	private Position agent;
	
	private CellType[][] track;
	private boolean outputMoves = false;
	/** Debug!!! */
	private int[][] agentPolicy;

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
		if (message.startsWith("set-policy")) {
			String[] tokens = message.split(" ");
			int state = Integer.parseInt(tokens[1]);
			int policy = Integer.parseInt(tokens[2]);
			agentPolicy[state/TRACK_WIDTH][state%TRACK_WIDTH] = policy;
			return "Understood";
		}
		return "Message not understood";
	}

	/**
	 * Solo necesita reinicializar la posicion del jugador a la inicial
	 */
	private void reset() {
		agent = new Position(start);
		agentPolicy = new int[TRACK_HEIGHT][TRACK_WIDTH];
		for (int y = 0 ; y < TRACK_HEIGHT ; y++) {
			for (int x = 0 ; x < TRACK_WIDTH ; x++) {
				agentPolicy[y][x] = -1;
			}
		}
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
			System.out.print(" ");
			for (int x = 0 ; x < track[y].length ; x++) {
				if (agentPolicy[y][x] == -1) {
					System.out.print(' ');
				} else {
					System.out.print(agentPolicy[y][x]);
				}
			}
			System.out.println();
		}
	}

	/**
	 * Imprime el estado actual del entorno y posicion del agente, ademas de la ultima accion ejecutada
	 * y sus consecuencias
	 * @param action
	 * @param oldPos
	 * @param newPos
	 */
	private void dumpState(int action, Position oldPos, Position newPos) {
		System.out.println("action: " + Direction.get(action) + "(" + action + "), " + oldPos + " -> " + newPos);
		dumpState();
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
		int nAction = action.getInt(0);
		Direction dir = Direction.get(nAction);

		CellType ct = track[agent.y][agent.x];
		dir = ct.T.attempt(dir);
		Position oldPos = new Position(agent);
		Position newPos = agent.moveNew(dir);
		if (inBounds(newPos) && inTrack(newPos)) {
			agent.move(dir);
		}

		if (outputMoves) {
			dumpState(nAction, oldPos, agent);
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
	
	private int getNumStates() {
		return TRACK_HEIGHT * TRACK_WIDTH;
	}
	
}
