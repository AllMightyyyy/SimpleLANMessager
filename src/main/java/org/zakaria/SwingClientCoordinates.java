// File: SwingClient.java
package org.zakaria;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;

/**
 * Swing-based client for the LAN Messenger.
 * Provides a graphical user interface for users to send and receive messages,
 * and view connected users. Clicking on a user will open their location on Google Maps.
 */
public class SwingClientCoordinates implements Runnable {
    private static final int PORT = 5000;
    private static final String HOST = "localhost";

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> listModel;
    private PrintWriter out;
    private BufferedReader in;
    private String userName;
    private Gson gson = new Gson();

    /**
     * Initializes the GUI components.
     */
    public SwingClientCoordinates() {
        initializeGUI();
    }

    /**
     * Sets up the Swing GUI.
     */
    private void initializeGUI() {
        frame = new JFrame("LAN Messenger");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        // User list
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(200, 0));
        userScrollPane.setBorder(BorderFactory.createTitledBorder("Users"));

        // Add mouse listener for user list clicks
        userList.addListSelectionListener(new ListSelectionListener() {
            private int lastIndex = -1;

            @Override
            public void valueChanged(ListSelectionEvent e) {
                int index = userList.getSelectedIndex();
                if (index != -1 && index != lastIndex && !e.getValueIsAdjusting()) {
                    lastIndex = index;
                    String selectedUser = listModel.getElementAt(index);
                    if (!selectedUser.equals(userName)) { // Prevent self-selection
                        requestUserCoordinates(selectedUser);
                    }
                }
            }
        });

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("Send");
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Add components to frame
        frame.add(chatScrollPane, BorderLayout.CENTER);
        frame.add(userScrollPane, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Action listener for send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        // Enter key sends message
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        frame.setVisible(true);
    }

    /**
     * Connects to the server and starts the message listening thread.
     */
    private void connectToServer() {
        try {
            Socket socket = new Socket(HOST, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Start a thread to listen for messages from the server
            new Thread(new IncomingReader()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Unable to connect to the server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    /**
     * Sends a message to the server.
     */
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            chatArea.append("Me: " + message + "\n");
            inputField.setText("");
        }
    }

    /**
     * Requests the coordinates of a specific user from the server.
     *
     * @param targetUser The username to request coordinates for.
     */
    private void requestUserCoordinates(String targetUser) {
        out.println("/get " + targetUser);
    }

    /**
     * Runnable class to handle incoming messages from the server.
     */
    private class IncomingReader implements Runnable {
        @Override
        public void run() {
            String serverMessage;
            try {
                while ((serverMessage = in.readLine()) != null) {
                    handleServerMessage(serverMessage);
                }
            } catch (IOException e) {
                chatArea.append("Disconnected from server.\n");
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                } catch (IOException e) {
                    // Ignore
                }
                System.exit(0);
            }
        }

        /**
         * Handles different types of messages from the server.
         *
         * @param message The message received from the server.
         */
        private void handleServerMessage(String message) {
            SwingUtilities.invokeLater(() -> {
                if (message.startsWith("Enter your username:")) {
                    userName = promptUser("Username", "Enter your username:");
                    if (userName != null && !userName.trim().isEmpty()) {
                        out.println(userName);
                    } else {
                        userName = "Anonymous";
                        out.println(userName);
                    }
                } else if (message.startsWith("Enter your latitude:")) {
                    String latStr = promptUser("Latitude", "Enter your latitude:");
                    out.println(latStr);
                } else if (message.startsWith("Enter your longitude:")) {
                    String lonStr = promptUser("Longitude", "Enter your longitude:");
                    out.println(lonStr);
                } else if (message.startsWith("USER_LIST:")) {
                    String users = message.substring(10);
                    updateUserList(users);
                } else if (message.startsWith("USER_COORDINATES:")) {
                    String json = message.substring(17).trim();
                    displayUserCoordinates(json);
                } else {
                    chatArea.append(message + "\n");
                }
            });
        }

        /**
         * Prompts the user for input using a dialog.
         *
         * @param title   The title of the dialog.
         * @param message The message to display.
         * @return The user's input as a String.
         */
        private String promptUser(String title, String message) {
            return JOptionPane.showInputDialog(frame, message, title, JOptionPane.PLAIN_MESSAGE);
        }

        /**
         * Updates the user list displayed in the GUI.
         *
         * @param users Comma-separated list of usernames.
         */
        private void updateUserList(String users) {
            listModel.clear();
            if (!users.isEmpty()) {
                String[] userArray = users.split(",");
                for (String user : userArray) {
                    listModel.addElement(user);
                }
            }
        }

        /**
         * Displays the coordinates of a user by opening Google Maps in the default browser.
         *
         * @param json The JSON payload containing the user's name and coordinates.
         */
        private void displayUserCoordinates(String json) {
            try {
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                String name = obj.get("userName").getAsString();
                double lat = obj.get("latitude").getAsDouble();
                double lon = obj.get("longitude").getAsDouble();

                String mapsUrl = String.format("https://www.google.com/maps?q=%f,%f", lat, lon);
                Desktop.getDesktop().browse(new URI(mapsUrl));
                chatArea.append("Opened " + name + "'s location on Google Maps.\n");
            } catch (Exception e) {
                chatArea.append("Failed to parse user coordinates.\n");
            }
        }
    }

    /**
     * Starts the client by connecting to the server.
     */
    @Override
    public void run() {
        connectToServer();
    }

    public static void main(String[] args) {
        // Start the client GUI
        SwingClient client = new SwingClient();
        new Thread(client).start();
    }
}
