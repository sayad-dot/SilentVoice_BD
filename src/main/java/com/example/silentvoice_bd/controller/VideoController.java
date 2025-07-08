package com.example.silentvoice_bd.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    private final VideoService videoService;
    private final VideoProcessingService videoProcessingService;
    private final AIProcessingService aiProcessingService;

   // @Autowired
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

        // Optionally start AI processing after a delay (to allow frame extraction)
        if (enableAI) {
            CompletableFuture.runAsync(() -> {
                try {
                    // Wait 15 seconds for frame extraction (adjust as needed)
                    Thread.sleep(15000);
                    aiProcessingService.processVideoAsync(savedVideo.getId());
                } catch (InterruptedException | RuntimeException e) {
                    System.err.println("AI processing failed for video " + savedVideo.getId() + ": " + e.getMessage());
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
