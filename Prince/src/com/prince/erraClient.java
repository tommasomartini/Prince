package com.prince;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;

public class erraClient 
{
	public static int CONNECTION_TIMEOUT=5000;		

	public static int MINIMUM_PAYLOAD=100;
	
	public static int UDP_PORT_ALIVE=7000;
	public static int UDP_ALIVE_ANSWER=8003;

	public static int TCP_PORT_WELCOME=7001;
	public static int TCP_PORT_FORWARDING=7002;
	public static int TCP_PORT_SENDING=7003;
	public static int TCP_PORT_REFRESH=7004;
	public static int TCP_BOOTSTRAP_PORT_WELCOME=8001;
	public static int TCP_BOOTSTRAP_PORT_GOODBYE=8002;

	public static String BOOTSTRAP_ADDRESS="87.4.245.122";
	public static String ERRA_ADDRESS="";
	public static int TOTAL_DEVICES=3;
	
	public static fileManager FM;		//Questo oggetto, disponibile a tutti, si occuperà di gestire i file pacchettizzati
	
	
	//============== Classi per gestire la generazione di files e il loro riassemblaggio  ====================
	
	public static class file
	{	public String fileName;
		public int packets;
		
		private List<filePart> parts = new ArrayList<filePart>();
		
		public file()
		{
			fileName="";packets=0;
		}

		public file(String name,int n)
		{
			fileName=name;
			packets=n;
		}
		
