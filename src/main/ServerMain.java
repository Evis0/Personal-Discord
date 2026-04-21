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
import main.concurrency.ChatLogger;
import main.concurrency.OnlineStatusManager;
import main.concurrency.SafeOnlineStatusManager;
import main.concurrency.SafeChatLogger;
import main.concurrency.UnsafeOnlineStatusManager;
import main.concurrency.UnsafeChatLogger;


import main.concurrency.*;

public class ServerMain {

    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, String> userStatuses = new HashMap<>();

    // username -> handler

    private static final ConcurrentHashMap<String, ClientHandler> activeUsernames = new ConcurrentHashMap<>();

    private static final ReentrantLock fileLock = new ReentrantLock();
    private static final Lock clientsMutex = new ReentrantLock();
    private static final Lock usernamesMutex = new ReentrantLock();

    private static RoomManager roomManager = new SafeRoomManager();
   // private static RoomManager roomManager = new UnsafeRoomManager();

    private static ChatLogger chatLogger = new SafeChatLogger();
  // private static ChatLogger chatLogger = new UnsafeChatLogger();

   // static OnlineStatusManager onlineStatusManager = new SafeOnlineStatusManager();
    private static OnlineStatusManager onlineStatusManager = new UnsafeOnlineStatusManager();



    public static String getOnlineStatusMessage() {
        return onlineStatusManager.getOnlineStatusMessage();
    }

    public static String getChatLoggerStats() {
        return chatLogger.getLogStats();
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ServerMain <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("chatlog.txt", false))) {

        } catch (IOException e) {
            System.out.println("[SERVER] Warning: could not clear chatlog.txt: " + e.getMessage());
            
        }

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
        if (!message.startsWith("SERVER:")) {
            chatLogger.logMessage(message);
            System.out.println("[SERVER] " + chatLogger.getLogStats());
        }


        // write to log

//        fileLock.lock();
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter("chatlog.txt", true))) {
//            writer.write(message);
//            writer.newLine();
//        } catch (IOException e) {
//            System.out.println("[SERVER] Error writing to chat log: " + e.getMessage());
//        } finally {
//            fileLock.unlock();
//        }

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

        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("chatlog.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                client.sendMessage(line);
            }

        } catch (java.io.FileNotFoundException e) {
        } catch (IOException e) {
            System.out.println("[SERVER] Error reading chat log: " + e.getMessage());
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

    public static boolean checkAndRegisterUsername(String username, ClientHandler handler) { // comment for unsafe
        ClientHandler existing = activeUsernames.putIfAbsent(username, handler);
        if (existing != null) {
            return false;
        }
        System.out.println("[SERVER] Username registered: '" + username + "' | Total active users: " + activeUsernames.size());
        return true;
    }

//      public static boolean checkAndRegisterUsername(String username, ClientHandler handler) { //uncomment for unsafe username - This implementation is not thread-safe because the check (containsKey) and the update (put) are performed as separate operations.
//        if (activeUsernames.containsKey(username)) {
//            return false;
//        }
//
//        try {
//            Thread.sleep(10);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//
//        activeUsernames.put(username, handler);
//
//       System.out.println("[SERVER] Username registered: '" + username + "' | Total active users: " + activeUsernames.size());
//        return true;
//
//    }






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
        roomManager.renameRoom(newName, username);
    }


    public static String getRoomName() {
        return roomManager.getRoomName();
    }

    public static String getRoomStats() {
        return roomManager.getRoomStats();
    }
}
