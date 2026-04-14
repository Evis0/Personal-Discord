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

    private static volatile ClientGUI gui;

    // If we ask Y/N, we store one pending thing here.
    private static volatile PendingOffer pendingOffer;
    private static volatile PendingVideoOffer pendingVideoOffer;

    // Keep the sender object around so its thread doesn't die.
    private static volatile VideoCallSender activeVideoSender;

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

            Thread receiverThread = new Thread(ClientMain::receiveMessages, "ClientReceiver");
            receiverThread.setDaemon(true);
            receiverThread.start();

            // GUI is the main input for now.
            // If you want terminal input for debugging, you can bring sendMessages() back.

        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleUserInputFromGui(String text) {
        handleOutgoingLine(text);
    }

    // Returns false if we should exit the client.
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

        // Don't stack multiple prompts during demo.
        boolean hasPending = pendingOffer != null || pendingVideoOffer != null;

        if (pendingVideoOffer != null) {
            if (message.equalsIgnoreCase("Y")) {
                acceptTwoWayVideoCall(
                        pendingVideoOffer.senderUser,
                        pendingVideoOffer.senderIP,
                        pendingVideoOffer.senderPort
                );
                pendingVideoOffer = null;
                return true;
            } else if (message.equalsIgnoreCase("N")) {
                System.out.println("Declined video call from @" + pendingVideoOffer.senderUser + ".");
                out.println("P2P_VIDEO_DECLINE|" + pendingVideoOffer.senderUser);
                pendingVideoOffer = null;
                return true;
            } else {
                System.out.println("Reply Y or N for the video call.");
                return true;
            }
        }

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
                System.out.println("Reply Y or N.");
                return true;
            }
        }

        if (message.startsWith("/sendfile ")) {
            if (hasPending) {
                System.out.println("You have a pending request. Reply Y/N first.");
                return true;
            }
            handleSendFile(message);
            return true;
        }

        if (message.startsWith("/call ")) {
            if (hasPending) {
                System.out.println("You have a pending request. Reply Y/N first.");
                return true;
            }
            handleCallCommand(message);
            return true;
        }

        out.println(message);
        return true;
    }

    private static void receiveMessages() {
        try {
            System.out.println("Listening for incoming messages...");
            String received;
            while ((received = in.readLine()) != null) {
                if (received.startsWith("ONLINE_USERS|")) {
                    String csv = received.substring("ONLINE_USERS|".length()).trim();
                    List<String> users = new ArrayList<>();
                    if (!csv.isEmpty()) {
                        String[] parts = csv.split(",");
                        for (String u : parts) {
                            String name = u.trim();
                            if (!name.isEmpty()) {
                                users.add(name);
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
                    handleVideoAccepted(received);
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
        // P2P_FILE_OFFER|senderUsername|fileName|senderIP|senderPort|fileSize
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

        pendingOffer = new PendingOffer(senderUser, fileName, senderIP, senderPort, fileSize);
        System.out.println("Incoming '" + fileName + "' from @" + senderUser + " (" + fileSize + " bytes). Accept? [Y/N]");
    }

    private static void handleVideoAccepted(String message) {
        // P2P_VIDEO_ACCEPTED|acceptorUsername|acceptorIP|acceptorPort
        String[] parts = message.split("\\|");
        if (parts.length != 4) {
            System.out.println("Invalid video accepted format.");
            return;
        }

        String acceptorUser = parts[1];
        String acceptorIP = parts[2];
        int acceptorPort;

        try {
            acceptorPort = Integer.parseInt(parts[3]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid acceptor video port.");
            return;
        }

        System.out.println("@" + acceptorUser + " accepted your video call.");
        VideoCallReceiver.receiveVideo(acceptorIP, acceptorPort, acceptorUser);
    }

    private static void handleVideoOffer(String message) {
        // P2P_VIDEO_INCOMING|senderUsername|senderIP|senderPort
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
                    acceptTwoWayVideoCall(senderUser, senderIP, senderPort);
                } else {
                    out.println("P2P_VIDEO_DECLINE|" + senderUser);
                }
            });
            return;
        }

        pendingVideoOffer = new PendingVideoOffer(senderUser, senderIP, senderPort);
        System.out.println("Incoming video call from @" + senderUser + ". Accept? [Y/N]");
    }

    private static void handleVideoStreamOffer(String message) {
        // P2P_VIDSTREAM_INCOMING|senderUsername|fileName|senderIP|senderPort|fileSize
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

        System.out.println("Incoming video stream from @" + senderUser + ". Please accept/decline in the GUI.");
    }

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

        handleOutgoingLine("/sendfile @" + target + " " + file.getAbsolutePath());
    }

    private static void guiDownloadNotImplemented() {
        if (gui != null) {
            gui.appendMessage("SERVER: Download button isn't used in this stage. Incoming files auto-download on accept.");
        }
    }

    private static void handleSendFile(String message) {
        // /sendfile @username /path/to/file
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
            FileSender sender = new FileSender(file);
            sender.waitAndSend();

            String signal = "P2P_OFFER|" + targetUser + "|" + file.getName() + "|" + sender.getPort() + "|" + file.length();
            out.println(signal);

            System.out.println("Sending '" + file.getName() + "' to @" + targetUser + "...");
        } catch (IOException e) {
            System.out.println("Failed to start file sender: " + e.getMessage());
        }
    }

    private static int startOutgoingVideoSender() {
        activeVideoSender = new VideoCallSender();
        return activeVideoSender.startVideoCall();
    }

    private static void acceptTwoWayVideoCall(String callerUser, String callerIP, int callerPort) {
        int myReturnPort = startOutgoingVideoSender();
        if (myReturnPort == -1) {
            System.out.println("Failed to start webcam for return stream.");
            out.println("P2P_VIDEO_DECLINE|" + callerUser);
            return;
        }

        out.println("P2P_VIDEO_ACCEPT|" + callerUser + "|" + myReturnPort);
        VideoCallReceiver.receiveVideo(callerIP, callerPort, callerUser);

        System.out.println("Accepted video call from @" + callerUser + ".");
    }

    private static void handleCallCommand(String message) {
        // /call @username
        String args = message.substring("/call ".length()).trim();
        if (!args.startsWith("@") || args.length() < 2 || args.contains(" ")) {
            System.out.println("Usage: /call @username");
            return;
        }

        String targetUser = args.substring(1);

        int port = startOutgoingVideoSender();
        if (port == -1) {
            System.out.println("Failed to start video call sender.");
            return;
        }

        out.println("P2P_VIDEO_OFFER|" + targetUser + "|" + port);
        System.out.println("Video call request sent to @" + targetUser + ". Waiting for response...");
    }
}
