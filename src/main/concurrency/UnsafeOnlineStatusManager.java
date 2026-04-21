package main.concurrency;

import main.ServerMain;

public class UnsafeOnlineStatusManager implements OnlineStatusManager {

    @Override
    public String getOnlineStatusMessage() {
        int count = ServerMain.getOnlineCount();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String usernames = ServerMain.getOnlineUsernames();

        if (count == 1) {
            return "SERVER: There is currently 1 client online: " + usernames;
        } else {
            return "SERVER: There are currently " + count + " clients online: " + usernames;
        }
    }
}