// File: SwingServer.java
package org.zakaria;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.ArrayList;

/**
 * Server class for the LAN Messenger.
 * Listens for incoming client connections and handles message broadcasting.
 */
public class SwingServerCoordinates {
    private static final int PORT = 5000;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static List<User> users = new ArrayList<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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
     * Saves all user data to a JSON file.
     */
    public static void saveUsersToJSON() {
        try (FileWriter writer = new FileWriter("users.json")) {
            gson.toJson(users, writer);
            System.out.println("User data saved to users.json");
        } catch (IOException e) {
            System.err.println("Error saving users to JSON: " + e.getMessage());
        }
    }

    /**
     * Retrieves a User object by username.
     *
     * @param userName The username to search for.
     * @return The User object if found; otherwise, null.
     */
    public static User getUserByName(String userName) {
        synchronized (users) {
            for (User user : users) {
                if (user.getUserName().equalsIgnoreCase(userName)) {
                    return user;
                }
            }
        }
        return null;
    }

    /**
     * Inner class to handle each connected client.
     */
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String userName;
        private double latitude;
        private double longitude;

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
                // Prompt for username
                out.println("Enter your username:");
                userName = in.readLine();
                if (userName == null || userName.trim().isEmpty()) {
                    userName = "Anonymous";
                }
                System.out.println("User connected: " + userName);
                out.println("Welcome to the chat room, " + userName + "!");

                // Prompt for latitude
                out.println("Enter your latitude:");
                String latStr = in.readLine();
                // Prompt for longitude
                out.println("Enter your longitude:");
                String lonStr = in.readLine();

                try {
                    latitude = Double.parseDouble(latStr);
                    longitude = Double.parseDouble(lonStr);
                } catch (NumberFormatException e) {
                    out.println("Invalid coordinates. Connection will be closed.");
                    closeEverything();
                    return;
                }

                // Add user to the list
                User user = new User(userName, latitude, longitude);
                synchronized (users) {
                    users.add(user);
                }

                // Notify all clients about the new user
                broadcast(userName + " has joined the chat.", this);
                updateUserList();

                String message;
                // Continuously listen for messages from the client
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/save")) {
                        saveUsersToJSON();
                        out.println("User data has been saved.");
                    } else if (message.startsWith("/get ")) {
                        String targetUser = message.substring(5).trim();
                        User target = getUserByName(targetUser);
                        if (target != null) {
                            String json = gson.toJson(target);
                            out.println("USER_COORDINATES:" + json);
                        } else {
                            out.println("User not found.");
                        }
                    } else {
                        System.out.println("[" + userName + "]: " + message);
                        // Broadcast the message to other clients
                        broadcast("[" + userName + "]: " + message, this);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error in client communication: " + e.getMessage());
            } finally {
                // Client has disconnected
                System.out.println("User disconnected: " + userName);
                clients.remove(this);
                synchronized (users) {
                    users.removeIf(u -> u.getUserName().equalsIgnoreCase(userName));
                }
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
