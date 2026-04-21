package main.concurrency; //safe and unsafe username registry is now unused

import main.ClientHandler;

import java.util.concurrent.ConcurrentHashMap;

public class SafeUsernameRegistry implements UsernameRegistry {

    private final ConcurrentHashMap<String, ClientHandler> users = new ConcurrentHashMap<>(); // Use a concurrent map so username registration can be done with atomic operations (putIfAbsent)

    @Override
    public boolean register(String username, ClientHandler handler) {
        return users.putIfAbsent(username, handler) == null;
    }

    @Override
    public void unregister(String username) {
        users.remove(username);
    }

    @Override
    public ClientHandler getClient(String username) {
        return users.get(username);
    }

    @Override
    public String getOnlineUsernames() {
        if (users.isEmpty()) {
            return "none";
        }
        return String.join(", ", users.keySet());
    }

    @Override
    public int size() {
        return users.size();
    }
}