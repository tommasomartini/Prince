package com.prince;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/* Database:
 * prova
nome: erra
username: principe
password: principe

sudo apt-get install libmysql-java
 */

public class BootstrapNode {

	//	Listening ports
	private static final int PORT_JOINED_NODE = 8001;
	private static final int PORT_DEPARTED_NODE = 8002;
	private static final int PORT_ALIVE_NODE = 8003;

	//	Speaking ports
	private static final int PORT_ASK_ALIVE_NODES = 8000;
	private static final int PORT_TABLE = 8004;

	// Subject node ports
	private static final int PORT_ALIVE_LISTENER = 7000;
	private static final int PORT_REFRESH_TABLE_LISTENER = 7004;
	
	//	Times and periods
	private static final long DELAY_ASK_FOR_ALIVE = 1000;
	private static final long PERIOD_ASK_FOR_ALIVE = 10000;
	private static final long PERIOD_ASK_FOR_ALIVE_AGAIN = 2000;
	private static final int TIMES_TO_ASK_AGAIN = 3;

	//	States
	private static final int STATE_RUNNING = 0;
	private static final int STATE_ROLL_CALLING = 1;

	//	Subject states ("stati del suddito")
//	private static final int SUBJECT_STATE_MISSING = 0;
//	private static final int SUBJECT_STATE_ALIVE = 1;
//	private static final int SUBJECT_STATE_DEAD = 2;
	private enum SubjectState {
		SUBJECT_STATE_MISSING, 
		SUBJECT_STATE_ALIVE,
		SUBJECT_STATE_DEAD
	}

	private static int currentState;

	private static int counter = 0;
	private static int node_id = 0;

	//	ServerSockets
	private ServerSocket joinedNodeListener;
	private ServerSocket departedNodeListener;
	private DatagramSocket aliveNodeListener;

	//	Listening threads
	private JoinedNodeListenerThread joinedNodeListenerThread;
	private DepartedNodeListenerThread departedNodeListenerThread;
	private AliveNodeListenerThread aliveNodeListenerThread;

	//	Speaking threads
	private AliveAskerThread aliveAskerThread;

	//	Storage and registers
	private Map<Integer, ErraNode> nodes;
	private Map<Integer, SubjectState> rollCallRegister;	// "registro per fare l'appello"
	//FIXME meglio gestirlo con un array? piu' veloce?

	private BootstrapNode() {
		nodes = new HashMap<Integer, BootstrapNode.ErraNode>();
		rollCallRegister = new HashMap<Integer, SubjectState>();
//		for (Map.Entry<Integer, Integer> entry : rollCallRegister.entrySet()) {
//			entry.setValue(SUBJECT_STATE_MISSING);
//		}
		/*
		 * per il testing
		 */
		populateForTesting();
		//////// TODO remove me

		currentState = STATE_RUNNING;
		
		joinedNodeListenerThread = new JoinedNodeListenerThread();
		departedNodeListenerThread = new DepartedNodeListenerThread();
		aliveNodeListenerThread = new AliveNodeListenerThread();
//		aliveAskerThread = new AliveAskerThread();
	}	// BootstrapNode()

	public static void main(String[] args) {
		BootstrapNode bootstrapNode = new BootstrapNode();
		bootstrapNode.runBootstrap();
	}	// main()

	private void runBootstrap() {
		joinedNodeListenerThread.start();
		departedNodeListenerThread.start();
		aliveNodeListenerThread.start();
		Timer timer = new Timer();
		TimerTask task = new AliveAskerTask();
		timer.schedule(task, DELAY_ASK_FOR_ALIVE, PERIOD_ASK_FOR_ALIVE);
	}

