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

    // Thread-safe: reads size inside the same lock used for add/remove
    public static int getOnlineCount() {
        synchronized (clients) {
            return clients.size();
        }
    }

    // Thread-safe: returns comma-separated list of online usernames
    public static String getOnlineUsernames() {
        synchronized (activeUsernames) {
            if (activeUsernames.isEmpty()) {
                return "none";
            }
            return String.join(", ", activeUsernames.keySet());
        }
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
      
        synchronized (activeUsernames) {
            return activeUsernames.containsKey(username);
        }
        
    }

    // THREAD-SAFE: Atomically check and register username
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


//     public static boolean checkAndRegisterUsername(String username, ClientHandler handler) {
    
//     // lock has been removed - without synchronisation multiple threads can run the method and access hashmap

//     //check is username exists in map
//     if (activeUsernames.containsKey(username)) {
//         return false;
//     }

    
//     // Time window for another thread
//     try { 
//         System.out.println("[DEBUG] " + username + " is checking the map...");
//         Thread.sleep(5000); 
//     } catch (InterruptedException e) {}

//     //  Both threads write to the map
//     activeUsernames.put(username, handler);
//     System.out.println("[SERVER] Successfully registered: " + username);
//     return true;

    
// }

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

    // Get a client handler by username (for P2P signalling)
    public static ClientHandler getClientByUsername(String username) {
        synchronized (activeUsernames) {
            return activeUsernames.get(username);
        }
    }


    public static void renameRoom( String newName, String username) {

        //change group name (thread safe version)
       synchronized (roomLock) {
            renameCount++;
            roomName = newName;
            renameHistory.add(newName);
        }


        //unsafe thread version

       /* int temp = renameCount;      //read
        Thread.yield();              //encourage thread interleaving
        renameCount = temp + 1;      //write (lost updates possible)

        roomName = newName;
        renameHistory.add(newName);


        */
    }

    //comment out synchronized in both below to show unsafe reads
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
