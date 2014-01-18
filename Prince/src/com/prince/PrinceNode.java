package com.prince;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import com.prince.ErraNode.NodeState;
import com.prince.ErraNode.NodeType;

public class PrinceNode extends NewErraClient {

	private boolean ACTIVE_ALIVE_RQST = true;

	//	Times and periods in milliseconds
	private static final long DELAY_ASK_FOR_ALIVE = 1000 * 3;	// seconds
	private static final long PERIOD_ASK_FOR_ALIVE = 1000 * 15;
	private static final long PERIOD_ASK_FOR_ALIVE_AGAIN = 1000 * 2;
	private static final int TIMES_TO_ASK_AGAIN = 3;
	private static final long DELAY_WAIT_FOR_CALLING_TO_FINISH = 1000 * 1;	// if I have to update the tables and the Bootstrap is not on "running" mode I'll wait for this time before attempting again to access tables
	private static final long INITIALIZATION_PERIOD = 1000 * 30; // initialization duration

	//	private static final String BOOTSTRAP_PASSWORD = "lupo";

	//	States
	private enum PrinceState {
		STATE_RUNNING,
		STATE_ROLL_CALLING,
		STATE_SPREADING_CHANGES,
		STATE_SHUTTING_DOWN,
		STATE_INITIALIZING
	}
	private static PrinceState currentState;

	private ErraNode me;

	//	ServerSockets
	private ServerSocket joinedNodeListener;
	private ServerSocket departedNodeListener;
	private ServerSocket ambassadorListenerSocket;
	private DatagramSocket aliveNodeListener;

	//	Listening threads
	private JoinedNodeListenerThread joinedNodeListenerThread;
	private DepartedNodeListenerThread departedNodeListenerThread;
	private AliveNodeListenerThread aliveNodeListenerThread;
	private AmbassadorListenerThread ambassadorListenerThread;

	//	Speaking threads
	private AliveAskerThread aliveAskerThread;

	private InitializePrinceNodeThread initializePrinceNodeThread;

	//	private Map<String, ErraNode> nodes;
	private Map<String, ErraNode.NodeState> rollCallRegister;	// "registro per fare l'appello"
	private Map<String, ErraNode> princes;	// other active bootstraps

	private NodeViewer nodeViewer;

	private PrinceNode() {
		String myIPAddress = getMyIP();
		me = new ErraNode(myIPAddress, NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);	
		me.setInMyCounty(true);

		nodes = new HashMap<String, ErraNode>();
		rollCallRegister = new HashMap<String, NodeState>();
		princes = new HashMap<String, ErraNode>();

		findOtherPrinces();

		nodeViewer = new NodeViewer();

		currentState = PrinceState.STATE_RUNNING;

		joinedNodeListenerThread = new JoinedNodeListenerThread();
		departedNodeListenerThread = new DepartedNodeListenerThread();
		aliveNodeListenerThread = new AliveNodeListenerThread();
		ambassadorListenerThread = new AmbassadorListenerThread();
	}	// PrinceNode()

