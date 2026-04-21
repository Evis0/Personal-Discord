package main.concurrency;

import java.util.HashSet;
import java.util.Set;

public class UnsafeOnlineStatusManager implements OnlineStatusManager {

    private final Set<String> onlineUsers = new HashSet<>();

    @Override
    public void markOnline(String username) {
        onlineUsers.add(username);
    }

    @Override
    public void markOffline(String username) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        onlineUsers.remove(username);
    }

    @Override
    public int getOnlineCount() {
        return onlineUsers.size();
    }

    @Override
    public String getOnlineStatusMessage() {
        int count = onlineUsers.size();

        if (count == 1) {
            return "SERVER: There is currently 1 client online.";
        } else {
            return "SERVER: There are currently " + count + " clients online.";
        }
    }
}
