package com.example.silentvoice_bd.learning.dto;

import java.util.List;
import java.util.Map;

public class FeedbackRequest {

    private Long lessonId;
    private String expectedSign;
    private List<Map<String, Object>> poseLandmarks;
    private String sessionId;
    private Long timestamp;

    // Default constructor
    public FeedbackRequest() {
    }

    // Constructor with all fields
    public FeedbackRequest(Long lessonId, String expectedSign, List<Map<String, Object>> poseLandmarks,
            String sessionId, Long timestamp) {
        this.lessonId = lessonId;
        this.expectedSign = expectedSign;
        this.poseLandmarks = poseLandmarks;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
    }

    // Getters
    public Long getLessonId() {
        return lessonId;
    }

    public String getExpectedSign() {
        return expectedSign;
    }

    public List<Map<String, Object>> getPoseLandmarks() {
        return poseLandmarks;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public void setExpectedSign(String expectedSign) {
        this.expectedSign = expectedSign;
    }

    public void setPoseLandmarks(List<Map<String, Object>> poseLandmarks) {
        this.poseLandmarks = poseLandmarks;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
