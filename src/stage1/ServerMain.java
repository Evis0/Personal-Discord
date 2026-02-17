package stage1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("Usage: java ServerMain <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server listening on port " + port);
            System.out.println("Waiting for client...");

            Socket clientSocket = serverSocket.accept();

            System.out.println("Client connected from: "
                    + clientSocket.getRemoteSocketAddress());

            // start chat session instead of sleeping
            ChatSession session = new ChatSession(clientSocket);
            session.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
