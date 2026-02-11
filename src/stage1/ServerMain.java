package stage1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static void main(String[] args) {

        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server listening on port " + port);
            System.out.println("Waiting for client...");

            Socket clientSocket = serverSocket.accept();

            System.out.println("Client connected from: "
                    + clientSocket.getRemoteSocketAddress());

            // Keep the socket open for messaging later
            // For now just hold it open
            Thread.sleep(600000);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
