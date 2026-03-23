package main.FileTransfer;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Simple GUI to display images and play videos.
 * Handles .jpg, .png, .gif for images
 * Handles .mp4, .avi, .mov for videos (basic support)
 */
public class MediaViewer {

    /**
     * Opens a window to display an image or play a video
     * @param filePath path to the file to display/play
     */
    public static void viewMedia(String filePath) {
        File file = new File(filePath);
        
        if (!file.exists()) {
            JOptionPane.showMessageDialog(null, "File not found: " + filePath, 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String filename = file.getName().toLowerCase();

        // Image files
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || 
            filename.endsWith(".png") || filename.endsWith(".gif")) {
            displayImage(filePath);
        }
        // Video files
        else if (filename.endsWith(".mp4") || filename.endsWith(".avi") || 
                 filename.endsWith(".mov") || filename.endsWith(".mkv")) {
            playVideo(filePath);
        }
        else {
            JOptionPane.showMessageDialog(null, 
                "Unsupported file type: " + filename + "\n\n" +
                "Supported: .jpg, .png, .gif (images) or .mp4, .avi, .mov (videos)",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Display an image in a window
     */
    private static void displayImage(String filePath) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Image Viewer - " + new File(filePath).getName());
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            // Load and display the image
            ImageIcon icon = new ImageIcon(filePath);
            
            // Scale image to fit window if too large
            Image img = icon.getImage();
            Image scaledImg = img.getScaledInstance(800, 600, Image.SCALE_SMOOTH);
            icon = new ImageIcon(scaledImg);

            JLabel label = new JLabel(icon);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);

            JScrollPane scrollPane = new JScrollPane(label);
            frame.add(scrollPane);

            frame.setVisible(true);
            System.out.println("[VIEWER] Opened image: " + filePath);
        });
    }

    /**
     * Play a video file
     * Note: Requires JavaFX or external player for full functionality
     * This version uses a basic approach
     */
    private static void playVideo(String filePath) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Try to open with system default video player
                String os = System.getProperty("os.name").toLowerCase();
                
                if (os.contains("win")) {
                    // Windows
                    Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", filePath});
                    System.out.println("[VIEWER] Opened video with Windows player: " + filePath);
                } 
                else if (os.contains("mac")) {
                    // Mac
                    Runtime.getRuntime().exec(new String[]{"open", filePath});
                    System.out.println("[VIEWER] Opened video with Mac player: " + filePath);
                } 
                else if (os.contains("nux")) {
                    // Linux
                    Runtime.getRuntime().exec(new String[]{"xdg-open", filePath});
                    System.out.println("[VIEWER] Opened video with Linux player: " + filePath);
                }

                // Show confirmation dialog
                JOptionPane.showMessageDialog(null,
                    "Opening video: " + new File(filePath).getName() + "\n\n" +
                    "Video will play in your system's default media player.",
                    "Video Playback", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Could not open video player:\n" + e.getMessage() + "\n\n" +
                    "Try opening the file manually:\n" + filePath,
                    "Error", JOptionPane.ERROR_MESSAGE);
                System.out.println("[VIEWER] Error opening video: " + e.getMessage());
            }
        });
    }
}
