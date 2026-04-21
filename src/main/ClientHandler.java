package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean registered = false;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("[SERVER] Asking for username from " + socket.getRemoteSocketAddress());
            out.println("SERVER: Please enter your username:");
            username = in.readLine();

            if (username == null || username.trim().isEmpty()) {
                System.out.println("[SERVER] Invalid username from " + socket.getRemoteSocketAddress() + ". Disconnecting.");
                out.println("SERVER: Invalid username. Disconnecting.");
                socket.close();
                return;
            }

            username = username.trim();
            System.out.println("[SERVER] Client wants username: '" + username + "'");

            if (!ServerMain.checkAndRegisterUsername(username, this)) {
                System.out.println("[SERVER] Username taken: '" + username + "'");
                out.println("SERVER: Username '" + username + "' is already taken. Disconnecting.");
                username = null;
                socket.close();
                return;
            }

            registered = true;
            ServerMain.markOnline(username);
            ServerMain.addClient(this);

            ServerMain.broadcast("SERVER: " + username + " has joined the chat!", this);
            out.println("SERVER: Welcome " + username + "! You are now connected.");
            out.println("SERVER: You are in the '" + ServerMain.getRoomName() + "' room.");
            out.println("SERVER: Commands: /rename <name> | /roomname | /online | /sendfile @username <filepath> | /call @username");
            ServerMain.sendChatHistory(this);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.trim().isEmpty()) continue;

                // File transfer offer
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
                            System.out.println("[SERVER] P2P file: " + username + " -> " + targetUser + " (" + fileName + ")");
                        } else {
                            sendMessage("SERVER: User '" + targetUser + "' is not online.");
                        }
                    } else {
                        sendMessage("SERVER: Invalid P2P offer format.");
                    }
                    continue;
                }

                if (message.startsWith("P2P_ACCEPT|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length == 2) {
                        String senderUsername = parts[1];
                        ClientHandler sender = ServerMain.getClientByUsername(senderUsername);
                        if (sender != null) {
                            sender.sendMessage("P2P_ACCEPTED|" + username);
                        }
                    }
                    continue;
                }

                if (message.startsWith("P2P_DECLINE|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length == 2) {
                        String senderUsername = parts[1];
                        ClientHandler sender = ServerMain.getClientByUsername(senderUsername);
                        if (sender != null) {
                            sender.sendMessage("P2P_DECLINED|" + username);
                        }
                    }
                    continue;
                }

                // Video call offer
                if (message.startsWith("P2P_VIDEO_OFFER|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length == 3) {
                        String targetUser = parts[1];
                        String senderPort = parts[2];
                        String senderIP = socket.getInetAddress().getHostAddress();

                        ClientHandler target = ServerMain.getClientByUsername(targetUser);
                        if (target != null) {
                            target.sendMessage("P2P_VIDEO_INCOMING|" + username + "|" + senderIP + "|" + senderPort);
                            sendMessage("SERVER: Video call offer sent to " + targetUser);
                            System.out.println("[SERVER] Video offer: " + username + " -> " + targetUser + " at " + senderIP + ":" + senderPort);
                        } else {
                            sendMessage("SERVER: User '" + targetUser + "' is not online.");
                        }
                    } else {
                        sendMessage("SERVER: Invalid P2P video offer format.");
                    }
                    continue;
                }

                // Two-way accept: include acceptor port so caller can connect back
                if (message.startsWith("P2P_VIDEO_ACCEPT|")) {
                    String[] parts = message.split("\\|");
                    // P2P_VIDEO_ACCEPT|callerUsername|acceptorPort
                    if (parts.length == 3) {
                        String callerUsername = parts[1];
                        String acceptorPort = parts[2];
                        String acceptorIP = socket.getInetAddress().getHostAddress();

                        ClientHandler originalCaller = ServerMain.getClientByUsername(callerUsername);
                        if (originalCaller != null) {
                            originalCaller.sendMessage("P2P_VIDEO_ACCEPTED|" + username + "|" + acceptorIP + "|" + acceptorPort);
                        }
                    } else {
                        sendMessage("SERVER: Invalid P2P video accept format.");
                    }
                    continue;
                }

                if (message.startsWith("P2P_VIDEO_DECLINE|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length == 2) {
                        String senderUsername = parts[1];
                        ClientHandler originalSender = ServerMain.getClientByUsername(senderUsername);
                        if (originalSender != null) {
                            originalSender.sendMessage("P2P_VIDEO_DECLINED|" + username);
                        }
                    }
                    continue;
                }

                // Video stream signalling (still supported)
                if (message.startsWith("P2P_VIDSTREAM_OFFER|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length == 5) {
                        String targetUser = parts[1];
                        String fileName = parts[2];
                        String senderPort = parts[3];
                        String fileSize = parts[4];
                        String senderIP = socket.getInetAddress().getHostAddress();

                        ClientHandler target = ServerMain.getClientByUsername(targetUser);
                        if (target != null) {
                            target.sendMessage("P2P_VIDSTREAM_INCOMING|" + username + "|" + fileName + "|" + senderIP + "|" + senderPort + "|" + fileSize);
                            sendMessage("SERVER: Video stream offer sent to " + targetUser);
                            System.out.println("[SERVER] Video stream offer: " + username + " -> " + targetUser + " (" + fileName + ")");
                        } else {
                            sendMessage("SERVER: User '" + targetUser + "' is not online.");
                        }
                    } else {
                        sendMessage("SERVER: Invalid P2P video stream offer format.");
                    }
                    continue;
                }

                if (message.startsWith("P2P_VIDSTREAM_ACCEPT|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length == 2) {
                        String senderUsername = parts[1];
                        ClientHandler originalSender = ServerMain.getClientByUsername(senderUsername);
                        if (originalSender != null) {
                            originalSender.sendMessage("P2P_VIDSTREAM_ACCEPTED|" + username);
                        }
                    }
                    continue;
                }

                if (message.startsWith("P2P_VIDSTREAM_DECLINE|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length == 2) {
                        String senderUsername = parts[1];
                        ClientHandler originalSender = ServerMain.getClientByUsername(senderUsername);
                        if (originalSender != null) {
                            originalSender.sendMessage("P2P_VIDSTREAM_DECLINED|" + username);
                        }
                    }
                    continue;
                }

                if (message.equalsIgnoreCase("/help")) {
                    sendMessage("SERVER: Commands:");
                    sendMessage("  /sendfile @username /path/to/file");
                    sendMessage("  /call @username");
                    sendMessage("  /streamvideo @username /path/to/video");
                    sendMessage("  /rename <newname>");
                    sendMessage("  /roomname");
                    sendMessage("  /online");
                    sendMessage("  /help");
                    continue;
                }

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

                if (message.equalsIgnoreCase("/roomname")){
                    sendMessage("SERVER: Current room name is: " + ServerMain.getRoomName());
                    continue;
                }

                if (message.equalsIgnoreCase("/roomstats")){
                    sendMessage("SERVER: Current room stats is: " + ServerMain.getRoomStats());
                    continue;
                }

                if (message.equalsIgnoreCase("/online")) {
                    int count = ServerMain.getOnlineCount();
                    String usernames = ServerMain.getOnlineUsernames();
                    if (count == 1) {
                        sendMessage("SERVER: There is currently 1 client online: " + usernames);
                    } else {
                        sendMessage("SERVER: There are currently " + count + " client(s) online: " + usernames);
                    }
                    continue;
                }

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
            if (registered && username != null) {
                System.out.println("[SERVER] Cleaning up user '" + username + "' - disconnecting...");
                ServerMain.markOffline(username);
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
