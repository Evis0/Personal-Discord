package stage1;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ClientHandler implements Runnable {
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

                
            if (message.startsWith("/status ")) {
                
                String newStatus = message.substring(8);
               
                ServerMain.updateStatus(socket.getRemoteSocketAddress().toString(), newStatus);
                sendMessage("SERVER: Your status is now: " + newStatus);
                continue; 
            } 
            
            if (message.equalsIgnoreCase("exit")) {
                break;
            }


                System.out.println("Received from " + socket.getRemoteSocketAddress() + ": " + message);

                if (message.equalsIgnoreCase("exit")) {
                    break;
                }

                // Broadcast to all other clients
                ServerMain.broadcast(message, this);
            }

            scanner.close();
            socket.close();
            ServerMain.removeClient(this);
            System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());

        } catch (Exception e) {
            if (!socket.isClosed()) {
                e.printStackTrace();
            }
            ServerMain.removeClient(this);
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

