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

    // called when a client does /downloadfile <id>
    // looks up the file and writes it back to the client w dataoutputstream
    public static void handleDownload(String fileId, DataOutputStream dataOut) throws IOException {
        FileTransferRequest request = FileTransferManager.getFile(fileId);

        if (request == null) {
            // file not found
            dataOut.writeInt(-1);
            dataOut.flush();
            return;
        }

        byte[] nameBytes = request.getFileName().getBytes("UTF-8");

        // write: name length, name, file size, file data
        dataOut.writeInt(nameBytes.length);
        dataOut.write(nameBytes);
        dataOut.writeLong(request.getFileData().length);
        dataOut.write(request.getFileData());
        dataOut.flush();

        System.out.println("[SERVER] Served file: " + request.getFileName() + " (ID: " + fileId + ")");
    }
}