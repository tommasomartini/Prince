package com.prince;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import com.prince.ErraNode.NodeState;
import com.prince.ErraNode.NodeType;
import com.prince.NewErraClient.manageRecovery;

public class PrinceNode extends NewErraClient 
{

	private boolean ACTIVE_ALIVE_RQST = true;

	//	Times and periods in milliseconds
	
	
	//	private static final long INITIALIZATION_PERIOD = 1000 * 5; // initialization duration


	
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
	private DatagramSocket aliveNodeListener;
	private DatagramSocket myAmbassadorReceiverSocket;
	private DatagramSocket foreignAmbassadorReceiverSocket;

	//	Listening threads
	private JoinedNodeListenerThread joinedNodeListenerThread;
	private DepartedNodeListenerThread departedNodeListenerThread;
	private AliveNodeListenerThread aliveNodeListenerThread;
	private MyAmbassadorListenerThread myAmbassadorListenerThread;
	private ForeignAmbassadorListenerThread foreignAmbassadorListenerThread;

	//	Speaking threads
	private AliveAskerThread aliveAskerThread;

	//	private Map<String, ErraNode> nodes;
	private Map<String, ErraNode.NodeState> rollCallRegister;	// "registro per fare l'appello"
	private Map<String, ErraNode> princes;	// other active bootstraps
	private Map<Integer, Long> ambassadorDepartureTimes;
	private Map<Integer, Long> ambassadorTripDuration;

	private NodeViewer nodeViewer;

	//	private boolean handshake;

	private Timer timer;

	private PrinceNode() {
		String myIPAddress = getMyIP();
		me = new ErraNode(myIPAddress, NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);	
		me.setInMyCounty(true);

		nodes = new HashMap<String, ErraNode>();
		rollCallRegister = new HashMap<String, NodeState>();
		princes = new HashMap<String, ErraNode>();
		ambassadorDepartureTimes = new HashMap<Integer, Long>();
		ambassadorTripDuration = new HashMap<Integer, Long>();

		timer = new Timer();

		findOtherPrinces();

		nodeViewer = new NodeViewer();

		currentState = PrinceState.STATE_RUNNING;

		joinedNodeListenerThread = new JoinedNodeListenerThread();
		departedNodeListenerThread = new DepartedNodeListenerThread();
		aliveNodeListenerThread = new AliveNodeListenerThread();
		myAmbassadorListenerThread = new MyAmbassadorListenerThread();
		foreignAmbassadorListenerThread = new ForeignAmbassadorListenerThread();
	}	// PrinceNode()

	public static void main(String[] args) throws UnsupportedEncodingException, UnknownHostException {

		/////////////////////////
		//	ErraClient functions
		ErraNodeVariables.parseConfigFile();
		
		answerAliveRequest A=new answerAliveRequest();
		A.start();
		
		FM = new fileManager();
		listenToForward f = new listenToForward();
		f.start();
		refreshTopology refresh = new refreshTopology();
		refresh.start();
		confirmReception C=new confirmReception();
		C.start();
		received=new LinkedList<String>();
		if(ErraNodeVariables.recovery)
		{
			manageRecovery R=new manageRecovery();
			R.start();
		}

		////////////////////////	

		PrinceNode princeNode = new PrinceNode();
//		princeNode.initializePrinceNode();
		while (currentState != PrinceState.STATE_RUNNING)
		{
		}
		princeNode.runPrinceNode();
		
		Scanner keyboard = new Scanner(System.in);
		
		while(true)
		{	
			String input = keyboard.nextLine();
			if (input.toUpperCase().equals("S"))
			{
				send();
			}
		}
		
	}	// main()

