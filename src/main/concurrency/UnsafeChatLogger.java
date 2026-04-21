package main.concurrency;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class UnsafeChatLogger implements ChatLogger { // Unsafe because loggedCount is updated with an unsynchronized read-modify-write, which can lose updates when multiple threads log messages concurrently.

    private final String logFilePath = "chatlog.txt";
    private int loggedCount = 0;

    @Override
    public void logMessage(String message) {
        int temp = loggedCount;

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("[SERVER] Error writing to chat log: " + e.getMessage());
        }

        loggedCount = temp + 1;
    }

    @Override
    public String getLogStats() {
        return "loggedCount =" + loggedCount;
    }
}


