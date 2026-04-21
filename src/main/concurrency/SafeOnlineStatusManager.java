package main.concurrency;

import java.util.HashSet;
import java.util.Set;

public class SafeOnlineStatusManager implements OnlineStatusManager {

    private final Object onlineLock = new Object();
    private final Set<String> onlineUsers = new HashSet<>();

    @Override
    public void markOnline(String username) {
        synchronized (onlineLock) {
            onlineUsers.add(username);
        }
    }

    @Override
    public void markOffline(String username) {
        synchronized (onlineLock) {
            onlineUsers.remove(username);
        }
    }

    @Override
    public int getOnlineCount() {
        synchronized (onlineLock) {
            return onlineUsers.size();
        }
    }

    @Override
    public String getOnlineStatusMessage() {
        synchronized (onlineLock) {
            int count = onlineUsers.size();

            if (count == 1) {
                return "SERVER: There is currently 1 client online.";
            } else {
                return "SERVER: There are currently " + count + " clients online.";
            }
        }
    }
}
