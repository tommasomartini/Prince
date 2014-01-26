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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class NewErraClient 
{
	public static boolean TO=false;
	public static String BOOTSTRAP_ADDRESS="";
	public static fileManager FM;

	public static class file
	{	
		public String fileName;
		public int packets;
		public String sender;
		public Date addDate;
		private List<filePart> parts = new ArrayList<filePart>();

		public file(String name,int n,String S)
		{
			fileName=name;
			packets=n;
			sender=S;
			addDate=Calendar.getInstance().getTime();
		}

		public boolean add(int SN,byte[] data)
		{
			filePart t=new filePart(SN,data);
			parts.add(t);
			System.out.println("Received packet "+SN+ " ["+data.length+" bytes] belonging to "+fileName);
			if (parts.size()==packets)
			{
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
						Date d1 = addDate;
						Date now=Calendar.getInstance().getTime();

						long diff = now.getTime()-d1.getTime();
						float TX=(float)(diff/1000);
						
						String S="File "+fileName+", coming from "+sender+", has been correctly written. Transmission time: "+TX+" seconds.";
						
						JOptionPane.showMessageDialog(null, S, "File received", JOptionPane.INFORMATION_MESSAGE);
						System.out.println(S);	
						output.close();

						Socket ACKSocket = new Socket();
						try
						{
							ACKSocket.connect(new InetSocketAddress(sender,ErraNodeVariables.TCP_FILERECEIVED),ErraNodeVariables.CONNECTION_TIMEOUT);
							DataOutputStream streamToServer = new DataOutputStream(ACKSocket.getOutputStream());
							streamToServer.writeBytes(getMyIP()+" has correctly received "+fileName + "!"+'\n');	
							ACKSocket.close();
						}
						catch (IOException e)
						{System.err.println("I haven't been able to notify the sender for the correct reception of file.");}

					}
					return true;
				}
				catch (IOException e)
				{System.err.println("Error during the file generation.");}
			}
			return false;
		}	

		private static class filePart implements Comparable<filePart>
		{
			public int SN;
			public byte[] data;
			
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
		}
	
	}

	public static class fileManager
	{
		private List<file> fileList;

		public fileManager(){fileList=null;}

		public void add(String header,byte[] packet)
		{
			int SN=Integer.parseInt(header.substring(0,header.indexOf("@")));		
			header=header.substring(header.indexOf("@")+1);

			int parts=Integer.parseInt(header.substring(0,header.indexOf("@")));
			header=header.substring(header.indexOf("@")+1);

			String filename=header.substring(0,header.indexOf("@"));
			header=header.substring(header.indexOf("@")+1);

			String sender=header.substring(0,header.indexOf("@"));

			boolean esito=false;
			file t=null;

			if (fileList==null)
			{
				t=new file(filename,parts,sender);
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
					t=new file(filename,parts,sender);
					t.add(SN,packet);
					esito=fileList.add(t);
				}
			}
			if (esito)
			{
				fileList.remove(t);			//The add operation has completed the file, remove it!
			}
		}

		public int filePending()
		{return (fileList==null)?0:fileList.size();}

		public void showOldFiles()
		{
			if(fileList==null || fileList.size()==0)
			{return;}


			System.out.println("========== Pending files ==========");
			System.out.println("Filename\tFirst packet arrival date\tTime limit for arrival\t\tReceived packets");

			file toRemove=null;

			for (Iterator<file> i = fileList.iterator(); i.hasNext();)
			{
				file t = (file)(i.next());

				System.out.print(t.fileName+'\t');
				System.out.print(t.addDate.toLocaleString()+'\t'+'\t');

				double expectedTXTime=((ErraNodeVariables.MAX_PAYLOAD*8*t.packets)/(10e8))*10000*nodes.size();
				long TX=(long)(expectedTXTime);

				Date expire=new Date(t.addDate.getTime()+TX);

				System.out.print(expire.toLocaleString()+'\t'+'\t');

				System.out.println(t.parts.size()+"/"+t.packets);

				Date d1 = t.addDate;
				Date now=Calendar.getInstance().getTime();

				long diff = now.getTime()-d1.getTime() ;

				if(diff>expectedTXTime*ErraNodeVariables.beta)
				{
					System.out.println("The first packet of file "+t.fileName+" has been received at "+t.addDate.toLocaleString());
					System.out.println("More then "+(int)((expectedTXTime*ErraNodeVariables.beta)/1000)+" seconds has passed, and the file is still incomplete. It will now be dropped and I ask for the retransmission.");
					toRemove=t;
					break;
				}
			}

			if(toRemove!=null)
			{
				fileList.remove(toRemove);
				Socket ACKSocket = new Socket();
				try
				{
					ACKSocket.connect(new InetSocketAddress(toRemove.sender,ErraNodeVariables.TCP_FILERECEIVED),ErraNodeVariables.CONNECTION_TIMEOUT);
					DataOutputStream streamToServer = new DataOutputStream(ACKSocket.getOutputStream());
					streamToServer.writeBytes(getMyIP()+" has NOT RECEIVED "+toRemove.fileName + "! Please, send it again!"+'\n');	
					ACKSocket.close();
					System.out.println("I asked the sender for the retransmission");
				}
				catch (IOException e)
				{System.err.println("The sender is not available anymore.");}
			}	
		}

	}

	public static Map<String, ErraNode> nodes;

	public static boolean initializeErra(String address)
	{
		nodes = new HashMap<String, ErraNode>();
		try
		{
			Socket TCPClientSocket = new Socket();
			try
			{
				TCPClientSocket.connect(new InetSocketAddress(address,ErraNodeVariables.PORT_PRINCE_JOINED_NODE),ErraNodeVariables.CONNECTION_TIMEOUT);
			}
			catch (IOException e)
			{	
				TCPClientSocket.close();
				return false;
			}
			System.out.println("...connection established, waiting for the welcome table.");

			BOOTSTRAP_ADDRESS=address;

			String joinMessage="J";

			DataOutputStream streamToServer = new DataOutputStream(TCPClientSocket.getOutputStream());
			BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(TCPClientSocket.getInputStream()));

			streamToServer.writeBytes(joinMessage + '\n');	

			String fromServer="";
			fromServer=streamFromServer.readLine();
			TCPClientSocket.close(); 

			if (fromServer.charAt(0)=='W')
			{
				fromServer=fromServer.substring(2);
				updateStructure(fromServer);		
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

	public static void updateStructure(String fromServer)
	{
		nodes.clear();
		while(fromServer.length()>0)
		{
			String IP=fromServer.substring(0,fromServer.indexOf('#'));
			ErraNode e=new ErraNode(IP);
			nodes.put(IP,e);
			fromServer=fromServer.substring(fromServer.indexOf('#')+1);
		}
	}

	public static boolean openBootstrapFile() throws IOException
	{	
		boolean isConnected=false;

		JFileChooser chooser = new JFileChooser("");				//Mostro una finestra per la scelta del file da inviare
		chooser.setCurrentDirectory(new File("."));
		chooser.setDialogTitle("Choose bootstrap list");
		int r = chooser.showOpenDialog(new JFrame());

		if (r == JFileChooser.APPROVE_OPTION) 
		{
			String path=chooser.getSelectedFile().getPath();
			File file = new File(path);
			BufferedReader reader = null;

			List<String> candidate = new ArrayList<String>();
			try
			{
				reader = new BufferedReader(new FileReader(file));
				String IP = null;
				while ((IP = reader.readLine()) != null && validate(IP)) 
				{
					candidate.add(IP);
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
				} catch (IOException t){} 

			}

			while(!isConnected && candidate.size()>0)
			{

				if (candidate.size()>=5)
				{

					Collections.shuffle(candidate);
					String CandidateIP=candidate.get(0);
					System.out.print("Connecting with "+CandidateIP+"...");
					if(initializeErra(CandidateIP))
					{return true;}
					else
					{
						System.out.println("connection with "+CandidateIP+" has failed.");
						candidate.remove(CandidateIP);
					}
				}
				else if (candidate.size()==1)
				{
					String bestIP=candidate.get(0);
					System.out.print("Connecting with "+bestIP+"...");
					if(initializeErra(bestIP))
					{return true;}
					else
					{
						System.err.println("connection with "+bestIP+" has failed.");
						return false;
					}
				}
				else
				{
					double minRTT=Double.MAX_VALUE;
					String bestIP="";
					for (Iterator<String> i = candidate.iterator(); i.hasNext();)
					{
						String candidateIP=(String)(i.next());
						double RTT=measureRTT(candidateIP);
						if (RTT<minRTT && RTT!=0.0)
						{
							bestIP=candidateIP;
							minRTT=RTT;
						}
					}

					if(bestIP.equals(""))
						return false;

					System.out.print("Connecting with "+bestIP+" (RTT="+minRTT+")...");
					if(initializeErra(bestIP))
					{return true;}
					else
					{
						System.err.println("connection with "+bestIP+" has failed.");
						candidate.remove(bestIP);
					}
				}
			}
		}
		return isConnected;
	}

	public static boolean validate(final String ip)
	{        
		String PATTERN = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
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

	public static long factorial(int n)
	{
		long ris=1;
		if (n==0 || n==1)
			return ris;
		
		if(n<=10)
		{
			while (n>1)
				ris=ris*(n--);
			return ris;
		}
		
		//Uso la formula di stirling
		double e=Math.E;
		double pi=Math.PI;
		double approx=Math.sqrt(2*pi*n)*Math.pow((n/e),n);
		return (long)(approx);
	}
	
	public static double measureRTT(String IP) throws IOException
	{
		if(!(validate(IP)))return 0;
		if(System.getProperty("os.name").contains("Windows"))return 0;

		String command[] = {"ping", "-c2","-w2", IP};
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line;
		while((line = in.readLine()) != null)
		{
			if (line.contains("avg"))
			{
				line=line.substring(23);
				line=line.substring(line.indexOf("/")+1);
				line=line.substring(0, line.indexOf("/"));
				return Double.parseDouble(line);
			}
		}
		return 0;
	}

	public static final class answerAliveRequest extends Thread
	{	
		private static DatagramSocket UDP;

		public void run()
		{	
			try
			{	UDP = new DatagramSocket(ErraNodeVariables.PORT_SUBJECT_ALIVE_LISTENER);
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
					System.out.println("The UDP alive socket has been closed.");
					return;	
				}
				String message=new String(receivedPacket.getData(),0,receivedPacket.getLength());
				if (message.charAt(0)=='?')
				{
					String sentence = "!";					
					sendData = sentence.getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, receivedPacket.getAddress(), ErraNodeVariables.PORT_PRINCE_ALIVE_NODE);
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
					server=new ServerSocket(ErraNodeVariables.PORT_SUBJECT_REFRESH_TABLE_LISTENER);		//Mi metto in ascolto in attesa di connessioni TCP
					try
					{	
						s=server.accept();							//Quanto ho una richiesta di connessione la accetto!
					}
					catch (SocketException e)
					{	System.out.println("The TCP topology socket has been closed.");
					return;
					}
					BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
					String table="";
					table=streamFromServer.readLine();
					s.close();

					if (table.charAt(0)=='T')
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

	public static class listenToForward extends Thread
	{
		private ServerSocket serverSocket; 
		private Socket s;

		public void run()
		{
			try
			{
				serverSocket = new ServerSocket(ErraNodeVariables.PORT_SUBJECT_FILE_FORWARDING);

				while(true)
				{
					s = serverSocket.accept();
					SocketAddress A=s.getRemoteSocketAddress();
					String IP=A.toString().substring(1, A.toString().indexOf(":"));
					if (!(nodes.containsKey(IP)))
					{
						System.err.print("The forward request comes from an unknown host, the packet will be dropped. Informing the sender...");
						Socket AbusiveSocket = new Socket();
						try
						{
							AbusiveSocket.connect(new InetSocketAddress(IP,ErraNodeVariables.TCP_THRASHEDOUT),ErraNodeVariables.CONNECTION_TIMEOUT);
							DataOutputStream streamToServer = new DataOutputStream(AbusiveSocket.getOutputStream());
							streamToServer.writeBytes("TO" + '\n');	
							AbusiveSocket.close();
							System.err.println("sender has been told he is out of the net!");
						}
						catch (IOException e)
						{System.err.println("sender unreachable.");}
					}
					else
					{
						System.out.print("Incoming packet from "+IP.toString()+"...");
						forward F=new forward(s);
						F.start();
					}
				}
			}
			catch (IOException e)
			{
				System.out.println("TCP forwarding socket has been closed");
			}
		}	

		public void releasePort() throws IOException
		{
			serverSocket.close();
		}
	}

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
					System.out.println("the incoming packet is intended for me!");
					byte[] header=new byte[hL];
					System.arraycopy(packet, 12, header, 0, hL);
					String sH=new String(header, "US-ASCII");
					byte[] data=new byte[packet.length-12-hL];
					System.arraycopy(packet, 12+hL, data, 0, data.length);
					FM.add(sH,data);								//Ficco dentro questo frammento all'interno della classe che si occupa di gestire il tutto
				}
				else
				{
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
						System.out.println("the incoming packet is NOT intended for me.");
						System.out.print("Start forwarding to "+nextIP+"...");
						Socket TCPClientSocket = new Socket();
						TCPClientSocket.connect(new InetSocketAddress(nextIP,ErraNodeVariables.PORT_SUBJECT_FILE_FORWARDING),ErraNodeVariables.CONNECTION_TIMEOUT);
						OutputStream out = TCPClientSocket.getOutputStream(); 
						DataOutputStream dos = new DataOutputStream(out);
						dos.write(forwardPacket, 0, forwardPacket.length);
						TCPClientSocket.close(); 
						System.out.println("forwarding completed successfully.");
					}
					catch (IOException e)
					{
						System.err.println("forwarding has failed. The next hop is not alive.");	
						if (rL==2)
						{
							System.err.println("The packet is intended for an host unreachable, the packet will be dropped");
						}
						else
						{
							byte[] destinatario=new byte[4];				
							System.arraycopy(forwardPacket, 8+(rL-2)*4, destinatario, 0, 4);		
							String IPDest=InetAddress.getByAddress(destinatario).getHostAddress();

							byte[] directPacket=new byte[packet.length-(rL-1)*4];

							byte[] headlengthByte= new byte[4];
							headlengthByte=ByteBuffer.allocate(4).putInt(hL).array();

							byte[] routingLengthByte= new byte[4];
							routingLengthByte=ByteBuffer.allocate(4).putInt(1).array();

							byte[] bytesIP = InetAddress.getByName(IPDest).getAddress();

							System.arraycopy(routingLengthByte, 0, directPacket, 0, 4);
							System.arraycopy(headlengthByte, 0, directPacket,4, 4);
							System.arraycopy(bytesIP, 0, directPacket,8, 4);
							System.arraycopy(packet, 4*(2+rL), directPacket,12, hL);
							System.arraycopy(packet, 4*(2+rL)+hL, directPacket,12+hL, packet.length-(4*(2+rL)+hL));

							System.out.print("Starting direct forwarding to "+IPDest+"...");
							Socket TCPClientSocket = new Socket();
							TCPClientSocket.connect(new InetSocketAddress(IPDest,ErraNodeVariables.PORT_SUBJECT_FILE_FORWARDING),ErraNodeVariables.CONNECTION_TIMEOUT);
							OutputStream out = TCPClientSocket.getOutputStream(); 
							DataOutputStream dos = new DataOutputStream(out);
							dos.write(directPacket, 0, directPacket.length);
							TCPClientSocket.close(); 
							System.out.println("done.");
						}
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

	public static void sayGoodbye() throws UnknownHostException, IOException
	{
		Socket TCPClientSocket = new Socket();
		try
		{
			TCPClientSocket.connect(new InetSocketAddress(BOOTSTRAP_ADDRESS, ErraNodeVariables.PORT_PRINCE_DEPARTED_NODE),ErraNodeVariables.CONNECTION_TIMEOUT);
			String leaveMessage="E@"+getMyIP();
			DataOutputStream streamToServer = new DataOutputStream(TCPClientSocket.getOutputStream());
			streamToServer.writeBytes(leaveMessage + '\n');	
			TCPClientSocket.close();
			System.out.println("Bootstrap node has been noticed I'm leaving-");
		}
		catch(IOException e)
		{
			System.err.println("Bootstrap node has not been noticed I'm leaving because it's unreachable.");
		}
	}

	public static void showTopology()
	{
		if(nodes==null)
		{System.out.println("No nodes in the network...");return;}

		System.out.println("=========== NETWORK TOPOLOGY ===========");
		System.out.println(nodes.size()+" nodes active in the ERRA network.");
		for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) 
		{
			ErraNode currentNode = entry.getValue();
			System.out.println(currentNode.getIPAddress());
		}
	}

	public static LinkedList<byte[]> wrap(String filename,String erraDest)
	{
		File file = new File(filename);
		if((int)file.length()==0)
			return null;

		int packets=nodes.size()-2;	

		if (nodes.size()<=3)
			packets=1;


		while((int)file.length()/packets<ErraNodeVariables.MINIMUM_PAYLOAD && packets>1)
			packets--;	

		while((int)file.length()/packets>ErraNodeVariables.MAX_PAYLOAD)
			packets++;	
		
		boolean valid=false;
		
		while(!(valid))
		{
			String p= JOptionPane.showInputDialog("Number of packet to split the file in: ",packets);
			int N=Integer.parseInt(p);
			if (N>0)
			{
				valid=true;
				packets=N;
			}
		}
		

		int packets_length=(int)file.length()/packets;
		int residual_pck=0;


		if ((int)file.length()%packets!=0)
		{
			residual_pck=(int)file.length()-packets*packets_length;
			packets=packets+1;
		}

		if (packets==1)
			System.out.print("Reading file ["+filename+"]. The file will not be splitted. ");
		else
			System.out.print("Reading file ["+filename+"]. The file will be splitted into "+packets+" packets.");

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

			//Infine copio l'indirizzo IP del destinatario!!
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
			header=Integer.toString(SN+i)+"@"+packets+"@"+name+"@"+getMyIP()+"@";

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
		{	System.out.println("Packetization finished: "+(int)file.length()+" bytes successfully read.");
		input.close();
		}
		}
		catch (FileNotFoundException ex) 
		{
			System.out.println("File not found");
		}
		catch (IOException ex) 
		{
			System.out.println(ex);
		}
		return pcks;
	}

	public static void send() throws UnsupportedEncodingException, UnknownHostException 
	{
		if (nodes==null || nodes.size()<=1)
		{
			System.out.println("You are alone in the network, file sending is not allowed.");
			return;
		}

		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("."));
		int r = chooser.showOpenDialog(new JFrame());
		if (r == JFileChooser.APPROVE_OPTION) 
		{
			String path=chooser.getSelectedFile().getPath();

			JPanel panel = new JPanel();
			panel.add(new JLabel("Choose who the file is intended to: "));

			DefaultComboBoxModel model = new DefaultComboBoxModel();

			for(Map.Entry<String, ErraNode> entry : nodes.entrySet()) 
			{
				ErraNode currentNode = entry.getValue();
				if (!(currentNode.getIPAddress().equals(getMyIP())))
				{
					model.addElement((currentNode.getIPAddress()));
				}
			}

			JComboBox comboBox = new JComboBox(model);
			panel.add(comboBox);

			int result = JOptionPane.showConfirmDialog(null, panel, "Receiver", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

			String IPDest="";

			if (result==JOptionPane.OK_OPTION)
			{
				IPDest=(String)comboBox.getSelectedItem();
				System.out.println("You've choosen to send the file to "+IPDest);
			}
			else return;

			LinkedList<byte[]> pcks=wrap(path,IPDest);

			if (pcks==null || pcks.size()==0)
			{
				System.err.println("File will not be sent, packetization has failed.");
				return;
			}

			int i=1;
			for (Iterator<byte[]> it = pcks.iterator(); it.hasNext();)
			{
				byte[] packet = (byte[])(it.next());
				byte[] next=new byte[4];
				System.arraycopy(packet, 8, next, 0, 4);
				String nextIP=InetAddress.getByAddress(next).getHostAddress();

				Socket TCPClientSocket=null;
				boolean sendOK=false;

				System.out.print("Start sending packet "+i+"/"+pcks.size()+" to "+nextIP+"...");
				try
				{	
					TCPClientSocket = new Socket();
					TCPClientSocket.connect(new InetSocketAddress(nextIP,ErraNodeVariables.PORT_SUBJECT_FILE_FORWARDING),ErraNodeVariables.CONNECTION_TIMEOUT);
					OutputStream out = TCPClientSocket.getOutputStream(); 
					DataOutputStream dos = new DataOutputStream(out);
					dos.write(packet, 0, packet.length);
					TCPClientSocket.close(); 
					System.out.println("finished.");i++;
					sendOK=true;
				}
				catch (IOException e)
				{	
					System.err.print("packet "+i+" has not been sent to the next hop ["+nextIP+"]");	
				}

				if (!(sendOK))
				{
					System.out.println("...sending the packet to the final host.");	
					try
					{	
						//Devo prima di tutto ricreare un pacchetto tutto nuovo artificiale
						byte[] routingL=new byte[4];
						System.arraycopy(packet, 0, routingL, 0, 4);

						byte[] headerL=new byte[4];
						System.arraycopy(packet, 4, headerL, 0, 4);

						int rL = ByteBuffer.wrap(routingL).getInt();		//Questo mi esprime il numero di indirizzi IP contenuti nel routing		
						int hL = ByteBuffer.wrap(headerL).getInt();

						byte[] directPacket=new byte[packet.length-(rL-1)*4];

						byte[] headlengthByte= new byte[4];
						headlengthByte=ByteBuffer.allocate(4).putInt(hL).array();

						byte[] routingLengthByte= new byte[4];
						routingLengthByte=ByteBuffer.allocate(4).putInt(1).array();

						byte[] bytesIP = InetAddress.getByName(IPDest).getAddress();


						System.arraycopy(routingLengthByte, 0, directPacket, 0, 4);
						System.arraycopy(headlengthByte, 0, directPacket,4, 4);
						System.arraycopy(bytesIP, 0, directPacket,8, 4);
						System.arraycopy(packet, 4*(2+rL), directPacket,12, hL);
						System.arraycopy(packet, 4*(2+rL)+hL, directPacket,12+hL, packet.length-(4*(2+rL)+hL));

						TCPClientSocket = new Socket();
						TCPClientSocket.connect(new InetSocketAddress(IPDest,ErraNodeVariables.PORT_SUBJECT_FILE_FORWARDING),ErraNodeVariables.CONNECTION_TIMEOUT);
						OutputStream out = TCPClientSocket.getOutputStream(); 
						DataOutputStream dos = new DataOutputStream(out);
						dos.write(directPacket, 0, directPacket.length);
						TCPClientSocket.close(); 
						System.out.println("The packet "+(i)+"/"+pcks.size()+" has successfully been sent to "+IPDest);	i++;
						sendOK=true;
					}
					catch (IOException e)
					{	
						System.err.println("Packet dropped, "+IPDest+" is unreachable");	
					}
				}
			}
			System.out.println("File processing completed");
			return;
		}
	}

	public static class trashedOut extends Thread
	{
		private ServerSocket serverSocket; 
		private Socket s;

		public void run()
		{
			try
			{
				serverSocket = new ServerSocket(ErraNodeVariables.TCP_THRASHEDOUT);
				while(true)
				{
					try
					{	
						s=serverSocket.accept();							//Quanto ho una richiesta di connessione la accetto!
					}
					catch (SocketException e)
					{	System.out.println("Thrashed out socket has been closed.");
					return;
					}
					BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
					String message="";
					message=streamFromServer.readLine();
					s.close();
					if (message.equals("TO") && !(TO))
					{
						System.err.println("This host, for some reasons, has been thrashed out from the ERRA system. All packets coming from this host will be dropped. Restart the program to join the ERRA network again.");
						TO=true;
					}
				}
			}
			catch (IOException e)
			{
				System.out.println("Thrashed out socket has been closed.");
			}
		}	

		public void releasePort() throws IOException
		{
			serverSocket.close();
		}

	}

	public static class confirmReception extends Thread
	{
		private ServerSocket serverSocket; 
		private Socket s;

		public void run()
		{
			try
			{
				serverSocket = new ServerSocket(ErraNodeVariables.TCP_FILERECEIVED);
				while(true)
				{
					try
					{	
						s=serverSocket.accept();
					}
					catch (SocketException e)
					{	System.out.println("ACK listen server has been closed!");
					return;
					}
					BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(s.getInputStream()));
					String message="";
					message=streamFromServer.readLine();
					s.close();
					System.out.println(message);
				}
			}
			catch (IOException e)
			{
				System.out.println("ACK listen server has been closed!");
			}
		}	

		public void releasePort() throws IOException
		{
			serverSocket.close();
		}

	}

	public static synchronized void checkFilePending()
	{
		FM.showOldFiles();
	}

	public static class manageRecovery extends Thread
	{
		public void run()
		{
			try {
				while(true)
				{
					sleep(ErraNodeVariables.PENDING_REFRESH_RATE);
					checkFilePending();
				}

			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}	
	}

	public static void main(String[] args) throws InterruptedException, IOException
	{	
		ErraNodeVariables.parseConfigFile();
		
		boolean esito=false;

		if(args.length!=0)
		{
			if  (validate(args[0]))
			{
				esito=initializeErra(args[0]);
			}
			else
			{
				System.err.println("The specified IP ("+args[0]+") is not valid.");
			}
		}
		else
		{
			esito=openBootstrapFile();
		}

		if (!esito)
		{
			System.err.println("The connection with all the available bootstrap has been refused, the program will be closed.");
			System.exit(0);
			return;
		}

		showTopology();

		answerAliveRequest imAlive=new answerAliveRequest();
		imAlive.start();

		refreshTopology refresh=new refreshTopology();
		refresh.start();

		trashedOut T=new trashedOut();
		T.start();

		confirmReception C=new confirmReception();
		C.start();

		FM=new fileManager();
		listenToForward F=new listenToForward();
		F.start();

		manageRecovery R=new manageRecovery();
		R.start();

		Scanner keyboard = new Scanner(System.in);

		while(true)
		{	
			String input = keyboard.nextLine();
			if (input.toUpperCase().equals("E"))
			{
				System.out.print("I'm leaving...");
				try
				{
					sayGoodbye();
					imAlive.releasePort();	
					refresh.releasePort();	
					F.releasePort();
					T.releasePort();
					C.releasePort();
					keyboard.close();
					System.exit(0);
					return;
				}
				catch (UnknownHostException e){e.printStackTrace();}
				catch (IOException e){e.printStackTrace();}
			}
			if (input.toUpperCase().equals("S"))
			{
				if (!(TO))
					send();
				else
				{System.err.println("This host is not allowed to send files because it has been thrashed out! Restart the program and join the ERRA network again.");}
			}
			if (input.equals("?"))
			{
				showTopology();
			}
		}
	} 
}