	private void runPrinceNode() {
		System.out.println("=====\n#Prince " + me.getIPAddress() + " activated.");
		nodeViewer.showNetwork(nodes, me);
		joinedNodeListenerThread.start();
		departedNodeListenerThread.start();
		aliveNodeListenerThread.start();
		TimerTask task = new AliveAskerTask();
		if (ACTIVE_ALIVE_RQST) {
			timer.schedule(task, ErraNodeVariables.DELAY_ASK_FOR_ALIVE, ErraNodeVariables.periodAskForAlive);
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
	}	// AliveAskerTask

	//	private class StopHandshakeTask extends TimerTask {
	//		@Override
	//		public void run() {
	//			handshake = false;
	//			System.out.println("Stop handshaking!");
	//		}
	//	}	// StopHandshakeTask

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

	private class ForeignAmbassadorListenerThread extends Thread {

		public ForeignAmbassadorListenerThread() {
			super();
			try {
				foreignAmbassadorReceiverSocket = new DatagramSocket(ErraNodeVariables.PORT_PRINCE_FOREIGN_AMBASSADOR_LISTENER);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			try {
				//				System.out.println("Waiting for foreign ambassadors.");	//TODO remove me
				byte[] receivedBuffer = new byte[1000];
				DatagramPacket receivedPacket;
				DatagramPacket sendingPacket;
				DatagramSocket sendingSocket = new DatagramSocket();
				String foreignAmbassadorMsg;
				String answerMsg = ErraNodeVariables.MSG_PRINCE_ANSWER_HANDSHAKE + ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR;
				int foreignAmbassadorID;
				while (true) {
					receivedPacket = new DatagramPacket(receivedBuffer, receivedBuffer.length);
					foreignAmbassadorReceiverSocket.receive(receivedPacket);
					System.out.println("Ip: " + receivedPacket.getAddress().getHostAddress());
					foreignAmbassadorMsg = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
					String[] info = foreignAmbassadorMsg.split(ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR);
					if (info[0].equalsIgnoreCase(ErraNodeVariables.MSG_PRINCE_HANDSHAKE)) {
						foreignAmbassadorID = Integer.parseInt(info[1]);
						System.out.println("Foreign ambassador with ID: " + foreignAmbassadorID + ".");	//TODO remove me
						byte[] sendingBuffer = (answerMsg + foreignAmbassadorID).getBytes();
						sendingPacket = new DatagramPacket(sendingBuffer, sendingBuffer.length, receivedPacket.getAddress(), ErraNodeVariables.PORT_PRINCE_MY_AMBASSADOR_LISTENER);
						sendingSocket.send(sendingPacket);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	// AmbassadorListenerThread

	private class MyAmbassadorListenerThread extends Thread {

		public MyAmbassadorListenerThread() {
			super();
			try {
				myAmbassadorReceiverSocket = new DatagramSocket(ErraNodeVariables.PORT_PRINCE_MY_AMBASSADOR_LISTENER);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			try {
				System.out.println("Waiting for my ambassadors.");
				byte[] buffer = new byte[1000];
				DatagramPacket datagramPacket;
				String ambassadorReport;
				int ambassadorID;
				while (currentState == PrinceState.STATE_INITIALIZING) {
					datagramPacket = new DatagramPacket(buffer, buffer.length);
					myAmbassadorReceiverSocket.receive(datagramPacket);
					ambassadorReport = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
					String[] info = ambassadorReport.split(ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR);
					if (info[0].equalsIgnoreCase(ErraNodeVariables.MSG_PRINCE_ANSWER_HANDSHAKE)) {
						ambassadorID = Integer.parseInt(info[1]);
						System.out.println("Ricevuto " + ambassadorID);
						if (ambassadorDepartureTimes.containsKey(ambassadorID)) {
							long tripDuration = System.nanoTime() - ambassadorDepartureTimes.get(ambassadorID);
							ambassadorTripDuration.put(ambassadorID, tripDuration);
							System.out.println("Ambassador " + ambassadorID + " back home. Time " + tripDuration);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
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
//				DatagramSocket datagramSocket = new DatagramSocket(ErraNodeVariables.PORT_PRINCE_ASK_ALIVE_NODES);
				DatagramSocket datagramSocket = new DatagramSocket();
				DatagramPacket datagramPacket;
				byte[] msg = (new String(ErraNodeVariables.MSG_PRINCE_ALIVE_REQUEST)).getBytes();
				for(Map.Entry<String, ErraNode.NodeState> entry : rollCallRegister.entrySet()) {
					datagramPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(entry.getKey()), ErraNodeVariables.PORT_SUBJECT_ALIVE_LISTENER);
					datagramSocket.send(datagramPacket);
				}
				int rollCallingCounter = ErraNodeVariables.TIMES_TO_ASK_AGAIN;
				boolean stillMissingNodes = true;
				List<String> missingNodes = new LinkedList<String>();
				while (rollCallingCounter >= 0 && stillMissingNodes) {
					System.out.println("Waiting for alive answers...");
					showNetworkTable();
					Thread.sleep((ErraNodeVariables.TIMES_TO_ASK_AGAIN - rollCallingCounter + 1) * ErraNodeVariables.periodAskForALiveAgain);	// TODO controllare qui!
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
						int numRqst = ErraNodeVariables.TIMES_TO_ASK_AGAIN - rollCallingCounter + 1;
						System.out.println("Missing nodes! Send request bis number: " + numRqst + "/" + ErraNodeVariables.TIMES_TO_ASK_AGAIN + " to the following nodes:"); // TODO remove me!!!
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
						System.out.println("Node " + missingNodeIPAddress + " lost.");
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
							sleep(ErraNodeVariables.DELAY_WAIT_FOR_CALLING_TO_FINISH);
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
						sleep(ErraNodeVariables.DELAY_WAIT_FOR_CALLING_TO_FINISH);
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

	private class InitializePrinceNodeThread extends Thread {

		public InitializePrinceNodeThread() {
			super();
		}

		@Override
		public void run() {
			super.run();
			//			TimerTask task;
			//			boolean ambassadorsAnswered = false;
			if (princes.size() >= 1) {
				while (true) {
					//				task = new StopHandshakeTask();	// Qui uso la variabile handshake!!
					//				timer.schedule(task, INITIALIZATION_PERIOD);
					//				handshake = true;
					try {
						DatagramSocket datagramSocket = new DatagramSocket();
						String msg = ErraNodeVariables.MSG_PRINCE_HANDSHAKE + ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR;
						DatagramPacket handshakePacket;
						InetAddress currentPrinceAddress;
						int currentHandshakeID = new Random().nextInt(1000);
						Collection<ErraNode> princeCollection = princes.values();
						Iterator<ErraNode> princesIterator = princeCollection.iterator();
						System.out.println("I am sending my ambassadors around.");
						while (princesIterator.hasNext()) {
							msg += currentHandshakeID++;
							byte[] buf = msg.getBytes();
							currentPrinceAddress = InetAddress.getByName(((ErraNode)princesIterator.next()).getIPAddress());
							handshakePacket = new DatagramPacket(buf, buf.length, currentPrinceAddress, ErraNodeVariables.PORT_PRINCE_FOREIGN_AMBASSADOR_LISTENER);
							ambassadorDepartureTimes.put(currentHandshakeID, System.nanoTime());
							datagramSocket.send(handshakePacket);
						}
						sleep(1000 * 2);	// wait some seconds...
						if (ambassadorTripDuration.isEmpty()) {
							System.out.println("No ambassador answered: the procedure will be repeated.");
						} else {
							long sumTripTime = 0;
							for (Map.Entry<Integer, Long> entry : ambassadorTripDuration.entrySet()) {
								sumTripTime += entry.getValue();
								System.out.println("Summing time of ambassador: " + entry.getKey() + ".");
							}
							ErraNodeVariables.periodAskForALiveAgain = (sumTripTime / ambassadorTripDuration.size()) * 2;	// twice the mean value
							ErraNodeVariables.periodAskForAlive = 2^(ErraNodeVariables.TIMES_TO_ASK_AGAIN) * ErraNodeVariables.periodAskForALiveAgain * 2;	// in totale chiedo 2^(TIMES_TO_ASK_AGAIN) - 1 volte. Raddoppio questo tempo.
							System.out.println("Period ask for alive again set to: " + ErraNodeVariables.periodAskForALiveAgain + " milliseconds.");
							System.out.println("Period ask for alive set to: " + ErraNodeVariables.periodAskForAlive + " milliseconds.");
							datagramSocket.close();
							System.out.println("Exit from initializing...");
							break;
						}
						datagramSocket.close();
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
				myAmbassadorReceiverSocket.close();
			}
			updatePrinceState(PrinceState.STATE_RUNNING);
			System.out.println("I won't accept other ambassadors of mine any more.");
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
		int fileChooserOption = fileChooser.showOpenDialog(new JFrame());
		if (fileChooserOption != JFileChooser.APPROVE_OPTION) {
			System.out.println("You haven't selected any file, so I suppose you are the only prince!");
			//System.exit(0);
		}
		else
		{
			String path = fileChooser.getSelectedFile().getPath();
			File princeAddressesFile = new File(path);
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(princeAddressesFile));
				String ipAddress = null;
				while ((ipAddress = reader.readLine()) != null) 
				{
					if (validate(ipAddress) && !ipAddress.equalsIgnoreCase(me.getIPAddress())) {
						ErraNode princeNode = new ErraNode(ipAddress, NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);
						princeNode.setInMyCounty(false);
						nodes.put(ipAddress, princeNode);
						princes.put(ipAddress, princeNode);
						rollCallRegister.put(ipAddress, NodeState.NODE_STATE_ALIVE);
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

	private void initializePrinceNode() {
		updatePrinceState(PrinceState.STATE_INITIALIZING);
		System.out.println("Initializing Prince node...");
		myAmbassadorListenerThread.start();
		foreignAmbassadorListenerThread.start();
		InitializePrinceNodeThread initializePrinceNodeThread = new InitializePrinceNodeThread();	// FIXME
		initializePrinceNodeThread.start();
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
		nodes.get(erraIPAddress).setNodeState(nodeState);	// TODO problemi qui?
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