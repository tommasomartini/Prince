package com.prince;

import java.net.*;
import java.util.Scanner;
import java.io.*;

public class AliveNodeTester {
	
	private static boolean alive = true;

	private static final String ADDRESS = "127.0.0.1";
	private static final int PORT = 8002;

	private Socket socket;
	private ServerSocket aliveSocket;

	public static void main(String args[]) {
		AliveNodeTester aliveNodeTester = new AliveNodeTester();
		aliveNodeTester.runTester();
	}

	public AliveNodeTester() {
		try {
			System.out.println("Nodo vivo? s/n");
			Scanner scanner = new Scanner(System.in);
			if (scanner.next().equalsIgnoreCase("n")) {
				alive = false;
				System.out.println("Il nodo NON e' attivo!");
			} else {
				System.out.println("Il nodo e' attivo!");
			}
			aliveSocket = new ServerSocket(8000);
			System.out.println("Sono in ascolto...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void runTester() {
		while (true) {
			try {
				Socket socket = aliveSocket.accept();
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String msg = reader.readLine();
				if (!msg.substring(0, 1).equalsIgnoreCase("!")) {
					System.err.println("Il primo carattere ricevuto non e' 1");
				}
				sendMessage();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendMessage() {
		
	}
}
