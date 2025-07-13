package com.example.silentvoice_bd.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.silentvoice_bd.ai.services.AIProcessingService;
import com.example.silentvoice_bd.dto.VideoUploadResponse;
import com.example.silentvoice_bd.model.VideoFile;
import com.example.silentvoice_bd.processing.VideoProcessingService;
import com.example.silentvoice_bd.service.VideoService;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "http://localhost:3000")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;
    private final VideoProcessingService videoProcessingService;
    private final AIProcessingService aiProcessingService;

    public VideoController(
            VideoService videoService,
            VideoProcessingService videoProcessingService,
            AIProcessingService aiProcessingService
    ) {
        this.videoService = videoService;
        this.videoProcessingService = videoProcessingService;
        this.aiProcessingService = aiProcessingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "enableAI", defaultValue = "true") boolean enableAI
    ) {
        VideoFile savedVideo = videoService.storeVideoFile(file, description);

        // Start video processing (extract frames, etc.)
        videoProcessingService.processVideoAsync(savedVideo.getId());

        // Start AI processing after a delay (to allow frame extraction)
        if (enableAI) {
            CompletableFuture.runAsync(() -> {
                try {
                    // Wait 15 seconds for frame extraction
                    Thread.sleep(15000);
                    aiProcessingService.processVideoAsync(savedVideo.getId());
                } catch (InterruptedException | RuntimeException e) {
                    logger.error("AI processing failed for video {}: {}", savedVideo.getId(), e.getMessage(), e);
                    Thread.currentThread().interrupt();
                }
            });
        }

        VideoUploadResponse response = new VideoUploadResponse(
                savedVideo.getId(),
                savedVideo.getFilename(),
                savedVideo.getOriginalFilename(),
                savedVideo.getContentType(),
                savedVideo.getFileSize(),
                savedVideo.getUploadTimestamp(),
                enableAI
                        ? "Video uploaded, processing and AI analysis started!"
                        : "Video uploaded and processing started!"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/status")
public ResponseEntity<Map<String, Object>> getVideoStatus(@PathVariable UUID id) {
    try {
        Optional<VideoFile> videoFile = videoService.getVideoFile(id);
        if (videoFile.isEmpty()) {
            logger.warn("Video not found for ID: {}", id);

            // Return JSON error instead of HTML 404
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Video not found");
            errorResponse.put("videoId", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        Map<String, Object> status = new HashMap<>();
        status.put("success", true);
        status.put("videoId", id);
        status.put("filename", videoFile.get().getOriginalFilename());
        status.put("uploadTime", videoFile.get().getUploadTimestamp());

        // Check AI processing status
        try {
            List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(id);
            logger.info("Checking predictions for video {}: found {} predictions", id, predictions.size());

            if (!predictions.isEmpty()) {
                SignLanguagePrediction latestPrediction = predictions.get(0);
                status.put("aiComplete", true);
                status.put("prediction", latestPrediction.getPredictedText());
                status.put("confidence", latestPrediction.getConfidenceScore());
                status.put("predictionId", latestPrediction.getId());
                status.put("modelVersion", latestPrediction.getModelVersion());
                status.put("processingTime", latestPrediction.getProcessingTimeMs());

                logger.info("Returning AI complete status for video {}: {}", id, latestPrediction.getPredictedText());
            } else {
                status.put("aiComplete", false);
                status.put("status", "AI processing in progress...");
                logger.info("AI processing still in progress for video {}", id);
            }
        } catch (Exception e) {
            logger.error("Error checking AI prediction status for video {}: {}", id, e.getMessage(), e);
            status.put("aiComplete", false);
            status.put("status", "AI processing in progress...");
            status.put("error", "Error checking prediction status");
        }

        // Ensure JSON response with proper headers
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(status);

    } catch (Exception e) {
        logger.error("Error getting video status for ID: {}", id, e);

        // Return JSON error instead of HTML error page
        Map<String, Object> errorStatus = new HashMap<>();
        errorStatus.put("success", false);
        errorStatus.put("error", "Failed to get video status: " + e.getMessage());
        errorStatus.put("videoId", id);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorStatus);
    }
}







    @GetMapping
    public ResponseEntity<List<VideoFile>> getAllVideos() {
        List<VideoFile> videos = videoService.getAllVideoFiles();
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadVideo(@PathVariable UUID id) {
        Resource resource = videoService.loadVideoFileAsResource(id);
        Optional<VideoFile> videoFile = videoService.getVideoFile(id);

        return videoFile.map(file -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                .body(resource)).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable UUID id) {
        Resource resource = videoService.loadVideoFileAsResource(id);
        Optional<VideoFile> videoFile = videoService.getVideoFile(id);

        return videoFile.map(file -> ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource)).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVideo(@PathVariable UUID id) {
        videoService.deleteVideoFile(id);
        return ResponseEntity.ok("Video deleted successfully");
    }

    @GetMapping("/{id}/info")
    public ResponseEntity<VideoFile> getVideoInfo(@PathVariable UUID id) {
        Optional<VideoFile> videoFile = videoService.getVideoFile(id);
        return videoFile.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

