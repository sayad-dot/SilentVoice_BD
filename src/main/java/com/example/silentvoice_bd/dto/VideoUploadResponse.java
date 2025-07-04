package com.example.silentvoice_bd.dto;



import java.time.LocalDateTime;
import java.util.UUID;

public class VideoUploadResponse {
    private UUID id;
    private String filename;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadTimestamp;
    private String message;

    public VideoUploadResponse(UUID id, String filename, String originalFilename,
                              String contentType, Long fileSize, LocalDateTime uploadTimestamp, String message) {
        this.id = id;
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadTimestamp = uploadTimestamp;
        this.message = message;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getUploadTimestamp() { return uploadTimestamp; }
    public void setUploadTimestamp(LocalDateTime uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
