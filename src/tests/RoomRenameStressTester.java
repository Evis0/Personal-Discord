package tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class RoomRenameStressTester {


    public static void main(String[] args) {
        String host = "localhost";
        int port = 8082;
        int threadCount = 20; // number of rename requests


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

                    out.println("/rename room" + id); // each client attempts to rename the room

                    Thread.sleep(1000); // short delay to keep connection open

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
        //  separate client to request final room stats
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
            // Request room statistics
            out.println("/roomstats");

            // Print final stats

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