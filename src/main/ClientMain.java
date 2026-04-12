package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import main.FileTransfer.FileReceiver;

public class ClientMain {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ClientMain <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            Socket socket = new Socket(host, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to server at " + host + ":" + port);

            ClientGUI[] guiHolder = new ClientGUI[1];

            ClientGUI gui = new ClientGUI(
                    message -> out.println(message),

                    () -> {

                    String selectedUser = guiHolder[0].getSelectedUser();

                        if (selectedUser == null || selectedUser.trim().isEmpty()) {
                            JOptionPane.showMessageDialog(
                                guiHolder[0].getFrame(),
                                "Please select a user from the online users list."
                            );
                            return;
                        }

                        JFileChooser chooser = new JFileChooser();
                        int result = chooser.showOpenDialog(guiHolder[0].getFrame());

                        if (result == JFileChooser.APPROVE_OPTION) {
                            File selectedFile = chooser.getSelectedFile();
                            out.println("/sendfile @" + selectedUser + " " + selectedFile.getAbsolutePath());
                            guiHolder[0].appendMessage("SERVER: File offer sent to " + selectedUser);
                        }
                    },

                    () -> {
                        String id = JOptionPane.showInputDialog(guiHolder[0].getFrame(), "Enter file ID:");
                        if (id != null && !id.trim().isEmpty()) {
                            out.println("/downloadfile " + id.trim());
                        }
                    }
            );

            guiHolder[0] = gui;

            String username = JOptionPane.showInputDialog(gui.getFrame(), "Enter your username:");
            if (username == null || username.trim().isEmpty()) {
                JOptionPane.showMessageDialog(gui.getFrame(), "Username is required.");
                socket.close();
                return;
            }

            out.println(username.trim());

            // Thread to receive messages from server
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        // check if server is going to send a file
                        if (message.equals("SERVER_FILE_INCOMING")) {
                            gui.appendMessage("SERVER: Incoming file transfer.");

                            /** DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                             int nameLength = dataIn.readInt();

                             if (nameLength == -1) {
                             gui.appendMessage("SERVER: File not found.");
                             } else {
                             FileReceiver.receiveFile(dataIn, nameLength);
                             gui.appendMessage("SERVER: File downloaded to Downloads folder.");
                             }**/
                        }

                            //p2p file offer
                            else if(message.startsWith("P2P_FILE_OFFER|")){
                                String[] parts = message.split("\\|");

                                if (parts.length == 6) {
                                    String sender = parts[1];
                                    String fileName = parts[2];
                                    String senderIp = parts[3];
                                    String senderPort = parts[4];
                                    String fileSize = parts[5];

                                    int choice = JOptionPane.showConfirmDialog(
                                            gui.getFrame(),
                                            sender + " wants to send:\n"
                                            + fileName + " (" + fileSize + " bytes)\n\n"
                                            + "Acccept file?",
                                            "Incoming File",
                                            JOptionPane.YES_NO_OPTION
                                    );

                                            if (choice == JOptionPane.YES_OPTION) {
                                                gui.appendMessage("SERVER: Accepted File '" + fileName + "' from" + sender);
                                            } else {
                                                gui.appendMessage("SERVER: Rejected File '" + fileName + "' from" + sender);
                                    }
                                }else{
                                    gui.appendMessage("SERVER: Invalid file offer recieved");
                                }
                            }

                        else if (message.startsWith("SERVER: There are currently")
                                || message.startsWith("SERVER: There is currently")) {

                            gui.appendMessage(message); // still show in chat

                            // find the usernames after the colon
                            int colonIndex = message.lastIndexOf(":");
                            if (colonIndex != -1) {
                                String usersPart = message.substring(colonIndex + 1).trim();

                                if (!usersPart.equalsIgnoreCase("none")) {
                                    String[] users = usersPart.split(",");
                                    java.util.List<String> userList = new java.util.ArrayList<>();

                                    for (String user : users) {
                                        userList.add(user.trim());
                                    }

                                    gui.setOnlineUsers(userList);
                                } else {
                                    gui.setOnlineUsers(java.util.Collections.emptyList());
                                }
                            }
                        }
                        else if (message.startsWith("SERVER: There are currently")
                                || message.startsWith("SERVER: There is currently")) {

                            gui.appendMessage(message); // still show in chat

                            // find the usernames after the colon
                            int colonIndex = message.lastIndexOf(":");
                            if (colonIndex != -1) {
                                String usersPart = message.substring(colonIndex + 1).trim();

                                if (!usersPart.equalsIgnoreCase("none")) {
                                    String[] users = usersPart.split(",");
                                    java.util.List<String> userList = new java.util.ArrayList<>();

                                    for (String user : users) {
                                        userList.add(user.trim());
                                    }

                                    gui.setOnlineUsers(userList);
                                } else {
                                    gui.setOnlineUsers(java.util.Collections.emptyList());
                                }
                            }
                        }
                        else {
                            gui.appendMessage(message);
                        }
                    }
                } catch (IOException e) {
                    gui.appendMessage("Connection to server lost.");
                }
            });

            receiveThread.start();

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}