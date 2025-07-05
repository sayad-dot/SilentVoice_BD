package com.example.silentvoice_bd.ai.dto;

import java.util.List;
import java.util.UUID;

public class AIProcessingRequest {
    
    private UUID videoId;
    private List<UUID> frameIds;
    private boolean enableAsync = true;
    private Double confidenceThreshold = 0.7;
    
    // Constructors
    public AIProcessingRequest() {}
    
    public AIProcessingRequest(UUID videoId) {
        this.videoId = videoId;
    }
    
    // Getters and setters
    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }
    
    public List<UUID> getFrameIds() { return frameIds; }
    public void setFrameIds(List<UUID> frameIds) { this.frameIds = frameIds; }
    
    public boolean isEnableAsync() { return enableAsync; }
    public void setEnableAsync(boolean enableAsync) { this.enableAsync = enableAsync; }
    
    public Double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(Double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }
}
