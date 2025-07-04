package com.example.silentvoice_bd.processing;

import com.example.silentvoice_bd.model.*;
import com.example.silentvoice_bd.repository.VideoRepository;
import com.example.silentvoice_bd.repository.VideoProcessingJobRepository;
import com.example.silentvoice_bd.repository.VideoMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class VideoProcessingService {

    @Autowired
    private VideoProcessingJobRepository jobRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoMetadataRepository videoMetadataRepository;

    @Autowired
    private VideoMetadataService metadataService;

    @Autowired
    private FrameExtractionService frameExtractionService;

    @Autowired
    private ThumbnailService thumbnailService;

    public VideoProcessingJob createProcessingJob(UUID videoFileId, VideoProcessingJob.JobType jobType) {
        VideoProcessingJob job = new VideoProcessingJob(videoFileId, jobType);
        return jobRepository.save(job);
    }

    @Async("videoProcessingExecutor")
    public CompletableFuture<VideoProcessingJob> processVideoAsync(UUID videoFileId) {
        VideoProcessingJob job = createProcessingJob(videoFileId, VideoProcessingJob.JobType.FULL_PROCESSING);

        try {
            job.setStatus(VideoProcessingJob.ProcessingStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            job.setProgress(0);
            jobRepository.save(job);

            Optional<VideoFile> videoFileOpt = videoRepository.findById(videoFileId);
            if (!videoFileOpt.isPresent()) {
                throw new Exception("Video file not found: " + videoFileId);
            }

            VideoFile videoFile = videoFileOpt.get();

            // Step 1: Extract metadata (25% progress)
            job.setProgress(10);
            jobRepository.save(job);

            VideoMetadata metadata = metadataService.extractMetadata(videoFile);

            job.setProgress(25);
            jobRepository.save(job);

            // Step 2: Generate thumbnail (50% progress)
            String thumbnailPath = thumbnailService.generateThumbnail(videoFile);
            metadata.setThumbnailPath(thumbnailPath);
            videoMetadataRepository.save(metadata);

            job.setProgress(50);
            jobRepository.save(job);

            // Step 3: Extract frames (100% progress)
            List<ExtractedFrame> frames = frameExtractionService.extractFrames(videoFile, metadata);

            job.setProgress(100);
            job.setStatus(VideoProcessingJob.ProcessingStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setResultData(String.format("{\"framesExtracted\": %d, \"thumbnailGenerated\": true}", frames.size()));

            // Update video file status
            videoFile.setProcessingStatus("PROCESSED");
            videoRepository.save(videoFile);

        } catch (Exception e) {
            job.setStatus(VideoProcessingJob.ProcessingStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
        }

        jobRepository.save(job);
        return CompletableFuture.completedFuture(job);
    }

    public List<VideoProcessingJob> getJobsByVideoId(UUID videoFileId) {
        return jobRepository.findByVideoFileIdOrderByCreatedAtDesc(videoFileId);
    }

    public Optional<VideoProcessingJob> getJobById(UUID jobId) {
        return jobRepository.findById(jobId);
    }

    public List<VideoProcessingJob> getPendingJobs() {
        return jobRepository.findByStatusOrderByCreatedAtAsc(VideoProcessingJob.ProcessingStatus.PENDING);
    }

    @Transactional
    public void deleteVideoProcessingData(UUID videoFileId) {
        // Delete frames
        frameExtractionService.deleteFrames(videoFileId);

        // Delete thumbnail
        Optional<VideoMetadata> metadata = metadataService.getMetadataByVideoId(videoFileId);
        if (metadata.isPresent() && metadata.get().getThumbnailPath() != null) {
            thumbnailService.deleteThumbnail(metadata.get().getThumbnailPath());
        }

        // Delete metadata
        metadataService.deleteMetadata(videoFileId);

        // Delete jobs
        List<VideoProcessingJob> jobs = jobRepository.findByVideoFileIdOrderByCreatedAtDesc(videoFileId);
        jobRepository.deleteAll(jobs);
    }
}
