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
import java.util.Map;
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
	private static final int PORT_ALIVE_NODE = 8004;

	//	Speaking ports
	private static final int PORT_ASK_ALIVE_NODES = 8000;
	//	private static final int PORT_TABLE = 8003;

	//	Testing ports
	private static final int PORT_TESTING = 7777;

	private static final long DELAY_ASK_FOR_ALIVE = 1000;
	private static final long PERIOD_ASK_FOR_ALIVE = 1000;

	//	States
	private static final int STATE_RUNNING = 0;
	private static final int STATE_ROLL_CALLING = 1;

	//	Subject states ("stati del suddito")
	private static final int SUBJECT_STATE_MISSING = 0;
	private static final int SUBJECT_STATE_ALIVE = 1;
	private static final int SUBJECT_STATE_DEAD = 2;

	private static int currentState;

	private static int counter = 0;
	private static int node_id = 0;

	//	ServerSockets
	private ServerSocket serverSocket;	
	private ServerSocket joinedNodeListener;
	private ServerSocket departedNodeListener;
	private ServerSocket aliveNodeListener;

	//	Listening threads
	JoinedNodeListenerThread joinedNodeListenerThread;
	DepartedNodeListenerThread departedNodeListenerThread;
	AliveNodeListenerThread aliveNodeListenerThread;

	private Map<Integer, Node> nodes;
	private Map<Integer, Integer> rollCallRegister;	// "registro per fare l'appello"

	private BootstrapNode() {
		nodes = new HashMap<Integer, BootstrapNode.Node>();
		rollCallRegister = new HashMap<Integer, Integer>();

		currentState = STATE_RUNNING;

		try {
			serverSocket = new ServerSocket(PORT_TESTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Server di test creato con successo!");

		Timer timer = new Timer();
		TimerTask task = new AliveAskerTask();
		timer.schedule(task, DELAY_ASK_FOR_ALIVE, PERIOD_ASK_FOR_ALIVE);

		//		Start listening threads
		joinedNodeListenerThread = new JoinedNodeListenerThread();
		joinedNodeListenerThread.start();

		departedNodeListenerThread = new DepartedNodeListenerThread();
		departedNodeListenerThread.start();

		aliveNodeListenerThread = new AliveNodeListenerThread();
		aliveNodeListenerThread.start();
	}	// BootstrapNode()

	public static void main(String[] args) {
		BootstrapNode btNode = new BootstrapNode();
		btNode.runBootstrap();
	}	// main()

	private void runBootstrap() {

		while (true) {			
			try {
				System.out.println("Server in attesa di richieste…");
				Socket newSocket = serverSocket.accept();
				//				logger.log(Level.INFO, "risposta a ServerSocket");
				ClientThread clientThread = new ClientThread(newSocket, counter++);
				clientThread.start();
			}
			catch(IOException e) {
				System.out.println("Conversazione interrotta");
			}
		}
	}	// runBootstrap()

	//	W@erraid@numeronodiattivinellarete@ip#erraid%ip#erraid%ip#erraid%...%
	private String getNodesMapToString() {
		String mapToString = String.valueOf(nodes.size()) + "@";
		for(Map.Entry<Integer, Node> entry : nodes.entrySet()) {
			Node currentNode = entry.getValue();
			mapToString += String.valueOf(currentNode.getID()) + "#" + currentNode.getIP_ADDRESS() + "%";
		}
		return mapToString;
	}

	private class ClientThread extends Thread {

		private Socket socket;
		private int id;

		public ClientThread(Socket newSocket, int myId) {
			super();
			socket = newSocket;
			id = myId;
		}

		@Override
		public void run() {
			System.out.println("---Iniziato thread con ID: " + id);
			readFromClient();
		}

		private void readFromClient() {

			try {
				PrintStream toClient = new PrintStream(socket.getOutputStream());	// scrivo al client
				toClient.println("Il client e' connesso al server con id " + id);
				String rcvMsg = "";
				String usrMsg = "";
				BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				while (!rcvMsg.equalsIgnoreCase("bye") && !usrMsg.equalsIgnoreCase("quit")) {
					rcvMsg = fromClient.readLine();
					System.out.println("Il client " + id + " scrive: " + rcvMsg);		
				}

				socket.close();
				System.out.println("---Disconnesso dal client numero " + id);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}	// ClientThread

	private class AliveAskerTask extends TimerTask {

		@Override
		public void run() {
			for (Map.Entry<Integer, Integer> entry : rollCallRegister.entrySet()) {
				entry.setValue(SUBJECT_STATE_MISSING);
			}
			AliveAskerThread aliveAskerThread = new AliveAskerThread();
			aliveAskerThread.start();
			//			try {
			//				notifiedAliveNodeListener = new ServerSocket(PORT_ALIVE_NODE);
			//				while (true) {
			//					
			//				}
			//				Socket newSocket = notifiedAliveNodeListener.accept();
			//			} catch (IOException e) {
			//				e.printStackTrace();
			//			} 
			//			
			//			NotifiedAliveNodeThread notifiedAliveNodeThread = new NotifiedAliveNodeThread();
			//			notifiedAliveNodeThread.start();
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
					DeleteNodeThread deleteNodeThread = new DeleteNodeThread(socket);
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
				aliveNodeListener = new ServerSocket(PORT_ALIVE_NODE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			System.out.println("Avviato AliveNodeListenerThread");

			while (true) {
				try {
					Socket newSocket = aliveNodeListener.accept();
					NotifiedAliveNodeThread notifiedAliveNodeThread = new NotifiedAliveNodeThread(newSocket);
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

		@Override
		public void run() {
			super.run();
			try {
				DatagramSocket datagramSocket = new DatagramSocket(PORT_ASK_ALIVE_NODES);
				DatagramPacket datagramPacket;
				byte[] msg = (new String("?")).getBytes();
				for(Map.Entry<Integer, Node> entry : nodes.entrySet()) {
					Node currentNode = entry.getValue();
					datagramPacket = new DatagramPacket(msg, msg.length, InetAddress.getByName(currentNode.getIP_ADDRESS()), PORT_ASK_ALIVE_NODES);
					datagramSocket.send(datagramPacket);
				}
				datagramSocket.close();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
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
					//				TODO lanciare un'eccezione perche' ho ricevuto un indirizzo vuoto!
				} else {
					String ipString = String.valueOf(address[0]);
					for (int i = 1; i < address.length; i++) {
						ipString += ":" + address[i];
					}
					int currentId = node_id++;
					Node node = new Node(currentId, ipString);
					nodes.put(currentId, node);
					rollCallRegister.put(currentId, SUBJECT_STATE_ALIVE);

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

	private class DeleteNodeThread extends Thread {

		private Socket socket;

		public DeleteNodeThread(Socket newSocket) {
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
				if (nodes.remove(identifier) != null) {
					System.out.println("Nodo " + identifier + " rimosso dalla rete");
					rollCallRegister.remove(identifier);
					//rollCallRegister.put(identifier, SUBJECT_STATE_DEAD);
				} else {
					System.out.println("Il nodo " + identifier + " non e' presente nella rete");
					//					TODO devo avvertire tutti i nodi che il nodo corrente ha lasciato la rete
				}
			}
		}	
	}	// DeleteNodeThread

	private class NotifiedAliveNodeThread extends Thread {

		private Socket socket;

		public NotifiedAliveNodeThread(Socket newSocket) {
			super();
			socket = newSocket;
		}

		@Override
		public void run() {
			super.run();
			String msgFromNode = null;
			try {
				//				Message in the form: !@erraid
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				msgFromNode = bufferedReader.readLine();
				if (msgFromNode == null || msgFromNode.length() == 0 || !msgFromNode.substring(0, 1).equalsIgnoreCase("!")) {
					// TODO lanciare un'eccezione perche' ho ricevuto un indirizzo vuoto!
					System.out.println("Il messaggio del client che risponde di essere attivo e' vuoto o diverso da \'!\'");
					System.out.println(msgFromNode);
				} else {
					String[] msgValues = msgFromNode.split("@");
					int identifier = Integer.parseInt(msgValues[1]);
					rollCallRegister.put(identifier, SUBJECT_STATE_ALIVE);
				}
			} catch (IOException e) {
				e.printStackTrace();
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
	private class Node {

		private final int ID;
		private final String IP_ADDRESS;

		public Node(int id, String ip) {
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
}