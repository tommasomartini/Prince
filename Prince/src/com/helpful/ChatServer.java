package com.helpful;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class ChatServer extends Thread {

	private MessageSenderThread messageSenderThread;
	private ServerSocket serverSocket;
	private Socket socket;

	public static void main(String[] args) {
		ChatServer chatServer = new ChatServer();
		chatServer.start();
	}

	private ChatServer() {
		super();
		try {
			serverSocket = new ServerSocket(19000);
			System.out.println("# In attesa di connessione...");
			messageSenderThread = new MessageSenderThread();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		super.run();
		try {
			socket = serverSocket.accept();
			System.out.println("# ...connesso a un server.");
			messageSenderThread.start();
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String msg;
			while (true) {
				msg = fromClient.readLine();
				System.out.println("<- From client: " + msg);
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
				PrintStream toClient = new PrintStream(socket.getOutputStream());
				String fromKeyboard;
				while (true) {
					fromKeyboard = scanner.nextLine();
					toClient.println(fromKeyboard);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
