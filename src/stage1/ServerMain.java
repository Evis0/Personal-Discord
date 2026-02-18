package stage1;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ServerMain {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java ServerMain <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server listening on port " + port);
            System.out.println("Waiting for first client...");

            Socket client1 = serverSocket.accept();
            System.out.println("Client 1 connected from: " + client1.getRemoteSocketAddress());

            System.out.println("Waiting for second client...");
            Socket client2 = serverSocket.accept();
            System.out.println("Client 2 connected from: " + client2.getRemoteSocketAddress());

            System.out.println("Both clients connected. Relaying messages...");

            // Relay messages from client1 to client2
            Thread relay1to2 = new Thread(() -> relayMessages(client1, client2, "Client1->Client2"));

            // Relay messages from client2 to client1
            Thread relay2to1 = new Thread(() -> relayMessages(client2, client1, "Client2->Client1"));

            relay1to2.start();
            relay2to1.start();

            // Wait for both relay threads to finish
            relay1to2.join();
            relay2to1.join();

            System.out.println("Chat session ended.");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void relayMessages(Socket from, Socket to, String label) {
        try {
            InputStream in = from.getInputStream();
            OutputStream out = to.getOutputStream();

            Scanner scanner = new Scanner(in, StandardCharsets.UTF_8);

            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();
                System.out.println("[" + label + "] " + message);

                // Forward the message to the other client
                out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }
            }

            scanner.close();
            from.close();
            to.close();

        } catch (Exception e) {
            if (!from.isClosed()) {
                e.printStackTrace();
            }
        }
    }
}
