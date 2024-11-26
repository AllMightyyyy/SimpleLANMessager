package org.zakaria;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class ServerCalculator {
    private static final int port = 5000;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server is running on port " + port + " and waiting for connections...");

            // Accept connections from clients
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                ClientHandler client = new ClientHandler(clientSocket);
                clients.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Broadcast message to all clients except the sender
    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.getOut().println(message);
            }
        }
    }

    // Inner class to handle each client connection
    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String userName;
        private ScriptEngine engine;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;

            try {
                // Initialize input and output streams
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Initialize Nashorn JS Engine
                ScriptEngineManager manager = new ScriptEngineManager();
                engine = manager.getEngineByName("Nashorn");
                if (engine == null) {
                    throw new RuntimeException("JavaScript engine not available. Add Graal.js to the classpath.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public PrintWriter getOut() {
            return out;
        }

        @Override
        public void run() {
            try {
                // Get client's username
                userName = in.readLine();
                System.out.println("New user connected: " + userName);
                out.println("Welcome to the chat room, " + userName + "!");
                out.println("You can send messages or mathematical expressions prefixed with 'EVAL:'. For example:");
                out.println("EVAL: 5 * (3 + 2)");

                String inLine;
                while ((inLine = in.readLine()) != null) {
                    if (inLine.startsWith("EVAL:")) {
                        // Handle evaluation request
                        String expression = inLine.substring(5).trim();
                        System.out.println("Received expression from " + userName + ": " + expression);
                        String result;
                        try {
                            Object evalResult = engine.eval(expression);
                            result = evalResult.toString();
                        } catch (ScriptException e) {
                            result = "Error evaluating expression.";
                        }
                        // Send the result back to the requesting client
                        out.println("RESULT:" + result);
                    } else {
                        // Regular chat message
                        System.out.println("[" + userName + "]: " + inLine);
                        broadcast("[" + userName + "]: " + inLine, this);
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection with " + userName + " lost.");
            } finally {
                clients.remove(this);
                try {
                    out.close();
                    in.close();
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }

        public void sendMessage(String message) {
            out.println(message);
            out.println("Write your message!");
        }
    }
}
