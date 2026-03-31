package main;

import java.util.ArrayList;
import java.util.List;

public class SafeRoomManager implements RoomManager {  // Safe because all shared room state is updated inside one synchronized critical section.
 
    private final Object roomLock = new Object();
    private String roomName = "Main";
    private int renameCount = 0;
    private final List<String> renameHistory = new ArrayList<>();

    @Override
    public void renameRoom(String newName, String username) {
        synchronized (roomLock) {
            renameCount++;
            roomName = newName;
            renameHistory.add(newName);
        }
    }

    @Override
    public String getRoomName() {
        synchronized (roomLock) {
            return roomName;
        }
    }

    @Override
    public String getRoomStats() {
        synchronized (roomLock) {
            return "roomName =" + roomName +
                    " renameCount =" + renameCount +
                    " historySize =" + renameHistory.size();
        }
    }
}