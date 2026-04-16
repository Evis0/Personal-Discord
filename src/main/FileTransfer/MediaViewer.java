package main.FileTransfer;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Minimal media viewer helper:
 * - Images: display in a simple Swing window
 * - Videos/other: open with OS default application
 */
public final class MediaViewer {

    private MediaViewer() {}

    public static boolean isImageFileName(String fileName) {
        if (fileName == null) return false;
        String f = fileName.toLowerCase();
        return f.endsWith(".jpg") || f.endsWith(".jpeg") || f.endsWith(".png") || f.endsWith(".gif") || f.endsWith(".bmp");
    }

    public static boolean isVideoFileName(String fileName) {
        if (fileName == null) return false;
        String f = fileName.toLowerCase();
        return f.endsWith(".mp4") || f.endsWith(".avi") || f.endsWith(".mov") || f.endsWith(".mkv") || f.endsWith(".wmv");
    }

    public static void openIfMedia(File file) {
        if (file == null) return;
        String name = file.getName();
        if (isImageFileName(name)) {
            showImage(file);
            return;
        }
        if (isVideoFileName(name)) {
            openWithDefaultApp(file);
        }
    }

    public static void showImage(File file) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Image - " + file.getName());
            JLabel label = new JLabel("Loading...", SwingConstants.CENTER);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.add(new JScrollPane(label));
            frame.setVisible(true);

            try {
                BufferedImage img = ImageIO.read(file);
                if (img == null) {
                    label.setText("Unsupported image.");
                    return;
                }

                // Scale down if huge
                int maxW = 1400;
                int maxH = 900;
                Image toShow = img;
                if (img.getWidth() > maxW || img.getHeight() > maxH) {
                    double sx = maxW / (double) img.getWidth();
                    double sy = maxH / (double) img.getHeight();
                    double scale = Math.min(sx, sy);
                    int w = (int) Math.round(img.getWidth() * scale);
                    int h = (int) Math.round(img.getHeight() * scale);
                    toShow = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                }

                label.setText("");
                label.setIcon(new ImageIcon(toShow));
            } catch (IOException e) {
                label.setText("Failed to load image: " + e.getMessage());
            }
        });
    }

    public static void openWithDefaultApp(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException ignored) {
            // no-op
        }
    }
}

