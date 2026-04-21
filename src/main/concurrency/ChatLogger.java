package main.concurrency;

public interface ChatLogger {
    void logMessage(String message);
    String getLogStats();
}
