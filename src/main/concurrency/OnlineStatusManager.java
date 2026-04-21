package main.concurrency;

public interface OnlineStatusManager {
    void markOnline(String username);
    void markOffline(String username);
    int getOnlineCount();
    String getOnlineStatusMessage();
}
