package tests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class OnlineStatusStressTester {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8082;

        int stableClients = 5;
        int leavingClients = 10;
        int onlineChecks = 20;

        CountDownLatch stableReady = new CountDownLatch(stableClients);
        CountDownLatch leaversReady = new CountDownLatch(leavingClients);
        CountDownLatch startLeaving = new CountDownLatch(1);
        CountDownLatch leaversDone = new CountDownLatch(leavingClients);

        Socket[] stableSockets = new Socket[stableClients];

        for (int i = 0; i < stableClients; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    stableSockets[id] = connectClient(host, port, "stableUser" + id);
                } catch (Exception e) {
                    System.out.println("Stable client error: " + e.getMessage());
                } finally {
                    stableReady.countDown();
                }
            }).start();
        }

        stableReady.await();

        for (int i = 0; i < leavingClients; i++) {
            final int id = i;
            new Thread(() -> {
                try (Socket socket = connectClient(host, port, "leaverUser" + id)) {
                    leaversReady.countDown();
                    startLeaving.await();
                    Thread.sleep(100 + (id * 80));
                } catch (Exception e) {
                    System.out.println("Leaver client error: " + e.getMessage());
                } finally {
                    leaversDone.countDown();
                }
            }).start();
        }

        leaversReady.await();

        int successfulResponses = 0;
        int inconsistentResponses = 0;

        try (Socket observer = new Socket(host, port);
             PrintWriter out = new PrintWriter(observer.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(observer.getInputStream()))) {

            observer.setSoTimeout(3000);

            in.readLine();
            out.println("onlineObserver");
            waitUntilReady(in);

            startLeaving.countDown();

            for (int i = 0; i < onlineChecks; i++) {
                out.println("/online");

                String response = readOnlineResponse(in);
                if (response != null) {
                    successfulResponses++;

                    int count = parseCount(response);
                    int listedUsers = parseListedUsers(response);

                    System.out.println(response);

                    if (count != listedUsers && !(count == 0 && listedUsers == 0)) {
                        inconsistentResponses++;
                    }
                }

                Thread.sleep(75);
            }
        }

        leaversDone.await();

        for (Socket socket : stableSockets) {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }
        }

        System.out.println();
        System.out.println("Expected online responses: " + onlineChecks);
        System.out.println("Actual online responses: " + successfulResponses);
        System.out.println("Inconsistent responses: " + inconsistentResponses);

        if (successfulResponses == onlineChecks && inconsistentResponses == 0) {
            System.out.println("Online feature appears thread-safe for this run.");
        } else {
            System.out.println("Online feature appears inconsistent under concurrency.");
        }
    }

    private static Socket connectClient(String host, int port, String username) throws Exception {
        Socket socket = new Socket(host, port);
        socket.setSoTimeout(3000);

        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        in.readLine(); // username prompt
        out.println(username);
        waitUntilReady(in);

        return socket;
    }

    private static void waitUntilReady(BufferedReader in) throws Exception {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("SERVER: Commands:") || line.startsWith("SERVER: Welcome")) {
                break;
            }
        }
    }

    private static String readOnlineResponse(BufferedReader in) throws Exception {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("SERVER: There is currently")
                    || line.startsWith("SERVER: There are currently")) {
                return line;
            }
        }
        return null;
    }

    private static int parseCount(String response) {
        String text = response
                .replace("SERVER: There is currently ", "")
                .replace("SERVER: There are currently ", "");

        int firstSpace = text.indexOf(' ');
        return firstSpace == -1 ? 0 : Integer.parseInt(text.substring(0, firstSpace));
    }

    private static int parseListedUsers(String response) {
        int colonIndex = response.indexOf(": ", response.indexOf("online"));
        if (colonIndex == -1) {
            return 0;
        }

        String usernames = response.substring(colonIndex + 2).trim();
        if (usernames.isEmpty() || usernames.equals("none")) {
            return 0;
        }

        return usernames.split(",\\s*").length;
    }
}