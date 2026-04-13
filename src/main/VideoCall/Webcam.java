package main.VideoCall;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class Webcam {

    private com.github.sarxos.webcam.Webcam webcam;

    public boolean open() {
        try {
            webcam = com.github.sarxos.webcam.Webcam.getDefault();

            if (webcam == null) {
                System.out.println("[WEBCAM] No webcam found.");
                return false;
            }

            webcam.setViewSize(new Dimension(320, 240));
            webcam.open();

            System.out.println("[WEBCAM] Opened webcam: " + webcam.getName());
            return true;
        } catch (Exception e) {
            System.out.println("[WEBCAM] Failed to open webcam: " + e.getMessage());
            return false;
        }
    }

    public BufferedImage captureFrame() {
        if (webcam == null || !webcam.isOpen()) {
            return null;
        }
        return webcam.getImage();
    }

    public void close() {
        try {
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        } catch (Exception e) {
            System.out.println("[WEBCAM] Error closing webcam: " + e.getMessage());
        }
    }
}