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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import com.prince.ErraNode.NodeState;
import com.prince.ErraNode.NodeType;

public class PrinceNode extends NewErraClient {

	private boolean ACTIVE_ALIVE_RQST = true;
	private boolean ACTIVE_INFO_LOG = true;
	private boolean ACTIVE_ERROR_LOG = true;

	private static final String PASSWORD = "erra";

	//	States
	public enum PrinceState {
		STATE_RUNNING,
		STATE_ROLL_CALLING,
		STATE_SPREADING_CHANGES,
		STATE_SHUTTING_DOWN,
		STATE_INITIALIZING
	}
	private static PrinceState currentState;

	private ErraNode me;
	private ErraNode protectorate;	// Prince I have to look after
	private String subProtectorate;	// Protectorate of my protectorate

	//	ServerSockets
	private ServerSocket joinedNodeListener;
	private ServerSocket departedNodeListener;
	private DatagramSocket aliveNodeListener;
	private ServerSocket immigrantListener;
	private ServerSocket protectorServerSocket;

	//	Listening threads
	private JoinedNodeListenerThread joinedNodeListenerThread;
	private DepartedNodeListenerThread departedNodeListenerThread;
	private AliveNodeListenerThread aliveNodeListenerThread;
	private ImmigrantListenerThread immigrantListenerThread;
	private ProtectorListenerThread protectorListenerThread;

	//	Speaking threads
	private AliveAskerThread aliveAskerThread;

	//	private Map<String, ErraNode> nodes;
	private Map<String, ErraNode.NodeState> rollCallRegister;	// "registro per fare l'appello"
	private Map<String, ErraNode> princes;	// other active bootstraps
	private Map<String, ErraNode> refugees;	// subjects I have to look after in case their Prince falls
	//	private NodeViewer nodeViewer;
	private PrinceGUI princeGui;

	private Timer timer;

	private PrinceNode() {
		String myIPAddress = getMyIP();
		me = new ErraNode(myIPAddress, NodeType.NODE_TYPE_PRINCE, NodeState.NODE_STATE_ALIVE);	
		me.setInMyCounty(true);

		nodes = new HashMap<String, ErraNode>();
		rollCallRegister = new HashMap<String, NodeState>();
		princes = new HashMap<String, ErraNode>();
		refugees = new HashMap<String, ErraNode>();

		nodes.put(myIPAddress, me);
		princes.put(myIPAddress, me);
		timer = new Timer();

		princeGui = new PrinceGUI(nodes, me);
		princeGui.setPrinceNode(this);

		findOtherPrinces();

		currentState = PrinceState.STATE_RUNNING;

		joinedNodeListenerThread = new JoinedNodeListenerThread();
		departedNodeListenerThread = new DepartedNodeListenerThread();
		aliveNodeListenerThread = new AliveNodeListenerThread();
		immigrantListenerThread = new ImmigrantListenerThread();
		protectorListenerThread = new ProtectorListenerThread();
	}	// PrinceNode()

	public static void main(String[] args) {

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
		confirmReception C = new confirmReception();
		C.start();
		notifications = new LinkedList<String>();
		if (ErraNodeVariables.recovery) {
			manageRecovery R=new manageRecovery();
			R.start();
		}
		PrinceNode princeNode = new PrinceNode();
		try {
			NewErraClient.addIpOnline(getMyIP());
		} catch (IOException e) {
			e.printStackTrace();
		}
		////////////////////////

		while (currentState != PrinceState.STATE_RUNNING) {

		}
		princeNode.runPrinceNode();
	}	// main()

