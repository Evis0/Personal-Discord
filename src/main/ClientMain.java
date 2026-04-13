package main;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import main.FileTransfer.FileSender;
import main.FileTransfer.FileReceiver;
import java.io.File;
import main.VideoCall.VideoCallReceiver;
import main.VideoCall.VideoCallSender;

public class ClientMain {
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    // GUI is optional; keep terminal-driven behavior working too.
    private static volatile ClientGUI gui;

    // Holds a single pending offer awaiting Y/N input (file / video call / video stream)
    private static volatile PendingOffer pendingOffer = null;
    private static volatile PendingVideoOffer pendingVideoOffer = null;
    private static volatile PendingVideoStreamOffer pendingVideoStreamOffer = null;

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

    private static class PendingVideoOffer {
        final String senderUser;
        final String senderIP;
        final int senderPort;

        PendingVideoOffer(String senderUser, String senderIP, int senderPort) {
            this.senderUser = senderUser;
            this.senderIP = senderIP;
            this.senderPort = senderPort;
        }
    }

    private static class PendingVideoStreamOffer {
        final String senderUser;
        final String fileName;
        final String senderIP;
        final int senderPort;
        final long fileSize;

        PendingVideoStreamOffer(String senderUser, String fileName, String senderIP, int senderPort, long fileSize) {
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

            gui = new ClientGUI(
                    ClientMain::handleUserInputFromGui,
                    ClientMain::guiSendFile,
                    ClientMain::guiDownloadNotImplemented
            );

            Thread receiver = new Thread(ClientMain::receiveMessages, "ClientReceiver");
            receiver.setDaemon(true);
            receiver.start();

            // IMPORTANT: don’t block the app waiting for terminal input.
            // GUI is now the primary input method.
            // If you still want terminal input for debugging, uncomment this.
            // sendMessages();

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleUserInputFromGui(String message) {
        // GUI uses the exact same behavior as the terminal.
        // We keep this minimal and student-friendly on purpose.
        handleOutgoingLine(message);
    }

    /**
     * Handles one outgoing line from either terminal or GUI.
     * @return false if the app should exit
     */
    private static boolean handleOutgoingLine(String message) {
        if (message == null) return true;
        message = message.trim();
        if (message.isEmpty()) return true;

        if (message.equalsIgnoreCase("exit")) {
            out.println("exit");
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            return false;
        }

        // For demo simplicity: don't allow multiple different pending prompts at the same time.
        // (The protocol supports it, but overlapping prompts confuses people during demos.)
        boolean hasAnyPending = pendingOffer != null || pendingVideoOffer != null || pendingVideoStreamOffer != null;

        // NOTE: We check pending VIDEO prompts before pending FILE prompts.
        // That keeps Y/N responses deterministic during demos.

        // Handle pending video offer Y/N first
        if (pendingVideoOffer != null) {
            if (message.equalsIgnoreCase("Y")) {
                System.out.println("Accepted video call from @" + pendingVideoOffer.senderUser + ".");
                out.println("P2P_VIDEO_ACCEPT|" + pendingVideoOffer.senderUser);
                VideoCallReceiver.receiveVideo(
                        pendingVideoOffer.senderIP,
                        pendingVideoOffer.senderPort,
                        pendingVideoOffer.senderUser
                );
                pendingVideoOffer = null;
                return true;
            } else if (message.equalsIgnoreCase("N")) {
                System.out.println("Declined video call from @" + pendingVideoOffer.senderUser + ".");
                out.println("P2P_VIDEO_DECLINE|" + pendingVideoOffer.senderUser);
                pendingVideoOffer = null;
                return true;
            } else {
                System.out.println("Please respond with Y (yes) or N (no) for the video call.");
                return true;
            }
        }

        // Handle pending video stream offer Y/N
        if (pendingVideoStreamOffer != null) {
            if (message.equalsIgnoreCase("Y")) {
                System.out.println("Accepted video stream '" + pendingVideoStreamOffer.fileName + "' from @" + pendingVideoStreamOffer.senderUser + ".");
                out.println("P2P_VIDSTREAM_ACCEPT|" + pendingVideoStreamOffer.senderUser);
                VideoCallReceiver.receiveVideo(
                        pendingVideoStreamOffer.senderIP,
                        pendingVideoStreamOffer.senderPort,
                        pendingVideoStreamOffer.senderUser
                );
                pendingVideoStreamOffer = null;
                return true;
            } else if (message.equalsIgnoreCase("N")) {
                System.out.println("Declined video stream from @" + pendingVideoStreamOffer.senderUser + ".");
                out.println("P2P_VIDSTREAM_DECLINE|" + pendingVideoStreamOffer.senderUser);
                pendingVideoStreamOffer = null;
                return true;
            } else {
                System.out.println("Please respond with Y (yes) or N (no) for the video stream.");
                return true;
            }
        }

        // If we have a pending FILE offer, interpret simple Y/N
        if (pendingOffer != null) {
            if (message.equalsIgnoreCase("Y")) {
                System.out.println("Accepted. Downloading '" + pendingOffer.fileName + "'...");
                out.println("P2P_ACCEPT|" + pendingOffer.senderUser);
                FileReceiver.receiveFile(pendingOffer.senderIP, pendingOffer.senderPort);
                pendingOffer = null;
                return true;
            } else if (message.equalsIgnoreCase("N")) {
                System.out.println("Declined offer from @" + pendingOffer.senderUser + ".");
                out.println("P2P_DECLINE|" + pendingOffer.senderUser);
                pendingOffer = null;
                return true;
            } else {
                System.out.println("Please respond with Y (yes) or N (no).");
                return true;
            }
        }

        // Handle /sendfile command locally
        if (message.startsWith("/sendfile ")) {
            if (hasAnyPending) {
                System.out.println("You have a pending request. Respond Y/N first.");
                return true;
            }
            handleSendFile(message);
            return true;
        }

        // Handle /call command locally
        if (message.startsWith("/call ")) {
            if (hasAnyPending) {
                System.out.println("You have a pending request. Respond Y/N first.");
                return true;
            }
            handleCallCommand(message);
            return true;
        }

        // Handle /streamvideo command locally
        if (message.startsWith("/streamvideo ")) {
            if (hasAnyPending) {
                System.out.println("You have a pending request. Respond Y/N first.");
                return true;
            }
            // For Stage 4 demo simplicity: treat /streamvideo as a webcam-style video call.
            // (You can reintroduce real video-file streaming later if needed.)
            handleStreamVideoCommand(message);
            return true;
        }

        // Regular message or other command
        out.println(message);
        return true;
    }

    private static void receiveMessages() {
        try {
            System.out.println("Listening for incoming messages...");
            String received;
            while ((received = in.readLine()) != null) {
                // Update GUI user list (structured message from server)
                if (received.startsWith("ONLINE_USERS|")) {
                    String csv = received.substring("ONLINE_USERS|".length()).trim();
                    List<String> users = new ArrayList<>();
                    if (!csv.isEmpty()) {
                        String[] parts = csv.split(",");
                        for (String u : parts) {
                            String cleaned = u.trim();
                            if (!cleaned.isEmpty()) {
                                users.add(cleaned);
                            }
                        }
                    }
                    if (gui != null) {
                        gui.setOnlineUsers(users);
                    }
                    continue;
                }

                if (gui != null) {
                    gui.appendMessage(received);
                }

                if (received.startsWith("P2P_FILE_OFFER|")) {
                    handleFileOffer(received);
                    continue;
                }

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

                if (received.startsWith("P2P_VIDEO_INCOMING|")) {
                    handleVideoOffer(received);
                    continue;
                }

                if (received.startsWith("P2P_VIDEO_ACCEPTED|")) {
                    String receiverUser = received.substring("P2P_VIDEO_ACCEPTED|".length());
                    System.out.println("@" + receiverUser + " accepted your video call.");
                    continue;
                }
                if (received.startsWith("P2P_VIDEO_DECLINED|")) {
                    String receiverUser = received.substring("P2P_VIDEO_DECLINED|".length());
                    System.out.println("@" + receiverUser + " declined your video call.");
                    continue;
                }

                if (received.startsWith("P2P_VIDSTREAM_INCOMING|")) {
                    handleVideoStreamOffer(received);
                    continue;
                }

                if (received.startsWith("P2P_VIDSTREAM_ACCEPTED|")) {
                    String receiverUser = received.substring("P2P_VIDSTREAM_ACCEPTED|".length());
                    System.out.println("@" + receiverUser + " accepted your video stream.");
                    continue;
                }

                if (received.startsWith("P2P_VIDSTREAM_DECLINED|")) {
                    String receiverUser = received.substring("P2P_VIDSTREAM_DECLINED|".length());
                    System.out.println("@" + receiverUser + " declined your video stream.");
                    continue;
                }

                System.out.println(received);
            }
            System.out.println("Connection closed by server.");
        } catch (IOException e) {
            if (socket != null && !socket.isClosed()) {
                System.out.println("Error receiving message: " + e.getMessage());
            }
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

        // GUI prompt
        if (gui != null) {
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(
                        gui.getFrame(),
                        "Incoming file from @" + senderUser + "\n\n" + fileName + " (" + fileSize + " bytes)\n\nAccept?",
                        "File Transfer",
                        JOptionPane.YES_NO_OPTION
                );

                if (choice == JOptionPane.YES_OPTION) {
                    out.println("P2P_ACCEPT|" + senderUser);
                    FileReceiver.receiveFile(senderIP, senderPort);
                } else {
                    out.println("P2P_DECLINE|" + senderUser);
                }
            });
            return;
        }

        // Terminal fallback
        pendingOffer = new PendingOffer(senderUser, fileName, senderIP, senderPort, fileSize);
        System.out.println("Incoming '" + fileName + "' from @" + senderUser + " (" + fileSize + " bytes). Accept? [Y/N]");
    }

