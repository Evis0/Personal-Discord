package stage1;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ServerMain {

    private static final List<ClientHandler> clients = new ArrayList<>();

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
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public static void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private OutputStream out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream in = socket.getInputStream();
                out = socket.getOutputStream();

                Scanner scanner = new Scanner(in, StandardCharsets.UTF_8);

                while (scanner.hasNextLine()) {
                    String message = scanner.nextLine();
                    System.out.println("Received from " + socket.getRemoteSocketAddress() + ": " + message);

                    if (message.equalsIgnoreCase("exit")) {
                        break;
                    }

                    // Broadcast to all other clients
                    broadcast(message, this);
                }

                scanner.close();
                socket.close();
                removeClient(this);
                System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());

            } catch (Exception e) {
                if (!socket.isClosed()) {
                    e.printStackTrace();
                }
                removeClient(this);
            }
        }

        public void sendMessage(String message) {
            try {
                out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
