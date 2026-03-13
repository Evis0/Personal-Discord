package main.FileTransfer;

import java.io.*;

public class FileSender {

    // sends a file to the server over the socket's output stream
    // filename length (int) + filename bytes + file size (long) + file bytes
    public static void sendFile(String filePath, DataOutputStream dataOut) throws IOException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found: " + filePath);
        }

        byte[] fileData = new FileInputStream(file).readAllBytes();
        String fileName = file.getName();
        byte[] nameBytes = fileName.getBytes("UTF-8");

        // write: name length, name, file size, file data
        dataOut.writeInt(nameBytes.length);
        dataOut.write(nameBytes);
        dataOut.writeLong(fileData.length);
        dataOut.write(fileData);
        dataOut.flush();

        System.out.println("[CLIENT] Sent file: " + fileName + " (" + fileData.length + " bytes)");
    }
}