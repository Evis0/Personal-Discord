package main;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.BufferedReader;
import java.io.FileReader;

public class ServerMain {

    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, String> userStatuses = new HashMap<>();

    // username -> handler
    private static final ConcurrentHashMap<String, ClientHandler> activeUsernames = new ConcurrentHashMap<>();

    private static final ReentrantLock fileLock = new ReentrantLock();
    private static final Lock clientsMutex = new ReentrantLock();
    private static final Lock usernamesMutex = new ReentrantLock();

    private static final Object roomLock = new Object();
    private static String roomName = "Main";

    private static int renameCount = 0;
    private static final List<String> renameHistory = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ServerMain <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            System.out.println("Waiting for clients...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                // handler registers itself before we add it to the list
                new Thread(handler).start();
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        // write to log
        fileLock.lock();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("../chatlog.txt", true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("[SERVER] Error writing to chat log: " + e.getMessage());
        } finally {
            fileLock.unlock();
        }

        synchronized (clients) {
            System.out.println("[SERVER] Broadcasting: " + message);
            for (ClientHandler client : clients) {
                client.sendMessage(message);
            }
        }
    }

    // Format: ONLINE_USERS|user1,user2,user3
    public static void broadcastOnlineUsers() {
        String usernames;
        usernamesMutex.lock();
        try {
            usernames = String.join(",", activeUsernames.keySet());
        } finally {
            usernamesMutex.unlock();
        }

        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage("ONLINE_USERS|" + usernames);
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
        broadcastOnlineUsers();
    }

    public static void addClient(ClientHandler client) {
        synchronized (clients) {
            clients.add(client);
        }
        broadcastOnlineUsers();
    }

    public static void sendChatHistory(ClientHandler client) {
        fileLock.lock();
        try (BufferedReader reader = new BufferedReader(new FileReader("chatlog.txt"))) {
            client.sendMessage("SERVER: Chat History");
            String line;
            while ((line = reader.readLine()) != null) {
                client.sendMessage(line);
            }
            client.sendMessage("SERVER: End of History");
        } catch (IOException e) {
            client.sendMessage("SERVER: No chat history yet.");
        } finally {
            fileLock.unlock();
        }
    }

    public static int getOnlineCount() {
        clientsMutex.lock();
        try {
            return clients.size();
        } finally {
            clientsMutex.unlock();
        }
    }

    public static String getOnlineUsernames() {
        usernamesMutex.lock();
        try {
            if (activeUsernames.isEmpty()) {
                return "none";
            }
            return String.join(", ", activeUsernames.keySet());
        } finally {
            usernamesMutex.unlock();
        }
    }

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
        return activeUsernames.containsKey(username);
    }

    public static boolean checkAndRegisterUsername(String username, ClientHandler handler) {
        ClientHandler existing = activeUsernames.putIfAbsent(username, handler);
        if (existing != null) {
            return false;
        }
        System.out.println("[SERVER] Username registered: '" + username + "' | Total active users: " + activeUsernames.size());
        return true;
    }

    public static void registerUsername(String username, ClientHandler handler) {
        activeUsernames.put(username, handler);
        System.out.println("[SERVER] Username registered: '" + username + "' | Total active users: " + activeUsernames.size());
    }

    public static void unregisterUsername(String username) {
        activeUsernames.remove(username);
        System.out.println("[SERVER] Username unregistered: '" + username + "' | Total active users: " + activeUsernames.size());
        broadcastOnlineUsers();
    }

    public static ClientHandler getClientByUsername(String username) {
        return activeUsernames.get(username);
    }

    public static void renameRoom(String newName, String username) {
        synchronized (roomLock) {
            renameCount++;
            roomName = newName;
            renameHistory.add(newName);
        }
    }

    // comment out synchronized in both below to show unsafe reads
    public static String getRoomName() {
        synchronized (roomLock) {
            return roomName;
        }
    }

    public static String getRoomStats() {
        synchronized (roomLock) {
            return "roomName =" + roomName +
                    " renameCount =" + renameCount +
                    " historySize =" + renameHistory.size();
        }
    }
}
