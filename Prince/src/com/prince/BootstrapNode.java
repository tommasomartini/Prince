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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import com.prince.ErraNode.NodeState;
import com.prince.ErraNode.NodeType;

/* Database:
 * prova
nome: erra
username: principe
password: principe

sudo apt-get install libmysql-java
 */

public class BootstrapNode extends erraClient

{

	private boolean DEBUG = false;
	private boolean ACTIVE_ALIVE_RQST = false;

	//	Times and periods in milliseconds
	private static final long DELAY_ASK_FOR_ALIVE = 1000 * 1;	// seconds
	private static final long PERIOD_ASK_FOR_ALIVE = 1000 * 20;
	private static final long PERIOD_ASK_FOR_ALIVE_AGAIN = 1000 * 3;
	private static final int TIMES_TO_ASK_AGAIN = 3;
	private static final long DELAY_WAIT_FOR_CALLING_TO_FINISH = 1000 * 1;	// if I have to update the tables and the Bootstrap is not on "running" mode I'll wait for this time before attempting again to access tables

	private static final String BOOTSTRAP_PASSWORD = "lupo";

	//	States
	private enum BootstrapState
	{
		STATE_RUNNING,
		STATE_ROLL_CALLING,
		STATE_SPREADING_CHANGES,
		STATE_SHUTTING_DOWN
	}
	private static BootstrapState currentState;

	private ErraNode me;
	private static int newNodeID = 0;	// increasing ID assigned to every new node joining the network

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
	private Map<Integer, ErraNode.NodeState> rollCallRegister;	// "registro per fare l'appello"

	private NodeViewer nodeViewer;

