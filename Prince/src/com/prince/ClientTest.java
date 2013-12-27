package com.prince;

import java.net.*;
import java.io.*;

public class ClientTest {

	private static final String ADDRESS = "127.0.0.1";
	private static final int PORT = 8002;

	private Socket socket;
	
	private ServerSocket aliveSocket;

	public static void main(String args[]) {
		ClientTest client = new ClientTest();
//		client.sendMessage();
	}

	public ClientTest() {
		System.out.println("Apertura connessione");
//		try {
//			socket = new Socket(ADDRESS, PORT);
////			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));	//legge il messaggio dal server
////			System.out.println(bufferedReader.readLine());
////			sendMessage();
//			requestForJoin();
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	private void requestForJoin() {
		
		try {
			PrintStream toServer = new PrintStream(socket.getOutputStream());
			toServer.println("E@0\n");
//			toServer.println("J");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMessage() {
		
		String msgToSend = "";		
		InputStreamReader streamReader = new InputStreamReader(System.in);	// legge lo stream da tastiera e lo mette in un buffer
		BufferedReader fromKeyboards = new BufferedReader(streamReader);	// legge i dati salvati nel buffer della tastiera
		
		while (!msgToSend.equalsIgnoreCase("bye")) {
			try {
//				String msg = "Hello server!";
				msgToSend = fromKeyboards.readLine();
				PrintStream toServer = new PrintStream(socket.getOutputStream());
				toServer.println(msgToSend);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Chiusura connessione effettuata");
		System.exit(0);
	}
	
//	private class KeyboardListenerThread extends Thread {
//
//		public KeyboardListenerThread() {
//			super();
//		}
//		
//		@Override
//		public void run() {
//			String msg = "";
//			BufferedReader fromKeyboard = new BufferedReader(new InputStreamReader(System.in));
//			try {
//				msg = fromKeyboard.readLine();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			
//			String msgToSend = "";
//			
//			if (msg.substring(0, 1).equalsIgnoreCase("J")) {	// join the network
//				msgToSend = "J";
//			} else if (msg.substring(0, 1).equalsIgnoreCase("E")) {	// exit the network
//				msgToSend = "E@0";
//			} else if (msg.substring(0, 1).equalsIgnoreCase("L")) {	// listen
//				
//			} else if (msg.substring(0, 1).equalsIgnoreCase("bye")) {	// shutdown
//				msgToSend = "bye";
////			} elseif (msg.substring(0, 1).equalsIgnoreCase("J")) {
//
//			} else { 
//				
//			}
//			
//			while (!msgToSend.equalsIgnoreCase("bye")) {
//				try {
//					PrintStream toServer = new PrintStream(socket.getOutputStream());
//					toServer.println(msgToSend);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			try {
//				socket.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			System.out.println("Chiusura connessione effettuata");
//			System.exit(0);
//		}
//	}
}