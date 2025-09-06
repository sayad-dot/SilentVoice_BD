package com.example.silentvoice_bd.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.base.url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Store a file in the specified directory
     *
     * @param file The multipart file to store
     * @param subDirectory The subdirectory under uploads (e.g., "signs/videos")
     * @return The public URL to access the stored file
     */
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        try {
            // Clean the filename
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

            // Check if filename contains invalid characters
            if (originalFilename.contains("..")) {
                throw new IllegalArgumentException("Filename contains invalid path sequence: " + originalFilename);
            }

            // Generate unique filename to avoid conflicts
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(originalFilename, fileExtension);

            // Create the target directory
            Path targetLocation = createDirectoryPath(subDirectory);
            Path filePath = targetLocation.resolve(uniqueFilename);

            // Copy file to the target location
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Generate the public URL
            String publicUrl = generatePublicUrl(subDirectory, uniqueFilename);

            logger.info("✅ File stored successfully: {} -> {}", originalFilename, publicUrl);
            return publicUrl;

        } catch (IOException e) {
            logger.error("❌ Failed to store file: {}", e.getMessage());
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * Delete a file from storage
     *
     * @param fileUrl The file URL to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteFile(String fileUrl) {
        try {
            if (fileUrl == null || fileUrl.isEmpty()) {
                return false;
            }

            // Extract the relative path from the URL
            String relativePath = extractRelativePathFromUrl(fileUrl);
            if (relativePath == null) {
                return false;
            }

            Path filePath = Paths.get(uploadDir, relativePath);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                logger.info("✅ File deleted successfully: {}", fileUrl);
            } else {
                logger.warn("⚠️ File not found for deletion: {}", fileUrl);
            }

            return deleted;

        } catch (IOException e) {
            logger.error("❌ Failed to delete file: {} - {}", fileUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Check if a file exists in storage
     *
     * @param fileUrl The file URL to check
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String fileUrl) {
        try {
            String relativePath = extractRelativePathFromUrl(fileUrl);
            if (relativePath == null) {
                return false;
            }

            Path filePath = Paths.get(uploadDir, relativePath);
            return Files.exists(filePath);

        } catch (Exception e) {
            logger.error("❌ Error checking file existence: {} - {}", fileUrl, e.getMessage());
            return false;
        }
    }

    // Private helper methods
    private Path createDirectoryPath(String subDirectory) throws IOException {
        Path targetLocation = Paths.get(uploadDir).resolve(subDirectory);
        Files.createDirectories(targetLocation);
        return targetLocation;
    }

    private String generateUniqueFilename(String originalFilename, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String baseName = getBaseName(originalFilename);

        return String.format("%s_%s_%s%s", baseName, timestamp, uuid, extension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex);
    }

    private String getBaseName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "file";
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) {
            return filename;
        }

        return filename.substring(0, dotIndex);
    }

    private String generatePublicUrl(String subDirectory, String filename) {
        return String.format("%s/uploads/%s/%s", baseUrl, subDirectory, filename);
    }

    private String extractRelativePathFromUrl(String fileUrl) {
        try {
            // Extract path after "/uploads/"
            String uploadsMarker = "/uploads/";
            int index = fileUrl.indexOf(uploadsMarker);

            if (index != -1) {
                return fileUrl.substring(index + uploadsMarker.length());
            }

            return null;
        } catch (Exception e) {
            logger.error("❌ Failed to extract relative path from URL: {}", fileUrl);
            return null;
        }
    }

    /**
     * Get file size in bytes
     *
     * @param fileUrl The file URL
     * @return File size in bytes, or -1 if file doesn't exist
     */
    public long getFileSize(String fileUrl) {
        try {
            String relativePath = extractRelativePathFromUrl(fileUrl);
            if (relativePath == null) {
                return -1;
            }

            Path filePath = Paths.get(uploadDir, relativePath);
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }

            return -1;
        } catch (Exception e) {
            logger.error("❌ Error getting file size: {} - {}", fileUrl, e.getMessage());
            return -1;
        }
    }

    /**
     * Get the content type of a file based on its extension
     *
     * @param filename The filename
     * @return Content type string
     */
    public String getContentType(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "application/octet-stream";
        }

        String extension = getFileExtension(filename).toLowerCase();

        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".webp":
                return "image/webp";
            case ".mp4":
                return "video/mp4";
            case ".webm":
                return "video/webm";
            case ".mov":
                return "video/quicktime";
            case ".avi":
                return "video/x-msvideo";
            case ".pdf":
                return "application/pdf";
            case ".txt":
                return "text/plain";
            case ".json":
                return "application/json";
            default:
                return "application/octet-stream";
        }
    }
}
