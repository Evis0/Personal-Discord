package main;

public interface UsernameRegistry {
    boolean register(String username, ClientHandler handler);
    void unregister(String username);
    ClientHandler getClient(String username);
    String getOnlineUsernames();
    int size();
}