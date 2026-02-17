package stage1;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ChatSession {
    private Socket socket;
    private Scanner userInput;

    public ChatSession(Socket socket) {
        this.socket = socket;
        this.userInput = new Scanner(System.in);
    }

    public void start() {
        try {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            // sending thread
            Thread sender = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        String message = userInput.nextLine();
                        out.write((message + "\n").getBytes());
                        out.flush();

                        if (message.equals("exit")) {
                            socket.close();
                            break;
                        }
                    }
                } catch (Exception e) {
                    if (!socket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            });

            sender.start();

            // recieving thread
            Thread reciever = new Thread(() -> {
                try (Scanner socketScanner = new Scanner(in)) {
                    while (socketScanner.hasNextLine()) {
                        String recieved = socketScanner.nextLine();
                        System.out.println("Recieved: " + recieved);
                    }
                } catch (Exception e) {
                    if (!socket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            });

            reciever.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}