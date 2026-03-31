package main;

public interface RoomManager {  
    void renameRoom(String newName, String username);
    String getRoomName();
    String getRoomStats();
}