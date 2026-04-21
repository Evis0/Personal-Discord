package tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class ChatLoggingStressTester {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8080;
        int threadCount = 20;
        int messagesPerThread = 5;
        String runId = "LOGTEST-" + System.currentTimeMillis();

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

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

                    ready.countDown();
                    start.await();

                    for (int j = 0; j < messagesPerThread; j++) {
                        out.println(runId + "-T" + id + "-M" + j);
                    }

                    Thread.sleep(300);

                } catch (Exception e) {
                    System.out.println("Tester thread error: " + e.getMessage());
                } finally {
                    done.countDown();
                }
            }).start();
        }

        ready.await();
        start.countDown();
        done.await();

        Thread.sleep(1500);

        int expectedMessages = threadCount * messagesPerThread;
        int actualMessages = countLogLines(runId);

        System.out.println("Expected logged messages: " + expectedMessages);
        System.out.println("Actual logged messages: " + actualMessages);
        System.out.println("Logger stats: " + main.ServerMain.getChatLoggerStats());

        if (actualMessages == expectedMessages) {
            System.out.println("Chat logging appears thread-safe for this run.");
        } else {
            System.out.println("Chat logging may have race/loss issues. Missing log entries detected.");
        }
    }

    private static int countLogLines(String runId) {
        int count = 0;
        File logFile = new File("chatlog.txt");

        if (!logFile.exists()) {
            System.out.println("chatlog.txt not found at: " + logFile.getAbsolutePath());
            return 0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(": " + runId + "-T")) {
                    count++;
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading chat log: " + e.getMessage());
        }

        return count;

    }
}
