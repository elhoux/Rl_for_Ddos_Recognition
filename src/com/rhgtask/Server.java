package com.rhgtask;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 8080;
    private static final double THRESHOLD = 4.0; // Entropy threshold for detecting attack

    public static void main(String[] args) {
        // Try-with-resources for ServerSocket
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            boolean running = true; // Server running flag

            while (running) {
                running = handleClient(serverSocket); // Extracted method for handling a client
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }

        System.out.println("Server has stopped.");
    }

    /**
     * Handles a single client connection.
     * @param serverSocket The ServerSocket instance.
     * @return true if the server should continue running, false if it should stop.
     */
    private static boolean handleClient(ServerSocket serverSocket) {
        try (Socket socket = serverSocket.accept();
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Collect data packets
            List<String> packets = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                String packet = in.readUTF();
                if ("END".equals(packet)) { // Check for termination signal
                    System.out.println("Termination signal received. Shutting down the server.");
                    return false; // Signal to stop the server
                }
                packets.add(packet);
            }

            // Calculate entropy
            double entropy = EntropyUtils.calculateEntropy(packets);
            System.out.println("Calculated Entropy: " + entropy);

            // Check for DDoS attack based on entropy threshold
            if (entropy > THRESHOLD) {
                System.out.println("Possible DDoS Attack Detected!");
            } else {
                System.out.println("Traffic is normal.");
            }

        } catch (IOException e) {
            System.err.println("Error processing client connection: " + e.getMessage());
        }

        return true; // Continue running the server
    }
}
