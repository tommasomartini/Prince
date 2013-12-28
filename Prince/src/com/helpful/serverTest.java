package com.helpful;

import java.io.*;
import java.net.*;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


public class serverTest {



	public static void main(String[] args) throws IOException, InterruptedException
	
	{
		ServerSocket welcomeSocket = new ServerSocket(8001);
		Socket connectionSocket = welcomeSocket.accept();
		BufferedReader streamFromServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		String fromServer=streamFromServer.readLine();
		DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

		outToClient.writeBytes("W@3@192.168.0.2%192.168.0.3%192.168.0.4%\n"); 
		welcomeSocket.close();
		connectionSocket.close();

		Thread.sleep(1000);
		
		byte[] routing=new byte[4];
		
		String IP="192.168.0.2";
		byte[] bytesIP = InetAddress.getByName(IP).getAddress();
		System.arraycopy(bytesIP, 0, routing, 0,4);
		/*IP="192.168.0.3";
		bytesIP = InetAddress.getByName(IP).getAddress();
		System.arraycopy(bytesIP, 0, routing, 4,4);
		IP="192.168.0.4";
		bytesIP = InetAddress.getByName(IP).getAddress();
		System.arraycopy(bytesIP, 0, routing,8,4);*/


		String name="pippo.txt";
		
		int packets=1;
		int SN=5000;

		byte[] data=new byte[500];
		String pay="";
		for (int j=0;j<500;j++)
		{
			pay+="a";
		}
		data=pay.getBytes();
		
		String header="";
		header=Integer.toString(SN)+"@"+packets+"@"+name+"@";

		byte[] headerB=header.getBytes();

		byte[] headlengthByte= new byte[4];
		headlengthByte=ByteBuffer.allocate(4).putInt(headerB.length).array();

		byte[] routingLengthByte= new byte[4];
		routingLengthByte=ByteBuffer.allocate(4).putInt(routing.length/4).array();

		byte[] packet=new byte[8+routing.length+headerB.length+data.length];

		System.arraycopy(routingLengthByte, 0, packet, 0, 4);
		System.arraycopy(headlengthByte, 0, packet, 4, 4);
		System.arraycopy(routing, 0, packet, 8, routing.length);
		System.arraycopy(headerB, 0, packet,8+routing.length, headerB.length);
		System.arraycopy(data, 0, packet, 8+routing.length+headerB.length, data.length);

		System.out.println(packet.length);
		
		Socket TCPClientSocket = new Socket();
			TCPClientSocket.connect(new InetSocketAddress("192.168.0.2",7002),5000);
			OutputStream out = TCPClientSocket.getOutputStream(); 
			DataOutputStream dos = new DataOutputStream(out);
			dos.write(packet, 0, packet.length);
			TCPClientSocket.close(); 

}

}
