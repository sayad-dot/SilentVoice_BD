package com.example.silentvoice_bd.ai.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionWithAudioResponse {

    private UUID id;
    private UUID videoFileId;

    @JsonProperty("predicted_text")
    private String predictedText;

    @JsonProperty("confidence_score")
    private Double confidenceScore;

    @JsonProperty("processing_time_ms")
    private Integer processingTimeMs;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("audio_url")
    private String audioUrl;

    private boolean hasAudio;

    @JsonProperty("english_translation")
    private String englishTranslation;

    // Constructors
    public PredictionWithAudioResponse() {
    }

    public PredictionWithAudioResponse(UUID id, UUID videoFileId, String predictedText,
            Double confidenceScore, Integer processingTimeMs,
            String modelVersion, LocalDateTime createdAt) {
        this.id = id;
        this.videoFileId = videoFileId;
        this.predictedText = predictedText;
        this.confidenceScore = confidenceScore;
        this.processingTimeMs = processingTimeMs;
        this.modelVersion = modelVersion;
        this.createdAt = createdAt;
        this.englishTranslation = getTranslation(predictedText);
    }

    private String getTranslation(String banglaText) {
        // Common Bangla sign language translations
        switch (banglaText) {
            case "দাদা":
                return "Grandfather / Elder Brother";
            case "দাদি":
                return "Grandmother";
            case "মা":
                return "Mother";
            case "বাবা":
                return "Father";
            case "ভাই":
                return "Brother";
            case "বোন":
                return "Sister";
            case "আম":
                return "Mango";
            case "আপেল":
                return "Apple";
            case "চা":
                return "Tea";
            case "পানি":
                return "Water";
            case "খাবার":
                return "Food";
            case "ভালো":
                return "Good";
            case "খারাপ":
                return "Bad";
            case "হ্যালো":
                return "Hello";
            case "ধন্যবাদ":
                return "Thank you";
            case "দুঃখিত":
                return "Sorry";
            default:
                return "Translation not available";
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
        this.englishTranslation = getTranslation(predictedText);
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public boolean isHasAudio() {
        return hasAudio;
    }

    public void setHasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
    }

    public String getEnglishTranslation() {
        return englishTranslation;
    }

    public void setEnglishTranslation(String englishTranslation) {
        this.englishTranslation = englishTranslation;
    }
}
