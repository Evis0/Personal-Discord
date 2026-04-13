package main.concurrency;

public interface RoomManager {  
    void renameRoom(String newName, String username);
    String getRoomName();
    String getRoomStats();
}