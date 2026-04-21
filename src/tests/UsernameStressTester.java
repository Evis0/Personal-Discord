package tests;

import java.io.*;
import java.net.Socket;

public class UsernameStressTester {
    public static void main(String[] args) { //safe when 1 user connects, unsafe when all users connect.
        String host = "localhost";
        int port = 8080;
        String sharedUsername = "TestUser";

        // launch 10 threads at the same time
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try (Socket socket = new Socket(host, port);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    in.readLine();

                    out.println(sharedUsername);

                    String line;
                    while ((line = in.readLine()) != null) {

                        if (line.equals("SERVER: Welcome " + sharedUsername + "! You are now connected.")) {
                            System.out.println("[Thread " + Thread.currentThread().getId() + "] CONNECTED SUCCESSFULLY");
                            break;
                        }

                        if (line.equals("SERVER: Username '" + sharedUsername + "' is already taken. Disconnecting.")) {
                            System.out.println("[Thread " + Thread.currentThread().getId() + "] FAILED (USERNAME TAKEN)");
                            break;
                        }


                    }

                    Thread.sleep(5000);

                } catch (Exception e) {
                    System.out.println("Error in tester thread: " + e.getMessage());
                }
            }).start();
        }
    }
}
