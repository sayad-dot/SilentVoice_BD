package com.example.silentvoice_bd.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.silentvoice_bd.ai.services.AIProcessingService;
import com.example.silentvoice_bd.config.FileStorageProperties;
import com.example.silentvoice_bd.exception.FileUploadException;
import com.example.silentvoice_bd.model.VideoFile;
import com.example.silentvoice_bd.processing.FrameExtractionService;
import com.example.silentvoice_bd.processing.VideoProcessingService;
import com.example.silentvoice_bd.repository.VideoMetadataRepository;
import com.example.silentvoice_bd.repository.VideoProcessingJobRepository;
import com.example.silentvoice_bd.repository.VideoRepository;

import jakarta.annotation.PostConstruct;

@Service
public class VideoService {

    private static final Logger logger = LoggerFactory.getLogger(VideoService.class);

    private final VideoRepository videoRepository;
    private final FileStorageProperties fileStorageProperties;
    private final VideoProcessingService videoProcessingService;
    private final Tika tika = new Tika();
    private Path fileStorageLocation;

    // BdSLW-60 dataset configuration
    private final String DATASET_PATH = "dataset/bdslw60/archive";
    private final Random random = new Random();

    @Autowired(required = false)
    private VideoProcessingJobRepository videoProcessingJobRepository;

    @Autowired(required = false)
    private FrameExtractionService frameExtractionService;

    @Autowired(required = false)
    private AIProcessingService aiProcessingService;

    @Autowired(required = false)
    private VideoMetadataRepository videoMetadataRepository;

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

    // ========== EXISTING METHODS (Video Upload/Management) ==========
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

    @Transactional
    public void deleteVideoFile(UUID id) {
        VideoFile videoFile = videoRepository.findById(id)
                .orElseThrow(() -> new FileUploadException("Video file not found with id: " + id));

        try {
            logger.info("🗑️ Starting cascade delete for video: {} ({})", videoFile.getOriginalFilename(), id);

            // Step 1: Delete AI predictions first
            if (aiProcessingService != null) {
                try {
                    logger.info("🤖 Deleting AI predictions for video: {}", id);
                    aiProcessingService.deletePredictionsByVideoId(id);
                    logger.info("✅ AI predictions deleted for video: {}", id);
                } catch (Exception e) {
                    logger.warn("⚠️ Failed to delete AI predictions for video {}: {}", id, e.getMessage());
                }
            }

            // Step 2: Delete extracted frames
            if (frameExtractionService != null) {
                try {
                    logger.info("🎞️ Deleting extracted frames for video: {}", id);
                    frameExtractionService.deleteFramesByVideoId(id);
                    logger.info("✅ Extracted frames deleted for video: {}", id);
                } catch (Exception e) {
                    logger.warn("⚠️ Failed to delete frames for video {}: {}", id, e.getMessage());
                }
            }

            // Step 3: Delete video metadata (THIS WAS MISSING!)
            if (videoMetadataRepository != null) {
                try {
                    logger.info("📊 Deleting video metadata for video: {}", id);
                    videoMetadataRepository.deleteByVideoFileId(id);
                    logger.info("✅ Video metadata deleted for video: {}", id);
                } catch (Exception e) {
                    logger.error("❌ Failed to delete video metadata for video {}: {}", id, e.getMessage());
                    throw new FileUploadException("Failed to delete video metadata for video: " + id, e);
                }
            }

            // Step 4: Delete video processing jobs
            if (videoProcessingJobRepository != null) {
                try {
                    logger.info("⚙️ Deleting processing jobs for video: {}", id);
                    videoProcessingJobRepository.deleteByVideoFileId(id);
                    logger.info("✅ Processing jobs deleted for video: {}", id);
                } catch (Exception e) {
                    logger.error("❌ Failed to delete processing jobs for video {}: {}", id, e.getMessage());
                    throw new FileUploadException("Failed to delete processing jobs for video: " + id, e);
                }
            }

            // Step 5: Delete physical file
            try {
                Path filePath = Paths.get(videoFile.getFilePath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("📁 Physical file deleted: {}", filePath);
                } else {
                    logger.warn("⚠️ Physical file not found: {}", filePath);
                }
            } catch (IOException e) {
                logger.warn("⚠️ Failed to delete physical file for video {}: {}", id, e.getMessage());
            }

            // Step 6: Finally delete the video record
            videoRepository.delete(videoFile);
            logger.info("✅ Video record deleted from database: {}", id);
            logger.info("🎉 Complete cascade deletion successful for video: {}", id);

        } catch (Exception ex) {
            logger.error("💥 Failed to delete video {}: {}", id, ex.getMessage(), ex);
            throw new FileUploadException("Could not delete file: " + videoFile.getFilename(), ex);
        }
    }

    // ========== NEW METHODS (BdSLW-60 Dataset Integration) ==========
    /**
     * Get a random video file for a specific sign from BdSLW-60 dataset
     */
    public String getRandomVideoForSign(String signName) {
        try {
            Path signPath = Paths.get(DATASET_PATH).resolve(signName);
            if (Files.exists(signPath)) {
                List<String> videos = Files.list(signPath)
                        .filter(path -> path.toString().toLowerCase().endsWith(".mp4"))
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());

                if (!videos.isEmpty()) {
                    return videos.get(random.nextInt(videos.size()));
                }
            }
        } catch (IOException e) {
            logger.error("Error accessing videos for sign: " + signName, e);
        }
        return null;
    }

    /**
     * Get all video files for a specific sign from BdSLW-60 dataset
     */
    public List<String> getAllVideosForSign(String signName) {
        try {
            Path signPath = Paths.get(DATASET_PATH).resolve(signName);
            if (Files.exists(signPath)) {
                return Files.list(signPath)
                        .filter(path -> path.toString().toLowerCase().endsWith(".mp4"))
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            logger.error("Error accessing videos for sign: " + signName, e);
        }
        return List.of();
    }

    /**
     * Get all available signs from BdSLW-60 dataset
     */
    public List<String> getAvailableSigns() {
        try {
            Path archivePath = Paths.get(DATASET_PATH);
            if (Files.exists(archivePath)) {
                return Files.list(archivePath)
                        .filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            logger.error("Error accessing dataset signs", e);
        }
        return List.of();
    }

    /**
     * Load dataset video as resource for streaming
     */
    public Resource loadDatasetVideoAsResource(String signName, String fileName) {
        try {
            Path videoPath = Paths.get(DATASET_PATH)
                    .resolve(signName)
                    .resolve(fileName);

            Resource resource = new UrlResource(videoPath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileUploadException("Dataset video not found: " + signName + "/" + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileUploadException("Dataset video not found: " + signName + "/" + fileName, ex);
        }
    }

    /**
     * Check if BdSLW-60 dataset is available
     */
    public boolean isDatasetAvailable() {
        Path archivePath = Paths.get(DATASET_PATH);
        return Files.exists(archivePath) && Files.isDirectory(archivePath);
    }

    public String getVideoStreamUrl(UUID id) {
        return "/api/videos/" + id + "/stream";
    }

    public String getVideoDownloadUrl(UUID id) {
        return "/api/videos/" + id;
    }

    // ========== PRIVATE HELPER METHODS ==========
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
