package com.prince;

public class ErraNodeVariables {

	/*
	 * Strings
	 */
	public static final String DELIMITER_AFTER_MSG_CHAR = "@";
	public static final String DELIMITER_MSG_PARAMS = "#";

	// Prince
	public static final String MSG_PRINCE_WELCOME = "W";
	public static final String MSG_PRINCE_ALIVE_REQUEST = "?";
	public static final String MSG_PRINCE_TABLE_UPDATE = "T";
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
	//	Prince
	//	Listening ports
	public static final int PORT_PRINCE_JOINED_NODE = 8001;
	public static final int PORT_PRINCE_DEPARTED_NODE = 8002;
	public static final int PORT_PRINCE_ALIVE_NODE = 8003;
	public static final int PORT_PRINCE_ALIVE_LISTENER = 7000;
	public static final int PORT_PRINCE_MY_AMBASSADOR_LISTENER = 8004;
	public static final int PORT_PRINCE_FOREIGN_AMBASSADOR_LISTENER = 8005;

	//	Speaking ports
	public static final int PORT_PRINCE_ASK_ALIVE_NODES = 8000;
	//	public static final int PORT_PRINCE_REFRESH_TABLE = 8004;	// DEPRECATED

	//	Subject
	// Listening ports
	public static final int PORT_SUBJECT_ALIVE_LISTENER = 7000;
	public static final int PORT_SUBJECT_REFRESH_TABLE_LISTENER = 7004;

	// Speaking ports
	public static final int PORT_SUBJECT_HELLO = 7001;
//	public static final int PORT_SUBJECT_GOODBYE = 7002;	// DEPRECATED
	//	public static final int PORT_SUBJECT_CONFIRM_ALIVE = 7000;	// DEPRECATED
	public static final int PORT_SUBJECT_FILE_FORWARDING = 7002;
	public static final int PORT_SUBJECT_SENDING = 7003;
}
