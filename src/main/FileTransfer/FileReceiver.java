package main.FileTransfer;

import java.io.*;

public class FileReceiver {

    private static final String DOWNLOAD_DIR = "Downloads";

    // called from clientmain after the namelength has already been read (to check for -1 error)
    // filename bytes + file size (long) + file bytes
    public static void receiveFile(DataInputStream dataIn, int nameLength) throws IOException {
        // read filename
        byte[] nameBytes = new byte[nameLength];
        dataIn.readFully(nameBytes);
        String fileName = new String(nameBytes, "UTF-8");

        // read file data
        long fileSize = dataIn.readLong();
        byte[] fileData = new byte[(int) fileSize];
        dataIn.readFully(fileData);

        // make sure downloads directory exists
        File downloadDir = new File(DOWNLOAD_DIR);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }

        // save file
        File outputFile = new File(downloadDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(fileData);
        }

        System.out.println("[CLIENT] File downloaded: " + outputFile.getAbsolutePath());
    }
}