package com.prince;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;



public class erraClient 
{
	public static int CONNECTION_TIMEOUT=5000;		

	public static int UDP_PORT_ALIVE=7000;
	public static int UDP_ALIVE_ANSWER=8003;

	
	public static int TCP_PORT_WELCOME=7001;
	public static int TCP_PORT_FORWARDING=7002;
	public static int TCP_PORT_SENDING=7003;
	public static int TCP_PORT_REFRESH=7004;
	public static int TCP_BOOTSTRAP_PORT_WELCOME=8001;
	public static int TCP_BOOTSTRAP_PORT_GOODBYE=8002;


	public static String BOOTSTRAP_ADDRESS="10.192.23.46";

	public static String ERRA_ADDRESS="";

	public static int TOTAL_DEVICES=3;
	
	


	public static class erraHost
	{	public String IP;						//So che viola il principio di incapsulamento...ma chi se ne frega, meglio così senza fare 1000 metodi.
		public String erraAddress;
		public erraHost(){IP="";erraAddress="";}
		public erraHost(String IP,String Address){this.IP=IP;erraAddress=Address;}
	}

	//Questa lista contiene l'elenco di tutti i dispositivi erra attivi nella rete
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


	//Questo thread ascolta sulla porta 7002. Quando riceve una richiesta di connessione TCP genera un nuovo thread che si occupa di fare 
	
	private static class listenToForward extends Thread
	{
		private ServerSocket serverSocket; 
		private Socket s;
		
		public void run()
		{
			System.out.println("Sono pronto a sniffare i pacchetti e a forwardarli al giusto destinatario!");
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
				e.printStackTrace();
			}
		}	
	}
	
	
	//Questo thread legge un ERRA PACKET e lo invia al prossimo desitnatario
	
	private static class forward extends Thread
	{
		private Socket mySocket;
		
		public forward(Socket s)
		{	super();
			mySocket=s;
		}
		
		public void run()
		{
			try
			{
				System.out.println("Un socket TCP nuovo sta gestendo una richiesta di forwarding sulla porta 7002");
				BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
				String erraPacket="";
				erraPacket=streamFromServer.readLine();
				mySocket.close();
				System.out.println("Ho letto il pacchetto erra e chiudo la connessione con il mittente");
				System.out.println(erraPacket);	
				
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
		Socket TCPClientSocket = new Socket(BOOTSTRAP_ADDRESS, TCP_BOOTSTRAP_PORT_GOODBYE);
		String leaveMessage="E@"+ERRA_ADDRESS;
		DataOutputStream streamToServer = new DataOutputStream(TCPClientSocket.getOutputStream());
		streamToServer.writeBytes(leaveMessage + '\n');	
		TCPClientSocket.close(); 
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
	
	
	
	private static final String INPUT_FILE_NAME = "/home/mattia/Desktop/prova.pdf";
	
	public static void openFile()
	{
		  	System.out.println("Leggo in binario il file " + INPUT_FILE_NAME);
		    File file = new File(INPUT_FILE_NAME);
		    System.out.println("File length (bytes): "+(int)file.length());
		    
		    //pacchetto diviso per il numero di nodi attivi nella rete
		    int packets=TOTAL_DEVICES;
		    int packets_length=(int)file.length()/packets;
		    int residual_pck=0;
		 
		    
		    if ((int)file.length()%packets!=0)
		    {
		    	residual_pck=(int)file.length()-packets*packets_length;
		    	packets=packets+1;
		    }


		    /*		SERENA...questa roba dovrebbe verificare se ci sono pacchetti eccedenti rispetto a packets...quindi dovrebbe tenere in conto
		     * dei mezzi bits diciamo. Tipo. Devo frammentare 10 bit in 3 pacchetti, allora faccio 3 pacchetti da 3 bit e l'ultimo da un bit.
		     * Però non fa sta roba. Infatti aggiunge un pacchetto di dimensione nulla in questo caso: File length; 3.130.380, n=3
		     * Io ho usato il modulo sopra...lascio sta roba così poi se riguardi capisci cosa ho modificato.
		    if ((int)file.length()/packets!=0)
		    {
		    	residual_pck=(int)file.length()-packets*packets_length;
		    	packets=packets+1;
		    }
		    */
		    
		    System.out.println("Il file acquisito verra frammentato in "+packets+ " pacchetti di lunghezza "+ packets_length);
		 
		    LinkedList<byte[]> pcks= new LinkedList<byte[]>();
		    
		    try 
		    { 	InputStream input = null;
		      	try 
		      	{
		      		input = new BufferedInputStream(new FileInputStream(file));
		      		int numbyte= input.available();
		      		byte[] result = new byte[numbyte];
		      		input.read(result);	//array di byte con dentro lo stream di input
		      		
		      		Random random = new Random();	//sequence number primo pacchetto
		      		int SN = random.nextInt(5000);
		      		Integer[] erraAddresses=new Integer[]{5,25,4,10,40,1};
		      		     		
		      		for(int i=0;i<packets-1;i++)
		      		{      			
		      			String header="@";
		      			//path--ordine casuale
		      			Collections.shuffle(Arrays.asList(erraAddresses));
		      			for(int j=0;j<erraAddresses.length;j++)
		      			{
		      				header+= erraAddresses[j].toString();
		      				if (j!=erraAddresses.length-1)
		      				{
		      					header+= "#";
		      				}	
		      			}
		      			SN=SN+i;	
		      		    String convSN=Integer.toString(SN);
		      		    header+="@"+convSN+"@";
		      			
		      			byte[] newheader=header.getBytes();
		      			int head_length=newheader.length;
		      		    byte[] temp = BigInteger.valueOf(head_length).toByteArray(); //da int a byte
		      		    byte[] headlengthByte= new byte[4];
		      		    
		      			head_length=head_length+4;
		      			byte[] toInsert=new byte[head_length+packets_length];
		      				      			

		      			//pcks.add(toInsert);
		      		}
		      		
		        //Ora leggo l'ultimo pacchetto di dimensione minore degli altri
		        if(residual_pck!=0)
		        {
		        	byte[] last = new byte[residual_pck];
		        	for(int j=0;j<residual_pck;j++)
		        	{
		        		last[j]=result[(packets-1)*packets_length+j];
		        	}
		        	pcks.add(last);
		        }

		      }
		      finally 
		      {	
		    	System.out.println("Closing input stream.");
		        input.close();
		      }
		    }
		    catch (FileNotFoundException ex) {
		    	System.out.println("File not found.");
		    }
		    catch (IOException ex) {
		    	System.out.println(ex);
		    }
	}

	public static void main(String[] args) throws InterruptedException
	{	
		openFile();
	/*	System.out.println("Tentativo di connessione al nodo BOOTSTRAP...");
		boolean esito=initializeErra();		
		if (!esito)
		{
			System.err.println("Si è manifestato un errore nella connessione al nodo di BOOTSTRAP...l'applicazione verrà chiusa.");
			return;
		}

		answerAliveRequest imAlive=new answerAliveRequest();
		imAlive.start();

		refreshTopology refresh=new refreshTopology();
		refresh.start();
		
		ToForward F=new listenToForward();
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
					System.out.println("Segnalazione completata");
					return;
				}
				catch (UnknownHostException e){e.printStackTrace();}
				catch (IOException e){e.printStackTrace();}
			}
		}*/
	} 

}