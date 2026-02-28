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

public class ServerMain {

    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, String> userStatuses = new HashMap<>();
    private static final Map<String, ClientHandler> activeUsernames = new HashMap<>();
    private static final Object fileLock = new Object();

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

    // WITHOUT THREAD SAFETY (for demonstration purposes)
    // This method has a race condition - two threads could check at the same time
    // and both see the username as available before either registers it
    public static boolean isUsernameTaken(String username) {
        // UNSAFE VERSION: Comment out the synchronized block to demonstrate the race condition
        synchronized (activeUsernames) {
            return activeUsernames.containsKey(username);
        }
        // return activeUsernames.containsKey(username); // UNSAFE - uncomment to test race condition
    }

    // THREAD-SAFE: Atomically check and register username
    // This prevents the race condition by doing both operations in one synchronized block
    public static boolean checkAndRegisterUsername(String username, ClientHandler handler) {
        synchronized (activeUsernames) {
            if (activeUsernames.containsKey(username)) {
                return false; // Username already taken
            }
            activeUsernames.put(username, handler);
            System.out.println("[SERVER] Username registered: '" + username + "' | Total active users: " + activeUsernames.size());
            return true; // Successfully registered
        }
    }

    // Register a username (THREAD SAFE)
    public static void registerUsername(String username, ClientHandler handler) {
        synchronized (activeUsernames) {
            activeUsernames.put(username, handler);
            System.out.println("[SERVER] Username registered: '" + username + "' | Total active users: " + activeUsernames.size());
        }
    }

    // Unregister a username when client disconnects
    public static void unregisterUsername(String username) {
        synchronized (activeUsernames) {
            activeUsernames.remove(username);
            System.out.println("[SERVER] Username unregistered: '" + username + "' | Total active users: " + activeUsernames.size());
        }
    }


}
