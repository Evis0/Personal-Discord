package main.VideoCall;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class VideoCallReceiver {

    public static void receiveVideo(String senderIP, int senderPort, String senderUser) {
        Thread thread = new Thread(() -> runReceiver(senderIP, senderPort, senderUser));
        thread.start();
    }

    private static void runReceiver(String senderIP, int senderPort, String senderUser) {
        JFrame window = new JFrame("Video Call - " + senderUser);
        JLabel videoLabel = new JLabel("Connecting to @" + senderUser + "...", SwingConstants.CENTER);
        videoLabel.setHorizontalAlignment(JLabel.CENTER);
        videoLabel.setVerticalAlignment(JLabel.CENTER);

        SwingUtilities.invokeLater(() -> {
            window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            window.setSize(800, 600);
            window.setLocationRelativeTo(null);
            window.add(videoLabel);
            window.setVisible(true);
        });

        try (Socket socket = new Socket(senderIP, senderPort);
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            System.out.println("[VIDEO] Connected to sender.");

            while (true) {
                int frameSize;

                try {
                    frameSize = in.readInt();
                } catch (EOFException e) {
                    System.out.println("[VIDEO] Stream ended.");
                    break;
                }

                if (frameSize <= 0) {
                    continue;
                }

                byte[] frameBytes = new byte[frameSize];
                in.readFully(frameBytes);

                BufferedImage image = ImageIO.read(new ByteArrayInputStream(frameBytes));
                if (image == null) {
                    continue;
                }

                SwingUtilities.invokeLater(() -> {
                    videoLabel.setText("");
                    videoLabel.setIcon(new ImageIcon(image));
                });
            }

        } catch (IOException e) {
            System.out.println("[VIDEO] Receiver error: " + e.getMessage());
        } finally {
            SwingUtilities.invokeLater(window::dispose);
            System.out.println("[VIDEO] Receiver closed.");
        }
    }
}