	private void runPrinceNode() {
		System.out.println("\nPrince " + me.getIPAddress() + " activated.\n");
		princeGui.updateMessage("Prince " + me.getIPAddress() + " activated.");
		if (!ACTIVE_ALIVE_RQST) {
			System.err.println("Attention! Alive requests are not active!");
		}
		//		nodeViewer.showNetwork(nodes, me);
		princeGui.updateTable(nodes);
		joinedNodeListenerThread.start();
		departedNodeListenerThread.start();
		aliveNodeListenerThread.start();
		immigrantListenerThread.start();
		protectorListenerThread.start();
		TimerTask task = new AliveAskerTask();
		if (ACTIVE_ALIVE_RQST) {
			timer.schedule(task, ErraNodeVariables.DELAY_ASK_FOR_ALIVE, ErraNodeVariables.periodAskForAlive);
		}

		System.out.print("> ");
		Scanner fromKeyboard = new Scanner(System.in);
		while(true) {	
			String inputFromKeyboard = fromKeyboard.nextLine();
			if (inputFromKeyboard.equalsIgnoreCase("S")) {
				try {
					send("", "");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			} else if (inputFromKeyboard.equalsIgnoreCase("shutdown")) {
				Scanner psswdScanner = new Scanner(System.in);
				System.out.println("Enter the password: ");
				System.out.print("> ");
				String psswd = psswdScanner.next();
				if (psswd.equals(PASSWORD)) {
					System.out.println("Password accepted!");
					while (!abdicate()) {

					}
					break;
				} else {
					System.err.println("Wrong password!");
					System.out.print("> ");
				}
			} else if (inputFromKeyboard.equalsIgnoreCase("fquit")) {
				System.err.println("Forced exit.");
				break;
			} else if (inputFromKeyboard.equalsIgnoreCase("help")) {
				System.out.println("\nMANUAL\n- \"send\": send a file.\n- \"stat\": show statistics of this Prince node.\n- \"shutdown\": shutdown this prince node (password is: \"" + PASSWORD + "\").\n- \"help\": show this manual.\n");
				System.out.print("> ");
			} else if (inputFromKeyboard.equalsIgnoreCase("stat")) {
				System.out.println("Sorry! Statistics not yet available for this Prince node.");
				System.out.print("> ");
			} else {
				System.out.println("Unknown command!");
				System.out.print("> ");
			}
		}
		System.exit(0);
	}

	private class AliveAskerTask extends TimerTask {
		@Override
		public void run() {
			if (!nodes.isEmpty()) {
				aliveAskerThread = new AliveAskerThread();
				aliveAskerThread.start();
			} else {
				princeInfoLog("No nodes in the network, I won't send any alive request.");
			}
		}
	}	// AliveAskerTask

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

	private class ImmigrantListenerThread extends Thread {

		public ImmigrantListenerThread() {
			super();
			try {
				immigrantListener = new ServerSocket(ErraNodeVariables.PORT_PRINCE_IMMIGRANT);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			InputStreamReader inputStreamReader;
			BufferedReader bufferedReader;
			while (true) {
				try {
					Socket socket = immigrantListener.accept();
					inputStreamReader = new InputStreamReader(socket.getInputStream());
					bufferedReader = new BufferedReader(inputStreamReader);
					String msg = bufferedReader.readLine();
					if (msg.substring(0, 1).equalsIgnoreCase(ErraNodeVariables.MSG_PRINCE_SEND_IMMIGRANT)) {
						String message = msg.substring(2);	// remove "I@"
						String[] immigrants = message.split(ErraNodeVariables.DELIMITER_MSG_PARAMS);
						String leavingPrince = socket.getInetAddress().getHostAddress();
						removeErraNode(leavingPrince);
						if (protectorate.getIPAddress().equalsIgnoreCase(leavingPrince)) {
							protectorate = nodes.get(subProtectorate);
							subProtectorate = null;
						}
						princeInfoLog("Adding immigrant nodes:");
						for (int i = 0; i < immigrants.length; i++) {
							ErraNode myNewSubject = nodes.get(immigrants[i]);
							myNewSubject.setInMyCounty(true);
							myNewSubject.setBootstrapOwner(me);
							updateRegister(immigrants[i], NodeState.NODE_STATE_ALIVE);
							princeInfoLog("- " + immigrants[i]);
						}
					} else {
						princeErrorLog("Expected \"I\" received: " + msg);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}	// AliveNodeListenerThread

	private class ProtectorListenerThread extends Thread {

		public ProtectorListenerThread() {
			super();
			try {
				protectorServerSocket = new ServerSocket(ErraNodeVariables.PORT_PRINCE_PROTECTOR_LISTENER);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			InputStreamReader inputStreamReader;
			BufferedReader bufferedReader;
			while (true) {
				try {
					Socket socket = protectorServerSocket.accept();
					inputStreamReader = new InputStreamReader(socket.getInputStream());
					bufferedReader = new BufferedReader(inputStreamReader);
					PrintStream printStream = new PrintStream(socket.getOutputStream());
					String msg = bufferedReader.readLine();
					if (msg.equalsIgnoreCase(ErraNodeVariables.MSG_PRINCE_REFUGEES_LIST_REQUEST)) {
						String protectorAddress = socket.getInetAddress().getHostAddress();
						if (!nodes.containsKey(protectorAddress)) {
							princeErrorLog("Attention! A node outside the network asked to see my subjects!");
						} else {
							//							<myProtectorateIP>@<mySubj_1>#<mySubj_2>#<mySubjc_3>#...#
							String subjectList = protectorate.getIPAddress() + ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR;
							for (Map.Entry<String, NodeState> entry : rollCallRegister.entrySet()) {
								if (!princes.containsKey(entry.getKey())) {
									subjectList += entry.getKey() + ErraNodeVariables.DELIMITER_MSG_PARAMS;
								}
							}
							printStream.println(subjectList);
						}
						socket.close();
					} else {
						princeErrorLog("Expected \"" + ErraNodeVariables.MSG_PRINCE_REFUGEES_LIST_REQUEST + "\" received: " + msg);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}	// ProtectorListenerThread

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
			princeGui.updateTable(nodes);
			try {
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
					princeInfoLog("Waiting for alive answers...");
					Thread.sleep((ErraNodeVariables.TIMES_TO_ASK_AGAIN - rollCallingCounter + 1) * ErraNodeVariables.periodAskForALiveAgain);
					princeGui.updateTable(nodes);
					for (Map.Entry<String, NodeState> registerEntry : rollCallRegister.entrySet()) {
						if (registerEntry.getValue() == NodeState.NODE_STATE_MISSING && !missingNodes.contains(registerEntry.getKey())) {
							missingNodes.add(registerEntry.getKey());
						} else if (registerEntry.getValue() == NodeState.NODE_STATE_ALIVE && missingNodes.contains(registerEntry.getKey())) {
							missingNodes.remove(registerEntry.getKey());
						}
					}

					if (missingNodes.isEmpty()) {
						stillMissingNodes = false;
						princeInfoLog("No missing nodes from last roll call.");
					} else if (rollCallingCounter > 0) {	// send again
						int numRqst = ErraNodeVariables.TIMES_TO_ASK_AGAIN - rollCallingCounter + 1;
						princeInfoLog("Missing nodes! Send request bis number: " + numRqst + "/" + ErraNodeVariables.TIMES_TO_ASK_AGAIN + " to the following nodes:");
						for (Iterator<String> iterator = missingNodes.iterator(); iterator.hasNext();) {
							String missingNodeIPAddress = (String)iterator.next();
							princeInfoLog("- " + missingNodeIPAddress);
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
						if (protectorate.getIPAddress().equalsIgnoreCase(missingNodeIPAddress)) {	// My protectorate has fallen
							protectorate = nodes.get(subProtectorate);
							for (Map.Entry<String, ErraNode> entry : refugees.entrySet()) {
								ErraNode refugee = entry.getValue();
								updateRegister(refugee.getIPAddress(), NodeState.NODE_STATE_ALIVE);
								System.out.println("Ora tra i miei nodi ho: " + refugee.getIPAddress());
							}

							//Remove the dead prince from the online list
							try {
								NewErraClient.removeIpOnline(protectorate.getIPAddress());
								System.out.println("The node "+protectorate.getIPAddress()+" has been removed from the online list.");
							} catch (IOException e){
								e.printStackTrace();
							}		
						}
						deadNodes[indexMissingNodes] = removeErraNode(missingNodeIPAddress);
						princeInfoLog("Node " + missingNodeIPAddress + " lost.");
						indexMissingNodes++;
					}
					spreadNetworkChanges(deadNodes, false);
					princeGui.updateTable(nodes);
				}
				refreshRefugeeList();
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
				princeErrorLog("Il messaggio del client che chiede di aggiungersi alla rete e' vuoto o diverso da \'J\'");
			} else {
				InetAddress inetAddress = socket.getInetAddress();
				String ipString = inetAddress.getHostAddress();
				while (currentState != PrinceState.STATE_RUNNING) {
					try {
						sleep(ErraNodeVariables.DELAY_WAIT_FOR_CALLING_TO_FINISH);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				if (!nodes.containsKey(ipString)) {
					ErraNode node = new ErraNode(ipString, NodeType.NODE_TYPE_SUBJECT, NodeState.NODE_STATE_ALIVE);
					node.setInMyCounty(true);
					node.setBootstrapOwner(me);
					spreadNetworkChanges(new ErraNode[]{node}, true);
					addErraNode(node);
					//					princeGui.updateTable(nodes);
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
				princeErrorLog("Il messaggio del client che chiede di aggiungersi alla rete e' vuoto o diverso da \'E\'");
			} else {
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
						princeInfoLog("Node " + ipAddress + " removed from the network.");
						spreadNetworkChanges(new ErraNode[]{removedNode}, false);
					} else {
						princeErrorLog("Node " + ipAddress + " not in the network (or trying to remove myself).");
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
			msgFromNode = new String(datagramPacket.getData(), 0, datagramPacket.getLength());
			if (msgFromNode == null || msgFromNode.length() == 0 || !msgFromNode.substring(0, 1).equalsIgnoreCase(ErraNodeVariables.MSG_SUBJECT_ALIVE)) {
				princeErrorLog("Il messaggio del client che risponde di essere attivo e' vuoto o diverso da \'!\'");
			} else {
				String ipAddress = datagramPacket.getAddress().getHostAddress();
				princeInfoLog("Subject " + ipAddress + " is alive!");
				updateRegister(ipAddress, NodeState.NODE_STATE_ALIVE);
				princeGui.updateTable(nodes);
			}
		}
	}	// NotifiedAliveNodeThread

	/*
	 * ****************************************************************************
	 * END THREADS
	 */

	public boolean abdicate() {
		while (currentState != PrinceState.STATE_RUNNING) {

		}
		System.out.println("Shutting down this Prince Node...");
		int princesNumber = princes.size() - 1;
		if (princesNumber != 0) {
			int subjectsNumber = rollCallRegister.size() - princesNumber;
			int subjPerPrince = subjectsNumber / princesNumber;
			int remainingSubs = subjectsNumber - subjPerPrince * princesNumber;
			String[] mySubjectIPs = new String[subjectsNumber];
			int subjectIndex = 0;
			for (Map.Entry<String, NodeState> entry : rollCallRegister.entrySet()) {
				if (!princes.containsKey(entry.getKey())) {
					mySubjectIPs[subjectIndex] = entry.getKey();
					subjectIndex++;
				}
			}
			String msg = ErraNodeVariables.MSG_PRINCE_SEND_IMMIGRANT + ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR;
			int princeIndex = 0;
			subjectIndex = 0;
			for (Map.Entry<String, ErraNode> entry : princes.entrySet()) {
				ErraNode currentPrince = entry.getValue();
				if (!currentPrince.getIPAddress().equalsIgnoreCase(me.getIPAddress())) {
					int howManySubs = subjPerPrince;
					if (princeIndex < remainingSubs) {
						howManySubs++;
					}
					princeIndex++;
					if (howManySubs > 0) {
						for (int i = 0; i < howManySubs; i++) {
							msg += mySubjectIPs[subjectIndex++] + ErraNodeVariables.DELIMITER_MSG_PARAMS;
						}
						msg = msg.substring(0, msg.length() - 1);	// remove last "#"
						try {
							Socket socket = new Socket(currentPrince.getIPAddress(), ErraNodeVariables.PORT_PRINCE_IMMIGRANT);
							PrintStream toOtherPrince = new PrintStream(socket.getOutputStream());
							toOtherPrince.println(msg);
							socket.close();
						} catch (UnknownHostException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						break;
					}
				}
			}
		}
		spreadNetworkChanges(new ErraNode[]{me}, false);
		try {
			NewErraClient.removeIpOnline(getMyIP());
		} catch (IOException e){
			e.printStackTrace();
		}	
		System.out.println("...Prince node shut down. Goodbye!");
		return true;
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

	private void updatePrinceState(PrinceState newPrinceState) {
		currentState = newPrinceState;
		princeGui.updateState(newPrinceState);
	}

	private void findOtherPrinces() {

		JFileChooser fileChooser = new JFileChooser();				
		fileChooser.setCurrentDirectory(new File("~"));
		int fileChooserOption = fileChooser.showOpenDialog(new JFrame());
		if (fileChooserOption != JFileChooser.APPROVE_OPTION) {
			princeInfoLog("You haven't selected any file, so I suppose you are the only prince!");
		}
		else
		{
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
		findMyProtectorate();	
	}

	private void findMyProtectorate() {	
		Set<String> princesSet = princes.keySet();
		String[] princesArray = new String[princes.size()];
		Iterator<String> iterator = princesSet.iterator();
		int ind = 0;
		while (iterator.hasNext()) {
			princesArray[ind++] = iterator.next();
		}
		Arrays.sort(princesArray);
		int index;
		for (index = 0; index < princesArray.length; index++) {
			if (princesArray[index].equalsIgnoreCase(me.getIPAddress())) {
				break;
			}
		}
		index++;
		if (index == princesArray.length) {
			index = 0;
		}
		protectorate = princes.get(princesArray[index]);
	}

	private void refreshRefugeeList() {
		try {
			if (!protectorate.getIPAddress().equalsIgnoreCase(me.getIPAddress())) {
				Socket socket = new Socket(protectorate.getIPAddress(), ErraNodeVariables.PORT_PRINCE_PROTECTOR_LISTENER);
				InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				PrintStream printStream = new PrintStream(socket.getOutputStream());
				String sendMsg = ErraNodeVariables.MSG_PRINCE_REFUGEES_LIST_REQUEST;
				printStream.println(sendMsg);
				String receiveMsg = bufferedReader.readLine();
				String[] stringArray1 = receiveMsg.split(ErraNodeVariables.DELIMITER_AFTER_MSG_CHAR);
				subProtectorate = stringArray1[0];
				if (stringArray1.length == 2 && stringArray1[1] != null && stringArray1[1].length() > 0) {
					String[] stringArray2 = stringArray1[1].split(ErraNodeVariables.DELIMITER_MSG_PARAMS);
					for (int i = 0; i < stringArray2.length; i++) {
						refugees.put(stringArray2[i], nodes.get(stringArray2[i]));
					}
				}
				socket.close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void princeInfoLog(String messageToShow) {
		if (ACTIVE_INFO_LOG) {
			System.out.println(messageToShow);
			princeGui.updateMessage(messageToShow);
		}
	}

	private void princeErrorLog(String messageToShow) {
		if (ACTIVE_ERROR_LOG) {
			System.err.println(messageToShow);
		}
	}

	/*
	 * Synchronized operations on data registers
	 */	
	private synchronized void addErraNode(ErraNode erraNode) {
		nodes.put(erraNode.getIPAddress(), erraNode);
		rollCallRegister.put(erraNode.getIPAddress(), NodeState.NODE_STATE_ALIVE);	// update rollCallRegister too
		//		showNetworkTable();
		princeGui.updateTable(nodes);
	}

	private synchronized ErraNode removeErraNode(String erraNodeIPAddress) {
		rollCallRegister.remove(erraNodeIPAddress);
		if (!erraNodeIPAddress.equalsIgnoreCase(me.getIPAddress())) {	// against acking: I cannot remove myself
			ErraNode removedNode = nodes.remove(erraNodeIPAddress);
			return removedNode;
		} else {
			return null;
		}
	}

	private synchronized void updateRegister(String erraIPAddress, NodeState nodeState) {
		rollCallRegister.put(erraIPAddress, nodeState);
		nodes.get(erraIPAddress).setNodeState(nodeState);
		//		princeGui.updateTable(nodes);
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
			if (!currentNode.getIPAddress().equalsIgnoreCase(me.getIPAddress())) {
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
		}
		updatePrinceState(PrinceState.STATE_RUNNING);
	}
}
