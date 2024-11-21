package com.rhgtask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class Client {
	private static final String SERVER_ADDRESS = "localhost";
	private static final int SERVER_PORT = 8080;

	public static void main(String[] args) throws IOException {
		Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
		DataOutputStream out = new DataOutputStream(socket.getOutputStream());

		// Simulate sending data packets
		for (int i = 0; i < 1000; i++) {
			String packetData = generateRandomData();
			out.writeUTF(packetData); // Sending packet to the server
			System.out.println("Sent packet: " + packetData);
		}
		out.close();
		socket.close();
	}

	// Generate random data packet
	private static String generateRandomData() {
		Random rand = new Random();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10; i++) {
			sb.append((char) (rand.nextInt(26) + 'a'));
		}
		return sb.toString();
	}
}
