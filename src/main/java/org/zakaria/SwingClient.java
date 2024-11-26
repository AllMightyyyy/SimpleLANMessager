package org.zakaria;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Swing-based client for the LAN Messenger.
 * Provides a graphical user interface for users to send and receive messages.
 */
public class SwingClient implements Runnable {
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

    /**
     * Initializes the GUI components.
     */
    public SwingClient() {
        initializeGUI();
    }

    /**
     * Sets up the Swing GUI.
     */
    private void initializeGUI() {
        frame = new JFrame("LAN Messenger");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout());

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        // User list
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));
        userScrollPane.setBorder(BorderFactory.createTitledBorder("Users"));

        // Input panel
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
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

            // Prompt for username
            userName = JOptionPane.showInputDialog(frame, "Enter your username:", "Username", JOptionPane.PLAIN_MESSAGE);
            if (userName == null || userName.trim().isEmpty()) {
                userName = "Anonymous";
            }
            out.println(userName);

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
     * Runnable class to handle incoming messages from the server.
     */
    private class IncomingReader implements Runnable {
        @Override
        public void run() {
            String serverMessage;
            try {
                while ((serverMessage = in.readLine()) != null) {
                    if (serverMessage.startsWith("USER_LIST:")) {
                        String users = serverMessage.substring(10);
                        updateUserList(users);
                    } else {
                        chatArea.append(serverMessage + "\n");
                    }
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
         * Updates the user list displayed in the GUI.
         *
         * @param users Comma-separated list of usernames.
         */
        private void updateUserList(String users) {
            SwingUtilities.invokeLater(() -> {
                listModel.clear();
                if (!users.isEmpty()) {
                    String[] userArray = users.split(",");
                    for (String user : userArray) {
                        listModel.addElement(user);
                    }
                }
            });
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
        // Run multiple clients
        for (int i = 0; i < 3; i++) {
            SwingClient client = new SwingClient();
            new Thread(client).start();
        }
    }
}
