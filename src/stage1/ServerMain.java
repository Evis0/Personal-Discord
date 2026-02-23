package stage1;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerMain {

    private static final List<ClientHandler> clients = new ArrayList<>();
    private static final Map<String, String> userStatuses = new HashMap<>();
    
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
                synchronized (clients) {
                    clients.add(handler);
                }
                new Thread(handler).start();
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                // Send to all clients including sender for consistent display
                client.sendMessage(message);
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
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

    
}
