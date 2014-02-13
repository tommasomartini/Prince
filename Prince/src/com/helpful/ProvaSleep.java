package com.helpful;

public class ProvaSleep {

	public static void main(String[] args) {
		while(true) {
			System.out.println("Aspetto un secondo");
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
