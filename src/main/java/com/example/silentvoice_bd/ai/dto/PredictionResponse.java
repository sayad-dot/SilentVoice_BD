package com.example.silentvoice_bd.ai.dto;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionResponse {

    private boolean success = true;

    @JsonProperty("predicted_text")
    private String predictedText;

    private double confidence;

    @JsonProperty("processing_time_ms")
    private Integer processingTimeMs;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("prediction_id")
    private UUID predictionId;

    @JsonProperty("video_id")
    private UUID videoId;

    private String error;

    @JsonProperty("processing_info")
    private Map<String, Object> processingInfo;

    // Additional fields for debugging normalization issues
    @JsonProperty("normalization_applied")
    private Boolean normalizationApplied;

    @JsonProperty("data_statistics")
    private Map<String, Object> dataStatistics;

    // Constructors
    public PredictionResponse() {
    }

    public PredictionResponse(String predictedText, double confidence, Integer processingTimeMs, UUID predictionId) {
        this.predictedText = predictedText;
        this.confidence = confidence;
        this.processingTimeMs = processingTimeMs;
        this.predictionId = predictionId;
    }

    // Static factory methods
    public static PredictionResponse error(String errorMessage) {
        PredictionResponse response = new PredictionResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        response.setPredictedText("ত্রুটি"); // "Error" in Bangla
        response.setConfidence(0.0);
        return response;
    }

    public static PredictionResponse success(String predictedText, double confidence,
            Integer processingTimeMs, UUID predictionId) {
        return new PredictionResponse(predictedText, confidence, processingTimeMs, predictionId);
    }

    // Helper methods for debugging
    public boolean isHighConfidence() {
        return confidence > 0.7;
    }

    public boolean isLowConfidence() {
        return confidence < 0.1;
    }

    public String getConfidenceLevel() {
        if (confidence > 0.8) return "HIGH";
        if (confidence > 0.5) return "MEDIUM";
        if (confidence > 0.2) return "LOW";
        return "VERY_LOW";
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getPredictedText() {
        return predictedText;
    }

    public void setPredictedText(String predictedText) {
        this.predictedText = predictedText;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
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

    public UUID getPredictionId() {
        return predictionId;
    }

    public void setPredictionId(UUID predictionId) {
        this.predictionId = predictionId;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Map<String, Object> getProcessingInfo() {
        return processingInfo;
    }

    public void setProcessingInfo(Map<String, Object> processingInfo) {
        this.processingInfo = processingInfo;
    }

    public Boolean getNormalizationApplied() {
        return normalizationApplied;
    }

    public void setNormalizationApplied(Boolean normalizationApplied) {
        this.normalizationApplied = normalizationApplied;
    }

    public Map<String, Object> getDataStatistics() {
        return dataStatistics;
    }

    public void setDataStatistics(Map<String, Object> dataStatistics) {
        this.dataStatistics = dataStatistics;
    }

    @Override
    public String toString() {
        return "PredictionResponse{" +
                "success=" + success +
                ", predictedText='" + predictedText + '\'' +
                ", confidence=" + confidence +
                ", processingTimeMs=" + processingTimeMs +
                ", modelVersion='" + modelVersion + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
