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

	private static final String BOOTSTRAP_ADDRESS = "192.168.1.112";

	private AnswerAliveThread answerAliveThread;
	private InetAddress bootstrapInetAddress;
	private int myErraID;
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
			Socket joinSocket = new Socket(bootstrapInetAddress, ErraNodePorts.PORT_BOOTSTRAP_JOINED_NODE, InetAddress.getLocalHost(), ErraNodePorts.PORT_SUBJECT_HELLO);
			PrintStream toBootstrap = new PrintStream(joinSocket.getOutputStream());
			String msgToBootstrap = "J";
			toBootstrap.println(msgToBootstrap);
			BufferedReader fromBootstrap = new BufferedReader(new InputStreamReader(joinSocket.getInputStream()));
			String msgFromBootstrap;
			while (true) {
				msgFromBootstrap = fromBootstrap.readLine();
				String[] segments = msgFromBootstrap.split("@");
				if (segments[0].equalsIgnoreCase("W")) {
					myErraID = Integer.parseInt(segments[1]);
					System.out.println("Connessione avvenuta con succeso. Id numero: " + myErraID);
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
				aliveDatagramSocket = new DatagramSocket(ErraNodePorts.PORT_SUBJECT_ALIVE_LISTENER);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			super.run();
			byte[] receiverBuffer = new byte[10];
			try {
				while (true) {
					DatagramPacket receivedPacket = new DatagramPacket(receiverBuffer, receiverBuffer.length);
					aliveDatagramSocket.receive(receivedPacket);
					String msgFromBootstrap = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
					if (msgFromBootstrap.equalsIgnoreCase("?")) {
						System.out.println("icevuta richiesta di alive...");
						byte[] msgToBootstrap = new String("!@" + myErraID).getBytes();
						DatagramPacket sendingPacket = new DatagramPacket(msgToBootstrap, msgToBootstrap.length);
						aliveDatagramSocket.send(sendingPacket);
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
