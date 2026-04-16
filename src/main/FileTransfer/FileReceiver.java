package main.FileTransfer;

import java.io.*;
import java.net.Socket;
import main.FileTransfer.MediaViewer;

/**
 * P2P File Receiver - connects directly to the sender's IP:port to download.
 * Runs on the receiving client's machine.
 */
public class FileReceiver {

    private static final String DOWNLOAD_DIR = "src/Downloads";

    /**
     * Connects to the sender and downloads the file.
     * Should be called in a separate thread.
     *
     * @param senderIP   the IP address of the sender
     * @param senderPort the port the sender is listening on
     */
    public static void receiveFile(String senderIP, int senderPort) {
        new Thread(() -> {
            try {
                System.out.println("Connecting to sender...");
                Socket socket = new Socket(senderIP, senderPort);
                DataInputStream dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                // Read: filename length, filename, file size, file data
                int nameLen = dataIn.readInt();
                byte[] nameBytes = new byte[nameLen];
                dataIn.readFully(nameBytes);
                String fileName = new String(nameBytes, "UTF-8");

                long fileSize = dataIn.readLong();
                System.out.println("Receiving '" + fileName + "'...");

                // Ensure download directory exists
                File downloadDir = new File(DOWNLOAD_DIR);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }

                File outputFile = new File(downloadDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    long remaining = fileSize;
                    while (remaining > 0) {
                        int toRead = (int) Math.min(buffer.length, remaining);
                        int bytesRead = dataIn.read(buffer, 0, toRead);
                        if (bytesRead == -1) break;
                        fos.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }
                }

                System.out.println("Saved to: " + outputFile.getAbsolutePath());

                // Auto-display images / open videos with default app (minimal UI for demo)
                try {
                    MediaViewer.openIfMedia(outputFile);
                } catch (Exception ignored) {
                }

                socket.close();

            } catch (Exception e) {
                System.out.println("Download failed: " + e.getMessage());
            }
        }, "P2P-FileReceiver").start();
    }
}