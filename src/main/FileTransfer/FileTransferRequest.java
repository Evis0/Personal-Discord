package main.FileTransfer;

public class FileTransferRequest {
    private final String fileId;
    private final String fileName;
    private final long fileSize;
    private final String senderUsername;
    private final byte[] fileData;

    public FileTransferRequest(String fileId, String fileName, long fileSize, String senderUsername, byte[] fileData) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.senderUsername = senderUsername;
        this.fileData = fileData;
    }
}