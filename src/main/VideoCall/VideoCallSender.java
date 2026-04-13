package main.VideoCall;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class VideoCallSender {

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Webcam webcam;

    public int startVideoCall() {
        try {
            serverSocket = new ServerSocket(0);
            int port = serverSocket.getLocalPort();

            System.out.println("[VIDEO] Waiting for receiver on port " + port);

            Thread thread = new Thread(() -> {
                Socket socket = null;
                DataOutputStream out = null;
                ByteArrayOutputStream byteStream = null;

                try {
                    socket = serverSocket.accept();
                    System.out.println("[VIDEO] Receiver connected.");

                    webcam = new Webcam();
                    if (!webcam.open()) {
                        System.out.println("[VIDEO] Could not open webcam.");
                        return;
                    }

                    out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    byteStream = new ByteArrayOutputStream();

                    running = true;

                    while (running && !socket.isClosed()) {
                        BufferedImage frame = webcam.captureFrame();

                        if (frame == null) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ignored) {
                            }
                            continue;
                        }

                        byteStream.reset();
                        ImageIO.write(frame, "jpg", byteStream);
                        byte[] imageBytes = byteStream.toByteArray();

                        out.writeInt(imageBytes.length);
                        out.write(imageBytes);
                        out.flush();

                        try {
                            Thread.sleep(66); // about 15 FPS
                        } catch (InterruptedException ignored) {
                        }
                    }

                } catch (IOException e) {
                    System.out.println("[VIDEO] Sender error: " + e.getMessage());
                } finally {
                    running = false;

                    try {
                        if (out != null) out.close();
                    } catch (IOException ignored) {
                    }

                    try {
                        if (socket != null) socket.close();
                    } catch (IOException ignored) {
                    }

                    try {
                        if (serverSocket != null && !serverSocket.isClosed()) {
                            serverSocket.close();
                        }
                    } catch (IOException ignored) {
                    }

                    if (webcam != null) {
                        webcam.close();
                    }

                    System.out.println("[VIDEO] Video call ended.");
                }
            });

            thread.start();
            return port;

        } catch (IOException e) {
            System.out.println("[VIDEO] Failed to start video call: " + e.getMessage());
            return -1;
        }
    }

    public void stopVideoCall() {
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }

        if (webcam != null) {
            webcam.close();
        }
    }
}