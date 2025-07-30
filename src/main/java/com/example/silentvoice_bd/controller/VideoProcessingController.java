package com.example.silentvoice_bd.controller;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.silentvoice_bd.dto.ProcessingJobResponse;
import com.example.silentvoice_bd.model.ExtractedFrame;
import com.example.silentvoice_bd.model.VideoMetadata;
import com.example.silentvoice_bd.model.VideoProcessingJob;
import com.example.silentvoice_bd.processing.FrameExtractionService;
import com.example.silentvoice_bd.processing.VideoMetadataService;
import com.example.silentvoice_bd.processing.VideoProcessingService;

@RestController
@RequestMapping("/api/video-processing")
public class VideoProcessingController {

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired
    private VideoMetadataService metadataService;

    @Autowired
    private FrameExtractionService frameExtractionService;

    @PostMapping("/process/{videoId}")
    public ResponseEntity<ProcessingJobResponse> startProcessing(@PathVariable UUID videoId) {
        try {
            videoProcessingService.processVideoAsync(videoId);

            // Return the latest job for this video
            List<VideoProcessingJob> jobs = videoProcessingService.getJobsByVideoId(videoId);
            if (!jobs.isEmpty()) {
                return ResponseEntity.ok(new ProcessingJobResponse(jobs.get(0)));
            }

            return ResponseEntity.accepted().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status/{videoId}")
    public ResponseEntity<List<ProcessingJobResponse>> getProcessingStatus(@PathVariable UUID videoId) {
        List<VideoProcessingJob> jobs = videoProcessingService.getJobsByVideoId(videoId);
        List<ProcessingJobResponse> responses = jobs.stream()
                .map(ProcessingJobResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<ProcessingJobResponse> getJobStatus(@PathVariable UUID jobId) {
        Optional<VideoProcessingJob> job = videoProcessingService.getJobById(jobId);

        if (job.isPresent()) {
            return ResponseEntity.ok(new ProcessingJobResponse(job.get()));
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/metadata/{videoId}")
    public ResponseEntity<VideoMetadata> getVideoMetadata(@PathVariable UUID videoId) {
        Optional<VideoMetadata> metadata = metadataService.getMetadataByVideoId(videoId);

        if (metadata.isPresent()) {
            return ResponseEntity.ok(metadata.get());
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/frames/{videoId}")
    public ResponseEntity<List<ExtractedFrame>> getExtractedFrames(@PathVariable UUID videoId) {
        List<ExtractedFrame> frames = frameExtractionService.getFramesByVideoId(videoId);
        return ResponseEntity.ok(frames);
    }

    @GetMapping("/frames/{videoId}/keyframes")
    public ResponseEntity<List<ExtractedFrame>> getKeyFrames(@PathVariable UUID videoId) {
        List<ExtractedFrame> keyFrames = frameExtractionService.getKeyFramesByVideoId(videoId);
        return ResponseEntity.ok(keyFrames);
    }

    @GetMapping("/thumbnail/{videoId}")
    public ResponseEntity<Resource> getThumbnail(@PathVariable UUID videoId) {
        Optional<VideoMetadata> metadata = metadataService.getMetadataByVideoId(videoId);

        if (metadata.isPresent() && metadata.get().getThumbnailPath() != null) {
            File thumbnailFile = new File(metadata.get().getThumbnailPath());

            if (thumbnailFile.exists()) {
                Resource resource = new FileSystemResource(thumbnailFile);

                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"thumbnail.jpg\"")
                        .body(resource);
            }
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/frame/{frameId}/image")
    public ResponseEntity<Resource> getFrameImage(@PathVariable UUID frameId) {
        // Implementation to serve individual frame images
        // This would require adding a method to find frame by ID
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/data/{videoId}")
    public ResponseEntity<String> deleteProcessingData(@PathVariable UUID videoId) {
        try {
            videoProcessingService.deleteVideoProcessingData(videoId);
            return ResponseEntity.ok("Processing data deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete processing data: " + e.getMessage());
        }
    }

    @GetMapping("/jobs/pending")
    public ResponseEntity<List<ProcessingJobResponse>> getPendingJobs() {
        List<VideoProcessingJob> pendingJobs = videoProcessingService.getPendingJobs();
        List<ProcessingJobResponse> responses = pendingJobs.stream()
                .map(ProcessingJobResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }
}
