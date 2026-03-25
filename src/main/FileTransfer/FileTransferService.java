package main.FileTransfer;

import java.io.*;

public class FileTransferService {

    // called when a client does /sendfile <filepath>
    // reads the file, stores it on the server, returns the id
    public static String handleUpload(String filePath, String senderUsername) throws IOException {
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found: " + filePath);
        }

        byte[] fileData;
        try (FileInputStream fis = new FileInputStream(file)) {
            fileData = fis.readAllBytes();
        }

        String fileId = FileTransferManager.storeFile(file.getName(), fileData, senderUsername);
        return fileId;
    }
}