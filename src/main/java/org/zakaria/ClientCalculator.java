package org.zakaria;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientCalculator {
    private static final String port = "5000";
    private static final String host = "localhost";

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(host, Integer.parseInt(port));
            System.out.println("CONNECTED TO THE CHAT SERVER!");

            // Initialize input and output streams
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Scanner sc = new Scanner(System.in);

            // Prompt for username
            System.out.print("Enter your username: ");
            String userName = sc.nextLine();
            out.println(userName);

            // Thread to listen for incoming messages from the server
            new Thread(() -> {
                try {
                    String serverResponse;
                    while ((serverResponse = in.readLine()) != null) {
                        if (serverResponse.startsWith("RESULT:")) {
                            // Handle evaluation result
                            String result = serverResponse.substring(7).trim();
                            System.out.println("Evaluation Result: " + result);
                        } else {
                            // Regular chat message
                            System.out.println(serverResponse);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Connection to server lost.");
                }
            }).start();

            // Main thread to send messages to the server
            String userInput;
            while (true) {
                userInput = sc.nextLine();
                out.println(userInput);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