	//	W@erraid@numeronodiattivinellarete@ip#erraid%ip#erraid%ip#erraid%...%
	private String getNodesMapToString() {
		String mapToString = String.valueOf(nodes.size()) + "@";
		for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			mapToString += String.valueOf(currentNode.getID()) + "#" + currentNode.getIP_ADDRESS() + "%";
		}
		return mapToString;
	}

	private class AliveAskerTask extends TimerTask {

		@Override
		public void run() {
			aliveAskerThread = new AliveAskerThread();
			aliveAskerThread.start();
		}
	}

	/*
	 * ****************************************************************************
	 * START THREADS
	 */

	/*
	 * 	Listening Threads
	 */

	private class JoinedNodeListenerThread extends Thread {

		public JoinedNodeListenerThread() {
			super();
			try {
				joinedNodeListener = new ServerSocket(PORT_JOINED_NODE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			//			logger.log(Level.INFO, "Avviato JoinedNodeListenerThread");
			System.out.println("Avviato JoinedNodeListenerThread");

			while (true) {
				super.run();
				try {
					Socket newSocket = joinedNodeListener.accept();
					AddNodeThread addNodeThread = new AddNodeThread(newSocket);
					addNodeThread.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}	// JoinedNodeListenerThread

	private class DepartedNodeListenerThread extends Thread {

		public DepartedNodeListenerThread() {
			super();
			try {
				departedNodeListener = new ServerSocket(PORT_DEPARTED_NODE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			//			logger.log(Level.INFO, "Avviato DepartedNodeListenerThread");
			System.out.println("Avviato DepartededNodeListenerThread");

			while (true) {
				try {
					Socket socket = departedNodeListener.accept();
					RemoveNodeThread deleteNodeThread = new RemoveNodeThread(socket);
					deleteNodeThread.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}	
	}	// DepartedNodeListenerThread

	private class AliveNodeListenerThread extends Thread {

		public AliveNodeListenerThread() {
			super();
			try {
				aliveNodeListener = new DatagramSocket(PORT_ALIVE_NODE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			System.out.println("Avviato AliveNodeListenerThread");
			byte[] receiverBuffer = new byte[10];
			while (true) {
				try {
					DatagramPacket receivedPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
					System.out.println("(AliveNodeListenerThread) Sto aspettando che qualcuna dica di esserci...");
					aliveNodeListener.receive(receivedPacket);
					NotifiedAliveNodeThread notifiedAliveNodeThread = new NotifiedAliveNodeThread(receivedPacket);
					notifiedAliveNodeThread.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}	// AliveNodeListenerThread

	/*
	 * Speaking Threads
	 */

	private class AliveAskerThread extends Thread {

		private int threadId = (new Random()).nextInt(1000);

		@Override
		public void run() {
			super.run();
			System.out.println("Nuovo AliveAskerThread con id: " + threadId);
			currentState = STATE_ROLL_CALLING;
			for (Map.Entry<Integer, SubjectState> entry : rollCallRegister.entrySet()) {
				entry.setValue(SubjectState.SUBJECT_STATE_MISSING);
			}
			try {
				DatagramSocket datagramSocket = new DatagramSocket(PORT_ASK_ALIVE_NODES);
				DatagramPacket datagramPacket;
				byte[] msg = (new String("?")).getBytes();
				for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
					ErraNode currentNode = entry.getValue();
					datagramPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(currentNode.getIP_ADDRESS()), PORT_ALIVE_LISTENER);
					datagramSocket.send(datagramPacket);
				}

				int rollCallingCounter = TIMES_TO_ASK_AGAIN;
				boolean stillMissingNodes = true;
				List<Integer> missingNodes = new LinkedList<Integer>();
				while (rollCallingCounter > 0 && stillMissingNodes) {
					System.out.println("AliveAskerThread con id: " + threadId + " sta aspettando per vedere se i nodi che mancano rispondono...");	//TODO remove me
					Thread.sleep(PERIOD_ASK_FOR_ALIVE_AGAIN);
					for (Map.Entry<Integer, SubjectState> registerEntry : rollCallRegister.entrySet()) {
						if (registerEntry.getValue() == SubjectState.SUBJECT_STATE_MISSING && !missingNodes.contains(registerEntry.getKey())) {
							missingNodes.add(registerEntry.getKey());
						}
					}

					if (missingNodes.isEmpty()) {
						stillMissingNodes = false;
						System.out.println("Non mancano nodi. (Thread: " + threadId + ")"); // TODO remove me!!!
					} else {
						System.out.println("Ci sono nodi mancanti! (Thread: " + threadId + ")"); // TODO remove me!!!
						for (Iterator<Integer> iterator = missingNodes.iterator(); iterator.hasNext();) {
							Integer missingNodeID = (Integer)iterator.next();
							ErraNode missingNode = nodes.get(missingNodeID);
							datagramPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(missingNode.getIP_ADDRESS()), PORT_ALIVE_LISTENER);
							datagramSocket.send(datagramPacket);
							rollCallingCounter--;
						}
					}
				}

				datagramSocket.close();

				if (stillMissingNodes) {
					for (Iterator<Integer> iterator = missingNodes.iterator(); iterator.hasNext();) {
						Integer missingNodeID = (Integer)iterator.next();
//						rollCallRegister.put(missingNodeID, SubjectState.SUBJECT_STATE_DEAD);
						removeNodeFromRegister(missingNodeID);	//TODO choose what to do!
						ErraNode removedNode = removeErraNode(missingNodeID);
						spreadNetworkChanges(removedNode, false);
						System.out.println("Perso nodo " + missingNodeID + "!");
					}
				}

				currentState = STATE_RUNNING;
				
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}	// AliveAskerThread

	/*
	 * Action Threads
	 */

	private class AddNodeThread extends Thread {

		private Socket socket;

		public AddNodeThread(Socket newSocket) {
			super();
			socket = newSocket;
		}

		@Override
		public void run() {
			super.run();
			String msgFromNode = null;
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				msgFromNode = bufferedReader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (msgFromNode == null || msgFromNode.length() == 0 || !msgFromNode.equalsIgnoreCase("J")) {
				System.out.println("Il messaggio del client che chiede di aggiungersi alla rete e' vuoto o diverso da \'J\'");
			} else {

				InetAddress inetAddress = socket.getInetAddress();
				byte[] address = inetAddress.getAddress();
				if (address.length <= 0) {
					//	TODO lanciare un'eccezione perche' ho ricevuto un indirizzo vuoto!
				} else {
					String ipString = String.valueOf(address[0]);
					for (int i = 1; i < address.length; i++) {
						ipString += ":" + address[i];
					}
					int currentId = node_id++;
					ErraNode node = new ErraNode(currentId, ipString);
					spreadNetworkChanges(node, true);
					addErraNode(node);
					addNodeToRegister(currentId);
					System.out.println("Inserito nodo con id " + currentId);

					try {
						PrintStream toNode = new PrintStream(socket.getOutputStream());
						String table = "W@" + currentId + "@" + getNodesMapToString() + "\n";
						toNode.println(table);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}	
			}
		}	
	}	// AddNodeThread

	private class RemoveNodeThread extends Thread {

		private Socket socket;

		public RemoveNodeThread(Socket newSocket) {
			super();
			socket = newSocket;
		}

		@Override
		public void run() {
			super.run();
			String msgFromNode = null;
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				msgFromNode = bufferedReader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (msgFromNode == null || msgFromNode.length() == 0 || !msgFromNode.substring(0, 1).equalsIgnoreCase("E")) {
				// TODO lanciare un'eccezione perche' ho ricevuto un indirizzo vuoto!
				System.out.println("Il messaggio del client che chiede di aggiungersi alla rete e' vuoto o diverso da \'J\'");
				System.out.println(msgFromNode);
			} else {
				// messaggio nella forma E@erraid\n
				int identifier = Integer.parseInt(msgFromNode.substring(2));
				ErraNode removedNode = removeErraNode(identifier);
				if (removedNode != null) {
					System.out.println("Nodo " + identifier + " rimosso dalla rete");
					spreadNetworkChanges(removedNode, false);
					removeNodeFromRegister(identifier);
					//rollCallRegister.put(identifier, SUBJECT_STATE_DEAD);
				} else {
					System.out.println("Il nodo " + identifier + " non e' presente nella rete");
					//					TODO devo avvertire tutti i nodi che il nodo corrente ha lasciato la rete
				}
			}
		}	
	}	// DeleteNodeThread

	private class NotifiedAliveNodeThread extends Thread {

		private DatagramPacket datagramPacket;

		public NotifiedAliveNodeThread(DatagramPacket newDatagramPacket) {
			super();
			datagramPacket = newDatagramPacket;
		}

		@Override
		public void run() {
			super.run();
			String msgFromNode = null;
			//Message in the form: !@erraid
			msgFromNode = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
			if (msgFromNode == null || msgFromNode.length() == 0 || !msgFromNode.substring(0, 1).equalsIgnoreCase("!")) {
				// TODO lanciare un'eccezione perche' ho ricevuto un indirizzo vuoto!
				System.out.println("Il messaggio del client che risponde di essere attivo e' vuoto o diverso da \'!\'");
				System.out.println(msgFromNode);
			} else {
				String[] msgValues = msgFromNode.split("@");
				int identifier = Integer.parseInt(msgValues[1]);
				System.out.println("(NotifiedAliveNodeThread) Il suddito " + identifier + " ha risposto che e' vivo!");
				addNodeToRegister(identifier);
			}
		}
	}	// NotifiedAliveNodeThread

	/*
	 * ****************************************************************************
	 * END THREADS
	 */

	/**
	 * Un esemplare di questa classe e' un bean che contiene tutte le informazioni su ogni nodo entrato nella rete.
	 * Creo questi oggetti solo per salvarli nel database in modo compatto
	 * @author martinit
	 *
	 */
	private class ErraNode {

		private final int ID;
		private final String IP_ADDRESS;

		public ErraNode(int id, String ip) {
			ID = id;
			IP_ADDRESS = ip;
		}

		public int getID() {
			return ID;
		}

		public String getIP_ADDRESS() {
			return IP_ADDRESS;
		}
	}

	private void populateForTesting() {
		ErraNode node = new ErraNode(18, "127.0.0.1");
		nodes.put(18, node);
		rollCallRegister.put(18, SubjectState.SUBJECT_STATE_ALIVE);
	}

	/*
	 * Synchronized operations on data registers
	 */
	private synchronized void addErraNode(ErraNode erraNode) {
		nodes.put(erraNode.getID(), erraNode);
	}

	private synchronized ErraNode removeErraNode(int erraNodeID) {
		return nodes.remove(erraNodeID);
	}

	private synchronized void addNodeToRegister(int erraNodeID) {
		System.out.println("Qualcuno sta modificando il registro!");
		rollCallRegister.put(erraNodeID, SubjectState.SUBJECT_STATE_ALIVE);
	}

	private synchronized SubjectState removeNodeFromRegister(int erraNodeID) {
		System.out.println("Qualcuno sta modificando il registro!");
		return rollCallRegister.remove(erraNodeID);
	}
	
	private synchronized void spreadNetworkChanges(ErraNode changedNode, boolean added) {
		String msg = "T@";
		if (added) {
			msg += "+";
		} else {
			msg += "-";
		}
		msg += changedNode.getID();
		for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			try {
				Socket socket = new Socket(InetAddress.getByName(currentNode.getIP_ADDRESS()), PORT_REFRESH_TABLE_LISTENER, InetAddress.getLocalHost(), PORT_TABLE);
				PrintStream toNode = new PrintStream(socket.getOutputStream());
				toNode.println(msg);
				socket.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}