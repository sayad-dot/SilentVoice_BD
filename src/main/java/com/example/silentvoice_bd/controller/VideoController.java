package com.example.silentvoice_bd.controller;



import com.example.silentvoice_bd.dto.VideoUploadResponse;
import com.example.silentvoice_bd.model.VideoFile;
import com.example.silentvoice_bd.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(origins = "http://localhost:3000")
public class VideoController {

    private final VideoService videoService;

    @Autowired
    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        VideoFile savedVideo = videoService.storeVideoFile(file, description);

        VideoUploadResponse response = new VideoUploadResponse(
            savedVideo.getId(),
            savedVideo.getFilename(),
            savedVideo.getOriginalFilename(),
            savedVideo.getContentType(),
            savedVideo.getFileSize(),
            savedVideo.getUploadTimestamp(),
            "Video uploaded successfully!"
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

        if (videoFile.isPresent()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(videoFile.get().getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                           "attachment; filename=\"" + videoFile.get().getOriginalFilename() + "\"")
                    .body(resource);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable UUID id) {
        Resource resource = videoService.loadVideoFileAsResource(id);
        Optional<VideoFile> videoFile = videoService.getVideoFile(id);

        if (videoFile.isPresent()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(videoFile.get().getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        }

        return ResponseEntity.notFound().build();
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

