package com.example.silentvoice_bd.ai.dto;

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
    
    private String error;
    
    // Constructors
    public PredictionResponse() {}
    
    public PredictionResponse(String predictedText, double confidence, Integer processingTimeMs, UUID predictionId) {
        this.predictedText = predictedText;
        this.confidence = confidence;
        this.processingTimeMs = processingTimeMs;
        this.predictionId = predictionId;
    }
    
    public static PredictionResponse error(String errorMessage) {
        PredictionResponse response = new PredictionResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        response.setPredictedText("[ত্রুটি]"); // "Error" in Bangla
        response.setConfidence(0.0);
        return response;
    }
    
    public static PredictionResponse success(String predictedText, double confidence, Integer processingTimeMs, UUID predictionId) {
        return new PredictionResponse(predictedText, confidence, processingTimeMs, predictionId);
    }
    
    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getPredictedText() { return predictedText; }
    public void setPredictedText(String predictedText) { this.predictedText = predictedText; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public Integer getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Integer processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    
    public UUID getPredictionId() { return predictionId; }
    public void setPredictionId(UUID predictionId) { this.predictionId = predictionId; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
