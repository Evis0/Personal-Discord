package stage1;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ChatSession {
    private final Socket socket;
    private final Scanner userInput;

    public ChatSession(Socket socket) {
        this.socket = socket;
        this.userInput = new Scanner(System.in, StandardCharsets.UTF_8);
    }

    public void start() {
        try {
            System.out.println("ChatSession.start() called!");
            System.out.flush();

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            //sending thread
            Thread sender = new Thread(() -> {
                try {
                    System.out.println("Ready to send messages. Type your message:");
                    System.out.flush();
                    while (!socket.isClosed()) {
                        String message = userInput.nextLine();

                        System.out.println("Sending: " + message);
                        out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
                        out.flush();

                        if (message.equalsIgnoreCase("exit")) {
                            socket.close();
                            break;
                        }
                    }
                } catch (Exception e) {
                    if (!socket.isClosed()) e.printStackTrace();
                }
            });

            //receiving thread
            Thread receiver = new Thread(() -> {
                try (Scanner socketScanner = new Scanner(in, StandardCharsets.UTF_8)) {
                    System.out.println("Listening for incoming messages...");
                    System.out.flush();
                    while (socketScanner.hasNextLine()) {
                        String received = socketScanner.nextLine();

                    }
                    System.out.println("Connection closed by peer.");
                } catch (Exception e) {
                    if (!socket.isClosed()) e.printStackTrace();
                }
            });

            sender.start();
            receiver.start();

            // Wait for threads to complete
            sender.join();
            receiver.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
