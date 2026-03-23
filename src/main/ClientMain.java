package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import main.FileTransfer.FileSender;
import main.FileTransfer.FileReceiver;
import java.io.File;

public class ClientMain {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    // Holds a single pending file offer awaiting Y/N input
    private static volatile PendingOffer pendingOffer = null;

    private static class PendingOffer {
        final String senderUser;
        final String fileName;
        final String senderIP;
        final int senderPort;
        final long fileSize;

        PendingOffer(String senderUser, String fileName, String senderIP, int senderPort, long fileSize) {
            this.senderUser = senderUser;
            this.fileName = fileName;
            this.senderIP = senderIP;
            this.senderPort = senderPort;
            this.fileSize = fileSize;
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ClientMain <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to server at " + host + ":" + port);

            // Receiver thread - listens for messages from server
            Thread receiver = new Thread(ClientMain::receiveMessages, "ClientReceiver");
            receiver.setDaemon(true);
            receiver.start();

            // Sender thread - reads user input
            sendMessages();

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void sendMessages() {
        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8)) {
            System.out.println("Ready to send messages. Type your message (type 'exit' to quit):");
            while (true) {
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("exit")) {
                    out.println("exit");
                    socket.close();
                    break;
                }

                // If we have a pending offer, interpret simple Y/N
                if (pendingOffer != null) {
                    if (message.equalsIgnoreCase("Y")) {
                        System.out.println("Accepted. Downloading '" + pendingOffer.fileName + "'...");
                        // Notify sender via server
                        out.println("P2P_ACCEPT|" + pendingOffer.senderUser);
                        // Start P2P download
                        FileReceiver.receiveFile(pendingOffer.senderIP, pendingOffer.senderPort);
                        pendingOffer = null;
                        continue;
                    } else if (message.equalsIgnoreCase("N")) {
                        System.out.println("Declined offer from @" + pendingOffer.senderUser + ".");
                        // Notify sender via server
                        out.println("P2P_DECLINE|" + pendingOffer.senderUser);
                        pendingOffer = null;
                        continue;
                    } else {
                        // Ignore other input until Y/N is provided; show quick hint
                        System.out.println("Please respond with Y (yes) or N (no).");
                        continue;
                    }
                }

                // Handle /sendfile command locally
                if (message.startsWith("/sendfile ")) {
                    handleSendFile(message);
                    continue;
                }

                // Regular message or other command
                out.println(message);
            }
        } catch (IOException e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }

    private static void receiveMessages() {
        try {
            System.out.println("Listening for incoming messages...");
            String received;
            while ((received = in.readLine()) != null) {
                // Handle P2P file offer from server
                if (received.startsWith("P2P_FILE_OFFER|")) {
                    handleFileOffer(received);
                    continue;
                }

                // Notify sender about receiver decision
                if (received.startsWith("P2P_ACCEPTED|")) {
                    String receiverUser = received.substring("P2P_ACCEPTED|".length());
                    System.out.println("@" + receiverUser + " accepted your file. Starting transfer...");
                    continue;
                }
                if (received.startsWith("P2P_DECLINED|")) {
                    String receiverUser = received.substring("P2P_DECLINED|".length());
                    System.out.println("@" + receiverUser + " declined your file.");
                    continue;
                }

                // Regular message from server
                System.out.println(received);
            }
            System.out.println("Connection closed by server.");
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.out.println("Error receiving message: " + e.getMessage());
            }
        }
    }

    private static void handleSendFile(String message) {
        // Format: /sendfile @username /path/to/file
        String args = message.substring("/sendfile ".length()).trim();
        if (!args.startsWith("@")) {
            System.out.println("Usage: /sendfile @username /path/to/file");
            return;
        }

        int spaceIdx = args.indexOf(' ');
        if (spaceIdx == -1) {
            System.out.println("Usage: /sendfile @username /path/to/file");
            return;
        }

        String targetUser = args.substring(1, spaceIdx);
        String filePath = args.substring(spaceIdx + 1).trim();

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + filePath);
            return;
        }

        try {
            // Open a P2P server socket and wait in background
            FileSender sender = new FileSender(file);
            sender.waitAndSend();

            // Tell the server to forward our offer to the target
            String signal = "P2P_OFFER|" + targetUser + "|" + file.getName() + "|" + sender.getPort() + "|" + file.length();
            out.println(signal);

            System.out.println("Sending '" + file.getName() + "' to @" + targetUser + "...");
        } catch (IOException e) {
            System.out.println("Failed to start file sender: " + e.getMessage());
        }
    }

    private static void handleFileOffer(String message) {
        // Format: P2P_FILE_OFFER|senderUsername|fileName|senderIP|senderPort|fileSize
        String[] parts = message.split("\\|");
        if (parts.length != 6) {
            System.out.println("Invalid file offer format.");
            return;
        }

        String senderUser = parts[1];
        String fileName = parts[2];
        String senderIP = parts[3];
        int senderPort = Integer.parseInt(parts[4]);
        long fileSize = Long.parseLong(parts[5]);

        // Record pending offer and prompt for Y/N
        pendingOffer = new PendingOffer(senderUser, fileName, senderIP, senderPort, fileSize);
        System.out.println("Incoming '" + fileName + "' from @" + senderUser + " (" + fileSize + " bytes). Accept? [Y/N]");
    }
}