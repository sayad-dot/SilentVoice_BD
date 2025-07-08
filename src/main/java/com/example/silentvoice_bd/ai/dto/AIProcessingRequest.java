package com.example.silentvoice_bd.ai.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class AIProcessingRequest {

    @NotNull(message = "Video ID is required")
    private UUID videoId;

    private List<UUID> frameIds;

    private boolean enableAsync = true;

    @DecimalMin(value = "0.0", message = "Confidence threshold must be >= 0")
    @DecimalMax(value = "1.0", message = "Confidence threshold must be <= 1")
    private Double confidenceThreshold = 0.7;

    private boolean saveToDatabase = true;

    private String modelVersion = "bangla_lstm_v1";

    // Constructors
    public AIProcessingRequest() {
    }

    public AIProcessingRequest(UUID videoId) {
        this.videoId = videoId;
    }

    public AIProcessingRequest(UUID videoId, boolean enableAsync, Double confidenceThreshold) {
        this.videoId = videoId;
        this.enableAsync = enableAsync;
        this.confidenceThreshold = confidenceThreshold;
    }

    // Getters and setters
    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public List<UUID> getFrameIds() {
        return frameIds;
    }

    public void setFrameIds(List<UUID> frameIds) {
        this.frameIds = frameIds;
    }

    public boolean isEnableAsync() {
        return enableAsync;
    }

    public void setEnableAsync(boolean enableAsync) {
        this.enableAsync = enableAsync;
    }

    public Double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public void setConfidenceThreshold(Double confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public boolean isSaveToDatabase() {
        return saveToDatabase;
    }

    public void setSaveToDatabase(boolean saveToDatabase) {
        this.saveToDatabase = saveToDatabase;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }
}
