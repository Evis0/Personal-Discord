package main.VideoCall;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Simple webcam capture using Java's Robot class and JFrame
 * This is a basic implementation that captures screen area (for testing)
 * For real webcam access, use JavaCV or webcam-capture library
 */
public class Webcam {

    private Robot robot;
    private Rectangle captureArea;
    private boolean isOpen = false;

    /**
     * Opens the webcam (or screen capture for testing)
     */
    public boolean open() {
        try {
            robot = new Robot();
            // For testing: capture a small area of the screen
            // In production: use actual webcam library
            captureArea = new Rectangle(0, 0, 320, 240);
            isOpen = true;
            System.out.println("[WEBCAM] Opened (using screen capture for testing)");
            return true;
        } catch (AWTException e) {
            System.out.println("[WEBCAM] Failed to open: " + e.getMessage());
            return false;
        }
    }

    /**
     * Captures a single frame
     */
    public BufferedImage captureFrame() {
        if (!isOpen || robot == null) {
            return null;
        }
        return robot.createScreenCapture(captureArea);
    }

    /**
     * Closes the webcam
     */
    public void close() {
        isOpen = false;
        System.out.println("[WEBCAM] Closed");
    }

    public boolean isOpen() {
        return isOpen;
    }
}
