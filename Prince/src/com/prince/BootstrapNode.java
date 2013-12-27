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
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

/* Database:
 * prova
nome: erra
username: principe
password: principe

sudo apt-get install libmysql-java
C:\Users\Mattia\Desktop\Google Chrome.lnk
 */

public class BootstrapNode {
	
	//	Times and periods
	private static final long DELAY_ASK_FOR_ALIVE = 1000;
	private static final long PERIOD_ASK_FOR_ALIVE = 10000;
	private static final long PERIOD_ASK_FOR_ALIVE_AGAIN = 2000;
	private static final int TIMES_TO_ASK_AGAIN = 3;
	private static final long DELAY_WAIT_FOR_CALLING_TO_FINISH = 1000;	// if I have to update the tables and the Bootstrap is not on "running" mode I'll wait for this time before attempting again to access tables
	
	private static final String BOOTSTRAP_PASSWORD = "lupo";
	
	//	States
	private enum BootstrapState {
		STATE_RUNNING,
		STATE_ROLL_CALLING,
		STATE_SPREADING_CHANGES
	}

	//	Subject states ("stati del suddito")
	private enum SubjectState {
		SUBJECT_STATE_MISSING, 
		SUBJECT_STATE_ALIVE,
		SUBJECT_STATE_DEAD
	}

	private static BootstrapState currentState;
	
	private InetAddress myIPAddress;

	private static int node_id = 0;	// increasing ID assigned to every new node joining the network

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
	
	private NodeViewer nodeViewer;

	private BootstrapNode() {
		try {
			myIPAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}	
		nodes = new HashMap<Integer, ErraNode>();
		rollCallRegister = new HashMap<Integer, SubjectState>();

		populateForTesting();	// TODO remove me, just for testing

		currentState = BootstrapState.STATE_RUNNING;
		
		joinedNodeListenerThread = new JoinedNodeListenerThread();
		departedNodeListenerThread = new DepartedNodeListenerThread();
		aliveNodeListenerThread = new AliveNodeListenerThread();
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
		
		String msgFromKeyboard;
		while (true) {
			System.out.print("input$: ");
			Scanner scanner = new Scanner(System.in);
			msgFromKeyboard = scanner.nextLine();
			if (msgFromKeyboard.equalsIgnoreCase("shutdown")) {
				System.out.print("Insert password to shutdown the current Bootsrap Node: ");
				String password = (new Scanner(System.in)).nextLine();
				if (password.equalsIgnoreCase(BOOTSTRAP_PASSWORD)) {
					System.out.println("Correct password.\nThe Bootstrap Node will be disconnected...");
					shutdown();	// TODO prepare for closing...
					System.out.println("...bye!");
					System.exit(0);
				} else {
					System.out.println("Wrong password");
				}
			} else if (msgFromKeyboard.equalsIgnoreCase("net")) {
				showNetworkTable();
			} else {
				System.out.println("Echo-> " + msgFromKeyboard);
			}
		}
	}

	private class AliveAskerTask extends TimerTask {