    private static void handleVideoOffer(String message) {
        // Format: P2P_VIDEO_INCOMING|senderUsername|senderIP|senderPort
        String[] parts = message.split("\\|");
        if (parts.length != 4) {
            System.out.println("Invalid video offer format.");
            return;
        }

        String senderUser = parts[1];
        String senderIP = parts[2];
        int senderPort;
        try {
            senderPort = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid video sender port.");
            return;
        }

        if (gui != null) {
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(
                        gui.getFrame(),
                        "Incoming video call from @" + senderUser + "\n\nAccept?",
                        "Video Call",
                        JOptionPane.YES_NO_OPTION
                );

                if (choice == JOptionPane.YES_OPTION) {
                    out.println("P2P_VIDEO_ACCEPT|" + senderUser);
                    VideoCallReceiver.receiveVideo(senderIP, senderPort, senderUser);
                } else {
                    out.println("P2P_VIDEO_DECLINE|" + senderUser);
                }
            });
            return;
        }

        // Terminal fallback
        pendingVideoOffer = new PendingVideoOffer(senderUser, senderIP, senderPort);
        System.out.println("Incoming video call from @" + senderUser + ". Accept? [Y/N]");
    }

    private static void handleVideoStreamOffer(String message) {
        // Accepting a stream offer starts a regular P2P webcam receiver.
        // Format: P2P_VIDSTREAM_INCOMING|senderUsername|fileName|senderIP|senderPort|fileSize
        String[] parts = message.split("\\|");
        if (parts.length != 6) {
            System.out.println("Invalid video stream offer format.");
            return;
        }

        String senderUser = parts[1];
        String senderIP = parts[3];
        int senderPort;

        try {
            senderPort = Integer.parseInt(parts[4]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid video stream sender port.");
            return;
        }

        if (gui != null) {
            SwingUtilities.invokeLater(() -> {
                int choice = JOptionPane.showConfirmDialog(
                        gui.getFrame(),
                        "Incoming video stream from @" + senderUser + "\n\nAccept?",
                        "Video Stream",
                        JOptionPane.YES_NO_OPTION
                );

                if (choice == JOptionPane.YES_OPTION) {
                    out.println("P2P_VIDSTREAM_ACCEPT|" + senderUser);
                    VideoCallReceiver.receiveVideo(senderIP, senderPort, senderUser);
                } else {
                    out.println("P2P_VIDSTREAM_DECLINE|" + senderUser);
                }
            });
            return;
        }

        // Terminal fallback
        pendingVideoStreamOffer = new PendingVideoStreamOffer(senderUser, parts[2], senderIP, senderPort, 0);
        System.out.println("Incoming video stream from @" + senderUser + ". Accept? [Y/N]");
    }

    // Simple GUI helper: send file to the currently selected user.
    private static void guiSendFile() {
        if (gui == null) return;
        String target = gui.getSelectedUser();
        if (target == null || target.trim().isEmpty()) {
            gui.appendMessage("SERVER: Select a user in the list first.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(gui.getFrame());
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        if (file == null || !file.exists() || !file.isFile()) {
            gui.appendMessage("SERVER: Invalid file.");
            return;
        }

        // reuse existing terminal command handler
        handleOutgoingLine("/sendfile @" + target + " " + file.getAbsolutePath());
    }

    private static void guiDownloadNotImplemented() {
        if (gui != null) {
            gui.appendMessage("SERVER: Download button isn't used in this stage. Incoming files auto-download on accept.");
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

    private static void handleCallCommand(String message) {
        // Format: /call @username
        String args = message.substring("/call ".length()).trim();
        if (!args.startsWith("@") || args.length() < 2 || args.contains(" ")) {
            System.out.println("Usage: /call @username");
            return;
        }

        String targetUser = args.substring(1);

        VideoCallSender sender = new VideoCallSender();
        int port = sender.startVideoCall();
        if (port == -1) {
            System.out.println("Failed to start video call sender.");
            return;
        }

        // Server is signaling-only; actual media is P2P to this sender port.
        out.println("P2P_VIDEO_OFFER|" + targetUser + "|" + port);
        System.out.println("Video call request sent to @" + targetUser + ". Waiting for response...");
    }

    private static void handleStreamVideoCommand(String message) {
        // Previously: stream a stored video file.
        // Now (simple Stage 4): treat this as a webcam video call offer to keep P2P demo working.
        // Format: /streamvideo @username
        String args = message.substring("/streamvideo ".length()).trim();
        if (!args.startsWith("@") || args.length() < 2 || args.contains(" ")) {
            System.out.println("Usage: /streamvideo @username");
            return;
        }

        String targetUser = args.substring(1);

        VideoCallSender sender = new VideoCallSender();
        int port = sender.startVideoCall();
        if (port == -1) {
            System.out.println("[VIDEO] Failed to start webcam call.");
            return;
        }

        // Keep using the existing stream-signaling message to avoid changing server code right now.
        // (Server forwards P2P_VIDSTREAM_INCOMING which the receiver treats as a video call.)
        out.println("P2P_VIDSTREAM_OFFER|" + targetUser + "|" + "webcam" + "|" + port + "|0");
        System.out.println("Video stream (webcam) offer sent to @" + targetUser + ".");
    }
}
