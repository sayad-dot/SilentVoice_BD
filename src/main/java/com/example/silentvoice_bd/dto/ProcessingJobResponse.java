package com.example.silentvoice_bd.dto;

import com.example.silentvoice_bd.model.VideoProcessingJob;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessingJobResponse {
    private UUID id;
    private UUID videoFileId;
    private String jobType;
    private String status;
    private Integer progress;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private String resultData;
    private LocalDateTime createdAt;

    public ProcessingJobResponse(VideoProcessingJob job) {
        this.id = job.getId();
        this.videoFileId = job.getVideoFileId();
        this.jobType = job.getJobType().name();
        this.status = job.getStatus().name();
        this.progress = job.getProgress();
        this.startedAt = job.getStartedAt();
        this.completedAt = job.getCompletedAt();
        this.errorMessage = job.getErrorMessage();
        this.resultData = job.getResultData();
        this.createdAt = job.getCreatedAt();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getVideoFileId() { return videoFileId; }
    public void setVideoFileId(UUID videoFileId) { this.videoFileId = videoFileId; }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getResultData() { return resultData; }
    public void setResultData(String resultData) { this.resultData = resultData; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
