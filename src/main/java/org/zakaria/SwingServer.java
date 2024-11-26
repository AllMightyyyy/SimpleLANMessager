package org.zakaria;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Server class for the LAN Messenger.
 * Listens for incoming client connections and handles message broadcasting.
 */
public class SwingServer {
    private static final int PORT = 5000;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT + " and waiting for connections...");

            // Continuously accept new client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Create and start a new ClientHandler thread for the connected client
                ClientHandler client = new ClientHandler(clientSocket);
                clients.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    /**
     * Broadcasts a message to all connected clients except the sender.
     *
     * @param message The message to broadcast.
     * @param sender  The client sending the message.
     */
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            // Don't send the message back to the sender
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Updates all clients with the current list of connected users.
     */
    public static void updateUserList() {
        StringBuilder userList = new StringBuilder("USER_LIST:");
        for (ClientHandler client : clients) {
            userList.append(client.getUserName()).append(",");
        }
        // Remove trailing comma if present
        if (userList.length() > 10) {
            userList.setLength(userList.length() - 1);
        }
        String userListMessage = userList.toString();
        for (ClientHandler client : clients) {
            client.sendMessage(userListMessage);
        }
    }

    /**
     * Inner class to handle each connected client.
     */
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String userName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                // Initialize input and output streams
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                System.err.println("Error initializing client handler: " + e.getMessage());
                closeEverything();
            }
        }

        public String getUserName() {
            return userName;
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        @Override
        public void run() {
            try {
                // First message from client is the username
                userName = in.readLine();
                System.out.println("User connected: " + userName);
                out.println("Welcome to the chat room, " + userName + "!");
                broadcast(userName + " has joined the chat.", this);
                updateUserList();

                String message;
                // Continuously listen for messages from the client
                while ((message = in.readLine()) != null) {
                    System.out.println("[" + userName + "]: " + message);
                    broadcast("[" + userName + "]: " + message, this);
                }
            } catch (IOException e) {
                System.err.println("Error in client communication: " + e.getMessage());
            } finally {
                // Client has disconnected
                System.out.println("User disconnected: " + userName);
                clients.remove(this);
                broadcast(userName + " has left the chat.", this);
                updateUserList();
                closeEverything();
            }
        }

        /**
         * Closes all resources associated with the client.
         */
        private void closeEverything() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing resources for " + userName + ": " + e.getMessage());
            }
        }
    }
}
