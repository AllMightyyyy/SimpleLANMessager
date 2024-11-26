package org.zakaria;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String port = "5000";
    private static final String host = "localhost";

    public static void main(String[] args) {
        try {
            Socket socket = new Socket(host, Integer.parseInt(port));
            System.out.println("CONNECTED TO THE CHAT SERVER !");

            // Configure the flow of entry and exit of data
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            Scanner sc = new Scanner(System.in);

            System.out.print("Enter your username: ");
            String userName = sc.nextLine();
            out.println(userName);

            // Initialize the thread to manage the incoming messages
            new Thread(() -> {
               try {
                   String serverResponse;
                   while ((serverResponse = in.readLine()) != null) {
                       System.out.println(serverResponse);
                   }
               } catch (IOException e) {
                   throw new RuntimeException(e);
               }
            }).start();

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