	private BootstrapNode() {
		try {
			String myIPAddress = InetAddress.getLocalHost().getHostAddress();
			int myErraID = new Random().nextInt(1000);
			me = new ErraNode(myErraID, myIPAddress, NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}	
		nodes = new HashMap<Integer, ErraNode>();
		rollCallRegister = new HashMap<Integer, NodeState>();

		if (DEBUG) {
			populateForTesting();
		}

		nodeViewer = new NodeViewer();

		currentState = BootstrapState.STATE_RUNNING;

		joinedNodeListenerThread = new JoinedNodeListenerThread();
		departedNodeListenerThread = new DepartedNodeListenerThread();
		aliveNodeListenerThread = new AliveNodeListenerThread();
	}	// BootstrapNode()

	public static void main(String[] args) {
		// ErraClient functions
		answerAliveRequest A=new answerAliveRequest();
		A.start();
		FM = new fileManager();
		listenToForward f = new listenToForward();
		f.start();

		BootstrapNode bootstrapNode = new BootstrapNode();
		bootstrapNode.runBootstrap();
	}	// main()

	private void runBootstrap() {

		System.out.println("#Bootstrap " + me.getID() + " activated. Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
		joinedNodeListenerThread.start();
		departedNodeListenerThread.start();
		aliveNodeListenerThread.start();
		Timer timer = new Timer();
		TimerTask task = new AliveAskerTask();
		if (ACTIVE_ALIVE_RQST) {
			timer.schedule(task, DELAY_ASK_FOR_ALIVE, PERIOD_ASK_FOR_ALIVE);
		}

		String msgFromKeyboard;
		while (true) {
			System.out.print("input$: ");
			Scanner scanner = new Scanner(System.in);
			msgFromKeyboard = scanner.nextLine();
			if (msgFromKeyboard.equalsIgnoreCase("shutdown")) {
				System.out.print("Insert password to shutdown the current Bootsrap Node: ");
				Scanner scanner2 = new Scanner(System.in);
				String password = scanner2.nextLine();
				if (password.equalsIgnoreCase(BOOTSTRAP_PASSWORD)) {
					System.out.println("Correct password.\nThis Bootstrap Node will be disconnected...");
					shutdown();
					scanner.close();
					System.out.println("...bye!");
					System.exit(0);
				} else {
					System.out.println("Wrong password!");
				}
				scanner2.close();
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
			for (Map.Entry<Integer, NodeState> entry : rollCallRegister.entrySet()) {
				updateRegister(entry.getKey(), NodeState.NODE_STATE_MISSING);
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
					System.out.println("...in attesa...");
					Thread.sleep(PERIOD_ASK_FOR_ALIVE_AGAIN);
					System.out.println("...controllo che nodi hanno risposto...");
					for (Map.Entry<Integer, NodeState> registerEntry : rollCallRegister.entrySet()) {
						if (registerEntry.getValue() == NodeState.NODE_STATE_MISSING && !missingNodes.contains(registerEntry.getKey())) {
							missingNodes.add(registerEntry.getKey());
						} else if (registerEntry.getValue() == NodeState.NODE_STATE_ALIVE && missingNodes.contains(registerEntry.getKey())) {
							missingNodes.remove(registerEntry.getKey());
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
							System.out.println("Missing node: " + missingNodeID);
							datagramPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(missingNode.getIP_ADDRESS()), ErraNodePorts.PORT_SUBJECT_ALIVE_LISTENER);
							datagramSocket.send(datagramPacket);
						}
						rollCallingCounter--;

						//						da correggere. messo qua perche funzioni
						for (Map.Entry<Integer, NodeState> registerEntry : rollCallRegister.entrySet()) {
							if (registerEntry.getValue() == NodeState.NODE_STATE_MISSING && !missingNodes.contains(registerEntry.getKey())) {
								missingNodes.add(registerEntry.getKey());
							} else if (registerEntry.getValue() == NodeState.NODE_STATE_ALIVE && missingNodes.contains(registerEntry.getKey())) {
								missingNodes.remove(registerEntry.getKey());
							}
						}

						if (missingNodes.isEmpty()) {
							stillMissingNodes = false;
							System.out.println("Non mancano nodi. (Thread: " + threadId + ")"); // TODO remove me!!!
						}
						//						
					}
				}

				datagramSocket.close();

				if (stillMissingNodes) {
					ErraNode[] deadNodes = new ErraNode[missingNodes.size()];
					int indexMissingNodes = 0;
					for (Iterator<Integer> iterator = missingNodes.iterator(); iterator.hasNext();) {
						Integer missingNodeID = (Integer)iterator.next();
						deadNodes[indexMissingNodes] = removeErraNode(missingNodeID);
						System.out.println("Perso nodo " + missingNodeID + "!");
						indexMissingNodes++;
					}
					spreadNetworkChanges(deadNodes, false);
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
				for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
					if (ipString.equalsIgnoreCase(entry.getValue().getIP_ADDRESS())) {
						try {
							PrintStream toNode = new PrintStream(socket.getOutputStream());
							String table = "W@" + getNodesMapToString() + "\n";
							toNode.println(table);
							toNode.close();
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						return;
					}
				}
				int currentId = newNodeID++;
				ErraNode node = new ErraNode(currentId, ipString, NodeType.UNKNOWN, NodeState.NODE_STATE_ALIVE);
				while (currentState != BootstrapState.STATE_RUNNING) {
					try {
						sleep(DELAY_WAIT_FOR_CALLING_TO_FINISH);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				spreadNetworkChanges(new ErraNode[]{node}, true);
				addErraNode(node);
				System.out.println("Inserito nodo con id " + currentId);	// TODO remove me
				try {
					PrintStream toNode = new PrintStream(socket.getOutputStream());
					String table = "W@" + currentId + "@" + getNodesMapToString() + "\n";
					toNode.println(table);
					toNode.close();
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
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
				System.out.println("Il messaggio del client che chiede di aggiungersi alla rete e' vuoto o diverso da \'J\'");
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
					spreadNetworkChanges(new ErraNode[]{removedNode}, false);
				} else {
					System.out.println("Il nodo " + identifier + " non e' presente nella rete");
				}
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
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
				updateRegister(identifier, NodeState.NODE_STATE_ALIVE);
			}
		}
	}	// NotifiedAliveNodeThread

	/*
	 * ****************************************************************************
	 * END THREADS
	 */

	private void populateForTesting() {
		int numOfNodes = 20;
		for (int i = 0; i < numOfNodes; i++) {
			ErraNode node = new ErraNode(newNodeID++, "127.0.0." + String.valueOf(new Random().nextInt(255)), NodeType.NODE_TYPE_SUBJECT, NodeState.NODE_STATE_ALIVE);
			nodes.put(node.getID(), node);
			rollCallRegister.put(node.getID(), NodeState.NODE_STATE_ALIVE);
		}
	}

	//	W@erraid@numeronodiattivinellarete@ip#erraid%ip#erraid%ip#erraid%...%
	private String getNodesMapToString() {
		String mapToString = String.valueOf(nodes.size() + 1) + "@";
		for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			mapToString += currentNode.getIP_ADDRESS() + "#" + String.valueOf(currentNode.getID()) + "%";
		}
		mapToString += me.getIP_ADDRESS() + "#" + me.getID() + "%";	// add me
		return mapToString;
	}

	private void showNetworkTable() {
		nodeViewer.showNetwork(nodes, me);
	}

	private boolean shutdown() {
		while (currentState != BootstrapState.STATE_RUNNING) {
		}
		currentState = BootstrapState.STATE_SHUTTING_DOWN;
		// TODO avverto gli altri principi (e gli altri sudditi)
		System.out.println("...(closing operations)...");
		return true;
	}
	
/*
 * Convert from Bootstrap structure to ErraClient structure
 */
	private void convertNetworkStructure() {
		topology.clear();
		for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			erraHost currentErraHost = new erraHost(currentNode.getIP_ADDRESS(), String.valueOf(currentNode.getID()));
			topology.add(currentErraHost);
		}
	}

	/*
	 * Synchronized operations on data registers
	 */	
	private synchronized void addErraNode(ErraNode erraNode) {
		nodes.put(erraNode.getID(), erraNode);
		rollCallRegister.put(erraNode.getID(), NodeState.NODE_STATE_ALIVE);	// update rollCallRegister too
		showNetworkTable();
		convertNetworkStructure();
	}

	private synchronized ErraNode removeErraNode(int erraNodeID) {
		rollCallRegister.remove(erraNodeID);
		ErraNode removedNode = nodes.remove(erraNodeID);
		showNetworkTable();
		convertNetworkStructure();
		return removedNode;
	}

	private synchronized void updateRegister(int erraNodeID, NodeState nodeState) {
		rollCallRegister.put(erraNodeID, nodeState);
	}

	// T@+[-]erraID#erraIP%erraID#erraIP%erraID#erraIP%...
	private synchronized void spreadNetworkChanges(ErraNode[] changedNodes, boolean added) {
		currentState = BootstrapState.STATE_SPREADING_CHANGES;
		String msg = "T@";
		if (added) {
			msg += "+";
		} else {
			msg += "-";
		}
		for (int i = 0; i < changedNodes.length; i++) {
			ErraNode changedNode = changedNodes[i];
			msg += changedNode.getID() + "#" + changedNode.getIP_ADDRESS() + "%";
		}

		Socket socket;
		for(Map.Entry<Integer, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			try {
				socket = new Socket(InetAddress.getByName(currentNode.getIP_ADDRESS()), ErraNodePorts.PORT_SUBJECT_REFRESH_TABLE_LISTENER);
				PrintStream toNode = new PrintStream(socket.getOutputStream());
				toNode.println(msg);
				toNode.close();
				socket.close();
				while(!socket.isClosed()){

				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		currentState = BootstrapState.STATE_RUNNING;
	}
}