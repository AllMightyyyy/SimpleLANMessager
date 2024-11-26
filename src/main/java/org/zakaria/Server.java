package org.zakaria;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final int port = 5000;
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<ClientHandler>();

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server is running on port " + port + " and waiting for connections...");

            // Accept connections of clients
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

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.getOut().println(message);
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        PrintWriter out;
        BufferedReader in;
        String userName;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;

            try {
                // Create the flow of data ( in and out ) for communication
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public Socket getClientSocket() {
            return clientSocket;
        }

        public void setClientSocket(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public PrintWriter getOut() {
            return out;
        }

        public void setOut(PrintWriter out) {
            this.out = out;
        }

        public BufferedReader getIn() {
            return in;
        }

        public void setIn(BufferedReader in) {
            this.in = in;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        @Override
        public void run() {
            try {
                // Get client's user name
                userName = in.readLine();
                System.out.println("New user connected: " + userName);
                out.println("Welcome to the chat room, " + userName + " !");
                out.println("Write any message you want :D");
                String inLine;
                while (true) {
                    if ((inLine = in.readLine()) == null) break;
                    System.out.println("[" + userName + "]: " + inLine);
                    // Send the message to all clients
                    broadcast("[ " + userName + " ]: " + inLine, this);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
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
            out.println("Write your message !");
        }
    }
}