package com.prince;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class AliveNodeTester {
	
	private static boolean alive = true;
	private DatagramSocket aliveSocket;
	private InetAddress bootstrapAddress;
	private int bootstrapPort;
	private static final int PORT_ALIVE = 7000;
	private static final int PORT_ANSWER_ALIVE = 8003;

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
			aliveSocket = new DatagramSocket(PORT_ALIVE);
			System.out.println("Sono in ascolto sulla porta " + PORT_ALIVE + "...");
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void runTester() {
		byte[] receiveData = new byte[10];
		while (true) {
			try {
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                aliveSocket.receive(receivePacket);
                String msg = new String(receivePacket.getData());
                bootstrapAddress = receivePacket.getAddress();
                bootstrapPort = receivePacket.getPort();
				System.out.println("Letta la stringa: " + msg);
				if (!msg.substring(0, 1).equalsIgnoreCase("?")) {
					System.err.println("Il primo carattere ricevuto non e' ?");
				}
				if (alive) {
					sendMessage();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void sendMessage() {
		try {
			byte[] buf = (new String("!@18")).getBytes();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName("127.0.0.1"), PORT_ANSWER_ALIVE);
			aliveSocket.send(packet);
			System.out.println("Pacchetto inviato al bootstrap tramite la porta " + aliveSocket.getLocalPort() + " sulla porta " + PORT_ANSWER_ALIVE + " del bootstrap.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
