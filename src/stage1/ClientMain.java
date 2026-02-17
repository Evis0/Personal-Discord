package stage1;

import java.io.IOException;
import java.net.Socket;

public class ClientMain {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage: java ClientMain <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            Socket socket = new Socket(host, port);
            System.out.println("Connected to server at " + host + ":" + port);

            // start the chat session
            ChatSession session = new ChatSession(socket);
            session.start();

        } catch (Exception e) {
            System.out.println("Connection failed. Is the server running?");
            e.printStackTrace();

        }
    }
}
