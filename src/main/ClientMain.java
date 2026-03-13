package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import main.FileTransfer.FileReceiver;
import java.io.DataInputStream;

public class ClientMain {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ClientMain <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server at " + host + ":" + port);

            // Thread to receive messages from server
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        // check if server is gonna send a file
                        if (message.equals("SERVER_FILE_INCOMING")) {
                            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                            int nameLength = dataIn.readInt();
                            if (nameLength == -1) {
                                System.out.println("SERVER: File not found.");
                            } else {
                                FileReceiver.receiveFile(dataIn, nameLength);
                            }
                        } else {
                            System.out.println(message);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection to server lost.");
                }
            });
            receiveThread.start();

            // Main thread sends messages to server
            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();
                out.println(message);
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

