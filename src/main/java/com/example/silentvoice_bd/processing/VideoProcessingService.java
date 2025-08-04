package com.example.silentvoice_bd.processing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.silentvoice_bd.model.ExtractedFrame;
import com.example.silentvoice_bd.model.VideoFile;
import com.example.silentvoice_bd.model.VideoMetadata;
import com.example.silentvoice_bd.model.VideoProcessingJob;
import com.example.silentvoice_bd.repository.VideoMetadataRepository;
import com.example.silentvoice_bd.repository.VideoProcessingJobRepository;
import com.example.silentvoice_bd.repository.VideoRepository;

@Service
public class VideoProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingService.class);

    @Autowired
    private VideoProcessingJobRepository jobRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoMetadataRepository videoMetadataRepository;

    @Autowired
    private FrameExtractionService frameExtractionService;

    @Autowired(required = false)
    private ThumbnailService thumbnailService;

    public VideoProcessingJob createProcessingJob(UUID videoFileId, VideoProcessingJob.JobType jobType) {
        VideoProcessingJob job = new VideoProcessingJob(videoFileId, jobType);
        return jobRepository.save(job);
    }

    @Async("videoProcessingExecutor")
    public CompletableFuture<VideoProcessingJob> processVideoAsync(UUID videoFileId) {
        VideoProcessingJob job = createProcessingJob(videoFileId, VideoProcessingJob.JobType.FULL_PROCESSING);

        logger.info("🚀 Starting async video processing for: {}", videoFileId);

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
            logger.info("📁 Processing video: {} ({})", videoFile.getOriginalFilename(), videoFile.getFilePath());

            // Check if video file exists on disk
            java.io.File diskFile = new java.io.File(videoFile.getFilePath());
            if (!diskFile.exists()) {
                throw new Exception("Video file not found on disk: " + videoFile.getFilePath());
            }

            // Step 1: Extract metadata (25% progress)
            job.setProgress(10);
            jobRepository.save(job);

            logger.info("📊 Extracting metadata for video: {}", videoFileId);
            VideoMetadata metadata = extractVideoMetadata(videoFile);

            job.setProgress(25);
            jobRepository.save(job);

            // Step 2: Generate thumbnail (50% progress) - Optional
            if (thumbnailService != null) {
                try {
                    logger.info("🖼️ Generating thumbnail for video: {}", videoFileId);
                    String thumbnailPath = thumbnailService.generateThumbnail(videoFile);
                    metadata.setThumbnailPath(thumbnailPath);
                    videoMetadataRepository.save(metadata);
                    logger.info("✅ Thumbnail generated successfully");
                } catch (Exception e) {
                    logger.warn("⚠️ Thumbnail generation failed (non-critical): {}", e.getMessage());
                }
            }

            job.setProgress(50);
            jobRepository.save(job);

            // Step 3: Extract frames (100% progress)
            logger.info("🎞️ Starting frame extraction for video: {}", videoFileId);
            List<ExtractedFrame> frames = frameExtractionService.extractFrames(videoFile, metadata);
            logger.info("✅ Successfully extracted {} frames", frames.size());

            job.setProgress(100);
            job.setStatus(VideoProcessingJob.ProcessingStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setResultData(String.format("{\"framesExtracted\": %d, \"thumbnailGenerated\": %s}",
                    frames.size(), metadata.getThumbnailPath() != null));

            // Update video file status
            videoFile.setProcessingStatus("PROCESSED");
            videoRepository.save(videoFile);

            logger.info("🎉 Video processing completed successfully for: {}", videoFileId);

        } catch (Exception e) {
            logger.error("💥 Video processing failed for {}: {}", videoFileId, e.getMessage(), e);
            job.setStatus(VideoProcessingJob.ProcessingStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(LocalDateTime.now());
        }

        jobRepository.save(job);
        return CompletableFuture.completedFuture(job);
    }

    /**
     * Extract video metadata using FFmpeg - SIMPLIFIED for existing schema
     */
    private VideoMetadata extractVideoMetadata(VideoFile videoFile) throws Exception {
        logger.info("📊 Extracting metadata for video: {}", videoFile.getId());

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile.getFilePath())) {
            grabber.start();

            VideoMetadata metadata = new VideoMetadata(videoFile.getId());

            // ONLY set fields that exist in your simplified database schema
            metadata.setDurationSeconds((int) (grabber.getLengthInTime() / 1000000));
            metadata.setFrameRate(BigDecimal.valueOf(grabber.getFrameRate()));
            metadata.setWidth(grabber.getImageWidth());
            metadata.setHeight(grabber.getImageHeight());
            metadata.setBitrate(grabber.getVideoBitrate());

            // Convert codec IDs to strings (handles FFmpeg returning int instead of String)
            int videoCodecId = grabber.getVideoCodec();
            metadata.setVideoCodec(videoCodecId != 0 ? String.valueOf(videoCodecId) : null);

            metadata.setHasAudio(grabber.getAudioChannels() > 0);

            // Audio codec (only if audio exists)
            if (grabber.getAudioChannels() > 0) {
                int audioCodecId = grabber.getAudioCodec();
                metadata.setAudioCodec(audioCodecId != 0 ? String.valueOf(audioCodecId) : null);
            }

            // File format from extension
            metadata.setFileFormat(getFileExtension(videoFile.getOriginalFilename()));

            // REMOVED: metadata.setMetadataExtracted(true); - Field doesn't exist
            // REMOVED: metadata.setProcessingStatus("COMPLETED"); - Field doesn't exist
            grabber.stop();

            VideoMetadata saved = videoMetadataRepository.save(metadata);
            logger.info("✅ Video metadata saved successfully: {}", saved);

            return saved;

        } catch (Exception e) {
            logger.error("💥 Failed to extract video metadata: {}", e.getMessage(), e);
            throw new Exception("Metadata extraction failed", e);
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        return "unknown";
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
        logger.info("🗑️ Starting cleanup for video processing data: {}", videoFileId);

        try {
            // Delete frames
            frameExtractionService.deleteFrames(videoFileId);
            logger.info("✅ Frames deleted for video: {}", videoFileId);

            // Delete thumbnail if exists
            Optional<VideoMetadata> metadataOpt = videoMetadataRepository.findByVideoFileId(videoFileId);
            if (metadataOpt.isPresent() && metadataOpt.get().getThumbnailPath() != null && thumbnailService != null) {
                try {
                    thumbnailService.deleteThumbnail(metadataOpt.get().getThumbnailPath());
                    logger.info("✅ Thumbnail deleted for video: {}", videoFileId);
                } catch (Exception e) {
                    logger.warn("⚠️ Failed to delete thumbnail: {}", e.getMessage());
                }
            }

            // Delete metadata
            videoMetadataRepository.deleteByVideoFileId(videoFileId);
            logger.info("✅ Metadata deleted for video: {}", videoFileId);

            // Delete processing jobs
            List<VideoProcessingJob> jobs = jobRepository.findByVideoFileIdOrderByCreatedAtDesc(videoFileId);
            jobRepository.deleteAll(jobs);
            logger.info("✅ Processing jobs deleted for video: {}", videoFileId);

        } catch (Exception e) {
            logger.error("💥 Error during cleanup for video {}: {}", videoFileId, e.getMessage(), e);
            throw new RuntimeException("Failed to cleanup video processing data", e);
        }

        logger.info("🎉 Video processing cleanup completed for: {}", videoFileId);
    }
}
