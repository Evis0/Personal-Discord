package main.concurrency;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class SafeChatLogger implements ChatLogger { // Safe because file writes and shared counters are updated inside one synchronized critical section.

    private final Object logLock = new Object();
    private final String logFilePath = "chatlog.txt";
    private int loggedCount = 0;

    @Override
    public void logMessage(String message) {
        synchronized (logLock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
                writer.write(message);
                writer.newLine();
                loggedCount++;
            } catch (IOException e) {
                System.out.println("[SERVER] Error writing to chat log: " + e.getMessage());
            }
        }
    }

    @Override
    public String getLogStats() {
        synchronized (logLock) {
            return "loggedCount =" + loggedCount;
        }
    }
}