		@Override
		public void run() {
			if (!nodes.isEmpty()) {
				aliveAskerThread = new AliveAskerThread();
				aliveAskerThread.start();
			} else {
				System.out.println("Non ci sono nodi nella rete, non invio alcuna richiesta");
			}
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
				joinedNodeListener = new ServerSocket(ErraNodePorts.PORT_BOOTSTRAP_JOINED_NODE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			while (true) {
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
				departedNodeListener = new ServerSocket(ErraNodePorts.PORT_BOOTSTRAP_DEPARTED_NODE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
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
				aliveNodeListener = new DatagramSocket(ErraNodePorts.PORT_BOOTSTRAP_ALIVE_NODE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			byte[] receiverBuffer = new byte[10];
			while (true) {
				try {
					DatagramPacket receivedPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
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
			currentState = BootstrapState.STATE_ROLL_CALLING;
			for (Map.Entry<Integer, SubjectState> entry : rollCallRegister.entrySet()) {
				entry.setValue(SubjectState.SUBJECT_STATE_MISSING);
			}
			try {
				DatagramSocket datagramSocket = new DatagramSocket(ErraNodePorts.PORT_BOOTSTRAP_ASK_ALIVE_NODES);
				DatagramPacket datagramPacket;
				byte[] msg = (new String("?")).getBytes();
				for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
					ErraNode currentNode = entry.getValue();
					datagramPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(currentNode.getIP_ADDRESS()), ErraNodePorts.PORT_SUBJECT_ALIVE_LISTENER);
					datagramSocket.send(datagramPacket);
				}

				int rollCallingCounter = TIMES_TO_ASK_AGAIN;
				boolean stillMissingNodes = true;
				List<Integer> missingNodes = new LinkedList<Integer>();
				while (rollCallingCounter > 0 && stillMissingNodes) {
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
						int numRqst = TIMES_TO_ASK_AGAIN - rollCallingCounter + 1;
						System.out.println("Alcuni nodi non hanno ancora risposto! Invio richiesta bis numero: " + numRqst + "/" + TIMES_TO_ASK_AGAIN + " (Thread: " + threadId + ")"); // TODO remove me!!!
						for (Iterator<Integer> iterator = missingNodes.iterator(); iterator.hasNext();) {
							Integer missingNodeID = (Integer)iterator.next();
							ErraNode missingNode = nodes.get(missingNodeID);
							datagramPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(missingNode.getIP_ADDRESS()), ErraNodePorts.PORT_SUBJECT_ALIVE_LISTENER);
							datagramSocket.send(datagramPacket);
						}
						rollCallingCounter--;
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

				currentState = BootstrapState.STATE_RUNNING;
				
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
				System.err.println("Il messaggio del client che chiede di aggiungersi alla rete e' vuoto o diverso da \'J\'");
			} else {

				InetAddress inetAddress = socket.getInetAddress();
				String ipString = inetAddress.getHostAddress();
				if (ipString.length() <= 0) {
					//	TODO lanciare un'eccezione perche' ho ricevuto un indirizzo vuoto!
				} else {
					int currentId = node_id++;
					ErraNode node = new ErraNode(currentId, ipString);
					while (currentState != BootstrapState.STATE_RUNNING) {
						try {
							sleep(DELAY_WAIT_FOR_CALLING_TO_FINISH);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
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
				while (currentState != BootstrapState.STATE_RUNNING) {
					try {
						sleep(DELAY_WAIT_FOR_CALLING_TO_FINISH);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				ErraNode removedNode = removeErraNode(identifier);
				if (removedNode != null) {
					System.out.println("Nodo " + identifier + " rimosso dalla rete");
//					spreadNetworkChanges(removedNode, false);
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
				System.out.println("Il messaggio del client che risponde di essere attivo e' vuoto o diverso da \'!\'");
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

	private void populateForTesting() {
		ErraNode node1 = new ErraNode(18, "127.0.0.1");
		ErraNode node2 = new ErraNode(63, "127.0.0.2");
		ErraNode node3 = new ErraNode(92, "127.0.0.3");
		ErraNode node4 = new ErraNode(99, "127.0.0.4");
		ErraNode node5 = new ErraNode(13, "127.0.0.5");
		ErraNode node6 = new ErraNode(43, "127.0.0.6");
		nodes.put(18, node1);
		rollCallRegister.put(18, SubjectState.SUBJECT_STATE_ALIVE);
		nodes.put(63, node2);
		rollCallRegister.put(63, SubjectState.SUBJECT_STATE_ALIVE);
		nodes.put(92, node3);
		rollCallRegister.put(92, SubjectState.SUBJECT_STATE_ALIVE);
		nodes.put(99, node4);
		rollCallRegister.put(99, SubjectState.SUBJECT_STATE_ALIVE);
		nodes.put(13, node5);
		rollCallRegister.put(13, SubjectState.SUBJECT_STATE_ALIVE);
		nodes.put(43, node6);
		rollCallRegister.put(43, SubjectState.SUBJECT_STATE_ALIVE);
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
	
	private void showNetworkTable() {
		nodeViewer = new NodeViewer();
		nodeViewer.showNetwork(nodes);
	}
	
	private boolean shutdown() {
		return true;
	}

	/*
	 * Synchronized operations on data registers
	 */	
	private synchronized void addErraNode(ErraNode erraNode) {
		nodes.put(erraNode.getID(), erraNode);
//		rollCallRegister.put(erraNode.getID(), SubjectState.SUBJECT_STATE_ALIVE);
	}

	private synchronized ErraNode removeErraNode(int erraNodeID) {
		return nodes.remove(erraNodeID);
	}

	private synchronized void addNodeToRegister(int erraNodeID) {
		rollCallRegister.put(erraNodeID, SubjectState.SUBJECT_STATE_ALIVE);
	}

	private synchronized SubjectState removeNodeFromRegister(int erraNodeID) {
		return rollCallRegister.remove(erraNodeID);
	}
	
	private synchronized void spreadNetworkChanges(ErraNode changedNode, boolean added) {
		String msg = "T@";
		if (added) {
			msg += "+";
		} else {
			msg += "-";
		}
		msg += changedNode.getID() + "#" + changedNode.getIP_ADDRESS();
		for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			try {
				Socket socket = new Socket(InetAddress.getByName(currentNode.getIP_ADDRESS()), ErraNodePorts.PORT_SUBJECT_REFRESH_TABLE_LISTENER, InetAddress.getLocalHost(), ErraNodePorts.PORT_BOOTSTRAP_REFRESH_TABLE);
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