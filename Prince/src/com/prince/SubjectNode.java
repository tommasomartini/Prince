//package com.prince;
//
//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
//import java.io.BufferedReader;
//import java.io.ByteArrayOutputStream;
//import java.io.DataOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.UnsupportedEncodingException;
//import java.net.ConnectException;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.Inet4Address;
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.net.NetworkInterface;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.net.SocketException;
//import java.net.UnknownHostException;
//import java.nio.ByteBuffer;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//import java.util.Random;
//import java.util.Scanner;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import javax.swing.JFileChooser;
//import javax.swing.JFrame;
//
//public class SubjectNode {
//	public static int CONNECTION_TIMEOUT=5000;		
//
//	public static int MINIMUM_PAYLOAD=100;
//	public static int MAX_PAYLOAD=2048000;
//
//	//	public static int UDP_PORT_ALIVE=7000;
//	//	public static int UDP_ALIVE_ANSWER=8003;
//	//
//	//	public static int TCP_PORT_WELCOME=7001;
//	//	public static int TCP_PORT_FORWARDING=7002;
//	//	public static int TCP_PORT_SENDING=7003;
//	//	public static int TCP_PORT_REFRESH=7004;
//	//	public static int TCP_BOOTSTRAP_PORT_WELCOME=8001;
//	//	public static int TCP_BOOTSTRAP_PORT_GOODBYE=8002;
//
//	public static String BOOTSTRAP_ADDRESS="192.168.0.4";
//	public static String ERRA_ADDRESS="";
//	public static int TOTAL_DEVICES=3;
//
//	public static FileManager FM;		//Questo oggetto, disponibile a tutti, si occuperà di gestire i file pacchettizzati
//	//Questa struttura contiene tutti i nodi attivi
//	public static Map<String, ErraNode> nodes;
//
//	//============== Classi per gestire la generazione di files e il loro riassemblaggio  ====================
//
//	private class SubjectFile {
//		private String fileName;
//		private int packets;
//		private List<FilePart> parts = new ArrayList<FilePart>();
//
//		public SubjectFile() {
//			fileName="";
//			packets=0;
//		}
//
//		public SubjectFile(String name,int n) {
//			fileName=name;
//			packets=n;
//		}
//
//		public boolean add(int SN, byte[] data) {
//			FilePart t = new FilePart(SN,data);
//			parts.add(t);
//			System.out.println("Ricevuto frammento " + SN + " [" + data.length + " bytes] appartenente al file " + fileName);
//			if (parts.size() == packets) {
//				//Ora che ho tutti gli elementi estraggo, riordino, scrivo!
//				Collections.sort(parts);
//				OutputStream output = null;
//				try {
//					if (fileName.lastIndexOf("\\") != -1) {
//						String FN = fileName.substring(fileName.lastIndexOf("\\"));
//						FN=FN.replace("\\", "");
//						output = new BufferedOutputStream(new FileOutputStream(FN));
//					}
//					else
//						output = new BufferedOutputStream(new FileOutputStream(fileName));
//
//					try
//					{
//						for (Iterator<FilePart> i = parts.iterator(); i.hasNext();)
//						{
//							FilePart f = (FilePart)(i.next());
//							output.write(f.data);
//						}
//					}
//					finally
//					{	
//						System.out.println("Ricezione del file "+fileName+" completata. Il file e' stato scritto correttamente");
//						output.close();
//					}
//					return true;
//				}
//				catch (IOException e)
//				{System.err.println("Impossibile generare il file");}
//			}
//			return false;
//		}	
//
//		private class FilePart implements Comparable<FilePart> {
//
//			private int sn;
//			private byte[] data;
//
//			public FilePart(int newSN, byte[] newData) {
//				sn = newSN;
//				data = new byte[newData.length];
//				System.arraycopy(newData, 0, data, 0, newData.length);
//			}
//			public int compareTo(FilePart compareObject) { 
//				if (sn < compareObject.getSn()) return -1; 
//				else if (sn == compareObject.getSn()) return 0; 
//				else return 1;
//			}
//			public int getSn() {
//				return sn;
//			}
//		}
//	}
//
//	private class FileManager {
//
//		private List<SubjectFile> fileList; 		//Contiene i file di cui ho ricevuto parte del contenuto e che stanno per essere scritti
//
//		public FileManager(){
//			fileList = null;
//		}
//
//		public void add(String header,byte[] packet) {
//			int sn = Integer.parseInt(header.substring(0,header.indexOf("@")));		
//			header = header.substring(header.indexOf("@") + 1);
//			int parts = Integer.parseInt(header.substring(0,header.indexOf("@")));
//			header = header.substring(header.indexOf("@") + 1);
//			String fileName = header.substring(0,header.indexOf("@"));
//			boolean esito=false;
//			SubjectFile subjectFile = null;
//
//			if (fileList == null) { //Nessun file e' presente, significa che tutti i file sono stati scritti e questa e' la primissima parte di un file che richevo
//				subjectFile = new SubjectFile(fileName, parts);
//				esito = subjectFile.add(sn, packet);
//				fileList = new ArrayList<SubjectFile>();
//				fileList.add(subjectFile);
//			} else {
//				boolean pending=false;
//				for (Iterator<SubjectFile> i = fileList.iterator(); i.hasNext();) {
//					subjectFile = (SubjectFile)(i.next());
//					if (subjectFile.fileName.equals(fileName)) {
//						pending=true;
//						esito=subjectFile.add(sn, packet);
//					}
//				}
//
//				if(!pending) {
//					//Significa che, sebbene vi siano altri file pending, questo frammento appartiene ad un altro file!
//					subjectFile = new SubjectFile(fileName, parts);
//					subjectFile.add(sn, packet);
//					esito=fileList.add(subjectFile);
//				}
//			}
//			if (esito) {
//				//Significa che il frammento ricevuto ha completato il file, che va quindi rimosso da quelli in sospeso
//				fileList.remove(subjectFile);
//			}
//		}
//		public int filePending() {
//			return (fileList==null)?0:fileList.size();
//		}
//	}
//
//	//	private class ErraHost {	
//	//		public String ipAddress;
//	//		public String erraAddress;
//	//		public ErraHost() {
//	//			ipAddress="";
//	//			erraAddress="";
//	//		}
//	//		public ErraHost(String newIPAddress, String newAddress) {
//	//			this.ipAddress = newIPAddress;
//	//			erraAddress = newAddress;
//	//		}
//	//	}
//
//	//============== Funzione per inizializzare il sistema ERRA e scoprirne la topologia ======================
//
//	private boolean initializeErra(String address) {
//
//		nodes = new HashMap<String, ErraNode>();
//
//		try {
//			Socket tcpClientSocket = new Socket();
//			try {
//				tcpClientSocket.connect(new InetSocketAddress(address, ErraNodePorts.PORT_PRINCE_JOINED_NODE), CONNECTION_TIMEOUT);
//			} catch (IOException e) {	
//				System.err.println("Impossibile raggiungere il bootstrap node " + address);
//				tcpClientSocket.close();
//				return false;
//			}
//			System.out.println("...connessione avvenuta correttamente.");
//			String joinMessage = "J";
//			DataOutputStream streamToServer = new DataOutputStream(tcpClientSocket.getOutputStream());
//			BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(tcpClientSocket.getInputStream()));
//			streamToServer.writeBytes(joinMessage + '\n');	
//
//			//Aspetto la risposta dal bootstrap che contiene la topologia della rete
//			String fromServer = "";
//			fromServer=streamFromServer.readLine();
//			tcpClientSocket.close(); 
//
//			//Ora analizzo la stringa per ricostruire la topologia della rete
//			if (fromServer.charAt(0) == 'W') {
//				fromServer=fromServer.substring(2);		//Taglio la welcome portion
//				updateStructure(fromServer);		
//			} else {
//				System.err.println("Ho ricevuto sulla porta " + ErraNodePorts.PORT_PRINCE_JOINED_NODE + " un dato che non e' una tabella della rete!!");
//			}
//		} catch (ConnectException e) {
//			e.printStackTrace();
//			return false;
//		} catch (IOException e) {
//			e.printStackTrace();
//			return false;
//		}
//		return true;
//	}
//
//	//============== Thread per la gestione delle risposte UDP al bootstrap ==========
//
//	private class AnswerAliveRequestThread extends Thread {	
//		private DatagramSocket udpDatagramSocket;
//
//		@ Override
//		public void run() {	
//			super.run();
//			try {	
//				udpDatagramSocket = new DatagramSocket(ErraNodePorts.PORT_SUBJECT_ALIVE_LISTENER);
//				byte[] receivedData = new byte[1024];
//				byte[] sendData = new byte[1024];
//
//				while(true) {	
//					DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
//					try {
//						udpDatagramSocket.receive(receivedPacket);
//					} catch(SocketException e) {
//						System.out.println("Il socket UDP che risponde ai ? e' stato chiuso.");
//						return;	//Questo chiude anche il thread
//					}
//					String message = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
//					if (message.charAt(0) != '?') {
//						System.err.println("Ho ricevuto sulla porta " + ErraNodePorts.PORT_SUBJECT_ALIVE_LISTENER + " un pacchetto che non riesco a decodificare.");
//						System.err.println("Il pacchetto e': " + message);
//					} else {
//						String sentence = "!@" + ERRA_ADDRESS;
//						sendData = sentence.getBytes();
//						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivedPacket.getAddress(), UDP_ALIVE_ANSWER);
//						udpDatagramSocket.send(sendPacket);
//					}
//				}
//			} catch (ConnectException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//		public void releasePort() {
//			udpDatagramSocket.close();
//		}
//	}
//
//
//	//============== Thread per ascoltare se il bootstrap comunica eventuali refresh sulla topologia ============
//
//	private class RefreshTopologyThread extends Thread {
//
//		private ServerSocket serverSpcket;
//		private Socket socket;
//
//		@ Override
//		public void run() {	
//			super.run();
//			while(true) {
//				try {	
//					serverSpcket = new ServerSocket(ErraNodePorts.PORT_SUBJECT_REFRESH_TABLE_LISTENER);		//Mi metto in ascolto in attesa di connessioni TCP
//					try {	
//						socket=serverSpcket.accept();							//Quanto ho una richiesta di connessione la accetto!
//					} catch (SocketException e) {	
//						System.out.println("Il socket TCP per il refresh della topologia e' stato chiuso");
//						return;
//					}
//					BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//					String table = "";
//					table = streamFromServer.readLine();
//					socket.close();
//					System.out.println("Aggiornamento sulla topologia di ERRA: " + table);
//					if (table.charAt(0) == 'T') { //Significa che il bootstrap segnala la variazione su un singolo nodo, devo aggiornare solo una parte della topologia
//						//T@-192.168.1.1#192.168.1.3#
//						table=table.substring(2);
//						if (table.charAt(0) == '-') {	
//							table=table.substring(1);
//							while (table.length() > 0) {
//								String ipString = table.substring(0, table.indexOf("#"));
//								for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
//									ErraNode currentNode = entry.getValue();
//									if(currentNode.getIPAddress().equals(ipString )) {
//										nodes.remove(ipString);
//										break;
//									}
//								}
//								table = table.substring(table.indexOf("#") + 1);
//							}
//						} else if (table.charAt(0) == '+') {	
//							table = table.substring(1);
//							while (table.length() > 0) {
//								String ipString = table.substring(0, table.indexOf("#"));
//								if(!(nodes.containsKey(ipString))) {
//									ErraNode erraNode = new ErraNode(ipString);
//									nodes.put(ipString, erraNode);
//								}
//								table = table.substring(table.indexOf("#") + 1);
//							}
//						}
//						showTopology();
//					} else {
//						System.err.println("Ho ricevuto sulla porta " + ErraNodePorts.PORT_SUBJECT_REFRESH_TABLE_LISTENER + " un dato che non e' una tabella della rete!!");
//					}
//					serverSpcket.close();
//					socket.close();
//				} catch (ConnectException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		private void releasePort() throws IOException {
//			if (socket != null) {
//				socket.close();
//				return;
//			}
//			serverSpcket.close();
//		}
//	}
//
//	//Questo thread ascolta sulla porta 7002. Quando riceve una richiesta di connessione TCP genera un nuovo thread che si occupa di fare forwarding
//	private class ListenToForwardThread extends Thread {
//		private ServerSocket serverSocket; 
//		private Socket socket;
//
//		@Override
//		public void run() {
//			super.run();
//			try {
//				serverSocket = new ServerSocket(ErraNodePorts.PORT_SUBJECT_FILE_FORWARDING);
//
//				while(true) {
//					socket = serverSocket.accept();
//					ForwardThread F=new ForwardThread(socket);
//					F.start();
//				}
//			}
//			catch (IOException e)
//			{
//				System.out.println("Il socket TCP per il forwarding e' stato chiuso");
//			}
//		}	
//
//		public void releasePort() throws IOException
//		{
//			serverSocket.close();
//		}
//
//
//	}
//
//
//	//Questo thread legge un ERRA PACKET e se e' per me lo tiene, altrimenti fa forwarding
//	private class ForwardThread extends Thread {
//
//		private Socket mySocket;
//
//		public ForwardThread(Socket socket) {	
//			mySocket = socket;
//		}
//
//		@Override
//		public void run() {
//			super.run();
//			try {
//				/*
//				 * Tutta questa prima parte serve per leggere uno stream di bytes da un socket TCP.
//				 * Il risultato della lettura e' memorizzato nella varibile byte[] packet.
//				 */
//
//				InputStream inputStream = mySocket.getInputStream();  
//				ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
//				byte[] content = new byte[2048];  
//				int bytesRead = -1;  
//				while((bytesRead = inputStream.read(content))!= -1) {  
//					baos.write( content, 0, bytesRead );  
//				} 
//
//				byte[] packet=baos.toByteArray();
//				byte[] routingL=new byte[4];
//				System.arraycopy(packet, 0, routingL, 0, 4);
//				byte[] headerL=new byte[4];
//				System.arraycopy(packet, 4, headerL, 0, 4);
//				int rL = ByteBuffer.wrap(routingL).getInt();		//Questo mi esprime il numero di indirizzi IP contenuti nel routing		
//				int hL = ByteBuffer.wrap(headerL).getInt();
//
//				if (rL==1) {
//					byte[] header = new byte[hL];
//					System.arraycopy(packet, 12, header, 0, hL);
//					String sH = new String(header, "US-ASCII");
//					byte[] data = new byte[packet.length-12-hL];
//					System.arraycopy(packet, 12+hL, data, 0, data.length);
//					FM.add(sH,data);	//Ficco dentro questo frammento all'interno della classe che si occupa di gestire il tutto*/
//				} else {
//					//Il pacchetto non e' assolutamente per me, purtroppo
//					//Devo rimuovere il mio IP, ricavare il prossimo e fare il forwarding
//					byte[] next = new byte[4];				//Estraggo il primo IP della catena
//					System.arraycopy(packet, 12, next, 0, 4);		//4byte x Rl, 4 byte per hL, 4 byte mio IP, 4 byte prossimo
//					String nextIP = InetAddress.getByAddress(next).getHostAddress();
//					byte[] forwardPacket = new byte[packet.length - 4];	//Il nuovo pacchetto ha solamente l'indirizzo IP in meno!
//
//					byte[] newRoutingLen = new byte[4];
//					newRoutingLen = ByteBuffer.allocate(4).putInt(rL-1).array();
//
//					System.arraycopy(newRoutingLen, 0, forwardPacket, 0, 4);		//Riscrivo la lunghezza del campo routing
//					System.arraycopy(packet, 4, forwardPacket, 4, 4);				//Copio la lunghezza del campo header
//					System.arraycopy(packet, 12, forwardPacket, 8,packet.length-12);		//Riscrivo a partire dal 2° IP tutto quanto
//
//					try {
//						Socket tcpClientSocket = new Socket();
//						tcpClientSocket.connect(new InetSocketAddress(nextIP, ErraNodePorts.PORT_SUBJECT_FILE_FORWARDING), CONNECTION_TIMEOUT);
//						OutputStream out = tcpClientSocket.getOutputStream(); 
//						DataOutputStream dataOutputStream = new DataOutputStream(out);
//						dataOutputStream.write(forwardPacket, 0, forwardPacket.length);
//						tcpClientSocket.close(); 
//						System.out.println("Forwarding all'indirizzo " + nextIP + " completato.");	
//					} catch (IOException e) {
//						System.err.println("Forwarding all'indirizzo "+nextIP+" fallito.");	
//					}
//				}
//				return;	
//			} catch (ConnectException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//	}
//
//	//=================== Riceve una stringa contenente la topologia di rete e aggiorna la lista ====================
//
//	private void updateStructure(String fromServer) {
//		nodes.clear();
//		//Ora procedo alla memorizzazione nei vettori della rete
//		while(fromServer.length() > 0) {
//			//Estraggo host per host le informazioni
//			String ipAddress = fromServer.substring(0, fromServer.indexOf('#'));
//			ErraNode erraNode = new ErraNode(ipAddress);
//			nodes.put(ipAddress, erraNode);
//			fromServer = fromServer.substring(fromServer.indexOf('#') + 1);
//		}
//	}
//
//	//=================== Segnala al nodo BOOTSTRAP che me ne sto andando ====================
//
//	private void sayGoodbye() throws UnknownHostException, IOException {
//		//Mi connetto con il bootstrap e gli segnalo la mia dipartita....non mi aspetto niente altro!!
//		Socket tcpClientSocket = new Socket();
//		try {
//			tcpClientSocket.connect(new InetSocketAddress(BOOTSTRAP_ADDRESS, ErraNodePorts.PORT_PRINCE_DEPARTED_NODE), CONNECTION_TIMEOUT);
//			String leaveMessage="E@" + getMyIP();
//			DataOutputStream streamToServer = new DataOutputStream(tcpClientSocket.getOutputStream());
//			streamToServer.writeBytes(leaveMessage + '\n');	
//			tcpClientSocket.close(); 
//		} catch(IOException e) {
//			System.err.println("Il bootstrap non e' più raggiungibile");
//		}
//	}
//
//	//=================== Visualizza a video la topologia di ERRA ====================
//	private void showTopology() {
//		System.out.println("=======================================");
//		System.out.println(nodes.size() + " nodi attivi nella rete.");
//		System.out.println("IP");
//		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
//			ErraNode currentNode = entry.getValue();
//			System.out.println(currentNode.getIPAddress());
//		}
//		System.out.println("=======================================");
//	}
//
//	//=========Questa funzione apre un file e lo spezzetta in pacchetti pronti per essere inviati!
//	private LinkedList<byte[]> wrap(String filename, String erraDest) {
//		File file = new File(filename);
//		if ((int)file.length()==0)
//			return null;
//
//		int packets = nodes.size() - 2;		//Questa e' la base di partenza
//
//		if (nodes.size() <= 3)				//Se siamo in 3 o meno, esiste solo un percorso che posso fare da A a B, quindi spezzo in un solo pacchetto!!
//			packets = 1;
//
//		//Non vogliamo che, anche se ci sono diversi utenti nella rete, il file contenga un payload troppo piccolo
//		while((int)file.length() / packets<MINIMUM_PAYLOAD && packets > 1) {
//			packets--;	
//		}
//
//		//Non vogliamo nemmeno jumbo packets!
//		while((int)file.length() / packets>MAX_PAYLOAD) {
//			packets++;	
//		}
//
//		int packetsLength=(int)file.length() / packets;
//		int residualPck = 0;
//
//		if ((int)file.length() % packets != 0) {
//			residualPck = (int)file.length() - packets * packetsLength;
//			packets = packets + 1;
//		}
//
//		System.out.print("Reading file [" + filename + "]...");
//		LinkedList<byte[]> pcks= new LinkedList<byte[]>();		//Questa lista contiene i pacchetti pronti per essere spediti
//
//		try {
//			InputStream input = null;
//			try {
//				input = new BufferedInputStream(new FileInputStream(file));
//				Random random = new Random();
//				int sn = random.nextInt(5000);
//
//				for(int i=0; i < packets; i++) {   
//					byte[] data;
//					if (i == packets - 1 && residualPck != 0) {
//						data = new byte[residualPck];
//						input.read(data, 0, residualPck);
//					} else {
//						data = new byte[packetsLength];
//						input.read(data, 0, packetsLength);
//					}
//
//					List<ErraNode> list = new ArrayList<ErraNode>(nodes.values());
//					Collections.shuffle(list);
//					int routingLen = (nodes.size() - 1) * 4;
//					byte[] routing = new byte[routingLen];
//					int offset = 0;
//					for (Iterator<ErraNode> it = list.iterator(); it.hasNext();) {
//						ErraNode element = (ErraNode)(it.next());
//						String ipAddress = element.getIPAddress();
//						String myIPAddress = getMyIP();
//						if(!(ipAddress.equals(myIPAddress)) && !(ipAddress.equals(erraDest))) {
//							byte[] bytesIP = InetAddress.getByName(ipAddress).getAddress();
//							System.arraycopy(bytesIP, 0, routing, 4*offset++,4);	
//						}
//					}
//					//Infine copio l'indirizzo IP del in finale!!
//					byte[] bytesIP = InetAddress.getByName(erraDest).getAddress();
//					System.arraycopy(bytesIP, 0, routing, 4*offset,4 );	
//
//					String name = "";
//					if (filename.lastIndexOf('/') != -1) {	//Sono in ambiente LINUX e si usano le / a destra
//						name = filename.substring(filename.lastIndexOf('/') + 1);
//					} else if (filename.lastIndexOf('\\') != -1) {	//Sono in ambiente 	windows e uso le \ a sinistra
//						name = filename.substring(filename.lastIndexOf('\\') + 1);
//					}
//					String header = "";
//					header = Integer.toString(sn + i) + "@" + packets + "@" + name + "@";
//					byte[] headerB = header.getBytes();
//					byte[] headlengthByte = new byte[4];
//					headlengthByte = ByteBuffer.allocate(4).putInt(headerB.length).array();
//					byte[] routingLengthByte = new byte[4];
//					routingLengthByte = ByteBuffer.allocate(4).putInt(routing.length / 4).array();
//					byte[] packet = new byte[8 + routing.length + headerB.length + data.length];
//					System.arraycopy(routingLengthByte, 0, packet, 0, 4);
//					System.arraycopy(headlengthByte, 0, packet, 4, 4);
//					System.arraycopy(routing, 0, packet, 8, routing.length);
//					System.arraycopy(headerB, 0, packet,8+routing.length, headerB.length);
//					System.arraycopy(data, 0, packet, 8+routing.length+headerB.length, data.length);
//					pcks.add(packet);
//				}
//			} finally {	
//				System.out.println("finished: "+(int)file.length()+" bytes successfully read.\nThe file will be splitted into "+packets+" packets.");
//				input.close();
//			}
//		} catch (FileNotFoundException ex) {
//			System.out.println("File not found");
//		}
//		catch (IOException ex) {
//			System.out.println(ex);
//		}
//		return pcks;
//	}
//
//	//=========Questa funzione consente di inviare un file ad un erra host ================
//	private void send() throws UnsupportedEncodingException, UnknownHostException {
//
//		if (nodes.size() <= 1) {
//			System.out.println("Per inviare un file e' necessario che nella rete vi sia almeno un altro erraHost oltre a te!");
//			return;
//		}
//
//		JFileChooser chooser = new JFileChooser();				//Mostro una finestra per la scelta del file da inviare
//		chooser.setCurrentDirectory(new File("."));
//		int r = chooser.showOpenDialog(new JFrame());
//		if (r == JFileChooser.APPROVE_OPTION) {
//			String path=chooser.getSelectedFile().getPath();
//			System.out.println("Inserire l'IP del destinatario tra quelli elencati.");
//
//			for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) {
//				ErraNode currentNode = entry.getValue();
//				String IP=currentNode.getIPAddress();
//				if (!(IP.equals(getMyIP())))
//					System.out.println(IP);
//			}
//
//			String ipDest="";
//			boolean valid=false;
//			while (!valid) {
//				Scanner keyboard = new Scanner(System.in);
//				String input = keyboard.next();
//				if (nodes.containsKey(input) && !input.equals(getMyIP())) {
//					ipDest=input;break;
//				} else {
//					System.err.println("Hai inserito un destinatario non valido, riprova.");
//				}
//			}
//
//			LinkedList<byte[]> pcks=wrap(path,ipDest);		//Pacchettizzo il file e mi preparo per l'invio
//
//			if (pcks==null || pcks.size()==0) {
//				System.err.println("Impossibile acquisire il file specificato.");
//				return;
//			}
//
//			int i=1;
//			for (Iterator<byte[]> it = pcks.iterator(); it.hasNext();) {
//				byte[] packet = (byte[])(it.next());
//				byte[] next=new byte[4];				//Estraggo il primo IP della catena
//				System.arraycopy(packet, 8, next, 0, 4);
//				String nextIP=InetAddress.getByAddress(next).getHostAddress();
//				try {	
//					Socket TCPClientSocket = new Socket();
//					TCPClientSocket.connect(new InetSocketAddress(nextIP, ErraNodePorts.PORT_SUBJECT_FILE_FORWARDING), CONNECTION_TIMEOUT);
//					OutputStream out = TCPClientSocket.getOutputStream(); 
//					DataOutputStream dos = new DataOutputStream(out);
//					dos.write(packet, 0, packet.length);
//					TCPClientSocket.close(); 
//					System.out.println("Il pacchetto "+(i++)+"/"+pcks.size()+" all'indirizzo IP "+nextIP+" e' stato inviato");	
//				}
//				catch (IOException e)
//				{	
//					System.err.println("Il pacchetto "+(i++)+"/"+pcks.size()+" all'indirizzo IP "+nextIP+" non e' stato recapitato");	
//				}
//			}
//			System.out.println("File processing completed");
//			return;
//		}
//	}
//
//
//	//========== Restituisce l'IP dell'erraHost specificato =========================
//
//	public static boolean openBootstrapFile()
//	{	
//		int connectedBootstrap=0;
//		JFileChooser chooser = new JFileChooser();				//Mostro una finestra per la scelta del file da inviare
//		chooser.setCurrentDirectory(new File("."));
//		int r = chooser.showOpenDialog(new JFrame());
//		if (r == JFileChooser.APPROVE_OPTION) 
//		{
//			String path=chooser.getSelectedFile().getPath();
//			//Ora apro questo file che contiene gli indirizzi IP di tutti i bootstrap
//			File file = new File(path);
//			BufferedReader reader = null;
//			try 
//			{
//				reader = new BufferedReader(new FileReader(file));
//				String IP = null;
//				while ((IP = reader.readLine()) != null) 
//				{
//					if (validate(IP))
//						if(initializeErra(IP))connectedBootstrap++;
//				}
//			} 
//			catch (FileNotFoundException e) 
//			{e.printStackTrace();} 
//			catch (IOException e) 
//			{e.printStackTrace();} 
//			finally 
//			{
//				try {
//					if (reader != null) 
//					{
//						reader.close();
//					}
//				} catch (IOException e) 
//				{}
//			}
//		}
//		if (connectedBootstrap>0)
//			return true;
//		else
//			return false;
//	}
//
//	public static boolean validate(final String ip)
//	{        String PATTERN = 
//	"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
//			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
//			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
//			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
//	Pattern pattern = Pattern.compile(PATTERN);
//	Matcher matcher = pattern.matcher(ip);
//	return matcher.matches();             
//	}
//
//	public static String getMyIP()
//	{
//		String ipAddress = null;
//		Enumeration<NetworkInterface> net = null;
//		try {
//			net = NetworkInterface.getNetworkInterfaces();
//		} catch (SocketException e) {
//			throw new RuntimeException(e);
//		}
//		while(net.hasMoreElements()){
//			NetworkInterface element = net.nextElement();
//			Enumeration<InetAddress> addresses = element.getInetAddresses();
//			while (addresses.hasMoreElements()){
//				InetAddress ip = addresses.nextElement();
//				if (ip instanceof Inet4Address){
//					if (ip.isSiteLocalAddress())
//					{
//						ipAddress = ip.getHostAddress();
//
//					}
//				}
//			}
//		}
//		return ipAddress;
//	}
//
//	public static void main(String[] args) throws InterruptedException, IOException
//	{	
//
//		boolean esito=initializeErra("192.168.0.4");
//
//		if (!esito)
//		{
//			System.err.println("Connessione fallita con tutti i nodi bootstrap...l'applicazione verrà chiusa.");
//			System.exit(0);
//			return;
//		}
//
//
//		showTopology();
//
//		answerAliveRequest imAlive=new answerAliveRequest();
//		imAlive.start();
//
//		RefreshTopologyThread refresh=new RefreshTopologyThread();
//		refresh.start();
//
//		FM=new fileManager();
//		ListenToForwardThread F=new ListenToForwardThread();
//		F.start();
//		Scanner keyboard = new Scanner(System.in);
//
//		while(true)
//		{	
//			String input = keyboard.nextLine();
//			if (input.equals("E"))
//			{
//				System.out.println("Segnalo al bootstrap che me ne sto andando...");
//				try
//				{
//					sayGoodbye();
//					imAlive.releasePort();	//Chiudo la porta sulla quale stavo ascoltando 
//					refresh.releasePort();	//Chiudo la porta sulla quale stavo ascoltando
//					F.releasePort();		//Chiudo la porta sulla quale stavo ascoltando
//					System.out.println("Chiusura di tutti i thread completata");
//					System.exit(0);
//					return;
//				}
//				catch (UnknownHostException e){e.printStackTrace();}
//				catch (IOException e){e.printStackTrace();}
//			}
//			if (input.equals("S"))
//			{
//				send();
//			}
//		}
//	} 
//
//}
