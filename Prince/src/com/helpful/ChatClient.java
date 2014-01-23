package com.helpful;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient extends Thread {

	private MessageSenderThread messageSenderThread;
	private Socket socket;

	public static void main(String[] args) {
		ChatClient chatClient = new ChatClient();
		chatClient.start();
	}

	private ChatClient() {
		super();
		try {
			socket = new Socket("147.162.118.114", 19000);
//			socket = new Socket(InetAddress.getLocalHost(), 19000);
			System.out.println("# Connesso al server.");
			messageSenderThread = new MessageSenderThread();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		super.run();
		try {
			messageSenderThread.start();
			BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String msg;
			while (true) {
				msg = fromServer.readLine();
				System.out.println("<- From server: " + msg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private class MessageSenderThread extends Thread {

		@Override
		public void run() {
			super.run();
			try {
				Scanner scanner = new Scanner(System.in);
				PrintStream toServer = new PrintStream(socket.getOutputStream());
				String fromKeyboard;
				while (true) {
					fromKeyboard = scanner.nextLine();
					toServer.println(fromKeyboard);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
