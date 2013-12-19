package com.prince;

public class ErraNodePorts {
	
	//	Bootstrap
	//	Listening ports
	public static final int PORT_BOOTSTRAP_JOINED_NODE = 8001;
	public static final int PORT_BOOTSTRAP_DEPARTED_NODE = 8002;
	public static final int PORT_BOOTSTRAP_ALIVE_NODE = 8003;

	//	Speaking ports
	public static final int PORT_BOOTSTRAP_ASK_ALIVE_NODES = 8000;
	public static final int PORT_BOOTSTRAP_TABLE = 8004;
	
	//	Subject
	// Listening ports
	public static final int PORT_SUBJECT_ALIVE_LISTENER = 7000;
	public static final int PORT_SUBJECT_REFRESH_TABLE_LISTENER = 7004;

}
