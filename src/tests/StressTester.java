package tests;

import java.io.*;
import java.net.Socket;

public class StressTester {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 1234; 
        String sharedUsername = "CollisionUser";

        // launch 10 threads at the same time
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try (Socket socket = new Socket(host, port);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    
                    in.readLine(); 
                    
                    out.println(sharedUsername);
                    
                    String response = in.readLine();
                    System.out.println("[Thread " + Thread.currentThread().getId() + "] Server says: " + response);
                    
                    Thread.sleep(5000);

                } catch (Exception e) {
                    System.out.println("Error in tester thread: " + e.getMessage());
                }
            }).start();
        }
    }
}