package main;

import java.util.HashMap;
import java.util.Map;

public class UnsafeUsernameRegistry implements UsernameRegistry { // Unsafe because registration uses a non-atomic check-then-put sequence, allowing duplicate usernames under concurrency,  multiple threads can pass the check before the username is inserted.
                                                                    

    private final Map<String, ClientHandler> users = new HashMap<>();
    
    @Override
    public boolean register(String username, ClientHandler handler) {
        if (users.containsKey(username)) {
            return false;
        }

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        users.put(username, handler);
        return true;
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