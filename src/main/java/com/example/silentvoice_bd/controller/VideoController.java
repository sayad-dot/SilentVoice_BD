package com.example.silentvoice_bd.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.silentvoice_bd.ai.models.SignLanguagePrediction;
import com.example.silentvoice_bd.ai.services.AIProcessingService;
import com.example.silentvoice_bd.dto.VideoUploadResponse;
import com.example.silentvoice_bd.model.VideoFile;
import com.example.silentvoice_bd.processing.VideoProcessingService;
import com.example.silentvoice_bd.service.VideoService;

@RestController
@RequestMapping("/api/videos")
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
        logger.info("🎬 Received video upload: filename={}, size={} bytes, AI enabled={}",
                file.getOriginalFilename(), file.getSize(), enableAI);

        VideoFile savedVideo = videoService.storeVideoFile(file, description);
        logger.info("💾 Video saved with ID: {}, filename: {}", savedVideo.getId(), savedVideo.getFilename());

        // Start video processing (extract frames, etc.)
        videoProcessingService.processVideoAsync(savedVideo.getId());
        logger.info("🎞️ Started frame extraction for video: {}", savedVideo.getId());

        // Start AI processing after a delay (to allow frame extraction)
        if (enableAI) {
            CompletableFuture.runAsync(() -> {
                try {
                    logger.info("⏳ Waiting 15 seconds for frame extraction to complete for video: {}", savedVideo.getId());
                    Thread.sleep(15000);

                    logger.info("🤖 Starting AI processing for video: {}", savedVideo.getId());
                    aiProcessingService.processVideoAsync(savedVideo.getId());

                } catch (InterruptedException | RuntimeException e) {
                    logger.error("💥 AI processing failed for video {}: {}", savedVideo.getId(), e.getMessage(), e);
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

        logger.info("✅ Video upload response created for: {}", savedVideo.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> getVideoStatus(@PathVariable UUID id) {
        logger.info("🔍 Checking status for video: {}", id);

        try {
            Optional<VideoFile> videoFile = videoService.getVideoFile(id);
            if (videoFile.isEmpty()) {
                logger.warn("❌ Video not found for ID: {}", id);

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

            logger.info("📁 Video file found: {}", videoFile.get().getOriginalFilename());

            // Check AI processing status with enhanced logging
            try {
                List<SignLanguagePrediction> predictions = aiProcessingService.getPredictionsByVideoId(id);
                logger.info("🔍 Checking predictions for video {}: found {} predictions", id, predictions.size());

                if (!predictions.isEmpty()) {
                    SignLanguagePrediction latestPrediction = predictions.get(0);

                    // CRITICAL: Log prediction details for debugging
                    logger.info("🎯 AI Processing Results for video {}:", id);
                    logger.info("   📝 Predicted text: {}", latestPrediction.getPredictedText());
                    logger.info("   📊 Confidence: {:.2f}%", latestPrediction.getConfidenceScore().doubleValue() * 100);
                    logger.info("   🏷️ Model version: {}", latestPrediction.getModelVersion());
                    logger.info("   ⏱️ Processing time: {} ms", latestPrediction.getProcessingTimeMs());

                    // Check for low confidence (normalization issue indicator)
                    if (latestPrediction.getConfidenceScore().doubleValue() < 0.1) {
                        logger.error("❌ CRITICAL: Very low confidence ({:.2f}%) for video {}",
                                latestPrediction.getConfidenceScore().doubleValue() * 100, id);
                        logger.error("   🔧 This suggests normalization issues in pose extraction");
                        logger.error("   📋 Predicted text: {}", latestPrediction.getPredictedText());
                    } else if (latestPrediction.getConfidenceScore().doubleValue() > 0.7) {
                        logger.info("✅ High confidence prediction ({:.2f}%) - normalization likely working",
                                latestPrediction.getConfidenceScore().doubleValue() * 100);
                    } else {
                        logger.warn("⚠️ Medium confidence ({:.2f}%) - check normalization",
                                latestPrediction.getConfidenceScore().doubleValue() * 100);
                    }

                    status.put("aiComplete", true);
                    status.put("prediction", latestPrediction.getPredictedText());
                    status.put("confidence", latestPrediction.getConfidenceScore().doubleValue());
                    status.put("predictionId", latestPrediction.getId());
                    status.put("modelVersion", latestPrediction.getModelVersion());
                    status.put("processingTime", latestPrediction.getProcessingTimeMs());

                    logger.info("📤 Returning AI complete status for video {}: {}", id, latestPrediction.getPredictedText());
                } else {
                    status.put("aiComplete", false);
                    status.put("status", "AI processing in progress...");
                    logger.info("⏳ AI processing still in progress for video {}", id);
                }
            } catch (Exception e) {
                logger.error("💥 Error checking AI prediction status for video {}: {}", id, e.getMessage(), e);
                status.put("aiComplete", false);
                status.put("status", "AI processing in progress...");
                status.put("error", "Error checking prediction status");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(status);

        } catch (Exception e) {
            logger.error("💥 Error getting video status for ID: {}", id, e);

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
        logger.info("📋 Fetching all videos");
        List<VideoFile> videos = videoService.getAllVideoFiles();
        logger.info("📋 Found {} videos", videos.size());
        return ResponseEntity.ok(videos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> downloadVideo(@PathVariable UUID id) {
        logger.info("⬇️ Download request for video: {}", id);
        Resource resource = videoService.loadVideoFileAsResource(id);
        Optional<VideoFile> videoFile = videoService.getVideoFile(id);

        return videoFile.map(file -> {
            logger.info("✅ Serving download for: {}", file.getOriginalFilename());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getOriginalFilename() + "\"")
                    .body(resource);
        }).orElseGet(() -> {
            logger.warn("❌ Video not found for download: {}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable UUID id) {
        logger.info("📺 Stream request for video: {}", id);
        Resource resource = videoService.loadVideoFileAsResource(id);
        Optional<VideoFile> videoFile = videoService.getVideoFile(id);

        return videoFile.map(file -> {
            logger.info("✅ Serving stream for: {}", file.getOriginalFilename());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        }).orElseGet(() -> {
            logger.warn("❌ Video not found for streaming: {}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVideo(@PathVariable UUID id) {
        logger.info("🗑️ Delete request for video: {}", id);
        videoService.deleteVideoFile(id);
        logger.info("✅ Video deleted successfully: {}", id);
        return ResponseEntity.ok("Video deleted successfully");
    }

    @GetMapping("/{id}/info")
    public ResponseEntity<VideoFile> getVideoInfo(@PathVariable UUID id) {
        logger.info("ℹ️ Info request for video: {}", id);
        Optional<VideoFile> videoFile = videoService.getVideoFile(id);
        return videoFile.map(file -> {
            logger.info("✅ Returning info for: {}", file.getOriginalFilename());
            return ResponseEntity.ok(file);
        }).orElseGet(() -> {
            logger.warn("❌ Video not found for info: {}", id);
            return ResponseEntity.notFound().build();
        });
    }
}
