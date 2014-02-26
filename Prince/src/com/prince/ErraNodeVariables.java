package com.prince;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ErraNodeVariables {

	public static long DELAY_ASK_FOR_ALIVE = 1000 * 60;	// time before first alive request
	public static int TIMES_TO_ASK_AGAIN = 3;
	public static long DELAY_WAIT_FOR_CALLING_TO_FINISH = 1000 * 1;	// if I have to update the tables and the Bootstrap is not on "running" mode I'll wait for this time before attempting again to access tables
	public static long periodAskForALiveAgain = 1000 * 2;	// default: 2 seconds
	public static long periodAskForAlive = 1000 * 60;		// default: 60 seconds
//	public static long PERIOD_WAIT_FOR_REFUGEES = 1000;
	
	public static final String DELIMITER_AFTER_MSG_CHAR = "@";
	public static final String DELIMITER_MSG_PARAMS = "#";

	// Prince
	public static final String MSG_PRINCE_WELCOME = "W";
	public static final String MSG_PRINCE_ALIVE_REQUEST = "?";
	public static final String MSG_PRINCE_TABLE_UPDATE = "T";
	public static final String MSG_PRINCE_SEND_IMMIGRANT = "I";
	public static final String MSG_PRINCE_REFUGEES_LIST_REQUEST = "P";
//	public static final String MSG_PRINCE_EXILED_NODE = "X";	// DEPRECATED
//	public static final String MSG_PRINCE_OUT_OF_NETWORK = "O";	// DEPRECATED
//	public static final String MSG_PRINCE_MY_ROLE = "P";	// DEPRECATED
	public static final String MSG_PRINCE_HANDSHAKE = "H";
	public static final String MSG_PRINCE_ANSWER_HANDSHAKE = "S";

	// Subject
	public static final String MSG_SUBJECT_JOIN_REQUEST = "J";
	public static final String MSG_SUBJECT_DEPART_REQUEST = "E";
	public static final String MSG_SUBJECT_ALIVE = "!";
	
	
	/*
	 * Ports
	 */

	//	Listening ports
	public static final int PORT_PRINCE_JOINED_NODE = 8001;
	public static final int PORT_PRINCE_DEPARTED_NODE = 8002;
	public static final int PORT_PRINCE_ALIVE_NODE = 8003;
	public static final int PORT_PRINCE_IMMIGRANT = 8006;
	public static final int PORT_PRINCE_PROTECTOR_LISTENER = 8007;
	public static final int PORT_PRINCE_ALIVE_LISTENER = 7000;
//	public static final int PORT_PRINCE_MY_AMBASSADOR_LISTENER = 8004;		// DEPRECATED	
//	public static final int PORT_PRINCE_FOREIGN_AMBASSADOR_LISTENER = 8005;	// DEPRECTAED
    public static final int TCP_THRASHEDOUT=8004;
    public static final int TCP_FILERECEIVED=8005;

	// Speaking ports
	public static final int PORT_SUBJECT_ALIVE_LISTENER = 7000;
	
	public static final int PORT_SUBJECT_HELLO = 7001;
	public static final int PORT_SUBJECT_FILE_FORWARDING = 7002;
	public static final int PORT_SUBJECT_SENDING = 7003;
	public static final int PORT_SUBJECT_REFRESH_TABLE_LISTENER = 7004;
	
	public static int CONNECTION_TIMEOUT=5000;
	public static int MAX_PAYLOAD=2048000;
	public static int MINIMUM_PAYLOAD=100;
	public static int PENDING_REFRESH_RATE=15000;
	
	public static boolean recovery=true;
	public static boolean verbose=true;
	
	
	public static String logFilename="log.txt";
	
	public static double alpha=0.125;
	public static double beta=0.25;
	public static double k=3;
	
	public static void parseConfigFile()
	{
		File file = new File("config.txt");
		BufferedReader reader = null;

		try 
		{
			reader = new BufferedReader(new FileReader(file));
			String text = "";

			while ((text = reader.readLine()) != null)
			{
				String array[]=text.split("=");
				if ((!(array[0].equals("")))&&(!(array[1].equals(""))))
				{
					String varName="";
					double value=0;
					try
					{
						varName=array[0];
						value=Double.parseDouble(array[1]);
						
						if (varName.toUpperCase().equals("DELAY_ASK_FOR_ALIVE"))
							{DELAY_ASK_FOR_ALIVE=(int)value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("TIMES_TO_ASK_AGAIN"))
							{TIMES_TO_ASK_AGAIN=(int)value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
							
						if (varName.toUpperCase().equals("DELAY_WAIT_FOR_CALLING_TO_FINISH"))
							{DELAY_WAIT_FOR_CALLING_TO_FINISH=(int)value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("PERIODASKFORALIVEAGAIN"))
							 {periodAskForALiveAgain=(int)value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("PERIODASKFORALIVE"))
							 {periodAskForAlive=(int)value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("CONNECTION_TIMEOUT"))
							{CONNECTION_TIMEOUT=(int)value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("MAX_PAYLOAD"))
							{MAX_PAYLOAD=(int)value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("MINIMUM_PAYLOAD"))
							{MINIMUM_PAYLOAD=(int)value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("PENDING_REFRESH_RATE"))
							{PENDING_REFRESH_RATE=(int)value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("BETA"))
							{beta=value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("ALPHA"))
							{alpha=value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						if (varName.toUpperCase().equals("K"))
							{k=value;System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);}
						
						
						if (varName.toUpperCase().equals("RECOVERY"))
						{
							recovery=(value==0)?false:true;
							System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);
						}
						
						if (varName.toUpperCase().equals("VERBOSE"))
						{
							verbose=(value==0)?false:true;
							System.out.println("Variable "+varName.toUpperCase()+ " has been initialized to "+value);
						}
						
					}
					catch (java.lang.NumberFormatException a)
					{
						
					}
				}
			}
		} 
		catch (FileNotFoundException e){} 
		catch (IOException e) {}
		finally 
		{
			try 
			{
				if (reader != null) 
				{
					reader.close();
				}
			} catch (IOException e) {}
		}
	}
	
}
