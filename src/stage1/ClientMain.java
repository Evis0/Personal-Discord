package stage1;

import java.io.IOException;
import java.net.Socket;

public class ClientMain {

    public static void main(String[] args) {

        String host = "localhost";
        int port = 5000;

        try {
            Socket socket = new Socket(host, port);
            System.out.println("Connected to server at " + host + ":" + port);

            // Keep connection open so server doesn't exit
            Thread.sleep(600000);

            socket.close();

        } catch (IOException e) {
            System.out.println("Connection failed. Is the server running?");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
