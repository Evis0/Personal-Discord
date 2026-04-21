package tests;

import java.io.*;
import java.net.Socket;

public class OnlineStatusStressTester {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8082;

        // stable users stay connected
        for (int i = 0; i < 5; i++) {
            final int id = i;
            new Thread(() -> {
                try (Socket s = new Socket(host, port);
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

                    in.readLine(); // username prompt
                    out.println("StableUser" + id);

                    // Keep socket open
                    while (!s.isClosed() && s.isConnected()) {
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    System.out.println("Stable client error: " + e.getMessage());
                }
            }).start();
        }

        // leaving users disconnect after staggered delays
        for (int i = 0; i < 10; i++) {
            final int id = i;
            new Thread(() -> {
                try (Socket s = new Socket(host, port);
                     BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                     PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

                    in.readLine(); // username prompt
                    out.println("LeaverUser" + id);

                    // Spread disconnects over a wider window
                    Thread.sleep(1000 + (id * 300));
                } catch (Exception e) {
                    System.out.println("Leaver client error: " + e.getMessage());
                }
            }).start();
        }

        // let everyone connect but dont wait until leavers are already gone
        Thread.sleep(300);

        // observer checks /online over and over while leavers disconnect
        try (Socket s = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

            in.readLine(); // username prompt
            out.println("Observer");

            Thread.sleep(100);

            for (int i = 0; i < 20; i++) {
                out.println("/online");

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.contains("currently")) {
                        System.out.println("Check " + i + ": " + line);
                        break;
                    }
                }

                Thread.sleep(200);
            }
        }
    }
}