package main;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServerMain {

    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, String> userStatuses = new HashMap<>();
    private static UsernameRegistry usernameRegistry = new SafeUsernameRegistry(); // changed from private static final ConcurrentHashMap<String, ClientHandler> activeUsernames = new ConcurrentHashMap<>();
    // private static UsernameRegistry usernameRegistry = new UnsafeUsernameRegistry();
    private static final Object fileLock = new Object();
    private static final Lock clientsMutex = new ReentrantLock();
    private static RoomManager roomManager = new SafeRoomManager();
    // private static RoomManager roomManager = new UnsafeRoomManager();

    
    // private static final Object roomLock = new Object();
    // private static String roomName = "Main";

    // private static int renameCount = 0;
    // private static final List<String> renameHistory = new ArrayList<>();

    public static void main(String[] args) {
        

        if (args.length != 1) {
            System.out.println("Usage: java ServerMain <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server listening on port " + port);
            System.out.println("Waiting for clients to connect...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: "
                        + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                // Don't add to clients list yet - wait until they successfully register
                new Thread(handler).start();

            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {

        // safe file write
        synchronized (fileLock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("chatlog.txt", true))) {

                writer.write(message);
                writer.newLine();

            } catch (IOException e) {
                System.out.println("[SERVER] Error writing to chat log: " + e.getMessage());
            }
        }

        // send to clients
        synchronized (clients) {
            System.out.println("[SERVER] Broadcasting: " + message);
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
    }

    public static void addClient(ClientHandler client) {
        synchronized (clients) {
            clients.add(client);
        }
    }

    // Thread-safe: reads size using Mutex instead of synchronized
    public static int getOnlineCount() {
        clientsMutex.lock();
        try {
            return clients.size();
        } finally {
            clientsMutex.unlock();
        }
    }

    // Thread-safe: returns comma-separated list of online usernames
    public static String getOnlineUsernames() {
    return usernameRegistry.getOnlineUsernames();
}

    // new Thread(() -> unsafeAddClient(handler)).start(); // UNSAFE demo

     /*

    // UNSAFE demonstration
    private static int clientCount = 0;

    public static void unsafeAddClient(ClientHandler h) {
        int temp = clientCount;       // Step 1: Read
        // Simulate delay between read and write so another thread can interfere
        try { Thread.sleep(7000); } catch (InterruptedException e) {}
        clientCount = temp + 1;       // Step 2: Write back (may overwrite another thread's update)
    }

    public static int getOnlineCount() {
        return clientCount;           // No lock - reads potentially stale value
    }

      */


    public static void updateStatus(String username, String status) {
    synchronized (userStatuses) {
        userStatuses.put(username, status);
        }
    }

    public static String getStatus(String username) {
    synchronized (userStatuses) {
        return userStatuses.getOrDefault(username, "No status set.");
        }
    }

    
    public static boolean isUsernameTaken(String username) {
    return usernameRegistry.getClient(username) != null;
}

    // THREAD-SAFE (no explicit synchronized): atomically check and register username
    public static boolean checkAndRegisterUsername(String username, ClientHandler handler) {
    boolean success = usernameRegistry.register(username, handler);
    if (success) {
        System.out.println("[SERVER] Username registered: '" + username + "' | Total active users: " + usernameRegistry.size());
    }
    return success;
}

    

    public static void unregisterUsername(String username) {
    usernameRegistry.unregister(username);
    System.out.println("[SERVER] Username unregistered: '" + username + "' | Total active users: " + usernameRegistry.size());
}

    public static ClientHandler getClientByUsername(String username) {
    return usernameRegistry.getClient(username);
}


    public static void renameRoom(String newName, String username) {
    roomManager.renameRoom(newName, username);
}


       
    

    public static String getRoomName() {
    return roomManager.getRoomName();
}

    public static String getRoomStats() {
    return roomManager.getRoomStats();
}
}
