package com.example.silentvoice_bd.ai.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "sign_language_predictions")
public class SignLanguagePrediction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "video_file_id", nullable = false)
    private UUID videoFileId;
    
    @Column(name = "predicted_text", nullable = false, columnDefinition = "TEXT")
    private String predictedText;
    
    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceScore;
    
    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;
    
    @Column(name = "model_version", length = 50)
    private String modelVersion = "bangla_lstm_v1";
    
    @Column(name = "prediction_metadata", columnDefinition = "TEXT")
    private String predictionMetadata;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Constructors
    public SignLanguagePrediction() {}
    
    public SignLanguagePrediction(UUID videoFileId, String predictedText, BigDecimal confidenceScore) {
        this.videoFileId = videoFileId;
        this.predictedText = predictedText;
        this.confidenceScore = confidenceScore;
    }
    
    // Update timestamp before persist/update
    @PrePersist
    @PreUpdate
    public void updateTimestamps() {
        this.updatedAt = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getVideoFileId() { return videoFileId; }
    public void setVideoFileId(UUID videoFileId) { this.videoFileId = videoFileId; }
    
    public String getPredictedText() { return predictedText; }
    public void setPredictedText(String predictedText) { this.predictedText = predictedText; }
    
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    
    public Integer getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Integer processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    
    public String getPredictionMetadata() { return predictionMetadata; }
    public void setPredictionMetadata(String predictionMetadata) { this.predictionMetadata = predictionMetadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
