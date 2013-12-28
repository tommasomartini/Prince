package com.prince;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientAliveTest {
	
	private static final String BOOTSTRAP_ADDRESS = "127.0.0.4";
	
	private InetAddress bootstrapInetAddress;

	private ClientAliveTest() {
		try {
			bootstrapInetAddress = InetAddress.getByName(BOOTSTRAP_ADDRESS);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		ClientAliveTest clientAliveTest = new ClientAliveTest();
		clientAliveTest.runClientAliveTest();
	}
	
	private void runClientAliveTest() {
		try {
			Socket joinSocket = new Socket(bootstrapInetAddress, ErraNodePorts.PORT_BOOTSTRAP_JOINED_NODE);
			PrintStream toBootstrap = new PrintStream(joinSocket.getOutputStream());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
