package main.FileTransfer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FileTransferManager {
    private static final Map<String, FileTransferRequest> storedFiles = new HashMap<>();

    // store a file and return its generated id
    public static String storeFile(String fileName, byte[] fileData, String senderUsername) {
        String fileId = UUID.randomUUID().toString();
        FileTransferRequest request = new FileTransferRequest(fileId, fileName, fileData.length, senderUsername, fileData);
        synchronized (storedFiles) {
            storedFiles.put(fileId, request);
        }
        System.out.println("[SERVER] File stored: " + fileName + " with ID: " + fileId);
        return fileId;
    }
}