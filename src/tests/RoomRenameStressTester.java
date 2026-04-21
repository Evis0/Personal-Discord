package tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RoomRenameStressTester {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8082;
        int threadCount = 20;

        for (int i = 0; i < threadCount; i++) {
            final int id = i;

            new Thread(() -> {
                try (Socket socket = new Socket(host, port);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    in.readLine(); // username prompt
                    out.println("user" + id);

                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("SERVER: Commands:")) {
                            break;
                        }
                    }

                    out.println("/rename room" + id);

                    Thread.sleep(1000);

                } catch (Exception e) {
                    System.out.println("Tester thread error: " + e.getMessage());
                }
            }).start();
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            in.readLine(); // username prompt
            out.println("statsUser");

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("SERVER: Commands:")) {
                    break;
                }
            }

            out.println("/roomstats");

            while ((line = in.readLine()) != null) {
                if (line.startsWith("SERVER: Current room stats is:")) {
                    System.out.println("Final stats: " + line);
                    break;
                }
            }

        } catch (Exception e) {
            System.out.println("Error requesting room stats: " + e.getMessage());
        }
    }
}