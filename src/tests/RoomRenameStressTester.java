package tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;




public class RoomRenameStressTester {                       

    // If the implementation is thread-safe, renameCount and historySize
    // should always be equal in the final stats output.
    // In the unsafe version, concurrent renames can cause lost updates,
    // so renameCount and/or historySize may be lower due to race conditions



    public static void main(String[] args) throws Exception { 
        String host = "localhost";
        int port = 8082;
        int threadCount = 20;

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
                    out.println("user" + id);

                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("SERVER: Commands:")) {
                            break;
                        }
                    }

                    ready.countDown();
                    start.await();

                    out.println("/rename room" + id);

                    Thread.sleep(500);
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
        }
    }
}