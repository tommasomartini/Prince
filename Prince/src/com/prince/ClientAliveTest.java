package com.prince;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ClientAliveTest {

	private static final String BOOTSTRAP_ADDRESS = "192.168.1.223";

	private AnswerAliveThread answerAliveThread;
	private InetAddress bootstrapInetAddress;
	private DatagramSocket aliveDatagramSocket;

	private ClientAliveTest() {
		try {
			bootstrapInetAddress = InetAddress.getByName(BOOTSTRAP_ADDRESS);
			answerAliveThread = new AnswerAliveThread();
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
			Socket joinSocket = new Socket(bootstrapInetAddress, ErraNodeVariables.PORT_PRINCE_JOINED_NODE, InetAddress.getLocalHost(), ErraNodeVariables.PORT_SUBJECT_HELLO);
			PrintStream toBootstrap = new PrintStream(joinSocket.getOutputStream());
			String msgToBootstrap = "J";
			toBootstrap.println(msgToBootstrap);
			BufferedReader fromBootstrap = new BufferedReader(new InputStreamReader(joinSocket.getInputStream()));
			String msgFromBootstrap;
			while (true) {
				msgFromBootstrap = fromBootstrap.readLine();
				String[] segments = msgFromBootstrap.split("@");
				if (segments[0].equalsIgnoreCase("W")) {
					System.out.println("Connessione avvenuta con successo.");
					break;
				} else {
					System.out.println("Messaggio sconosciuto: " + msgFromBootstrap);
					break;
				}
			}
			joinSocket.close();
			answerAliveThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class AnswerAliveThread extends Thread {

		public AnswerAliveThread() {
			super();
			try {
				aliveDatagramSocket = new DatagramSocket(ErraNodeVariables.PORT_SUBJECT_ALIVE_LISTENER);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			byte[] receiverBuffer = new byte[10];
			try {
				int counter = 3;
				while (true) {
					DatagramPacket receivedPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
					aliveDatagramSocket.receive(receivedPacket);
					String msgFromBootstrap = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
					if (msgFromBootstrap.equalsIgnoreCase("?")) {
						System.out.print("Ricevuta richiesta di alive... ");
						byte[] msgToBootstrap = new String("!").getBytes();
						DatagramPacket sendingPacket = new DatagramPacket(msgToBootstrap, msgToBootstrap.length, bootstrapInetAddress, ErraNodeVariables.PORT_PRINCE_ALIVE_NODE);
						if (counter == 0) {
							aliveDatagramSocket.send(sendingPacket);
							System.out.println("risposto!");
							counter = 3;
						} else {
							System.out.println("non rispondo questo volta.");
						}
						counter--;
					} else {
						System.out.println("Ricevuta richiesta sconosciuta sulla porta di alive");
						break;
					}
				}
				aliveDatagramSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
