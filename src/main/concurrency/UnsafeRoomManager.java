package main.concurrency;

import java.util.ArrayList;
import java.util.List;

public class UnsafeRoomManager implements RoomManager { // Unsafe because renameCount is updated with an unsynchronized read-modify-write, which can lose updates when multiple threads rename the room concurrently.

    private String roomName = "Main";
    private int renameCount = 0;
    private final List<String> renameHistory = new ArrayList<>();

    @Override
    public void renameRoom(String newName, String username) {
        int temp = renameCount;

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        renameCount = temp + 1;
        roomName = newName;
        renameHistory.add(newName);
    }

    @Override
    public String getRoomName() {
        return roomName;
    }

    @Override
    public String getRoomStats() {
        return "roomName =" + roomName +
                " renameCount =" + renameCount +
                " historySize =" + renameHistory.size();
    }
}