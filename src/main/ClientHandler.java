package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import main.FileTransfer.FileSender;
import main.FileTransfer.FileTransferService;
import java.io.DataOutputStream;
import java.io.File;
import main.FileTransfer.FileTransferService;
import java.io.DataOutputStream;
import java.io.File;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean registered = false; // Track if user successfully registered

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Setup I/O streams
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Request username from client
            System.out.println("[SERVER] Requesting username from " + socket.getRemoteSocketAddress());
            out.println("SERVER: Please enter your username:");
            username = in.readLine();


            if (username == null || username.trim().isEmpty()) {
                System.out.println("[SERVER] Invalid username received from " + socket.getRemoteSocketAddress() + ". Disconnecting.");
                out.println("SERVER: Invalid username. Disconnecting.");
                socket.close();
                return;
            }

            username = username.trim();
            System.out.println("[SERVER] Client wants username: '" + username + "'");

            // Check and register username atomically to prevent race condition
            if (!ServerMain.checkAndRegisterUsername(username, this)) {
                System.out.println("[SERVER] Username '" + username + "' is already taken. Rejecting client.");
                out.println("SERVER: Username '" + username + "' is already taken. Disconnecting.");
                username = null; // Clear username since registration failed
                socket.close();
                return;
            }

            // Mark as successfully registered
            registered = true;

            // Add to broadcast list now that registration succeeded
            ServerMain.addClient(this);

            // Notify everyone that user joined
            ServerMain.broadcast("SERVER: " + username + " has joined the chat!", this);
            out.println("SERVER: Welcome " + username + "! You are now connected.");
            out.println("SERVER: You are in the '" + ServerMain.getRoomName() + "' room.");
            out.println("SERVER: Commands: /rename <name> - rename the room | /roomname - view room name | /online - see online users | /sendfile <filepath> - send a file | /downloadfile <id> - download a file by upload id");
            ServerMain.sendChatHistory(this);
            
            // Read and broadcast messages
            // Read and broadcast messages
            String message;
            while ((message = in.readLine()) != null) {
                if (message.trim().isEmpty()) continue;

                // P2P file send: /sendfile @recipient /path/to/file
                if (message.startsWith("/sendfile ")) {
                    String args = message.substring("/sendfile ".length()).trim();
                    if (!args.startsWith("@")) {
                        sendMessage("SERVER: Usage: /sendfile @username /path/to/file");
                        continue;
                    }

                    int spaceIdx = args.indexOf(' ');
                    if (spaceIdx == -1) {
                        sendMessage("SERVER: Usage: /sendfile @username /path/to/file");
                        continue;
                    }

                    String targetUser = args.substring(1, spaceIdx);
                    String filePath = args.substring(spaceIdx + 1).trim();

                    File file = new File(filePath);
                    if (!file.exists() || !file.isFile()) {
                        sendMessage("SERVER: File not found: " + filePath);
                        sendMessage("SERVER: File absolute path: " + file.getAbsolutePath());
                        sendMessage("SERVER: File exists: " + file.exists());
                        continue;
                    }


                    try {
                        FileSender sender = new FileSender(file);
                        sender.waitAndSend();

                        String signal = "P2P_OFFER|" + targetUser + "|" + file.getName() + "|" + sender.getPort() + "|" + file.length();
                        out.println(signal);
                        System.out.println("[SERVER] P2P offer from " + username + " to " + targetUser + " on port " + sender.getPort());
                    } catch (IOException e) {
                        sendMessage("SERVER: Failed to start file sender: " + e.getMessage());
                    }
                    continue;
                }

                // P2P signalling: sender tells server they're ready to serve a file
                if (message.startsWith("P2P_OFFER|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length == 5) {
                        String targetUser = parts[1];
                        String fileName = parts[2];
                        String senderPort = parts[3];
                        String fileSize = parts[4];

                        String senderIP = socket.getInetAddress().getHostAddress();

                        ClientHandler target = ServerMain.getClientByUsername(targetUser);
                        if (target != null) {
                            target.sendMessage("P2P_FILE_OFFER|" + username + "|" + fileName + "|" + senderIP + "|" + senderPort + "|" + fileSize);
                            sendMessage("SERVER: File offer sent to " + targetUser + ". Waiting for them to connect...");
                            System.out.println("[SERVER] Signalling P2P transfer: " + username + " -> " + targetUser + " (" + fileName + ")");
                        } else {
                            sendMessage("SERVER: User '" + targetUser + "' is not online.");
                        }
                    } else {
                        sendMessage("SERVER: Invalid P2P offer format.");
                    }
                    continue;
                }


                // Help command
                if (message.equalsIgnoreCase("/help")) {
                    sendMessage("SERVER: Commands:");
                    sendMessage("  /sendfile @username /path/to/file  - Send a file to another user");
                    sendMessage("  /rename <newname>                  - Rename the chat room");
                    sendMessage("  /roomname                          - View current room name");
                    sendMessage("  /online                            - See how many users are online");
                    sendMessage("  /help                              - Show this help message");
                    continue;
                }

                //rename group chat
                if (message.startsWith("/rename ")){
                    String newName = message.substring("/rename ".length()).trim();

                    if (newName.isEmpty()) {
                        sendMessage("SERVER: Usage: /rename <newName>");
                        continue;
                    }

                    ServerMain.renameRoom(newName, username);
                    ServerMain.broadcast("SERVER: " + username + " renamed the room to " + newName, this);
                    continue;
                }

                //show the current group chat name
                if (message.equalsIgnoreCase("/roomname")){
                    sendMessage("SERVER: Current room name is: " + ServerMain.getRoomName());
                    continue;
                }

                //debug command to prove race condition
                if (message.equalsIgnoreCase("/roomstats")){
                    sendMessage("SERVER: Current room stats is: " + ServerMain.getRoomStats());
                    continue;
                }

                if (message.equalsIgnoreCase("/online")) {
                    int count = ServerMain.getOnlineCount(); // use getOnlineCountUnsafe() for unsafe demo only
                    String usernames = ServerMain.getOnlineUsernames();
                    if (count == 1) {
                        sendMessage("SERVER: There is currently 1 client online: " + usernames);
                    } else {
                        sendMessage("SERVER: There are currently " + count + " client(s) online: " + usernames);
                    }
                    continue;
                }

                // Broadcast message with username prefix
                ServerMain.broadcast(username + ": " + message, this);
            }

        } catch (IOException e) {
            if (username != null) {
                System.out.println("[SERVER] Connection error with user '" + username + "': " + e.getMessage());
            } else {
                System.out.println("[SERVER] Connection error with client " + socket.getRemoteSocketAddress() + ": " + e.getMessage());
            }
        } finally {
            cleanup();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String getUsername() {
        return username;
    }

    private void cleanup() {
        try {
            // Only unregister and broadcast if user was successfully registered
            if (registered && username != null) {
                System.out.println("[SERVER] Cleaning up user '" + username + "' - disconnecting...");
                ServerMain.unregisterUsername(username);
                ServerMain.broadcast("SERVER: " + username + " has left the chat.", this);
            }
            ServerMain.removeClient(this);
            if (socket != null) {
                socket.close();
                System.out.println("[SERVER] Socket closed for " + (username != null ? "user '" + username + "'" : "client"));
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Error during cleanup: " + e.getMessage());
        }
    }
}

