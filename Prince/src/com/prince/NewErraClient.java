package com.prince;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class NewErraClient 
{
	public static int CONNECTION_TIMEOUT=5000;		

	public static int MINIMUM_PAYLOAD=100;
	public static int MAX_PAYLOAD=2048000;
	
	public static int UDP_PORT_ALIVE=7000;
	public static int UDP_ALIVE_ANSWER=8003;

	public static int TCP_PORT_WELCOME=7001;
	public static int TCP_PORT_FORWARDING=7002;
	public static int TCP_PORT_SENDING=7003;
	public static int TCP_PORT_REFRESH=7004;
	public static int TCP_BOOTSTRAP_PORT_WELCOME=8001;
	public static int TCP_BOOTSTRAP_PORT_GOODBYE=8002;

	public static String BOOTSTRAP_ADDRESS="192.168.0.4";
	public static String ERRA_ADDRESS="";
	public static int TOTAL_DEVICES=3;
	
	public static fileManager FM;		//Questo oggetto, disponibile a tutti, si occuperà di gestire i file pacchettizzati
	
	
	//============== Classi per gestire la generazione di files e il loro riassemblaggio  ====================
	
	public static class file
	{	public String fileName;
		public int packets;
		public java.util.Date addDate;
		private List<filePart> parts = new ArrayList<filePart>();
		
		public file()
		{
			fileName="";packets=0;addDate=(java.util.Date) Calendar.getInstance().getTime();
		}

		public file(String name,int n)
		{
			fileName=name;
			packets=n;
			addDate=(java.util.Date) Calendar.getInstance().getTime();
		}
		
		public boolean add(int SN,byte[] data) 			//Restituisco true se l'aggiunta del pacchetto ha completato il file
		{
			filePart t=new filePart(SN,data);
			parts.add(t);
			System.out.println("Ricevuto frammento "+SN+ " ["+data.length+" bytes] appartenente al file "+fileName);
			if (parts.size()==packets)
			{
				//Ora che ho tutti gli elementi estraggo, riordino, scrivo!
				Collections.sort(parts);
				OutputStream output = null;
				try{
					
					if (fileName.lastIndexOf("\\")!=-1)
					{
						String FN=fileName.substring(fileName.lastIndexOf("\\"));
						FN=FN.replace("\\", "");
						output = new BufferedOutputStream(new FileOutputStream(FN));
					}
					else
						output = new BufferedOutputStream(new FileOutputStream(fileName));
					
					try
					{
						for (Iterator<filePart> i = parts.iterator(); i.hasNext();)
						{
							filePart f = (filePart)(i.next());
							output.write(f.data);
						}
					}
					finally
					{	
						System.out.println("Ricezione del file "+fileName+" completata. Il file e' stato scritto correttamente");
						output.close();
					}
					return true;
				}
				catch (IOException e)
				{System.err.println("Impossibile generare il file");}
			}
			return false;
		}	
		
		private static class filePart implements Comparable<filePart>
		{
			public filePart(int N,byte[] d)
			{
				SN=N;
				data=new byte[d.length];
				System.arraycopy(d, 0, data, 0, d.length);
			}
			public int compareTo(filePart compareObject) 
			{ 
				if (SN < compareObject.SN) return -1; else if (SN == compareObject.SN) return 0; else return 1;
			}
			
			public int SN;
			public byte[] data;

		}
	}

	
	public static class fileManager
	{
		private List<file> fileList; 		//Contiene i file di cui ho ricevuto parte del contenuto e che stanno per essere scritti

		public fileManager(){fileList=null;}
		
		public void add(String header,byte[] packet)
		{
			int SN=Integer.parseInt(header.substring(0,header.indexOf("@")));		
			header=header.substring(header.indexOf("@")+1);
			
			int parts=Integer.parseInt(header.substring(0,header.indexOf("@")));
			header=header.substring(header.indexOf("@")+1);
			
			String filename=header.substring(0,header.indexOf("@"));
			
			boolean esito=false;
			file t=null;
			
			if (fileList==null)				//Nessun file e' presente, significa che tutti i file sono stati scritti e questa e' la primissima parte di un file che richevo
			{
				t=new file(filename,parts);
				esito=t.add(SN,packet);
				fileList= new ArrayList<file>();
				fileList.add(t);
			}
			else
			{
				boolean pending=false;
				for (Iterator<file> i = fileList.iterator(); i.hasNext();)
				{
				    t = (file)(i.next());
				    if (t.fileName.equals(filename))
				    {
				    	pending=true;
				    	esito=t.add(SN,packet);
				    }
				}
				
				if(!pending)
				{
					//Significa che, sebbene vi siano altri file pending, questo frammento appartiene ad un altro file!
					t=new file(filename,parts);
					t.add(SN,packet);
					esito=fileList.add(t);
				}
			}
			if (esito)
			{
				//Significa che il frammento ricevuto ha completato il file, che va quindi rimosso da quelli in sospeso
				fileList.remove(t);
			}
		}
	
		public int filePending(){return (fileList==null)?0:fileList.size();}
		
	}
	

	public static class erraHost
	{	public String IP;
		public String erraAddress;
		public erraHost(){IP="";erraAddress="";}
		public erraHost(String IP,String Address){this.IP=IP;erraAddress=Address;}
	}


	//Questa struttura contiene tutti i nodi attivi
	
	public static Map<String, ErraNode> nodes;
	
	//============== Funzione per inizializzare il sistema ERRA e scoprirne la topologia ======================

	public static boolean initializeErra(String address)
	{
		nodes = new HashMap<String, ErraNode>();
		
		try
		{
			Socket TCPClientSocket = new Socket();
			
			try
			{
				TCPClientSocket.connect(new InetSocketAddress(address,TCP_BOOTSTRAP_PORT_WELCOME),CONNECTION_TIMEOUT);
			}
			catch (IOException e)
			{	System.err.println("Impossibile raggiungere il bootstrap node "+address);
				TCPClientSocket.close();
				return false;
			}
			System.out.println("...connessione avvenuta correttamente.");
			
			String joinMessage="J";

			DataOutputStream streamToServer = new DataOutputStream(TCPClientSocket.getOutputStream());
			BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(TCPClientSocket.getInputStream()));

			streamToServer.writeBytes(joinMessage + '\n');	

			//Aspetto la risposta dal bootstrap che contiene la topologia della rete
			String fromServer="";
			fromServer=streamFromServer.readLine();
			TCPClientSocket.close(); 
			
			//Ora analizzo la stringa per ricostruire la topologia della rete
			if (fromServer.charAt(0)=='W')
			{
				fromServer=fromServer.substring(2);		//Taglio la welcome portion
				updateStructure(fromServer);		
			}
			else
			{
				System.err.println("Ho ricevuto sulla porta "+TCP_PORT_WELCOME+" un dato che non e' una tabella della rete!!");
			}
		}
		catch (ConnectException e)
		{
			e.printStackTrace();
			return false;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	
	//============== Thread per la gestione delle risposte UDP al bootstrap ==========

	public static final class answerAliveRequest extends Thread
	{	
		private static DatagramSocket UDP;
		
		public void run()
		{	
			try
			{	UDP = new DatagramSocket(UDP_PORT_ALIVE);
				byte[] receivedData = new byte[1024];
				byte[] sendData = new byte[1024];

			while(true)
			{	
				DatagramPacket receivedPacket = new DatagramPacket(receivedData, receivedData.length);
				try
				{
					UDP.receive(receivedPacket);
				}
				catch(SocketException e)
				{
					System.out.println("Il socket UDP che risponde ai ? e' stato chiuso.");
					return;	//Questo chiude anche il thread
				}
				String message=new String(receivedPacket.getData(),0,receivedPacket.getLength());
				if (message.charAt(0)!='?')
				{
					System.err.println("Ho ricevuto sulla porta "+UDP_PORT_ALIVE+" un pacchetto che non riesco a decodificare.");
					System.err.println("Il pacchetto e': "+message);
				}
				else
				{
					String sentence = "!@"+ERRA_ADDRESS;
					sendData = sentence.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivedPacket.getAddress(), UDP_ALIVE_ANSWER);
					UDP.send(sendPacket);
				}
			}
			}
			catch (ConnectException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	
		public void releasePort() 
		{UDP.close();}

	}
	

	//============== Thread per ascoltare se il bootstrap comunica eventuali refresh sulla topologia ============

	public static class refreshTopology extends Thread
	{ 	
		private ServerSocket server;
		private Socket s;
		
		public void run()
		{	
			while(true)
			{
				try
				{	
					server=new ServerSocket(TCP_PORT_REFRESH);		//Mi metto in ascolto in attesa di connessioni TCP
					try
					{	
						s=server.accept();							//Quanto ho una richiesta di connessione la accetto!
					}
					catch (SocketException e)
					{	System.out.println("Il socket TCP per il refresh della topologia e' stato chiuso");
						return;
					}
					BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
					String table="";
					table=streamFromServer.readLine();
					s.close();
					System.out.println("Aggiornamento sulla topologia di ERRA: "+table);
					
					if (table.charAt(0)=='T') //Significa che il bootstrap segnala la variazione su un singolo nodo, devo aggiornare solo una parte della topologia
					{	
						//T@-192.168.1.1#192.168.1.3#
						table=table.substring(2);
						if (table.charAt(0)=='-')
						{	
							table=table.substring(1);
							while (table.length()>0)
							{
								String IP=table.substring(0,table.indexOf("#"));
								for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) 
								{
									ErraNode currentNode = entry.getValue();
									if(currentNode.getIPAddress().equals(IP))
									{
										nodes.remove(IP);
										break;
									}
								}
								table=table.substring(table.indexOf("#")+1);
							}
						}
						else if (table.charAt(0)=='+')
						{	
							table=table.substring(1);
							while (table.length()>0)
							{
								String IP=table.substring(0,table.indexOf("#"));
								if(!(nodes.containsKey(IP)))
								{
									ErraNode e=new ErraNode(IP);
									nodes.put(IP, e);
								}
								table=table.substring(table.indexOf("#")+1);
							}
						}
						showTopology();
					}
					else
					{
						System.err.println("Ho ricevuto sulla porta "+TCP_PORT_REFRESH+" un dato che non e' una tabella della rete!!");
					}
					server.close();
					s.close();
				}
				catch (ConnectException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	
		public void releasePort() throws IOException
		{
			if (s!=null)
			{
				s.close();
				return;
			}
			server.close();
				
		}
	
	}


	//Questo thread ascolta sulla porta 7002. Quando riceve una richiesta di connessione TCP genera un nuovo thread che si occupa di fare forwarding
	
	public static class listenToForward extends Thread
	{
		private ServerSocket serverSocket; 
		private Socket s;
		
		public void run()
		{
			try
			{
				serverSocket = new ServerSocket(TCP_PORT_FORWARDING);
				
				while(true)
				{
					 s = serverSocket.accept();
					 SocketAddress A=s.getRemoteSocketAddress();
					 String IP=A.toString().substring(1, A.toString().indexOf(":"));
					 //TODO da testare questo caso
					 if (!(nodes.containsKey(IP)))
					 {
						 System.err.println("Richiesta abusiva, pacchetto non inoltrato");
					 }
					 else
					 {
						 System.out.println("Faccio il forwarding di un pacchetto autorizzato che proviene da "+A.toString());
						 forward F=new forward(s);
						 F.start();
					 }
				}
			}
			catch (IOException e)
			{
				System.out.println("Il socket TCP per il forwarding e' stato chiuso");
			}
		}	
	
		public void releasePort() throws IOException
		{
			serverSocket.close();
		}
	
	
	}
	
	
	//Questo thread legge un ERRA PACKET e se e' per me lo tiene, altrimenti fa forwarding
	
	public static class forward extends Thread
	{
		private Socket mySocket;
		
		public forward(Socket s)
		{	
			mySocket=s;
		}
		
		public void run()
		{
			try
			{
				/*
				 * Tutta questa prima parte serve per leggere uno stream di bytes da un socket TCP.
				 * Il risultato della lettura e' memorizzato nella varibile byte[] packet.
				 */
				
				InputStream inputStream = mySocket.getInputStream();  
				ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
				byte[] content = new byte[2048];  
				int bytesRead = -1;  
				while((bytesRead = inputStream.read(content))!= -1) 
				{  
					baos.write( content, 0, bytesRead );  
				} 

				byte[] packet=baos.toByteArray();
				
				byte[] routingL=new byte[4];
				System.arraycopy(packet, 0, routingL, 0, 4);
				
				byte[] headerL=new byte[4];
				System.arraycopy(packet, 4, headerL, 0, 4);
				
				int rL = ByteBuffer.wrap(routingL).getInt();		//Questo mi esprime il numero di indirizzi IP contenuti nel routing		
				int hL = ByteBuffer.wrap(headerL).getInt();
				
				if (rL==1)
				{
					//Il pacchetto ha raggiunto il destinatario, che sono io!
					byte[] header=new byte[hL];
					System.arraycopy(packet, 12, header, 0, hL);
					String sH=new String(header, "US-ASCII");
					byte[] data=new byte[packet.length-12-hL];
					System.arraycopy(packet, 12+hL, data, 0, data.length);
					FM.add(sH,data);	//Ficco dentro questo frammento all'interno della classe che si occupa di gestire il tutto*/
				}
				else
				{
					//Il pacchetto non è assolutamente per me, purtroppo
					//Devo rimuovere il mio IP, ricavare il prossimo e fare il forwarding
					byte[] next=new byte[4];				//Estraggo il primo IP della catena
				    System.arraycopy(packet, 12, next, 0, 4);		//4byte x Rl, 4 byte per hL, 4 byte mio IP, 4 byte prossimo
				    String nextIP=InetAddress.getByAddress(next).getHostAddress();
				    
				    byte[] forwardPacket=new byte[packet.length-4];	//Il nuovo pacchetto ha solamente l'indirizzo IP in meno!
				    
				    byte[] newRoutingLen= new byte[4];
				    newRoutingLen=ByteBuffer.allocate(4).putInt(rL-1).array();
				    
				    System.arraycopy(newRoutingLen, 0, forwardPacket, 0, 4);				//Riscrivo la lunghezza del campo routing
				    System.arraycopy(packet, 4, forwardPacket, 4, 4);						//Copio la lunghezza del campo header
	      			System.arraycopy(packet, 12, forwardPacket, 8,packet.length-12);		//Riscrivo a partire dal 2° IP tutto quanto

					try
					{
						Socket TCPClientSocket = new Socket();
						TCPClientSocket.connect(new InetSocketAddress(nextIP,TCP_PORT_FORWARDING),CONNECTION_TIMEOUT);
						OutputStream out = TCPClientSocket.getOutputStream(); 
						DataOutputStream dos = new DataOutputStream(out);
						dos.write(forwardPacket, 0, forwardPacket.length);
						TCPClientSocket.close(); 
						System.out.println("Forwarding all'indirizzo "+nextIP+" completato.");	
					}
					catch (IOException e)
					{
						System.err.println("Forwarding all'indirizzo "+nextIP+" fallito.");	
						
						//Qui bisogna provare a fare il recovery del file.
						
						
					}
				}
				return;	
			}
			catch (ConnectException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	//=================== Riceve una stringa contenente la topologia di rete e aggiorna la lista ====================

	public static void updateStructure(String fromServer)

	{
		nodes.clear();
		//Ora procedo alla memorizzazione nei vettori della rete
		while(fromServer.length()>0)
		{
			//Estraggo host per host le informazioni
			String IP=fromServer.substring(0,fromServer.indexOf('#'));
			ErraNode e=new ErraNode(IP);
			nodes.put(IP,e);
			fromServer=fromServer.substring(fromServer.indexOf('#')+1);
		}
	}

	
	//=================== Segnala al nodo BOOTSTRAP che me ne sto andando ====================


	public static void sayGoodbye() throws UnknownHostException, IOException
	{
		//Mi connetto con il bootstrap e gli segnalo la mia dipartita....non mi aspetto niente altro!!
		Socket TCPClientSocket = new Socket();
		
		try
		{
			TCPClientSocket.connect(new InetSocketAddress(BOOTSTRAP_ADDRESS, TCP_BOOTSTRAP_PORT_GOODBYE),CONNECTION_TIMEOUT);
			String leaveMessage="E@"+getMyIP();
			DataOutputStream streamToServer = new DataOutputStream(TCPClientSocket.getOutputStream());
			streamToServer.writeBytes(leaveMessage + '\n');	
			TCPClientSocket.close(); 
		}
		catch(IOException e)
		{
			System.err.println("Il bootstrap non e' più raggiungibile");
		}
		
	}

	
	//=================== Visualizza a video la topologia di ERRA ====================
	

	public static void showTopology()
	{
		System.out.println("=======================================");
		System.out.println(nodes.size()+" nodi attivi nella rete.");
		System.out.println("IP");
		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) 
		{
			ErraNode currentNode = entry.getValue();
			System.out.println(currentNode.getIPAddress());
		}
		System.out.println("=======================================");
	}
	
	
	//=========Questa funzione apre un file e lo spezzetta in pacchetti pronti per essere inviati!
	
	public static LinkedList<byte[]> wrap(String filename,String erraDest)
	{
		    File file = new File(filename);
		    if((int)file.length()==0)
		    	return null;
		    
		    int packets=nodes.size()-2;		//Questa e' la base di partenza
		    
		    
		    if (nodes.size()<=3)				//Se siamo in 3 o meno, esiste solo un percorso che posso fare da A a B, quindi spezzo in un solo pacchetto!!
		    	packets=1;
		  
		    //Non vogliamo che, anche se ci sono diversi utenti nella rete, il file contenga un payload troppo piccolo
		    while((int)file.length()/packets<MINIMUM_PAYLOAD && packets>1)
		    {
		    	packets--;	
		    }

		    //Non vogliamo nemmeno jumbo packets!
		    while((int)file.length()/packets>MAX_PAYLOAD)
		    {
		    	packets++;	
		    }

		    
		    int packets_length=(int)file.length()/packets;
		    int residual_pck=0;
		 
		    
		    if ((int)file.length()%packets!=0)
		    {
		    	residual_pck=(int)file.length()-packets*packets_length;
		    	packets=packets+1;
		    }
		    
		    System.out.print("Reading file ["+filename+"]...");
		    
		    LinkedList<byte[]> pcks= new LinkedList<byte[]>();		//Questa lista contiene i pacchetti pronti per essere spediti

		    try 
		    { 	InputStream input = null;
		      	try 
		      	{
		      		input = new BufferedInputStream(new FileInputStream(file));
		      		Random random = new Random();
		      		int SN = random.nextInt(5000);
		      		
		      		for(int i=0;i<packets;i++)
		      		{   byte[] data;
		      		
		      			if (i==packets-1 && residual_pck!=0)
		      			{
		      				data= new byte[residual_pck];
		      				input.read(data,0,residual_pck);
		      			}
		      			else
		      			{
		      				data = new byte[packets_length];
		      				input.read(data,0,packets_length);		      				
		      			}
		      			
		      			List<ErraNode> list = new ArrayList<ErraNode>(nodes.values());
		      			Collections.shuffle(list);
		      			
		      			int routingLen=(nodes.size()-1)*4;
		      			
		      			byte[] routing=new byte[routingLen];
		      			int offset=0;
		      			for (Iterator<ErraNode> it = list.iterator(); it.hasNext();)
						{
		      				ErraNode element = (ErraNode)(it.next());
		      				String IP=element.getIPAddress();
		      				String myIPAddress = getMyIP();
		      				if(!(IP.equals(myIPAddress)) && !(IP.equals(erraDest)))
		      				{
		      					byte[] bytesIP = InetAddress.getByName(IP).getAddress();
		      					System.arraycopy(bytesIP, 0, routing, 4*offset++,4);	
		      				}
						}
		      			//Infine copio l'indirizzo IP del in finale!!
		      			byte[] bytesIP = InetAddress.getByName(erraDest).getAddress();
      					System.arraycopy(bytesIP, 0, routing, 4*offset,4 );	
		      			
		      			String name="";
		      			if(filename.lastIndexOf('/')!=-1)
		      			{	//Sono in ambiente LINUX e si usano le / a destra
		      				name=filename.substring(filename.lastIndexOf('/')+1);
		      			}
		      			else if(filename.lastIndexOf('\\')!=-1)
		      			{	//Sono in ambiente 	windows e uso le \ a sinistra
		      				name=filename.substring(filename.lastIndexOf('\\')+1);
		      			}

		      			String header="";
		      		    header=Integer.toString(SN+i)+"@"+packets+"@"+name+"@";
		      
		      			byte[] headerB=header.getBytes();
		      			
		      			byte[] headlengthByte= new byte[4];
		      			headlengthByte=ByteBuffer.allocate(4).putInt(headerB.length).array();

		      			byte[] routingLengthByte= new byte[4];
		      			routingLengthByte=ByteBuffer.allocate(4).putInt(routing.length/4).array();
		      			
		      			byte[] packet=new byte[8+routing.length+headerB.length+data.length];
		      			
		      			System.arraycopy(routingLengthByte, 0, packet, 0, 4);
		      			System.arraycopy(headlengthByte, 0, packet, 4, 4);
		      			System.arraycopy(routing, 0, packet, 8, routing.length);
		      			System.arraycopy(headerB, 0, packet,8+routing.length, headerB.length);
		      			System.arraycopy(data, 0, packet, 8+routing.length+headerB.length, data.length);

		      			pcks.add(packet);
		      		}
		      }
		      finally 
		      {	System.out.println("finished: "+(int)file.length()+" bytes successfully read.\nThe file will be splitted into "+packets+" packets.");
		    	input.close();
		      }
		    }
		    catch (FileNotFoundException ex) {
		    	System.out.println("File not found");
		    }
		    catch (IOException ex) 
		    {
		    	System.out.println(ex);
		    }
		    return pcks;
	}

	
	//=========Questa funzione consente di inviare un file ad un erra host ================


	public static void send() throws UnsupportedEncodingException, UnknownHostException 
	{
		
		if (nodes.size()<=1)
		{
			System.out.println("Per inviare un file e' necessario che nella rete vi sia almeno un altro erraHost oltre a te!");
			return;
		}
		
		JFileChooser chooser = new JFileChooser();				//Mostro una finestra per la scelta del file da inviare
		chooser.setCurrentDirectory(new File("."));
	    int r = chooser.showOpenDialog(new JFrame());
		if (r == JFileChooser.APPROVE_OPTION) 
		{
			String path=chooser.getSelectedFile().getPath();
			System.out.println("Inserire l'IP del destinatario tra quelli elencati.");

			for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) 
			{
				ErraNode currentNode = entry.getValue();
				String IP=currentNode.getIPAddress();
				if (!(IP.equals(getMyIP())))
					System.out.println(IP);
			}

			String IPDest="";
			boolean valid=false;
			while (!valid)
			{
				Scanner keyboard = new Scanner(System.in);
				String input = keyboard.next();
				if (nodes.containsKey(input) && !input.equals(getMyIP()))
				{IPDest=input;break;}
				else
				{System.err.println("Hai inserito un destinatario non valido, riprova.");}
			}

			LinkedList<byte[]> pcks=wrap(path,IPDest);		//Pacchettizzo il file e mi preparo per l'invio
			
			if (pcks==null || pcks.size()==0)
			{
				System.err.println("Impossibile acquisire il file specificato.");
				return;
			}
			
			int i=1;
			for (Iterator<byte[]> it = pcks.iterator(); it.hasNext();)
			{
			    byte[] packet = (byte[])(it.next());
			    byte[] next=new byte[4];				//Estraggo il primo IP della catena
			    System.arraycopy(packet, 8, next, 0, 4);
			    String nextIP=InetAddress.getByAddress(next).getHostAddress();
      			try
      			{	
      				Socket TCPClientSocket = new Socket();
      				TCPClientSocket.connect(new InetSocketAddress(nextIP,TCP_PORT_FORWARDING),CONNECTION_TIMEOUT);
          			OutputStream out = TCPClientSocket.getOutputStream(); 
          			DataOutputStream dos = new DataOutputStream(out);
          			dos.write(packet, 0, packet.length);
          			TCPClientSocket.close(); 
          			System.out.println("Il pacchetto "+(i++)+"/"+pcks.size()+" all'indirizzo IP "+nextIP+" e' stato inviato");	
      			}
      			catch (IOException e)
      			{	
      				System.err.println("Il pacchetto "+(i++)+"/"+pcks.size()+" all'indirizzo IP "+nextIP+" non e' stato recapitato");	
      			}
			}
			System.out.println("File processing completed");
			return;
		}
	}
	
	
	//========== Restituisce l'IP dell'erraHost specificato =========================

	public static boolean openBootstrapFile()
	{	
		int connectedBootstrap=0;
		JFileChooser chooser = new JFileChooser();				//Mostro una finestra per la scelta del file da inviare
		chooser.setCurrentDirectory(new File("."));
		int r = chooser.showOpenDialog(new JFrame());
		if (r == JFileChooser.APPROVE_OPTION) 
		{
			String path=chooser.getSelectedFile().getPath();
			//Ora apro questo file che contiene gli indirizzi IP di tutti i bootstrap
			File file = new File(path);
			BufferedReader reader = null;
			try 
			{
			    reader = new BufferedReader(new FileReader(file));
			    String IP = null;
			    while ((IP = reader.readLine()) != null) 
			    {
			    	if (validate(IP))
			    		if(initializeErra(IP))connectedBootstrap++;
			    }
			} 
			catch (FileNotFoundException e) 
			{e.printStackTrace();} 
			catch (IOException e) 
			{e.printStackTrace();} 
			finally 
			{
			    try {
			        if (reader != null) 
			        {
			            reader.close();
			        }
			    } catch (IOException e) 
			    {}
			}
		}
		if (connectedBootstrap>0)
			return true;
		else
			return false;
	}

	public static boolean validate(final String ip)
	{        String PATTERN = 
	"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
			"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
	Pattern pattern = Pattern.compile(PATTERN);
	Matcher matcher = pattern.matcher(ip);
	return matcher.matches();             
	}

	public static String getMyIP()
	{
		String ipAddress = null;
		Enumeration<NetworkInterface> net = null;
		try {
			net = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}
		while(net.hasMoreElements()){
			NetworkInterface element = net.nextElement();
			Enumeration<InetAddress> addresses = element.getInetAddresses();
			while (addresses.hasMoreElements()){
				InetAddress ip = addresses.nextElement();
				if (ip instanceof Inet4Address){
					if (ip.isSiteLocalAddress())
					{
						ipAddress = ip.getHostAddress();

					}
				}
			}
		}
		return ipAddress;
	}
	
	
	public static void main(String[] args) throws InterruptedException, IOException
	{	
		ERRA_ADDRESS="172.21.6.0";
		boolean esito=initializeErra(ERRA_ADDRESS);

		if (!esito)
		{
			System.err.println("Connessione fallita con tutti i nodi bootstrap...l'applicazione verrà chiusa.");
			System.exit(0);
			return;
		}
		
		
		showTopology();

		answerAliveRequest imAlive=new answerAliveRequest();
		imAlive.start();

		refreshTopology refresh=new refreshTopology();
		refresh.start();

		FM=new fileManager();
		listenToForward F=new listenToForward();
		F.start();
		Scanner keyboard = new Scanner(System.in);
		
		while(true)
		{	
		 	String input = keyboard.nextLine();
			if (input.equals("E"))
			{
				System.out.println("Segnalo al bootstrap che me ne sto andando...");
				try
				{
					sayGoodbye();
					imAlive.releasePort();	//Chiudo la porta sulla quale stavo ascoltando 
					refresh.releasePort();	//Chiudo la porta sulla quale stavo ascoltando
					F.releasePort();		//Chiudo la porta sulla quale stavo ascoltando
					System.out.println("Chiusura di tutti i thread completata");
					System.exit(0);
					return;
				}
				catch (UnknownHostException e){e.printStackTrace();}
				catch (IOException e){e.printStackTrace();}
			}
			if (input.equals("S"))
			{
				send();
			}
		}
	} 
	
}