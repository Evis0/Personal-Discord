package main.concurrency;

import main.ServerMain;

public class SafeOnlineStatusManager implements OnlineStatusManager {

    @Override
    public String getOnlineStatusMessage() {
        int count = ServerMain.getOnlineCount();
        String usernames = ServerMain.getOnlineUsernames();

        if (count == 1) {
            return "SERVER: There is currently 1 client online: " + usernames;
        } else {
            return "SERVER: There are currently " + count + " clients online: " + usernames;
        }
    }
}