package main.FileTransfer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * P2P File Sender - opens a temporary server socket and waits for the receiver to connect.
 * Runs on the sending client's machine.
 */
public class FileSender {

    private final File file;
    private final int port;
    private ServerSocket serverSocket;

    public FileSender(File file) throws IOException {
        this.file = file;
        // port 0 = OS picks a free port
        this.serverSocket = new ServerSocket(0);
        this.port = serverSocket.getLocalPort();
        // Timeout after 60 seconds if no one connects
        this.serverSocket.setSoTimeout(60000);
    }

    public int getPort() {
        return port;
    }

    /**
     * Waits for a peer to connect, then sends the file directly.
     * Should be called in a separate thread.
     */
    public void waitAndSend() {
        new Thread(() -> {
            try {
                System.out.println("Waiting for recipient...");
                Socket peerSocket = serverSocket.accept();
                System.out.println("Recipient connected");

                DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(peerSocket.getOutputStream()));

                // Send: filename length, filename, file size, file data
                byte[] nameBytes = file.getName().getBytes("UTF-8");
                dataOut.writeInt(nameBytes.length);
                dataOut.write(nameBytes);

                long fileSize = file.length();
                dataOut.writeLong(fileSize);

                // Stream the file in chunks (memory-efficient)
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        dataOut.write(buffer, 0, bytesRead);
                    }
                    dataOut.flush();
                    System.out.println("Send complete");
                }

                peerSocket.close();
            } catch (Exception e) {
                System.out.println("Send failed: " + e.getMessage());
            } finally {
                try { serverSocket.close(); } catch (IOException ignored) {}
            }
        }, "P2P-FileSender").start();
    }

    public void close() {
        try { serverSocket.close(); } catch (IOException ignored) {}
    }
}