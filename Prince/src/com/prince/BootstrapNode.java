package com.prince;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Logger;

/* Database:
nome: erra
username: principe
password: principe

sudo apt-get install libmysql-java
 */

public class BootstrapNode {

	//	Listening ports
	private static final int PORT_JOINED_NODE = 8001;
	private static final int PORT_DEPARTED_NODE = 8002;

	//	Speaking ports
	private static final int PORT_ASK_ALIVE_NODES = 8000;
	private static final int PORT_TABLE = 8003;

	//	Testing ports
	private static final int PORT_TESTING = 7777;

	private static final String LOGGER_NAME = "Bootstrap";
	private static final long DELAY_ASK_FOR_ALIVE = 1000;
	private static final long PERIOD_ASK_FOR_ALIVE = 1000;

	private static int counter = 0;
	private static int node_id = 0;

	private ServerSocket serverSocket;	
	private ServerSocket newNodeListener;
	private ServerSocket departedNodeListener;

	private Logger logger;

	private Map<Integer, Node> nodes;
	
	private Handler handler;	// DA USARE?? serve per il timer della richiesta alive

	private BootstrapNode() {
		logger = Logger.getLogger(LOGGER_NAME);
		nodes = new HashMap<Integer, BootstrapNode.Node>();

		try {
			serverSocket = new ServerSocket(PORT_TESTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Server di test creato con successo!");

		Timer timer = new Timer();
		TimerTask task = new AliveAskerTask();
		timer.schedule(task, DELAY_ASK_FOR_ALIVE, PERIOD_ASK_FOR_ALIVE);
		
		//		Faccio partire tutti i thread incaricati di ascoltare
		JoinedNodeListenerThread joinedNodeListenerThread = new JoinedNodeListenerThread();
		joinedNodeListenerThread.start();

		DepartedNodeListenerThread departedNodeListenerThread = new DepartedNodeListenerThread();
		departedNodeListenerThread.start();
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
			AliveAskerThread aliveAskerThread = new AliveAskerThread();
			aliveAskerThread.start();
		}
	}
	
	/*
	 * 	Threads
	 */

	private class AliveAskerThread extends Thread {

		@Override
		public void run() {
			System.out.println("time!");
		}
	}

	private class JoinedNodeListenerThread extends Thread {

		public JoinedNodeListenerThread() {
			super();
			try {
				newNodeListener = new ServerSocket(PORT_JOINED_NODE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			//			logger.log(Level.INFO, "Avviato JoinedNodeListenerThread");
			System.out.println("Avviato JoinedNodeListenerThread");

			while (true) {
				try {
					Socket socket = newNodeListener.accept();
					AddNodeThread addNodeThread = new AddNodeThread(socket);
					addNodeThread.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}

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
	}

	private class NotifiedAliveNodeThread extends Thread {

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
		}
	}

	private class AddNodeThread extends Thread {

		Socket socket;

		public AddNodeThread(Socket newSocket) {
			super();
			socket = newSocket;
		}

		@Override
		public void run() {

			String msgFromNode = null;
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				msgFromNode = bufferedReader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (msgFromNode.length() == 0 || !msgFromNode.equalsIgnoreCase("J")) {
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

					System.out.println("Inserito nodo con id " + currentId);
//									TODO ora devo dire al nodo che id ha
//					W@erraid@numeronodiattivinellarete@ip#erraid%
					
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
	}

	private class DeleteNodeThread extends Thread {

		Socket socket;

		public DeleteNodeThread(Socket newSocket) {
			super();
			socket = newSocket;
		}

		@Override
		public void run() {
			String msgFromNode = null;
			try {
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				msgFromNode = bufferedReader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (msgFromNode.length() == 0 || !msgFromNode.substring(0, 1).equalsIgnoreCase("E")) {
//				TODO lanciare un'eccezione perche' ho ricevuto un indirizzo vuoto!
				System.out.println("Il messaggio del client che chiede di aggiungersi alla rete e' vuoto o diverso da \'J\'");
				System.out.println(msgFromNode);
			} else {
//				messaggio nella forma E@erraid\n
				int identifier = Integer.parseInt(msgFromNode.substring(2));
				if (nodes.remove(identifier) != null) {
					System.out.println("Nodo " + identifier + " rimosso dalla rete");
				} else {
					System.out.println("Il nodo " + identifier + " non e' presente nella rete");
//				TODO devo avvertire tutti i nodi che il nodo corrente ha lasciato la rete
				}
			}
		}	
	}

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