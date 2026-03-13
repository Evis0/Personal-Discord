package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
            out.println("SERVER: Commands: /rename <name> - rename the room | /roomname - view room name | @online - see online users | /sendfile <filepath> - send a file | /downloadfile <id> - download a file by upload id");

            // Read and broadcast messages
            String message;
            while ((message = in.readLine()) != null) {
                if (message.trim().isEmpty()) continue;

                // sendfile
                if (message.startsWith("/sendfile ")) {
                    String filePath = message.substring("/sendfile ".length()).trim();
                    try {
                        String fileId = FileTransferService.handleUpload(filePath, username);
                        // get the filename for the broadcast message
                        String fileName = new File(filePath).getName();
                        ServerMain.broadcast(username + " sent file \"" + fileName + "\" [ID: " + fileId + "]. Use /downloadfile " + fileId + " to download it.", this);
                        // notify the sender
                        sendMessage("SERVER: File uploaded successfully. ID: " + fileId);
                    } catch (IOException e) {
                        sendMessage("SERVER: Failed to upload file - " + e.getMessage());
                    }
                    continue;
                }

                // downloadfile
                if (message.startsWith("/downloadfile ")) {
                    String fileId = message.substring("/downloadfile ".length()).trim();
                    try {
                        // write the file bakc with dataoutputstream
                        DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                        // signal to client that a file download is incoming
                        out.println("SERVER_FILE_INCOMING");
                        FileTransferService.handleDownload(fileId, dataOut);
                    } catch (IOException e) {
                        sendMessage("SERVER: Failed to download file - " + e.getMessage());
                    }
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

                if (message.equalsIgnoreCase("@online")) {
                    int count = ServerMain.getOnlineCount(); // use getOnlineCountUnsafe() for unsafe demo only
                    if (count == 1) {
                        sendMessage("SERVER: There is currently 1 client online.");
                    } else {
                        sendMessage("SERVER: There are currently " + count + " client(s) online.");
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

