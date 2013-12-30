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
import java.util.Timer;
import java.util.TimerTask;

import com.prince.ErraNode.NodeState;
import com.prince.ErraNode.NodeType;

public class PrinceNode {

	private boolean DEBUG = false;
	private boolean ACTIVE_ALIVE_RQST = true;

	//	Times and periods in milliseconds
	private static final long DELAY_ASK_FOR_ALIVE = 1000 * 3;	// seconds
	private static final long PERIOD_ASK_FOR_ALIVE = 1000 * 15;
	private static final long PERIOD_ASK_FOR_ALIVE_AGAIN = 1000 * 2;
	private static final int TIMES_TO_ASK_AGAIN = 3;
	private static final long DELAY_WAIT_FOR_CALLING_TO_FINISH = 1000 * 1;	// if I have to update the tables and the Bootstrap is not on "running" mode I'll wait for this time before attempting again to access tables

//	private static final String BOOTSTRAP_PASSWORD = "lupo";

	//	States
	private enum PrinceState {
		STATE_RUNNING,
		STATE_ROLL_CALLING,
		STATE_SPREADING_CHANGES,
		STATE_SHUTTING_DOWN
	}
	private static PrinceState currentState;

	private ErraNode me;

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

	private Map<String, ErraNode> nodes;
	private Map<String, ErraNode.NodeState> rollCallRegister;	// "registro per fare l'appello"

	private NodeViewer nodeViewer;

