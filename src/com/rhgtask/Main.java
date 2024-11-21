package com.rhgtask;

public class Main {
	public static void main(String[] args) {
		// Create and run the server and client
		System.out.println("Starting DDoS Detection System...");
		new Thread(() -> {
			try {
				Server.main(null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();

		// Run the client after the server is started
		try {
			Thread.sleep(1000); // Delay to ensure the server is up
			Client.main(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