	public static void main(String[] args) {

		/////////////////////////
		//	ErraClient functions
		answerAliveRequest A=new answerAliveRequest();
		A.start();
		FM = new fileManager();
		listenToForward f = new listenToForward();
		f.start();
		refreshTopology refresh = new refreshTopology();
		refresh.start();
		////////////////////////	

		PrinceNode princeNode = new PrinceNode();
		princeNode.initializePrinceNode();
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

	//	private class KeyboardListenerThread extends Thread {
	//		//		TODO
	//	}	// KeyboardListenerThread

	private class AmbassadorListenerThread extends Thread {

		public AmbassadorListenerThread() {
			super();
			try {
				ambassadorListenerSocket = new ServerSocket(ErraNodeVariables.PORT_PRINCE_AMBASSADOR_LISTENER);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			Socket socket;
			while (true) {
				try {
					socket = ambassadorListenerSocket.accept();
					AmbassadorReceiverThread ambassadorReceiverThread = new AmbassadorReceiverThread(socket);
					ambassadorReceiverThread.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}	// AmbassadorListenerThread

	/*
	 * Speaking Threads
	 */

	private class AliveAskerThread extends Thread {

		@Override
		public void run() {
			super.run();
			updatePrinceState(PrinceState.STATE_ROLL_CALLING);
			for (Map.Entry<String, NodeState> entry : rollCallRegister.entrySet()) {
				updateRegister(entry.getKey(), NodeState.NODE_STATE_MISSING);
			}
			showNetworkTable();
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
					showNetworkTable();
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
						System.out.println("No missing nodes."); // TODO remove me!!!
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

				if (stillMissingNodes) {
					ErraNode[] deadNodes = new ErraNode[missingNodes.size()];
					int indexMissingNodes = 0;
					for (Iterator<String> iterator = missingNodes.iterator(); iterator.hasNext();) {
						String missingNodeIPAddress = (String)iterator.next();
						deadNodes[indexMissingNodes] = removeErraNode(missingNodeIPAddress);
						byte[] exileMsg = ErraNodeVariables.MSG_PRINCE_EXILED_NODE.getBytes();
						DatagramPacket exilePacket = new DatagramPacket(exileMsg, exileMsg.length, InetAddress.getByName(missingNodeIPAddress), ErraNodeVariables.PORT_SUBJECT_ALIVE_LISTENER);
						datagramSocket.send(exilePacket);
						System.out.println("Node " + missingNodeIPAddress + " lost!");
						indexMissingNodes++;
					}
					spreadNetworkChanges(deadNodes, false);
				}

				datagramSocket.close();
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
				try {
					if (removedNode != null) {
						System.out.println("Node " + ipAddress + " removed from the network.");
						spreadNetworkChanges(new ErraNode[]{removedNode}, false);
					} else {
						System.err.println("Node " + ipAddress + " not in the network.");
					}
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

	private class AmbassadorReceiverThread extends Thread {

		private Socket socket;

		public AmbassadorReceiverThread(Socket newSocket) {
			super();
			socket = newSocket;
		}

		@Override
		public void run() {
			super.run();
			try {
				String msg = ErraNodeVariables.MSG_PRINCE_HANDSHAKE;
				PrintStream toPrince;
				toPrince = new PrintStream(socket.getOutputStream());
				toPrince.println(msg);
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	// AmbassadorReceiverThread

	private class InitializePrinceNodeThread extends Thread {


		public InitializePrinceNodeThread() {
			super();
		}

		@Override
		public void run() {
			super.run();
			boolean handshake = true;
			try {
				DatagramSocket datagramSocket = new DatagramSocket(ErraNodeVariables.PORT_PRINCE_AMBASSADOR_LISTENER);
				String msg = ErraNodeVariables.MSG_PRINCE_HANDSHAKE;
				byte[] buf = msg.getBytes();
				DatagramPacket handshakePacket = new DatagramPacket(buf, buf.length);
				while (handshake) {
					long startTime = System.currentTimeMillis();
					sleep(10000);
					long endTime = System.currentTimeMillis();
					long difference = endTime - startTime;
				}
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			updatePrinceState(PrinceState.STATE_RUNNING);
		}
	}	// InitializePrinceNodeThread

	/*
	 * ****************************************************************************
	 * END THREADS
	 */

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

	private void findOtherPrinces() {

		JFileChooser fileChooser = new JFileChooser();				
		fileChooser.setCurrentDirectory(new File("~"));
		boolean fileSelected = false;
		while (!fileSelected) {
			int fileChooserOption = fileChooser.showOpenDialog(new JFrame());
			if (fileChooserOption == JFileChooser.APPROVE_OPTION) {
				fileSelected = true;
			}
		}
		String path = fileChooser.getSelectedFile().getPath();
		File princeAddressesFile = new File(path);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(princeAddressesFile));
			String ipAddress = null;
			while ((ipAddress = reader.readLine()) != null) {
				if (validate(ipAddress) && !ipAddress.equalsIgnoreCase(me.getIPAddress())) {
					ErraNode princeNode = new ErraNode(ipAddress, NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);
					princeNode.setInMyCounty(false);
					nodes.put(ipAddress, princeNode);
					princes.put(ipAddress, princeNode);
				}
			}
			if (reader != null) {
				reader.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e)	{
			e.printStackTrace();
		} 
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

	private void initializeAliveTimer() {
		long maxPeriod;
		for(Map.Entry<String, ErraNode> entry : princes.entrySet()) {
			ErraNode currentPrinceNode = entry.getValue();
			long startTime = System.nanoTime();

			long endTime = System.nanoTime();
			long difference = endTime - startTime;
		}
	}

	private void initializePrinceNode() {
		updatePrinceState(PrinceState.STATE_INITIALIZING);
		System.out.println("Initializing Prince node...");
				initializePrinceNodeThread = new InitializePrinceNodeThread();
				initializePrinceNodeThread.start();
//		boolean handshake = true;
//		try {
//			DatagramSocket datagramSocket = new DatagramSocket(ErraNodeVariables.PORT_PRINCE_AMBASSADOR_LISTENER);
//			String msg = ErraNodeVariables.MSG_PRINCE_HANDSHAKE;
//			byte[] buf = msg.getBytes();
//			DatagramPacket handshakePacket = new DatagramPacket(buf, buf.length);
//			long startTime = System.nanoTime();
//			while (handshake) {
//				
//				long endTime = System.nanoTime();
//				long difference = endTime - startTime;
//			}
//		} catch (SocketException e) {
//			e.printStackTrace();
//		}
//		updatePrinceState(PrinceState.STATE_RUNNING);
	}

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