	private PrinceNode() {
		try {
//			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
//			while(networkInterfaces.hasMoreElements()) {
//			    NetworkInterface networkInterface = (NetworkInterface)networkInterfaces.nextElement();
//			    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
//			    while(inetAddresses.hasMoreElements()) {
//			        InetAddress currentInetAddress = (InetAddress)inetAddresses.nextElement();
//			        System.out.println(networkInterface.getName() + currentInetAddress.getHostAddress());
//			    }
//			}
			String myIPAddress = InetAddress.getLocalHost().getHostAddress();
			me = new ErraNode(myIPAddress, NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}	
		nodes = new HashMap<String, ErraNode>();
		rollCallRegister = new HashMap<String, NodeState>();

		if (DEBUG) {
			populateForTesting();
		}

		nodeViewer = new NodeViewer();

		currentState = PrinceState.STATE_RUNNING;

		joinedNodeListenerThread = new JoinedNodeListenerThread();
		departedNodeListenerThread = new DepartedNodeListenerThread();
		aliveNodeListenerThread = new AliveNodeListenerThread();
	}	// BootstrapNode()

	public static void main(String[] args) {
		// ErraClient functions	TODO attivare queste funzioni!
//		answerAliveRequest A=new answerAliveRequest();
//		A.start();
//		FM = new fileManager();
//		listenToForward f = new listenToForward();
//		f.start();

		PrinceNode princeNode = new PrinceNode();
		princeNode.runPrinceNode();
	}	// main()

	private void runPrinceNode() {
		System.out.println("#Prince " + me.getIPAddress() + " activated. Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()));
		nodeViewer.showNetwork(nodes, me);
		joinedNodeListenerThread.start();
		departedNodeListenerThread.start();
		aliveNodeListenerThread.start();
		Timer timer = new Timer();
		TimerTask task = new AliveAskerTask();
		if (ACTIVE_ALIVE_RQST) {
			timer.schedule(task, DELAY_ASK_FOR_ALIVE, PERIOD_ASK_FOR_ALIVE);
		}

		/*
		 * Disattivo gli input da tastiera per il momento 
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
		 */
	}

	private class AliveAskerTask extends TimerTask {

		@Override
		public void run() {
			if (!nodes.isEmpty()) {
				aliveAskerThread = new AliveAskerThread();
				aliveAskerThread.start();
			} else {
				System.out.println("No nodes in the network, I won't send any alive request.");
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
				joinedNodeListener = new ServerSocket(ErraNodeVariables.PORT_PRINCE_JOINED_NODE);
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
				departedNodeListener = new ServerSocket(ErraNodeVariables.PORT_PRINCE_DEPARTED_NODE);
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
				aliveNodeListener = new DatagramSocket(ErraNodeVariables.PORT_PRINCE_ALIVE_NODE);
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
			updatePrinceState(PrinceState.STATE_ROLL_CALLING);
			for (Map.Entry<String, NodeState> entry : rollCallRegister.entrySet()) {
				updateRegister(entry.getKey(), NodeState.NODE_STATE_MISSING);
			}
			try {
				DatagramSocket datagramSocket = new DatagramSocket(ErraNodeVariables.PORT_PRINCE_ASK_ALIVE_NODES);
				DatagramPacket datagramPacket;
				byte[] msg = (new String(ErraNodeVariables.MSG_PRINCE_ALIVE_REQUEST)).getBytes();
				for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
					ErraNode currentNode = entry.getValue();
					datagramPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(currentNode.getIPAddress()), ErraNodeVariables.PORT_SUBJECT_ALIVE_LISTENER);
					datagramSocket.send(datagramPacket);
				}
				int rollCallingCounter = TIMES_TO_ASK_AGAIN;
				boolean stillMissingNodes = true;
				List<String> missingNodes = new LinkedList<String>();
				while (rollCallingCounter >= 0 && stillMissingNodes) {
					System.out.println("Waiting for alive answers...");
					Thread.sleep(PERIOD_ASK_FOR_ALIVE_AGAIN);
					for (Map.Entry<String, NodeState> registerEntry : rollCallRegister.entrySet()) {
						if (registerEntry.getValue() == NodeState.NODE_STATE_MISSING && !missingNodes.contains(registerEntry.getKey())) {
							missingNodes.add(registerEntry.getKey());
						} else if (registerEntry.getValue() == NodeState.NODE_STATE_ALIVE && missingNodes.contains(registerEntry.getKey())) {
							missingNodes.remove(registerEntry.getKey());
						}
					}

					if (missingNodes.isEmpty()) {
						stillMissingNodes = false;
						System.out.println("No missing nodes. (Thread: " + threadId + ")"); // TODO remove me!!!
					} else if (rollCallingCounter > 0) {	// send again
						int numRqst = TIMES_TO_ASK_AGAIN - rollCallingCounter + 1;
						System.out.println("Missing nodes! Send request bis number: " + numRqst + "/" + TIMES_TO_ASK_AGAIN + " to the following nodes:"); // TODO remove me!!!
						for (Iterator<String> iterator = missingNodes.iterator(); iterator.hasNext();) {
							String missingNodeIPAddress = (String)iterator.next();
							System.out.println("- " + missingNodeIPAddress);
							datagramPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(missingNodeIPAddress), ErraNodeVariables.PORT_SUBJECT_ALIVE_LISTENER);
							datagramSocket.send(datagramPacket);
						}						
					}
					rollCallingCounter--;
				}

				datagramSocket.close();

				if (stillMissingNodes) {
					ErraNode[] deadNodes = new ErraNode[missingNodes.size()];
					int indexMissingNodes = 0;
					for (Iterator<String> iterator = missingNodes.iterator(); iterator.hasNext();) {
						String missingNodeIPAddress = (String)iterator.next();
						deadNodes[indexMissingNodes] = removeErraNode(missingNodeIPAddress);
						System.out.println("Node " + missingNodeIPAddress + " lost!");
						indexMissingNodes++;
					}
					spreadNetworkChanges(deadNodes, false);
				}

				updatePrinceState(PrinceState.STATE_RUNNING);

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

			if (msgFromNode == null || msgFromNode.length() == 0 || !msgFromNode.equalsIgnoreCase(ErraNodeVariables.MSG_SUBJECT_JOIN_REQUEST)) {
				System.err.println("Il messaggio del client che chiede di aggiungersi alla rete e' vuoto o diverso da \'J\'");
			} else {
				InetAddress inetAddress = socket.getInetAddress();
				String ipString = inetAddress.getHostAddress();
				if (!nodes.containsKey(ipString)) {
					ErraNode node = new ErraNode(ipString, NodeType.NODE_TYPE_SUBJECT, NodeState.NODE_STATE_ALIVE);
					node.setInMyCounty(true);
					node.setBootstrapOwner(me);
					while (currentState != PrinceState.STATE_RUNNING) {
						try {
							sleep(DELAY_WAIT_FOR_CALLING_TO_FINISH);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					spreadNetworkChanges(new ErraNode[]{node}, true);
					addErraNode(node);
				}
				try {
					PrintStream toNode = new PrintStream(socket.getOutputStream());
					String table = ErraNodeVariables.MSG_PRINCE_WELCOME + ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR + getNodesMapToString();
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
			if (msgFromNode == null || msgFromNode.length() == 0 || !msgFromNode.substring(0, 1).equalsIgnoreCase(ErraNodeVariables.MSG_SUBJECT_DEPART_REQUEST)) {
				System.err.println("Il messaggio del client che chiede di aggiungersi alla rete e' vuoto o diverso da \'E\'");
			} else {
				// messaggio nella forma E@erraIP
				String[] segments = msgFromNode.split(ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR);
				String ipAddress = segments[1];
				while (currentState != PrinceState.STATE_RUNNING) {
					try {
						sleep(DELAY_WAIT_FOR_CALLING_TO_FINISH);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				ErraNode removedNode = removeErraNode(ipAddress);
				if (removedNode != null) {
					System.out.println("Node " + ipAddress + " removed from the network.");
					spreadNetworkChanges(new ErraNode[]{removedNode}, false);
				} else {
					System.err.println("Node " + ipAddress + " not in the network.");
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
			this.setName(this.getClass().getName());
		}

		@Override
		public void run() {
			super.run();
			String msgFromNode = null;
			//Message in the form: !@erraIP
			msgFromNode = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
			if (msgFromNode == null || msgFromNode.length() == 0 || !msgFromNode.substring(0, 1).equalsIgnoreCase(ErraNodeVariables.MSG_SUBJECT_ALIVE)) {
				System.err.println("Il messaggio del client che risponde di essere attivo e' vuoto o diverso da \'!\'");
			} else {
				String ipAddress = datagramPacket.getAddress().getHostAddress();
				System.out.println("Subject " + ipAddress + " is alive!");
				updateRegister(ipAddress, NodeState.NODE_STATE_ALIVE);
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
			ErraNode node = new ErraNode("127.0.0." + String.valueOf(new Random().nextInt(255)), NodeType.NODE_TYPE_SUBJECT, NodeState.NODE_STATE_ALIVE);
			nodes.put(node.getIPAddress(), node);
			rollCallRegister.put(node.getIPAddress(), NodeState.NODE_STATE_ALIVE);
		}
	}

	private String getNodesMapToString() {
		String mapToString = "";
		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			mapToString += currentNode.getIPAddress() + ErraNodeVariables.DELIMITER_MSG_PARAMS;
		}
		mapToString += me.getIPAddress() + ErraNodeVariables.DELIMITER_MSG_PARAMS;	// add me
		return mapToString;
	}

	private void showNetworkTable() {
		nodeViewer.showNetwork(nodes, me);
	}
	
	private void updatePrinceState(PrinceState newPrinceState) {
		currentState = newPrinceState;
	}

//	TODO gestire uscita elegante del bootstrap
//	private boolean shutdown() {
//		while (currentState != BootstrapState.STATE_RUNNING) {
//		}
//		currentState = BootstrapState.STATE_SHUTTING_DOWN;
//		// TODO avverto gli altri principi (e gli altri sudditi)
//		System.out.println("...(closing operations)...");
//		return true;
//	}

	/*
	 * Synchronized operations on data registers
	 */	
	private synchronized void addErraNode(ErraNode erraNode) {
		nodes.put(erraNode.getIPAddress(), erraNode);
		rollCallRegister.put(erraNode.getIPAddress(), NodeState.NODE_STATE_ALIVE);	// update rollCallRegister too
		showNetworkTable();
	}

	private synchronized ErraNode removeErraNode(String erraNodeIPAddress) {
		rollCallRegister.remove(erraNodeIPAddress);
		ErraNode removedNode = nodes.remove(erraNodeIPAddress);
		showNetworkTable();
		return removedNode;
	}

	private synchronized void updateRegister(String erraIPAddress, NodeState nodeState) {
		rollCallRegister.put(erraIPAddress, nodeState);
	}

	private synchronized void spreadNetworkChanges(ErraNode[] changedNodes, boolean added) {
		updatePrinceState(PrinceState.STATE_SPREADING_CHANGES);
		String msg = ErraNodeVariables.MSG_PRINCE_TABLE_UPDATE + ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR;
		if (added) {
			msg += "+";
		} else {
			msg += "-";
		}
		for (int i = 0; i < changedNodes.length; i++) {
			ErraNode changedNode = changedNodes[i];
			msg += changedNode.getIPAddress() + ErraNodeVariables.DELIMITER_MSG_PARAMS;
		}
		Socket socket;
		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
			ErraNode currentNode = entry.getValue();
			try {
				socket = new Socket(InetAddress.getByName(currentNode.getIPAddress()), ErraNodeVariables.PORT_SUBJECT_REFRESH_TABLE_LISTENER);
				PrintStream toNode = new PrintStream(socket.getOutputStream());
				toNode.println(msg);
				toNode.close();
				socket.close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		updatePrinceState(PrinceState.STATE_RUNNING);
	}
}