		public boolean add(int SN,byte[] data) 			//Restituisco true se l'aggiunta del pacchetto ha completato il file
		{
			filePart t=new filePart(SN,data);
			parts.add(t);
			System.out.println("Ricevuto e archiviato frammento con SN "+SN+ " appartenente al file "+fileName);
			if (parts.size()==packets)
			{
				//Ora che ho tutti gli elementi estraggo, riordino, scrivo!
				Collections.sort(parts);
				OutputStream output = null;
				try{
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
						System.out.println("Ricezione del file "+fileName+" completata. Il file è stato scritto correttamente");
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
			
			if (fileList==null)				//Nessun file è presente, significa che tutti i file sono stati scritti e questa è la primissima parte di un file che richevo
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
				System.out.println("Un file è stato completato e rimosso dalla lista che ora conta "+fileList.size()+" elementi.");
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

	//============== Questa lista contiene l'elenco di tutti i dispositivi erra attivi nella rete
	
	public static List<erraHost> topology = new ArrayList<erraHost>();
	
	//============== Funzione per inizializzare il sistema ERRA e scoprirne la topologia ======================

	public static boolean initializeErra()
	{
		try
		{
			Socket TCPClientSocket = new Socket();
			
			try
			{
				TCPClientSocket.connect(new InetSocketAddress(BOOTSTRAP_ADDRESS,TCP_BOOTSTRAP_PORT_WELCOME),CONNECTION_TIMEOUT);
			}
			catch (IOException e)
			{	System.err.println("Impossibile raggiungere il bootstrap node.");
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
				fromServer=fromServer.substring(2);										//Taglio la welcome portion
				ERRA_ADDRESS=fromServer.substring(0,fromServer.indexOf('@'));			//Leggo il mio erraAddress
				fromServer=fromServer.substring(fromServer.indexOf('@')+1);
				updateStructure(fromServer);		
			}
			else
			{
				System.err.println("Ho ricevuto sulla porta "+TCP_PORT_WELCOME+" un dato che non è una tabella della rete!!");
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
		showTopology();
		return true;
	}

	
	//============== Thread per la gestione delle risposte UDP al bootstrap ==========

	private static class answerAliveRequest extends Thread
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
					System.out.println("Il socket UDP che risponde ai ? è stato chiuso.");
					return;	//Questo chiude anche il thread
				}
				String message=new String(receivedPacket.getData());
				if (message.charAt(0)!='?')
				{
					System.err.println("Ho ricevuto sulla porta "+UDP_PORT_ALIVE+" un pacchetto che non riesco a decodificare.");
					System.err.println("Il pacchetto è: "+message);
				}
				else
				{
					System.out.println("Segnalo al bootstrap che sono attivo.");
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

	private static class refreshTopology extends Thread
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
					{	System.out.println("Il socket TCP per il refresh della topologia è stato chiuso");
						return;
					}
					BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
					String table="";
					table=streamFromServer.readLine();
					
					System.out.println("Aggiornamento sulla topologia di ERRA: "+table);
					
					if (table.charAt(0)=='R')		//    R@3@192.168.1.1#25%192.168.1.2#26%192.168.1.3#27%
					{	
						updateStructure(table.substring(2));
						showTopology();
					}
					else if (table.charAt(0)=='T') //Significa che il bootstrap segnala la variazione su un singolo nodo, devo aggiornare solo una parte della topologia
					{	table=table.substring(2);
						if (table.charAt(0)=='-')
						{	
							String brokenErraAddress=table;
							for (Iterator<erraHost> i = topology.iterator(); i.hasNext();) 		//RImuovo dalla struttura il defunto nodo!
							{
							    erraHost element = (erraHost)(i.next());
							    if(element.erraAddress.equals(brokenErraAddress))
							    {
							        i.remove();
							    }
							}
						}
						else if (table.charAt(0)=='+')
						{	
							erraHost H=new erraHost(table.substring(0,table.indexOf("#")),table.substring(table.indexOf("#")+1));
							//Prima di aggiungerlo alla mia topologia, verifico che non sia già presente!
							boolean presente=false;
							for (Iterator<erraHost> i = topology.iterator(); i.hasNext();) 		//RImuovo dalla struttura il defunto nodo!
							{
							    erraHost element = (erraHost)(i.next());
							    if(element.erraAddress.equals(table.substring(table.indexOf("#")+1)))
							    {
							        presente=true;
							        break;
							    }
							}	
							if(!(presente))topology.add(H);
						}
						showTopology();
					}
					else
					{
						System.err.println("Ho ricevuto sulla porta "+TCP_PORT_REFRESH+" un dato che non è una tabella della rete!!");
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
	
	private static class listenToForward extends Thread
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
					 forward F=new forward(s);
					 F.start();
				}
			}
			catch (IOException e)
			{
				System.out.println("Il socket TCP per il forwarding è stato chiuso");
			}
		}	
	
		public void releasePort() throws IOException
		{
			serverSocket.close();
		}
	
	
	}
	
	
	//Questo thread legge un ERRA PACKET e se è per me lo tiene, altrimenti fa forwarding
	
	private static class forward extends Thread
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
				 * Il risultato della lettura è memorizzato nella varibile byte[] packet.
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
				
				byte[] hLen=new byte[4];

				System.arraycopy(packet, 0, hLen, 0, 4);
				int l = ByteBuffer.wrap(hLen).getInt();			
				
				byte[] H=new byte[l];							//Questi sono i bytes che rappresentano l'header

				System.arraycopy(packet, 4, H, 0, l);
				String sH=new String(H, "US-ASCII");			//Questa è la stringa che codifica il mio header

				int j=sH.indexOf("#");
				sH=sH.substring(j+1);							//Tolgo dall'header il mio erraAddress, e poi guardo cosa c'è dopo!
				
				int k=sH.indexOf("#");							//Cerco il prossimo hop da fare, se c'è!
				int y=sH.indexOf("@");
				if (k!=-1 && k<y)
				{	//Il pacchetto è di qualcun altro...devo fare il forwarding. Tolgo il primo erraAddress e ne faccio il forwarding
					String nextHop=sH.substring(0,sH.indexOf("#"));	//Questo è l'erraAddress a cui devo inviare il pacchetto...
					byte[] headlengthByte= new byte[4];			//Questi sono i 4 bytes che descrivono la lunghezza dell'header
					headlengthByte=ByteBuffer.allocate(4).putInt(packet.length-j-5).array();		

					byte[] forwardPacket=new byte[packet.length-nextHop.length()-1];				

					System.arraycopy(headlengthByte, 0, forwardPacket, 0, 4);					
					System.arraycopy(packet, 5+nextHop.length(), forwardPacket, 4, packet.length-5-j);

					String nextIP=getIP(nextHop);
					try
					{
						Socket TCPClientSocket = new Socket();
						TCPClientSocket.connect(new InetSocketAddress(nextIP,TCP_PORT_FORWARDING),CONNECTION_TIMEOUT);
						OutputStream out = TCPClientSocket.getOutputStream(); 
						DataOutputStream dos = new DataOutputStream(out);
						dos.write(forwardPacket, 0, forwardPacket.length);
						TCPClientSocket.close(); 
						System.out.println("Forwarding all'erraHost "+nextHop+" completato.");	
					}
					catch (IOException e)
					{

						System.err.println("Forwarding all'erraHost "+nextHop+" fallito");	
					}
				}
				else
				{
					//Il pacchetto è mio e solamento mio (tessoooooro)
					//Estraggo dal mio pacchetto la porzione che riguarda il payload.
					int dataSize=packet.length-l-4;
					byte[] data=new byte[dataSize];
					System.arraycopy(packet, 4+l, data, 0,dataSize);	
					FM.add(sH,data);	//Ficco dentro questo frammento all'interno della classe che si occupa di gestire il tutto
				}
				try
				{
					mySocket.close();
				}
				catch (IOException e)
				{
					
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
		//Si tratta di una stringa di refresh che contiene la nuova topologia di rete
		TOTAL_DEVICES=Integer.parseInt(fromServer.substring(0,fromServer.indexOf('@')));
		fromServer=fromServer.substring(fromServer.indexOf('@')+1);

		topology.clear();
		
		//Ora ho spacchettato le informazioni generali e procedo alla memorizzazione nei vettori della rete
		while(fromServer.length()>0)
		{
			//Estraggo host per host le informazioni
			String host=fromServer.substring(0,fromServer.indexOf('%'));
			String IP=host.substring(0,host.indexOf('#'));host=host.substring(host.indexOf('#')+1);
			String Address=host;
			topology.add(new erraHost(IP,Address));
			fromServer=fromServer.substring(fromServer.indexOf('%')+1);
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
			String leaveMessage="E@"+ERRA_ADDRESS;
			DataOutputStream streamToServer = new DataOutputStream(TCPClientSocket.getOutputStream());
			streamToServer.writeBytes(leaveMessage + '\n');	
			TCPClientSocket.close(); 
		}
		catch(IOException e)
		{
			System.err.println("Il bootstrap non è più raggiungibile");
		}
		
	}

	
	//=================== Visualizza a video la topologia di ERRA ====================
	
	public static void showTopology()
	{
		//Questa è una funzione di supporto, mostra a video le informazioni sugli ERRA devices presenti
		System.out.println("=======================================");
		System.out.println(topology.size()+" nodi attivi");
		for (Iterator<erraHost> i = topology.iterator(); i.hasNext();) 
		{
		    erraHost element = (erraHost)(i.next());
		    System.out.println(element.erraAddress+'\t'+element.IP);
		}
		System.out.println("=======================================");
	}
	
	
	//=========Questa funzione apre un file e lo spezzetta in pacchetti pronti per essere inviati!
	
	public static LinkedList<byte[]> wrap(String filename,String erraDest)
	{
		    File file = new File(filename);
		    if((int)file.length()==0)
		    	return null;
		    
		    int packets=topology.size()-1;		//Questa è la base di partenza
		    
		    if (topology.size()<=3)				//Se siamo in 3 o meno, esiste solo un percorso che posso fare da A a B, quindi spezzo in un solo pacchetto!!
		    	packets=1;

		    while((int)file.length()/packets<MINIMUM_PAYLOAD && packets>1)
		    {
		    	packets--;	//Non vogliamo che, anche se ci sono diversi utenti nella rete, il file contenga un payload troppo piccolo
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
		      		
		      			if (i==packets-1 && packets>1)
		      			{
		      				data= new byte[residual_pck];
		      				input.read(data,0,residual_pck);
		      			}
		      			else
		      			{
		      				data = new byte[packets_length];
		      				input.read(data,0,packets_length);
		      			}
		      			String header="";
		      			Collections.shuffle(topology);
		      			for (Iterator<erraHost> it = topology.iterator(); it.hasNext();)
						{
						    erraHost element = (erraHost)(it.next());
						    //Nel path devo omettere il destinatario e il mittente!!
						    if (!((element.erraAddress.equals(erraDest)) || element.erraAddress.equals(ERRA_ADDRESS)))			///Aggiungi togliere me
						    header+=element.erraAddress+"#";
						}
		      				
		      		    header+=erraDest+"#"+Integer.toString(SN+i)+"@"+packets+"@"+filename.substring(filename.lastIndexOf('/')+1)+"@";
		      
		      			byte[] newheader=header.getBytes();
		      			
		      			byte[] headlengthByte= new byte[4];
		      			headlengthByte=ByteBuffer.allocate(4).putInt(newheader.length).array();		//Questi sono 4 bytes che esprimono la porzione di pacchetto destinata all'header
		      			
		      			byte[] packet=new byte[4+newheader.length+data.length];				//LunghezzaHeader+Routing+Dati
		      			System.arraycopy(headlengthByte, 0, packet, 0, 4);					//Copio il numero di bytes di cui è composto l'header
		      			System.arraycopy(newheader, 0, packet, 4, newheader.length);		//Copio l'header
		      			System.arraycopy(data, 0, packet, 4+newheader.length, data.length);
		      			pcks.add(packet);
		      		}
		      }
		      finally 
		      {	System.out.println("finished: "+(int)file.length()+" bytes successfully read. The file will be splitted into "+packets+" packets.");
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

	public static void send() throws UnsupportedEncodingException 
	{
		
		if (topology.size()<=1)
		{
			System.out.println("Per inviare un file è necessario che nella rete vi sia almeno un altro erraHost oltre a te!");
			return;
		}
		
		JFileChooser chooser = new JFileChooser();				//Mostro una finestra per la scelta del file da inviare
		chooser.setCurrentDirectory(new File("."));
	    int r = chooser.showOpenDialog(new JFrame());
		if (r == JFileChooser.APPROVE_OPTION) 
		{
			String path=chooser.getSelectedFile().getPath();
			System.out.println("Inserire l'erra host a cui è destinato il file specificato tra i seguenti.");
			for (Iterator<erraHost> i = topology.iterator(); i.hasNext();) 
			{
			    erraHost element = (erraHost)(i.next());
			    if (!(element.erraAddress.equals(ERRA_ADDRESS)))
			    System.out.print(element.erraAddress+", ");
			}
			
			String erraDest="";
			boolean valid=false;
			while (!valid)
			{
				Scanner keyboard = new Scanner(System.in);
				String input = keyboard.next();

				if(input.equals(ERRA_ADDRESS))
				{
					System.err.println("Specifica un destinatario diverso da te stesso");
				}
				else
				{
					for (Iterator<erraHost> i = topology.iterator(); i.hasNext();) 
					{
						erraHost element = (erraHost)(i.next());
						if (input.equals(element.erraAddress)){valid=true;break;}
					}
					if (!(valid)){System.err.println("L'erra address specificato non è valido, riprovare.");}
					if (valid)erraDest=input;
				}
			}

			LinkedList<byte[]> pcks=wrap(path,erraDest);		//Pacchettizzo il file e mi preparo per l'invio
			
			if (pcks==null)
			{
				System.err.println("Impossibile acquisire il file specificato.");
				return;
			}
			
			int i=1;
			for (Iterator it = pcks.iterator(); it.hasNext();)
			{
			    byte[] packet = (byte[])(it.next());
			    byte[] hLen=new byte[4];
      			System.arraycopy(packet, 0, hLen, 0, 4);
      			int l = ByteBuffer.wrap(hLen).getInt();			
      			
      			byte[] H=new byte[l];							//Questi sono i bytes che rappresentano l'header
      			
      			System.arraycopy(packet, 4, H, 0, l);
      			
      			String sH=new String(H, "US-ASCII");			//Questa è la stringa che codifica il mio header
      			System.out.println(sH);
      			String nextHop=sH.substring(0,sH.indexOf("#"));	//Questo è l'erraAddress a cui devo inviare il pacchetto...
      			String nextIP=getIP(nextHop);
      			try
      			{	
      				Socket TCPClientSocket = new Socket();
      				TCPClientSocket.connect(new InetSocketAddress(nextIP,TCP_PORT_FORWARDING),CONNECTION_TIMEOUT);
          			OutputStream out = TCPClientSocket.getOutputStream(); 
          			DataOutputStream dos = new DataOutputStream(out);
          			dos.write(packet, 0, packet.length);
          			TCPClientSocket.close(); 
          			System.out.println("Il pacchetto "+(i++)+"/"+pcks.size()+" all'erraHost "+nextHop+" all'indirizzo IP "+nextIP+" è stato inviato");	
      			}
      			catch (IOException e)
      			{	
      				System.err.println("Il pacchetto "+(i++)+"/"+pcks.size()+" all'erraHost "+nextHop+" all'indirizzo IP "+nextIP+" non è stato recapitato");	
      			}
			}
			System.out.println("File processing completed");
			return;
		}
	}
	
	
	//========== Restituisce l'IP dell'erraHost specificato =========================
	
	public static String getIP(String erraAdd)
	{
		for (Iterator<erraHost> i = topology.iterator(); i.hasNext();) 		//RImuovo dalla struttura il defunto nodo!
		{
		    erraHost element = (erraHost)(i.next());
		    if(element.erraAddress.equals(erraAdd))
		    {
		        return element.IP;
		    }
		}
		return "";
	}

	
	
	public static void main(String[] args) throws InterruptedException, IOException
	{	
		
		//System.out.println("Tentativo di connessione al nodo BOOTSTRAP...");
		/*boolean esito=initializeErra();		
		if (!esito)
		{
			System.err.println("Si è manifestato un errore nella connessione al nodo di BOOTSTRAP...l'applicazione verrà chiusa.");
			return;
		}*/


		topology.add(new erraHost("192.168.0.2","40"));
		topology.add(new erraHost("192.168.0.7","41"));
		//Ora devo solo dalla topologia prendermi il mio erraAddress!!
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

						for (Iterator<erraHost> it = topology.iterator(); it.hasNext();)
						{
							erraHost H = (erraHost)(it.next());
							if( H.IP.equals(ipAddress)){ERRA_ADDRESS=H.erraAddress;}
						}
					}
				}
			}
		}

		if(ERRA_ADDRESS.equals(""))
		{
			System.err.println("Sono tagliato fuori dalla topologia");
			return;
		}
		showTopology();
		System.out.println("Io sono "+ERRA_ADDRESS + " con IP "+ipAddress);

		answerAliveRequest imAlive=new answerAliveRequest();
		imAlive.start();

		refreshTopology refresh=new refreshTopology();
		refresh.start();

		FM=new fileManager();
		listenToForward F=new listenToForward();
		F.start();

		while(true)
		{	
			Scanner keyboard = new Scanner(System.in);
			String input = keyboard.next();
			if (input.equals("E"))
			{	keyboard.close();
				System.out.println("Segnalo al bootstrap che me ne sto andando...");
				try
				{
					sayGoodbye();
					imAlive.releasePort();	//Chiudo la porta sulla quale stavo ascoltando 
					refresh.releasePort();	//Chiudo la porta sulla quale stavo ascoltando
					F.releasePort();		//Chiudo la porta sulla quale stavo ascoltando
					System.out.println("Chiusura di tutti i thread completata");
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