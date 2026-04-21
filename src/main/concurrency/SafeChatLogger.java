package main.concurrency;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class SafeChatLogger implements ChatLogger {

    private final ReentrantLock logLock = new ReentrantLock();
    private final String logFilePath = "chatlog.txt";
    private int loggedCount = 0;

    @Override
    public void logMessage(String message) {
        logLock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(message);
            writer.newLine();
            loggedCount++;
        } catch (IOException e) {
            System.out.println("[SERVER] Error writing to chat log: " + e.getMessage());
        } finally {
            logLock.unlock();
        }
    }

    @Override
    public String getLogStats() {
        logLock.lock();
        try {
            return "loggedCount =" + loggedCount;
        } finally {
            logLock.unlock();
        }
    }
}