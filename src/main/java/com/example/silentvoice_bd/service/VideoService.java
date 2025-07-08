package com.example.silentvoice_bd.service;



import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.silentvoice_bd.config.FileStorageProperties;
import com.example.silentvoice_bd.exception.FileUploadException;
import com.example.silentvoice_bd.model.VideoFile;
import com.example.silentvoice_bd.processing.VideoProcessingService;
import com.example.silentvoice_bd.repository.VideoRepository;

import jakarta.annotation.PostConstruct;

@Service
public class VideoService {

    private final VideoRepository videoRepository;
    private final FileStorageProperties fileStorageProperties;
    private final VideoProcessingService videoProcessingService;
    private final Tika tika = new Tika();
    private Path fileStorageLocation;

   
    public VideoService(VideoRepository videoRepository, FileStorageProperties fileStorageProperties, 
                        VideoProcessingService videoProcessingService) {
        this.videoRepository = videoRepository;
        this.fileStorageProperties = fileStorageProperties;
        this.videoProcessingService = videoProcessingService;
    }

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileUploadException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public VideoFile storeVideoFile(MultipartFile file, String description) {
        // Validate file
        validateVideoFile(file);

        // Generate unique filename
        String originalFilenameRaw = file.getOriginalFilename();
        String originalFilename = StringUtils.cleanPath(
            originalFilenameRaw != null ? originalFilenameRaw : "unknown"
        );
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        try {
            // Copy file to the target location
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Create and save video file entity
            VideoFile videoFile = new VideoFile(
                uniqueFilename,
                originalFilename,
                file.getContentType(),
                file.getSize(),
                targetLocation.toString()
            );
            videoFile.setDescription(description);

            // Save the video file to the database
            VideoFile savedVideo = videoRepository.save(videoFile);

            // Automatically trigger video processing
            videoProcessingService.processVideoAsync(savedVideo.getId());

            return savedVideo;

        } catch (IOException ex) {
            throw new FileUploadException("Could not store file " + originalFilename + ". Please try again!", ex);
        }
    }

    public List<VideoFile> getAllVideoFiles() {
        return videoRepository.findAllOrderByUploadTimestampDesc();
    }

    public Optional<VideoFile> getVideoFile(UUID id) {
        return videoRepository.findById(id);
    }

    public Resource loadVideoFileAsResource(UUID id) {
        VideoFile videoFile = videoRepository.findById(id)
                .orElseThrow(() -> new FileUploadException("Video file not found with id: " + id));

        try {
            Path filePath = Paths.get(videoFile.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileUploadException("Video file not found: " + videoFile.getFilename());
            }
        } catch (MalformedURLException ex) {
            throw new FileUploadException("Video file not found: " + videoFile.getFilename(), ex);
        }
    }

    public void deleteVideoFile(UUID id) {
        VideoFile videoFile = videoRepository.findById(id)
                .orElseThrow(() -> new FileUploadException("Video file not found with id: " + id));

        try {
            Path filePath = Paths.get(videoFile.getFilePath());
            Files.deleteIfExists(filePath);
            videoRepository.delete(videoFile);
        } catch (IOException ex) {
            throw new FileUploadException("Could not delete file: " + videoFile.getFilename(), ex);
        }
    }

    private void validateVideoFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileUploadException("Cannot upload empty file");
        }

        // Validate file size (100MB limit)
        if (file.getSize() > 100 * 1024 * 1024) {
            throw new FileUploadException("File size exceeds maximum limit of 100MB");
        }

        // Validate file type using Tika for better security
        try {
            String detectedContentType = tika.detect(file.getBytes());
            if (!detectedContentType.startsWith("video/")) {
                throw new FileUploadException("Invalid file type. Only video files are allowed.");
            }
        } catch (IOException ex) {
            throw new FileUploadException("Could not validate file type", ex);
        }
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }
}
