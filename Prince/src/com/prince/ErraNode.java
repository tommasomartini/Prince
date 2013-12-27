package com.prince;

public class ErraNode {

	private final int ID;
	private final String IP_ADDRESS;

	public ErraNode(int id, String ip) {
		ID = id;
		IP_ADDRESS = ip;
	}

	public int getID() {
		return ID;
	}

	public String getIP_ADDRESS() {
		return IP_ADDRESS;
	}
}
