package com.example.silentvoice_bd.learning.dto;

import java.time.LocalDateTime;

public class FeedbackResponse {

    private Double confidenceScore;
    private String feedbackText;
    private Boolean isCorrect;
    private LocalDateTime timestamp;
    private String improvementTips;
    private String sessionId;

    // Default constructor
    public FeedbackResponse() {
    }

    // Constructor with all fields
    public FeedbackResponse(Double confidenceScore, String feedbackText, Boolean isCorrect,
            LocalDateTime timestamp, String improvementTips, String sessionId) {
        this.confidenceScore = confidenceScore;
        this.feedbackText = feedbackText;
        this.isCorrect = isCorrect;
        this.timestamp = timestamp;
        this.improvementTips = improvementTips;
        this.sessionId = sessionId;
    }

    // Getters
    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getImprovementTips() {
        return improvementTips;
    }

    public String getSessionId() {
        return sessionId;
    }

    // Setters
    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setImprovementTips(String improvementTips) {
        this.improvementTips = improvementTips;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
