package tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatLoggingStressTester {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8082;

        int threadCount = 50;
        int messagesPerThread = 20;

        String runId = "LOGTEST-" + System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int id = i;

            new Thread(() -> {
                try (Socket socket = new Socket(host, port);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    in.readLine(); // username prompt
                    out.println("logger" + id + "_" + runId);

                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("SERVER: Commands:")) {
                            break;
                        }
                    }

                    for (int j = 0; j < messagesPerThread; j++) {
                        out.println(runId + "-T" + id + "-M" + j);
                    }

                    Thread.sleep(5000);

                } catch (Exception e) {
                    System.out.println("Tester thread error: " + e.getMessage());
                }
            }).start();
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int expectedMessages = threadCount * messagesPerThread;
        int actualLoggedCount = requestLogStats(host, port, runId);

        System.out.println("Expected logged messages: " + expectedMessages);
        System.out.println("Actual logged messages: " + actualLoggedCount);
    }

    private static int requestLogStats(String host, int port, String runId) {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            in.readLine(); // username prompt
            out.println("statsUser_" + runId);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("SERVER: Commands:")) {
                    break;
                }
            }

            out.println("/logstats");

            while ((line = in.readLine()) != null) {
                if (line.startsWith("LOG_STATS|")) {
                    return extractLoggedCount(line);
                }
            }

        } catch (Exception e) {
            System.out.println("Error requesting log stats: " + e.getMessage());
        }

        return -1;
    }

    private static int extractLoggedCount(String statsLine) {
        try {
            return Integer.parseInt(statsLine.replace("LOG_STATS|loggedCount =", "").trim());
        } catch (Exception e) {
            return -1;
        }
    }
}