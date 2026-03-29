package main.VideoCall;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * P2P Video Call Sender - captures webcam and streams frames to receiver
 */
public class VideoCallSender {

    private ServerSocket serverSocket;
    private volatile boolean isStreaming = false;
    private Webcam webcam;

    /**
     * Starts a video call sender on a random available port
     * @return the port number, or -1 if failed
     */
    public int startVideoCall() {
        try {
            serverSocket = new ServerSocket(0); // Random available port
            int port = serverSocket.getLocalPort();
            System.out.println("[VIDEO] Waiting for receiver on port " + port);

            new Thread(() -> {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("[VIDEO] Receiver connected. Starting video stream...");

                    isStreaming = true;
                    streamVideo(socket);

                } catch (IOException e) {
                    if (isStreaming) {
                        System.out.println("[VIDEO] Stream error: " + e.getMessage());
                    }
                } finally {
                    stopVideoCall();
                }
            }, "VideoCallSender").start();

            return port;
        } catch (IOException e) {
            System.out.println("[VIDEO] Failed to start video call: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Streams video frames to the receiver
     */
    private void streamVideo(Socket socket) throws IOException {
        // Initialize webcam
        webcam = new Webcam();
        if (!webcam.open()) {
            System.out.println("[VIDEO] Failed to open webcam");
            return;
        }

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        try {
            System.out.println("[VIDEO] Streaming started. Press Ctrl+C or close connection to stop.");

            while (isStreaming && !socket.isClosed()) {
                // Capture frame from webcam
                BufferedImage frame = webcam.captureFrame();
                if (frame == null) {
                    Thread.sleep(100);
                    continue;
                }

                // Compress frame to JPEG
                byteStream.reset();
                ImageIO.write(frame, "jpg", byteStream);
                byte[] imageBytes = byteStream.toByteArray();

                // Send frame size and frame data
                out.writeInt(imageBytes.length);
                out.write(imageBytes);
                out.flush();

                // Target ~15 FPS
                Thread.sleep(66);
            }
        } catch (Exception e) {
            if (isStreaming) {
                System.out.println("[VIDEO] Streaming interrupted: " + e.getMessage());
            }
        } finally {
            webcam.close();
            socket.close();
            System.out.println("[VIDEO] Video call ended.");
        }
    }

    /**
     * Stops the video call
     */
    public void stopVideoCall() {
        isStreaming = false;
        try {
            if (webcam != null) webcam.close();
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }
}
