package com.example.silentvoice_bd.ai.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "sign_language_predictions")
public class SignLanguagePrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "video_file_id", nullable = false)
    @NotNull(message = "Video file ID is required")
    private UUID videoFileId;

    @Column(name = "predicted_text", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Predicted text cannot be blank")
    @Size(max = 1000, message = "Predicted text too long")
    private String predictedText;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    @NotNull(message = "Confidence score is required")
    @DecimalMin(value = "0.0", message = "Confidence must be >= 0")
    @DecimalMax(value = "1.0", message = "Confidence must be <= 1")
    private BigDecimal confidenceScore;

    @Column(name = "processing_time_ms")
    @Min(value = 0, message = "Processing time must be positive")
    private Integer processingTimeMs;

    @Column(name = "model_version", length = 50)
    @Size(max = 50, message = "Model version too long")
    private String modelVersion = "bangla_lstm_v1";

    @Column(name = "prediction_metadata", columnDefinition = "JSONB")
    private String predictionMetadata;

    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Constructors
    public SignLanguagePrediction() {
    }

    public SignLanguagePrediction(UUID videoFileId, String predictedText, BigDecimal confidenceScore) {
        this.videoFileId = videoFileId;
        this.predictedText = predictedText;
        this.confidenceScore = confidenceScore;
    }

    // Lifecycle callbacks
    @PrePersist
    @PreUpdate
    public void updateTimestamps() {
        this.updatedAt = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVideoFileId() {
        return videoFileId;
    }

    public void setVideoFileId(UUID videoFileId) {
        this.videoFileId = videoFileId;
    }

    public String getPredictedText() {
        return predictedText;
    }

    public void setPredictedText(String predictedText) {
        this.predictedText = predictedText;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public String getPredictionMetadata() {
        return predictionMetadata;
    }

    public void setPredictionMetadata(String predictionMetadata) {
        this.predictionMetadata = predictionMetadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SignLanguagePrediction{"
                + "id=" + id
                + ", videoFileId=" + videoFileId
                + ", predictedText='" + predictedText + '\''
                + ", confidence=" + confidenceScore
                + ", modelVersion='" + modelVersion + '\''
                + '}';
    }
}
